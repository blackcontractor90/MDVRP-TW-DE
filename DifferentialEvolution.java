import javafx.application.Platform;
import javafx.scene.chart.XYChart;
import java.util.*;

/**
 * ${user}blackcontractor@farid
 */
public class DifferentialEvolution {
    private final SolverContext solver;
    private final int populationSize;
    private final int maxGenerations;
    private double F;
    private double CR;
    private final Random rand = new Random();

    private double initialPenaltyWeight;
    private double currentPenaltyWeight;
    private final double penaltyIncreaseFactor = 1.05;
    private final double penaltyDecreaseFactor = 0.8;
    private final double minPenaltyWeight = 1000.0;
    private final double maxPenaltyWeight = 100_000.0;
    private final double targetFeasibilityRatio = 0.5;

    private final double LNS_REMOVAL_FRACTION = 0.4;
    private final int LNS_MAX_REMOVALS = 25;
    private int lnsImprovements = 0;
    private int lnsAttempts = 0;

    private final int TABU_TENURE = 15;
    private final Set<String> tabuList = new LinkedHashSet<>();

    private final boolean enableLNS;
    private final boolean enableTabu;
    private final boolean enableAdaptivePenalty;

    private XYChart.Series<Number, Number> convergenceSeries;

    // Adaptive parameter memories
    private final List<Double> successfulFs = new ArrayList<>();
    private final List<Double> successfulCRs = new ArrayList<>();

    /**
     * Full constructor. Use this to run a specific ablation variant
     * (e.g. plain DE with all three flags false, for a true baseline).
     */
    public DifferentialEvolution(SolverContext solver, int populationSize,
                                 double F, double CR, int maxGenerations,
                                 boolean enableLNS, boolean enableTabu, boolean enableAdaptivePenalty) {
        this.solver = solver;
        this.populationSize = populationSize;
        this.F = F;
        this.CR = CR;
        this.maxGenerations = maxGenerations;
        this.enableLNS = enableLNS;
        this.enableTabu = enableTabu;
        this.enableAdaptivePenalty = enableAdaptivePenalty;
    }

    /**
     * Backward-compatible constructor - defaults to the full HADE-LNS-Tabu variant
     * (all mechanisms enabled), matching the previous hardcoded behavior.
     */
    public DifferentialEvolution(SolverContext solver, int populationSize,
                                 double F, double CR, int maxGenerations) {
        this(solver, populationSize, F, CR, maxGenerations, true, true, true);
    }

    public void setConvergenceSeries(XYChart.Series<Number, Number> series) {
        this.convergenceSeries = series;
    }

