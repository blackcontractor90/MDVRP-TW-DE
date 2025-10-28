import java.util.*;

/**
 * ${user}blackcontractor@farid
 */
public class Decoder {

    /**
     * Decodes a chromosome into a solution for the MDVRPTW.
     * Supports flexible depot assignment and capacity-constrained route splitting.
     *
     * @param chromosome       Permutation of customer indices.
     * @param depotAssignment  [Optional] For each customer, the depot id to assign. If null, assign to nearest depot.
     * @param customers        List of customers.
     * @param depots           List of depots.
     * @return Solution object with routes, distance, penalties, and fitness.
     */
    public static Solution decode(
            int[] chromosome,
            int[] depotAssignment,
            List<Customer> customers,
            List<Depot> depots
    ) {
        Solution solution = (depotAssignment == null)
                ? new Solution(chromosome)
                : new Solution(chromosome);

        // 1. Assign customers to depots
        Map<Integer, List<Customer>> depotCustomerMap = new HashMap<>();
        for (Depot depot : depots) depotCustomerMap.put(depot.id, new ArrayList<>());

        for (int i = 0; i < chromosome.length; i++) {
            int custIdx = chromosome[i];
            Customer c = customers.get(custIdx);
            int depotId;
            if (depotAssignment != null && depotAssignment.length > custIdx) {
                depotId = depotAssignment[custIdx];
            } else {
                depotId = findNearestDepotId(c, depots);
            }
            depotCustomerMap.get(depotId).add(c);
        }

        // 2. For each depot, split customers into feasible routes (by capacity)
        double totalDistance = 0.0, totalPenalty = 0.0;
        int totalTWViolations = 0;
        int vehicleLimitPenalty = 0;

        for (Depot depot : depots) {
            List<Customer> depotCusts = depotCustomerMap.get(depot.id);

            // Sort customers by their appearance in chromosome for reproducibility
            depotCusts.sort(Comparator.comparingInt(c -> indexInChromosome(chromosome, c.id)));

            List<Route> routes = splitIntoFeasibleRoutes(depot, depotCusts);

            // Penalty for exceeding max vehicles per depot
            if (routes.size() > depot.maxVehicles) {
                vehicleLimitPenalty += (routes.size() - depot.maxVehicles);
            }

            for (Route route : routes) {
                // Optional: local improvement with 2-opt
                twoOpt(route);

                route.computeTotalDistance(); // sets route.distance
                route.evaluateTimeWindows();  // sets route.timeWindowViolations, etc.

                solution.routes.add(route);
                totalDistance += route.distance;
                totalPenalty += route.penalty;  // e.g., time window or load violation penalty
                totalTWViolations += route.timeWindowViolations;
            }
        }

        // 3. Compute aggregate penalties
        // Adjust penalty weights as used in your main algorithm!
        double penaltyWeight = 10000; // Example: set to match your main algorithm
        double fitness = totalDistance +
                penaltyWeight * (totalPenalty + vehicleLimitPenalty + totalTWViolations);

        solution.totalDistance = totalDistance;
        solution.totalPenalty = totalPenalty + vehicleLimitPenalty;
        solution.timeWindowViolations = totalTWViolations;
        solution.fitness = fitness;

        return solution;
    }

    // Find nearest depot by Euclidean distance
    private static int findNearestDepotId(Customer c, List<Depot> depots) {
        return depots.stream()
            .min(Comparator.comparingDouble(d -> Math.hypot(d.x - c.x, d.y - c.y)))
            .map(d -> d.id)
            .orElse(depots.get(0).id);
    }

    // Get index of customer id in chromosome
    private static int indexInChromosome(int[] chromosome, int customerId) {
        for (int i = 0; i < chromosome.length; i++) {
            if (chromosome[i] == customerId) return i;
        }
        return -1; // Should not happen
    }

    // Sequential split: start a new route if over capacity
    private static List<Route> splitIntoFeasibleRoutes(Depot depot, List<Customer> customers) {
        List<Route> routes = new ArrayList<>();
        Route currentRoute = new Route(depot);
        double currentLoad = 0.0;

        for (Customer c : customers) {
            if ((currentLoad + c.demand > depot.vehicleCapacity) && !currentRoute.customers.isEmpty()) {
                routes.add(currentRoute);
                currentRoute = new Route(depot);
                currentLoad = 0.0;
            }
            currentRoute.addCustomer(c);
            currentLoad += c.demand;
        }
        if (!currentRoute.customers.isEmpty()) routes.add(currentRoute);
        return routes;
    }

    /**
     * Simple 2-opt local search for route improvement.
     * Modifies the route in-place if an improvement is found.
     */
    private static void twoOpt(Route route) {
        if (route.customers.size() < 4) return;
        boolean improved;
        do {
            improved = false;
            double bestDelta = 0;
            int bestI = 0, bestK = 0;
            for (int i = 0; i < route.customers.size() - 2; i++) {
                for (int k = i + 2; k < route.customers.size(); k++) {
                    double delta = compute2OptDelta(route, i, k);
                    if (delta < bestDelta) {
                        bestDelta = delta;
                        bestI = i;
                        bestK = k;
                        improved = true;
                    }
                }
            }
            if (improved) {
                Collections.reverse(route.customers.subList(bestI + 1, bestK + 1));
            }
        } while (improved);
    }

    /**
     * Computes the change in distance for a 2-opt swap between positions i and k.
     */
    private static double compute2OptDelta(Route route, int i, int k) {
        Depot depot = route.depot;
        Customer prev = (i == -1) ? null : (i == 0 ? null : route.customers.get(i - 1));
        Customer n1 = route.customers.get(i);
        Customer n2 = route.customers.get(k);

        double before = 0, after = 0;

        // Edge before i+1
        if (i == -1) {
            before += distance(depot, route.customers.get(0));
            after += distance(depot, route.customers.get(k));
        } else {
            before += distance(prev, n1);
            after += distance(prev, n2);
        }

        // Edge after k
        if (k + 1 == route.customers.size()) {
            before += distance(depot, route.customers.get(k));
            after += distance(depot, n1);
        } else {
            before += distance(route.customers.get(k), route.customers.get(k + 1));
            after += distance(n1, route.customers.get(k + 1));
        }

        return after - before;
    }

    private static double distance(Depot depot, Customer customer) {
        return Math.hypot(depot.x - customer.x, depot.y - customer.y);
    }

    private static double distance(Customer c1, Customer c2) {
        return Math.hypot(c1.x - c2.x, c1.y - c2.y);
    }
}