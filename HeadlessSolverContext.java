import java.util.*;

import javafx.scene.paint.Color;

/**
 * Lightweight solver context for headless operation.
 * Implements SolverContext to work with DifferentialEvolution.
 * Provides just the methods and state that DifferentialEvolution needs,
 * without JavaFX or GUI dependencies.
 *
 * ${user}blackcontractor@farid
 */
public class HeadlessSolverContext implements SolverContext {
    private List<Depot> depots;
    private List<Customer> customers;
    private int vehicleCapacity;
    private double penaltyWeight;
    private double basePenaltyWeight;
    private double bestFitness = Double.MAX_VALUE;
    private String currentDatasetName = "unknown";

    private final Color[] routeColors = {
            Color.RED, Color.BLUE, Color.GREEN, Color.PURPLE, Color.ORANGE,
            Color.CYAN, Color.MAGENTA, Color.DARKGREEN, Color.DARKBLUE, Color.DARKRED,
            Color.DARKORANGE, Color.DARKCYAN, Color.DARKMAGENTA, Color.LIGHTGREEN
    };

    public HeadlessSolverContext(List<Depot> depots, List<Customer> customers,
                                 int vehicleCapacity, double penaltyWeight, long seed) {
        this.depots = new ArrayList<>(depots);
        this.customers = new ArrayList<>();
        this.vehicleCapacity = vehicleCapacity;
        this.penaltyWeight = penaltyWeight;
        this.basePenaltyWeight = penaltyWeight;

        // Deep-copy customers to avoid shared state across runs
        for (Customer c : customers) {
            this.customers.add(new Customer(c));
        }
    }

    @Override
    public void log(String message) {
        System.out.println("[LOG] " + message);
    }

    @Override
    public void evaluateSolution(Solution solution) {
        solution.routes = decodeSolution(solution);
        solution.totalDistance = 0;
        solution.totalPenalty = 0;
        solution.timeWindowViolations = 0;

        for (Route route : solution.routes) {
            solution.totalDistance += route.distance;
            solution.totalPenalty += route.penalty;
            solution.timeWindowViolations += route.timeWindowViolations;
        }
        solution.fitness = solution.totalDistance + penaltyWeight * solution.totalPenalty;
    }

    @Override
    public List<Route> decodeSolution(Solution solution) {
        List<Route> routes = new ArrayList<>();
        int colorIndex = 0;

        // Assign each customer to nearest depot
        for (Customer customer : customers) {
            if (customer.assignedDepotId <= 0) {
                Depot nearest = findNearestDepot(customer);
                customer.assignedDepotId = depots.indexOf(nearest) + 1;
            }
        }

        // Precompute chromosome positions for efficient sorting
        int n = customers.size();
        Map<Customer, Integer> chromosomePosition = new IdentityHashMap<>(n);
        for (int pos = 0; pos < solution.chromosome.length; pos++) {
            int gene = solution.chromosome[pos];
            if (gene >= 0 && gene < n) {
                chromosomePosition.put(customers.get(gene), pos);
            }
        }

        // Group customers by assigned depot
        Map<Depot, List<Customer>> depotCustomers = new HashMap<>();
        for (Depot depot : depots) depotCustomers.put(depot, new ArrayList<>());

        for (Customer customer : customers) {
            int depotIdx = customer.assignedDepotId - 1;
            if (depotIdx < 0 || depotIdx >= depots.size()) continue;
            Depot depot = depots.get(depotIdx);
            depotCustomers.get(depot).add(customer);
        }

        // Create and evaluate routes for each depot
        for (Depot depot : depots) {
            List<Customer> depotCustList = depotCustomers.get(depot);
            if (depotCustList.isEmpty()) continue;

            List<Customer> sortedCustomers = new ArrayList<>(depotCustList);
            sortedCustomers.sort(Comparator.comparingInt(
                    c -> chromosomePosition.getOrDefault(c, Integer.MAX_VALUE)));

            List<Route> depotRoutes = createRoutesForDepot(depot, sortedCustomers);
            for (Route route : depotRoutes) {
                route.color = routeColors[colorIndex % routeColors.length];
                evaluateRoute(route);
                routes.add(route);
                colorIndex++;
            }
        }
        return routes;
    }