    public Solution run() {
        initialPenaltyWeight = solver.getBasePenaltyWeight();
        solver.setPenaltyWeight(initialPenaltyWeight);
        currentPenaltyWeight = initialPenaltyWeight;
        log("Running variant: " + algorithmLabel());

        List<Solution> population = new ArrayList<>();
        for (int i = 0; i < populationSize; i++) {
            Solution sol = new Solution(generateRandomPermutation());
            evaluateSolution(sol);
            population.add(sol);
        }

        Solution bestSolution = findBestSolution(population);
        solver.setBestFitness(bestSolution.fitness);

        for (int gen = 0; gen < maxGenerations; gen++) {
            if (enableAdaptivePenalty && gen % 10 == 0) {
                adjustPenaltyWeight(population);
            }

            List<Solution> newPopulation = new ArrayList<>();
            successfulFs.clear();
            successfulCRs.clear();

            for (int i = 0; i < populationSize; i++) {
                double localF = adaptParameter(F, 0.1, 0.9);
                double localCR = adaptParameter(CR, 0.0, 1.0);

                Solution target = population.get(i);
                Solution mutant = mutate(population, i, localF);
                int[] trialChrom = orderCrossover(target.chromosome, mutant.chromosome, localCR);
                Solution trial = new Solution(trialChrom);
                evaluateSolution(trial);

                if (enableLNS) {
                    lnsLocalSearch(trial);
                    evaluateSolution(trial);
                }

                if (shouldAccept(trial, target)) {
                    newPopulation.add(trial);
                    successfulFs.add(localF);
                    successfulCRs.add(localCR);
                    if (trial.fitness < bestSolution.fitness && (!enableTabu || !isTabu(trial))) {
                        bestSolution = new Solution(trial);
                        solver.setBestFitness(bestSolution.fitness);
                        if (enableTabu) addToTabuList(trial);
                    }
                } else {
                    newPopulation.add(target);
                }
            }

            updateAdaptiveParameters();
            population = newPopulation;
            updateConvergenceChart(gen, solver.getBestFitness());

            if (enableLNS && gen % 30 == 0) {
                Solution improved = new Solution(bestSolution);
                lnsLocalSearch(improved);
                evaluateSolution(improved);
                if (improved.fitness < bestSolution.fitness && (!enableTabu || !isTabu(improved))) {
                    log("[LNS-BEST] Improved best fitness: " +
                            String.format("%.2f → %.2f", bestSolution.fitness, improved.fitness));
                    bestSolution = improved;
                    if (enableTabu) addToTabuList(improved);
                }
            }

            if (gen % 5 == 0) {
                try {
                    final int genCopy = gen;
                    final Solution clone = new Solution(bestSolution);
                    Platform.runLater(() -> solver.animateSolution(clone, genCopy));
                } catch (IllegalStateException e) {
                    // JavaFX toolkit not initialized (headless mode) - skip animation
                }
            }

            if (gen % 20 == 0) {
                log("Gen " + gen + ": Best = " + String.format("%.2f", solver.getBestFitness())
                        + " | PenaltyWeight = " + String.format("%.2f", currentPenaltyWeight)
                        + " | F = " + String.format("%.3f", F)
                        + " | CR = " + String.format("%.3f", CR));
            }
        }

        log(String.format("[LNS] Improvements: %d/%d (%.2f%%)",
                lnsImprovements, lnsAttempts,
                (100.0 * lnsImprovements / Math.max(1, lnsAttempts))));

        SolutionMetrics.saveRunToCSV(bestSolution, algorithmLabel(),
                populationSize, maxGenerations, F, CR, solver.getCurrentDatasetName());

        return bestSolution;
    }

    /**
     * Builds a label reflecting which mechanisms actually ran, e.g. "DE",
     * "DE+LNS", "DE+Tabu+Adaptive", "DE+LNS+Tabu+Adaptive". Previously the label
     * "DE+LNS+Tabu+Adaptive" was hardcoded here regardless of which mechanisms
     * were actually active, which combined with MDVRPTWSolver separately saving
     * the identical result as "DifferentialEvolution" meant Table 1's paired rows
     * were the same run saved twice under two names, not two independently
     * executed algorithms.
     */
    private String algorithmLabel() {
        StringBuilder sb = new StringBuilder("DE");
        if (enableLNS) sb.append("+LNS");
        if (enableTabu) sb.append("+Tabu");
        if (enableAdaptivePenalty) sb.append("+Adaptive");
        return sb.toString();
    }

    private void addToTabuList(Solution sol) {
        tabuList.add(Arrays.toString(sol.chromosome));
        if (tabuList.size() > TABU_TENURE) {
            Iterator<String> it = tabuList.iterator();
            it.next();
            it.remove();
        }
    }

    private boolean isTabu(Solution sol) {
        return tabuList.contains(Arrays.toString(sol.chromosome));
    }

    private void adjustPenaltyWeight(List<Solution> population) {
        int feasibleCount = 0;
        for (Solution sol : population)
            if (sol.timeWindowViolations == 0) feasibleCount++;

        double ratio = (double) feasibleCount / population.size();
        if (ratio < targetFeasibilityRatio)
            currentPenaltyWeight = Math.min(currentPenaltyWeight * penaltyIncreaseFactor, maxPenaltyWeight);
        else if (ratio > targetFeasibilityRatio)
            currentPenaltyWeight = Math.max(currentPenaltyWeight * penaltyDecreaseFactor, minPenaltyWeight);

        solver.setPenaltyWeight(currentPenaltyWeight);
    }

