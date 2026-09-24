import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Headless batch harness for 30-repeat MDVRPTW experiments.
 * Runs ablation variants (DE, DE+LNS, DE+Tabu, DE+LNS+Tabu, DE+LNS+Tabu+Adaptive)
 * with reproducible seeding (baseRunSeed + runNumber).
 *
 * Usage:
 *   java HeadlessHarness <instance_file> <num_runs> <output_dir> <base_seed>
 *
 * Example:
 *   java HeadlessHarness pr11a.txt 30 ./results 12345
 *
 * Output:
 *   - <output_dir>/harness_runs_<timestamp>.csv      (all individual runs)
 *   - <output_dir>/harness_summary_<timestamp>.csv   (means/SDs per config)
 *   - <output_dir>/harness_log_<timestamp>.txt       (console log)
 *
 * ${user}blackcontractor@farid
 */
public class HeadlessHarness {
    private static final int POPULATION_SIZE = 50;
    private static final int MAX_GENERATIONS = 200;
    private static final double F = 0.8;
    private static final double CR = 0.9;
    private static final double PENALTY_WEIGHT = 1000.0;

    private final String instanceFile;
    private final int numRuns;
    private final String outputDir;
    private final long baseSeed;
    private PrintWriter logWriter;

    private List<Depot> depots = new ArrayList<>();
    private List<Customer> customers = new ArrayList<>();
    private String currentDatasetName;

    // Bug fix: this used to be a hardcoded VEHICLE_CAPACITY = 200 constant,
    // passed into every HeadlessSolverContext regardless of which instance
    // was loaded. That happened to be correct for pr11a/pr11b (capacity 200)
    // but silently wrong for pr12a/pr12b (real capacity is 195) - those runs
    // let routes carry 5 more units of demand than the instance actually
    // permits. Now set from the instance file itself once it's loaded.
    private int vehicleCapacity;

    // Store results for summary stats
    private Map<String, List<RunResult>> resultsByConfig = new LinkedHashMap<>();

    private static class RunResult {
        int runNumber;
        long seed;
        double fitness;
        double distance;
        double penalty;
        int violations;
        boolean feasible;
        long wallClockMs;

        RunResult(int runNumber, long seed, double fitness, double distance,
                  double penalty, int violations, boolean feasible, long wallClockMs) {
            this.runNumber = runNumber;
            this.seed = seed;
            this.fitness = fitness;
            this.distance = distance;
            this.penalty = penalty;
            this.violations = violations;
            this.feasible = feasible;
            this.wallClockMs = wallClockMs;
        }
    }

    public HeadlessHarness(String instanceFile, int numRuns, String outputDir, long baseSeed) {
        this.instanceFile = instanceFile;
        this.numRuns = numRuns;
        this.outputDir = outputDir;
        this.baseSeed = baseSeed;

        // Initialize result storage for all 5 configs
        resultsByConfig.put("DE", new ArrayList<>());
        resultsByConfig.put("DE+LNS", new ArrayList<>());
        resultsByConfig.put("DE+Tabu", new ArrayList<>());
        resultsByConfig.put("DE+LNS+Tabu", new ArrayList<>());
        resultsByConfig.put("DE+LNS+Tabu+Adaptive", new ArrayList<>());
    }

    public static void main(String[] args) {
        if (args.length < 4) {
            System.err.println("Usage: java HeadlessHarness <instance_file> <num_runs> <output_dir> <base_seed>");
            System.err.println("Example: java HeadlessHarness pr11a.txt 30 ./results 12345");
            System.exit(1);
        }

        String instanceFile = args[0];
        int numRuns = Integer.parseInt(args[1]);
        String outputDir = args[2];
        long baseSeed = Long.parseLong(args[3]);

        HeadlessHarness harness = new HeadlessHarness(instanceFile, numRuns, outputDir, baseSeed);
        harness.run();
    }