    @Override
    public double[] evaluateDepotOnly(Depot depot, List<Customer> orderedCustomers) {
        List<Route> depotRoutes = createRoutesForDepot(depot, orderedCustomers);
        double totalDistance = 0;
        int totalViolations = 0;
        for (Route route : depotRoutes) {
            evaluateRoute(route);
            totalDistance += route.distance;
            totalViolations += route.timeWindowViolations;
        }
        return new double[]{totalDistance, totalViolations};
    }

    @Override
    public List<Depot> getDepots() {
        return depots;
    }

    @Override
    public List<Customer> getCustomers() {
        return customers;
    }

    @Override
    public double getPenaltyWeight() {
        return penaltyWeight;
    }

    @Override
    public void setPenaltyWeight(double weight) {
        this.penaltyWeight = weight;
    }

    @Override
    public double getBasePenaltyWeight() {
        return basePenaltyWeight;
    }

    @Override
    public void setBasePenaltyWeight(double weight) {
        this.basePenaltyWeight = weight;
    }

    @Override
    public double getBestFitness() {
        return bestFitness;
    }

    @Override
    public void setBestFitness(double fitness) {
        this.bestFitness = fitness;
    }

    @Override
    public String getCurrentDatasetName() {
        return currentDatasetName;
    }

    @Override
    public void animateSolution(Solution solution, int generation) {
        // No-op in headless mode
    }

    @Override
    public void drawSolution(Solution sol) {
        // No-op in headless mode
    }

    // Helper methods

    private List<Route> createRoutesForDepot(Depot depot, List<Customer> customers) {
        List<Route> routes = new ArrayList<>();
        Route currentRoute = new Route(depot);
        double currentLoad = 0;

        for (Customer customer : customers) {
            if (currentLoad + customer.demand > vehicleCapacity) {
                if (!currentRoute.customers.isEmpty()) routes.add(currentRoute);
                currentRoute = new Route(depot);
                currentLoad = 0;
            }
            currentRoute.customers.add(customer);
            currentLoad += customer.demand;
        }
        if (!currentRoute.customers.isEmpty()) routes.add(currentRoute);
        return routes;
    }

    private void evaluateRoute(Route route) {
        if (route.customers.isEmpty()) {
            route.distance = 0;
            route.penalty = 0;
            route.timeWindowViolations = 0;
            return;
        }

        double totalDistance = 0;
        int violations = 0;
        double currentTime = 0;
        Customer prev = null;

        Customer first = route.customers.get(0);
        totalDistance += distance(route.depot, first);
        currentTime += distance(route.depot, first);

        if (currentTime < first.readyTime) currentTime = first.readyTime;
        else if (currentTime > first.dueTime) violations++;
        currentTime += first.serviceTime;
        prev = first;

        for (int i = 1; i < route.customers.size(); i++) {
            Customer current = route.customers.get(i);
            double travelTime = distance(prev, current);
            totalDistance += travelTime;
            currentTime += travelTime;

            if (currentTime < current.readyTime) currentTime = current.readyTime;
            else if (currentTime > current.dueTime) violations++;
            currentTime += current.serviceTime;
            prev = current;
        }
        totalDistance += distance(prev, route.depot);

        route.distance = totalDistance;
        route.penalty = violations;
        route.timeWindowViolations = violations;
    }

    // Bug fix: this was an IDE-generated stub ("TODO Auto-generated method
    // stub") that always returned 0, meaning every route's final leg back to
    // its depot was silently treated as zero-distance in both the reported
    // totals AND the fitness the DE actually searched against. Customer
    // already has a working distanceTo(Depot) method - this just needed to
    // delegate to it.
    private double distance(Customer prev, Depot depot) {
        return prev.distanceTo(depot);
    }

    private Depot findNearestDepot(Customer customer) {
        Depot nearest = depots.get(0);
        double minDist = distance(nearest, customer);
        for (int i = 1; i < depots.size(); i++) {
            double dist = distance(depots.get(i), customer);
            if (dist < minDist) {
                minDist = dist;
                nearest = depots.get(i);
            }
        }
        return nearest;
    }

    private double distance(Depot depot, Customer customer) {
        double dx = depot.x - customer.x;
        double dy = depot.y - customer.y;
        return Math.sqrt(dx * dx + dy * dy);
    }

    private double distance(Customer a, Customer b) {
        double dx = a.x - b.x;
        double dy = a.y - b.y;
        return Math.sqrt(dx * dx + dy * dy);
    }
}