    private double adaptParameter(double base, double min, double max) {
        double noise = rand.nextGaussian() * 0.1;
        double adapted = base + noise;
        return Math.min(max, Math.max(min, adapted));
    }

    private void updateAdaptiveParameters() {
        if (!successfulFs.isEmpty()) {
            F = successfulFs.stream().mapToDouble(d -> d).average().orElse(F);
        }
        if (!successfulCRs.isEmpty()) {
            CR = successfulCRs.stream().mapToDouble(d -> d).average().orElse(CR);
        }
    }

    private Solution mutate(List<Solution> population, int currentIndex, double localF) {
        int a, b, c;
        do { a = rand.nextInt(populationSize); } while (a == currentIndex);
        do { b = rand.nextInt(populationSize); } while (b == currentIndex || b == a);
        do { c = rand.nextInt(populationSize); } while (c == currentIndex || c == a || c == b);

        Solution base = population.get(a);
        Solution diff1 = population.get(b);
        Solution diff2 = population.get(c);

        int[] mutant = Arrays.copyOf(base.chromosome, base.chromosome.length);
        int swaps = Math.max(1, (int)(localF * mutant.length));
        for (int i = 0; i < swaps; i++) {
            int idx1 = rand.nextInt(mutant.length);
            int customerId = diff1.chromosome[idx1];
            int idx2 = findPosition(diff2.chromosome, customerId);
            if (idx2 != -1 && idx1 != idx2) swap(mutant, idx1, idx2);
        }
        return new Solution(mutant);
    }

    private int[] orderCrossover(int[] p1, int[] p2, double localCR) {
        if (rand.nextDouble() > localCR) return Arrays.copyOf(p1, p1.length);

        int len = p1.length;
        int[] child = new int[len];
        Arrays.fill(child, -1);
        int start = rand.nextInt(len);
        int end = start + rand.nextInt(len - start);
        for (int i = start; i <= end; i++) child[i] = p1[i];
        int cur = (end + 1) % len;
        for (int i = 0; i < len; i++) {
            int idx = (end + 1 + i) % len;
            if (!contains(child, p2[idx])) {
                child[cur] = p2[idx];
                cur = (cur + 1) % len;
            }
        }
        return child;
    }