    public void run() {
        // Create output directory first
        try {
            Files.createDirectories(Paths.get(outputDir));
        } catch (IOException e) {
            System.err.println("Failed to create output directory: " + e.getMessage());
            System.exit(1);
        }

        // Initialize log file
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String logFilename = outputDir + "/harness_log_" + timestamp + ".txt";
        try {
            logWriter = new PrintWriter(new FileWriter(logFilename));
        } catch (IOException e) {
            System.err.println("Failed to create log file: " + e.getMessage());
            System.exit(1);
        }

        log("=== MDVRPTW Headless Batch Harness ===");
        log("Instance: " + instanceFile);
        log("Runs per config: " + numRuns);
        log("Output directory: " + outputDir);
        log("Base seed: " + baseSeed);
        log("Log file: " + logFilename);
        log("");

        // Load instance
        try {
            loadInstance();
            log("Loaded instance: " + currentDatasetName);
            log("  Depots: " + depots.size() + ", Customers: " + customers.size());
            log("  Vehicle capacity: " + vehicleCapacity);
            log("");
        } catch (IOException e) {
            log("Failed to load instance: " + e.getMessage());
            logWriter.close();
            System.exit(1);
        }

        // Run all 5 configurations
        String[] configs = {"DE", "DE+LNS", "DE+Tabu", "DE+LNS+Tabu", "DE+LNS+Tabu+Adaptive"};
        int totalRuns = configs.length * numRuns;
        int completedRuns = 0;

        for (String config : configs) {
            boolean enableLNS = config.contains("LNS");
            boolean enableTabu = config.contains("Tabu");
            boolean enableAdaptive = config.contains("Adaptive");

            log("Running " + numRuns + " iterations of " + config + "...");
            long configStartTime = System.currentTimeMillis();

            for (int run = 0; run < numRuns; run++) {
                long seed = baseSeed + run;
                String runMsg = "  Run " + (run + 1) + "/" + numRuns + " (seed=" + seed + ")... ";
                System.out.print(runMsg);
                log(runMsg);

                long runStartTime = System.currentTimeMillis();

                try {
                    Solution bestSolution = runSingleConfiguration(
                            enableLNS, enableTabu, enableAdaptive, seed
                    );

                    long wallClockMs = System.currentTimeMillis() - runStartTime;
                    RunResult result = new RunResult(
                            run,
                            seed,
                            bestSolution.fitness,
                            bestSolution.totalDistance,
                            bestSolution.totalPenalty,
                            bestSolution.timeWindowViolations,
                            bestSolution.isFeasible(),
                            wallClockMs
                    );

                    resultsByConfig.get(config).add(result);
                    completedRuns++;

                    String resultMsg = String.format("fitness=%.2f, dist=%.2f, viol=%d, time=%dms",
                            result.fitness, result.distance, result.violations, wallClockMs);
                    System.out.println(resultMsg);
                    log(resultMsg);

                } catch (Exception e) {
                    String errorMsg = "FAILED: " + e.getMessage();
                    System.err.println(errorMsg);
                    log(errorMsg);
                    e.printStackTrace();
                }

                String progressMsg = "  Progress: " + completedRuns + "/" + totalRuns + " runs";
                log(progressMsg);
            }

            long configTime = System.currentTimeMillis() - configStartTime;
            String configTimeMsg = "  Completed " + config + " in " + (configTime / 1000.0) + " seconds\n";
            System.out.println(configTimeMsg);
            log(configTimeMsg);
        }

        // Write results to CSV
        String runsFilename = outputDir + "/harness_runs_" + timestamp + ".csv";
        String summaryFilename = outputDir + "/harness_summary_" + timestamp + ".csv";

        writeRunsCSV(runsFilename);
        writeSummaryCSV(summaryFilename);

        log("=== Batch Complete ===");
        log("Runs CSV: " + runsFilename);
        log("Summary CSV: " + summaryFilename);
        log("Log file: " + logFilename);
        log("");
        printSummaryStats();

        // Close log file
        logWriter.close();

        System.out.println("=== Batch Complete ===");
        System.out.println("Runs CSV: " + runsFilename);
        System.out.println("Summary CSV: " + summaryFilename);
        System.out.println("Log file: " + logFilename);
    }

    /**
     * Log a message to both console and log file
     */
    private void log(String message) {
        System.out.println(message);
        if (logWriter != null) {
            logWriter.println(message);
            logWriter.flush();
        }
    }

    private Solution runSingleConfiguration(boolean enableLNS, boolean enableTabu,
                                           boolean enableAdaptive, long seed) {
        // Create a minimal solver-like object that DifferentialEvolution can work with
        HeadlessSolverContext context = new HeadlessSolverContext(
                depots, customers, vehicleCapacity, PENALTY_WEIGHT, seed
        );

        DifferentialEvolution de = new DifferentialEvolution(
                context,
                POPULATION_SIZE, F, CR, MAX_GENERATIONS,
                enableLNS, enableTabu, enableAdaptive
        );

        return de.run();
    }

