import java.util.*;

/**
 * ${user}blackcontractor@farid
 */
public class Solution {
    public int[] chromosome;
    public List<Route> routes = new ArrayList<>();
    public double fitness;
    public double totalDistance;
    public double totalPenalty;
    public int timeWindowViolations;
    public double penalty;

    public Solution(int[] chromosome) {
        this.chromosome = chromosome;
    }

    /*
    public Solution() {
        this.chromosome = null;
    }
*/
    public Solution(Solution other) {
        this.chromosome = (other.chromosome != null)
                ? Arrays.copyOf(other.chromosome, other.chromosome.length)
                : null;
        this.fitness = other.fitness;
        this.totalDistance = other.totalDistance;
        this.totalPenalty = other.totalPenalty;
        this.timeWindowViolations = other.timeWindowViolations;
        this.penalty = other.penalty;
        this.routes = new ArrayList<>();
        if (other.routes != null) {
            for (Route r : other.routes) {
                Route rClone = new Route(r.depot);
                rClone.color = r.color;
                rClone.distance = r.distance;
                rClone.waitTime = r.waitTime;
                rClone.penalty = r.penalty;
                rClone.timeWindowViolations = r.timeWindowViolations;
                rClone.totalLoad = r.totalLoad;
                rClone.totalDistance = r.totalDistance;
                // Deep-copy each Customer rather than just copying the List reference.
                // Without this, cloned routes across different Solution instances
                // shared the same underlying Customer objects, so writes to mutable
                // fields (arrivalTime, assignedDepotId) by one solution's evaluation
                // could silently corrupt state a different "independent" clone was
                // relying on.
                rClone.customers = new ArrayList<>();
                if (r.customers != null) {
                    for (Customer c : r.customers) {
                        rClone.customers.add(new Customer(c));
                    }
                }
                this.routes.add(rClone);
            }
        }
    }

    public boolean isFeasible() {
        return timeWindowViolations == 0;
    }

    @Override
    public String toString() {
        return "Fitness: " + String.format("%.2f", fitness) +
                ", Distance: " + String.format("%.2f", totalDistance) +
                ", Penalty: " + String.format("%.2f", totalPenalty) +
                ", TW Violations: " + timeWindowViolations +
                ", Feasible: " + isFeasible();
    }
}