    private void lnsLocalSearch(Solution sol) {
        double before = sol.fitness;
        lnsAttempts++;

        int n = sol.chromosome.length;
        int numRemove = Math.min(LNS_MAX_REMOVALS, Math.max(3, (int)(LNS_REMOVAL_FRACTION * n)));

        Set<Integer> toRemove = new HashSet<>();
        while (toRemove.size() < numRemove)
            toRemove.add(rand.nextInt(n));

        List<Route> currentRoutes = solver.decodeSolution(sol);
        Map<Depot, Double> depotDistance = new HashMap<>();
        Map<Depot, Integer> depotViolations = new HashMap<>();
        for (Route r : currentRoutes) {
            depotDistance.merge(r.depot, r.distance, Double::sum);
            depotViolations.merge(r.depot, r.timeWindowViolations, Integer::sum);
        }

        List<Integer> removed = new ArrayList<>();
        Map<Depot, List<Integer>> remainingByDepot = new HashMap<>();
        for (int gene : sol.chromosome) {
            if (toRemove.contains(gene)) {
                removed.add(gene);
            } else {
                Customer c = solver.getCustomers().get(gene);
                Depot d = solver.getDepots().get(c.assignedDepotId - 1);
                remainingByDepot.computeIfAbsent(d, k -> new ArrayList<>()).add(gene);
            }
        }

        while (!removed.isEmpty()) {
            int cust = removed.remove(rand.nextInt(removed.size()));
            Customer custObj = solver.getCustomers().get(cust);
            Depot depot = solver.getDepots().get(custObj.assignedDepotId - 1);
            List<Integer> depotList = remainingByDepot.computeIfAbsent(depot, k -> new ArrayList<>());

            double otherDepotsDistance = 0;
            int otherDepotsViolations = 0;
            for (Map.Entry<Depot, Double> e : depotDistance.entrySet())
                if (e.getKey() != depot) otherDepotsDistance += e.getValue();
            for (Map.Entry<Depot, Integer> e : depotViolations.entrySet())
                if (e.getKey() != depot) otherDepotsViolations += e.getValue();

            int bestPos = 0;
            double bestFitness = Double.POSITIVE_INFINITY;
            double bestDepotDistance = 0;
            int bestDepotViolations = 0;

            for (int i = 0; i <= depotList.size(); i++) {
                List<Integer> tempGenes = new ArrayList<>(depotList);
                tempGenes.add(i, cust);
                List<Customer> tempCustomers = new ArrayList<>(tempGenes.size());
                for (int g : tempGenes) tempCustomers.add(solver.getCustomers().get(g));

                double[] result = solver.evaluateDepotOnly(depot, tempCustomers);
                double candidateFitness = (otherDepotsDistance + result[0])
                        + currentPenaltyWeight * (otherDepotsViolations + result[1]);

                if (candidateFitness < bestFitness) {
                    bestFitness = candidateFitness;
                    bestPos = i;
                    bestDepotDistance = result[0];
                    bestDepotViolations = (int) result[1];
                }
            }

            depotList.add(bestPos, cust);
            depotDistance.put(depot, bestDepotDistance);
            depotViolations.put(depot, bestDepotViolations);
        }

        int[] newChromosome = new int[n];
        int idx = 0;
        for (List<Integer> list : remainingByDepot.values())
            for (int gene : list)
                newChromosome[idx++] = gene;

        sol.chromosome = newChromosome;
        evaluateSolution(sol); // one authoritative full decode for final fitness

        if (sol.fitness < before) {
            lnsImprovements++;
            log(String.format("[LNS] Improved fitness: %.2f → %.2f", before, sol.fitness));
        }
    }

    private boolean contains(int[] arr, int val) {
        for (int x : arr) if (x == val) return true;
        return false;
    }

    private void swap(int[] arr, int i, int j) {
        int temp = arr[i];
        arr[i] = arr[j];
        arr[j] = temp;
    }

    private int findPosition(int[] arr, int val) {
        for (int i = 0; i < arr.length; i++) if (arr[i] == val) return i;
        return -1;
    }

    private int[] generateRandomPermutation() {
        List<Integer> perm = new ArrayList<>();
        for (int i = 0; i < solver.getCustomers().size(); i++) perm.add(i);
        Collections.shuffle(perm, rand);
        return perm.stream().mapToInt(i -> i).toArray();
    }

    private Solution findBestSolution(List<Solution> population) {
        Solution best = population.get(0);
        for (Solution sol : population)
            if (sol.fitness < best.fitness) best = sol;
        return best;
    }

    private void updateConvergenceChart(int gen, double fitness) {
        if (convergenceSeries != null) {
            try {
                Platform.runLater(() -> convergenceSeries.getData().add(new XYChart.Data<>(gen, fitness)));
            } catch (IllegalStateException e) {
                // JavaFX toolkit not initialized (headless mode) - skip
            }
        }
    }

    public void evaluateSolution(Solution sol) {
        solver.evaluateSolution(sol);
    }

    private boolean shouldAccept(Solution trial, Solution target) {
        if (trial.timeWindowViolations == 0 && target.timeWindowViolations > 0) return true;
        if (target.timeWindowViolations == 0 && trial.timeWindowViolations > 0) return false;
        return trial.fitness < target.fitness;
    }

    private void log(String msg) {
        solver.log(msg);
    }
}