    private void loadInstance() throws IOException {
        File file = new File(instanceFile);
        if (!file.exists()) {
            throw new IOException("File not found: " + instanceFile);
        }

        currentDatasetName = file.getName();
        depots.clear();
        customers.clear();

        try (Scanner scanner = new Scanner(file)) {
            if (!scanner.hasNextLine()) {
                throw new IOException("File is empty or missing header line.");
            }

            String headerLine = scanner.nextLine().trim();
            String[] headerTokens = headerLine.split("\\s+");

            if (headerTokens.length < 4) {
                throw new IOException("Header must include: [type vehiclesPerDepot customers depots]");
            }

            int vehiclesPerDepot = Integer.parseInt(headerTokens[1]);
            int customerCount = Integer.parseInt(headerTokens[2]);
            int depotCount = Integer.parseInt(headerTokens[3]);

            double[] depotDuration = new double[depotCount];
            int[] depotCapacity = new int[depotCount];
            for (int i = 0; i < depotCount; i++) {
                if (!scanner.hasNextLine()) {
                    throw new IOException("Missing depot duration/capacity line at line " + (i + 2));
                }
                String line = scanner.nextLine().trim();
                String[] tokens = line.split("\\s+");
                if (tokens.length < 2) {
                    throw new IOException("Malformed depot duration/capacity line: " + line);
                }
                double duration = Double.parseDouble(tokens[0]);
                depotDuration[i] = (duration == 0) ? 9999.0 : duration;
                depotCapacity[i] = Integer.parseInt(tokens[1]);
            }

            // Load customers
            for (int i = 0; i < customerCount; i++) {
                if (!scanner.hasNextLine()) {
                    throw new IOException("Missing customer data at line " + (i + 2 + depotCount));
                }

                String line = scanner.nextLine().trim();
                String[] tokens = line.split("\\s+");

                if (tokens.length < 5) {
                    throw new IOException("Malformed customer line: " + line);
                }

                int id = Integer.parseInt(tokens[0]);
                double x = Double.parseDouble(tokens[1]);
                double y = Double.parseDouble(tokens[2]);
                double serviceTime = Double.parseDouble(tokens[3]);
                int demand = Integer.parseInt(tokens[4]);

                double readyTime = 0;
                double dueTime = 1000;
                if (tokens.length >= 7) {
                    readyTime = Double.parseDouble(tokens[tokens.length - 2]);
                    dueTime = Double.parseDouble(tokens[tokens.length - 1]);
                }

                customers.add(new Customer(x, y, "C" + id, demand, readyTime, dueTime, serviceTime));
            }

            // Load depot coordinates
            for (int i = 0; i < depotCount; i++) {
                if (!scanner.hasNextLine()) {
                    throw new IOException("Missing depot coordinate at line " + (i + 2 + depotCount + customerCount));
                }

                String line = scanner.nextLine().trim();
                if (line.isEmpty()) {
                    i--;
                    continue;
                }
                String[] tokens = line.split("\\s+");

                if (tokens.length < 2) {
                    throw new IOException("Malformed depot line: " + line);
                }

                double x, y;
                if (tokens.length >= 3) {
                    x = Double.parseDouble(tokens[1]);
                    y = Double.parseDouble(tokens[2]);
                } else {
                    x = Double.parseDouble(tokens[0]);
                    y = Double.parseDouble(tokens[1]);
                }

                Depot depot = new Depot(x, y, "D" + (i + 1));
                depot.maxVehicles = vehiclesPerDepot;
                depot.vehicleCapacity = depotCapacity[i];
                depot.maxDuration = depotDuration[i];

                depots.add(depot);
            }

        } catch (NumberFormatException e) {
            throw new IOException("Invalid number format in file: " + e.getMessage(), e);
        }

        // Bug fix: vehicleCapacity now comes from the file itself (all depots
        // in these instances share one capacity), instead of a hardcoded
        // constant that was wrong for pr12a/pr12b. See field comment above.
        if (depots.isEmpty()) {
            throw new IOException("No depots were loaded - cannot determine vehicle capacity.");
        }
        vehicleCapacity = depots.get(0).vehicleCapacity;
    }

    private void writeRunsCSV(String filename) {
        try (PrintWriter out = new PrintWriter(new FileWriter(filename))) {
            out.println("Config,Run,Seed,Fitness,Distance,Penalty,Violations,Feasible,WallClockMs");

            for (Map.Entry<String, List<RunResult>> entry : resultsByConfig.entrySet()) {
                String config = entry.getKey();
                for (RunResult result : entry.getValue()) {
                    out.printf("%s,%d,%d,%.4f,%.4f,%.4f,%d,%s,%d%n",
                            config, result.runNumber, result.seed,
                            result.fitness, result.distance, result.penalty,
                            result.violations, result.feasible, result.wallClockMs);
                }
            }
            log("Wrote runs CSV: " + filename);
        } catch (IOException e) {
            log("Failed to write runs CSV: " + e.getMessage());
        }
    }

    private void writeSummaryCSV(String filename) {
        try (PrintWriter out = new PrintWriter(new FileWriter(filename))) {
            out.println("Config,Mean_Fitness,StdDev_Fitness,Best_Fitness,Worst_Fitness," +
                    "Mean_Distance,StdDev_Distance,Mean_Violations,StdDev_Violations," +
                    "Feasible_Count,Feasible_Pct");

            for (Map.Entry<String, List<RunResult>> entry : resultsByConfig.entrySet()) {
                String config = entry.getKey();
                List<RunResult> results = entry.getValue();

                if (results.isEmpty()) continue;

                double[] fitnesses = results.stream().mapToDouble(r -> r.fitness).toArray();
                double[] distances = results.stream().mapToDouble(r -> r.distance).toArray();
                int[] violations = results.stream().mapToInt(r -> r.violations).toArray();
                int feasibleCount = (int) results.stream().filter(r -> r.feasible).count();

                double meanFitness = mean(fitnesses);
                double stddevFitness = stddev(fitnesses, meanFitness);
                double bestFitness = min(fitnesses);
                double worstFitness = max(fitnesses);

                double meanDistance = mean(distances);
                double stddevDistance = stddev(distances, meanDistance);

                double meanViolations = mean(Arrays.stream(violations).asDoubleStream().toArray());
                double stddevViolations = stddev(Arrays.stream(violations).asDoubleStream().toArray(), meanViolations);

                double feasiblePct = (100.0 * feasibleCount) / results.size();

                out.printf("%s,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%d,%.1f%%%n",
                        config,
                        meanFitness, stddevFitness, bestFitness, worstFitness,
                        meanDistance, stddevDistance,
                        meanViolations, stddevViolations,
                        feasibleCount, feasiblePct);
            }
            log("Wrote summary CSV: " + filename);
        } catch (IOException e) {
            log("Failed to write summary CSV: " + e.getMessage());
        }
    }

    private void printSummaryStats() {
        log("\n=== Summary Statistics ===");
        for (Map.Entry<String, List<RunResult>> entry : resultsByConfig.entrySet()) {
            String config = entry.getKey();
            List<RunResult> results = entry.getValue();

            if (results.isEmpty()) continue;

            double[] fitnesses = results.stream().mapToDouble(r -> r.fitness).toArray();
            double meanFitness = mean(fitnesses);
            double stddevFitness = stddev(fitnesses, meanFitness);
            int feasibleCount = (int) results.stream().filter(r -> r.feasible).count();

            String stats = String.format("%s: mean=%.2f (±%.2f), feasible=%d/%d",
                    config, meanFitness, stddevFitness, feasibleCount, results.size());
            log(stats);
        }
    }

    private double mean(double[] values) {
        if (values.length == 0) return 0.0;
        return Arrays.stream(values).sum() / values.length;
    }

    private double stddev(double[] values, double mean) {
        if (values.length <= 1) return 0.0;
        double sumSquaredDiffs = Arrays.stream(values)
                .map(v -> Math.pow(v - mean, 2))
                .sum();
        return Math.sqrt(sumSquaredDiffs / (values.length - 1));
    }

    private double min(double[] values) {
        return Arrays.stream(values).min().orElse(Double.MAX_VALUE);
    }

    private double max(double[] values) {
        return Arrays.stream(values).max().orElse(Double.MIN_VALUE);
    }
}