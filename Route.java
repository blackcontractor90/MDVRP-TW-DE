import java.util.*;
import javafx.scene.paint.Paint;
import javafx.scene.paint.Color;

/**
 * ${user}blackcontractor@farid
 */
public class Route {
    private static final List<Color> PRESET_COLORS = Arrays.asList(
        Color.RED, Color.BLUE, Color.GREEN, Color.ORANGE, Color.PURPLE,
        new Color(0.59, 0.29, 0.0, 1.0),
        Color.CYAN, Color.MAGENTA, Color.DARKGRAY, Color.PINK
    );

    public Depot depot;
    public List<Customer> customers = new ArrayList<>();
    public double totalDistance = 0;
    public int totalLoad = 0;
    public double penalty = 0;
    public int timeWindowViolations = 0;
    public Paint color;
    public double waitTime = 0;
    public double distance = 0;

    public Route(Depot depot) {
        this.depot = depot;
        this.color = PRESET_COLORS.get((PRESET_COLORS.size() + depot.name.hashCode()) % PRESET_COLORS.size());
    }

    public void addCustomer(Customer customer) {
        double arrivalTime = estimateArrivalTime(customer);
        if (arrivalTime < customer.readyTime) {
            waitTime += customer.readyTime - arrivalTime;
            arrivalTime = customer.readyTime;
        }
        if (arrivalTime > customer.dueTime) {
            timeWindowViolations++;
            penalty += arrivalTime - customer.dueTime;
        }
        if (!customers.isEmpty()) {
            Customer last = customers.get(customers.size() - 1);
            totalDistance += last.distanceTo(customer);
        } else {
            totalDistance += depot.distanceTo(customer);
        }
        customer.arrivalTime = arrivalTime;
        totalLoad += customer.demand;
        customers.add(customer);
    }

    public boolean canAddCustomer(Customer customer) {
        return (totalLoad + customer.demand <= depot.vehicleCapacity) &&
                (getRouteDurationWith(customer) <= depot.maxDuration);
    }

    public void finalizeRoute() {
        if (!customers.isEmpty()) {
            Customer last = customers.get(customers.size() - 1);
            totalDistance += last.distanceTo(depot);
            distance = totalDistance;
        }
    }

    public boolean isFeasible() {
        return totalLoad <= depot.vehicleCapacity &&
                timeWindowViolations == 0 &&
                getRouteDuration() <= depot.maxDuration;
    }

    public double getRouteDuration() {
        return getRouteDurationWith(null);
    }

    public double getRouteDurationWith(Customer nextCustomer) {
        double duration = 0;
        if (!customers.isEmpty()) {
            duration += depot.distanceTo(customers.get(0));
            for (int i = 0; i < customers.size() - 1; i++) {
                duration += customers.get(i).distanceTo(customers.get(i + 1)) + customers.get(i).serviceTime;
            }
            duration += customers.get(customers.size() - 1).serviceTime;
            if (nextCustomer != null) {
                duration += customers.get(customers.size() - 1).distanceTo(nextCustomer) + nextCustomer.serviceTime;
                duration += nextCustomer.distanceTo(depot);
            } else {
                duration += customers.get(customers.size() - 1).distanceTo(depot);
            }
        } else if (nextCustomer != null) {
            duration += depot.distanceTo(nextCustomer);
            duration += nextCustomer.serviceTime;
            duration += nextCustomer.distanceTo(depot);
        }
        return duration;
    }

    private double estimateArrivalTime(Customer next) {
        if (!customers.isEmpty()) {
            Customer last = customers.get(customers.size() - 1);
            return last.arrivalTime + last.serviceTime + last.distanceTo(next);
        } else {
            return depot.distanceTo(next);
        }
    }

    @Override
    public String toString() {
        return depot.name + " -> " + customers.size() + " customers | Load=" +
                totalLoad + " | TW Violations=" + timeWindowViolations +
                " | Distance=" + String.format("%.2f", totalDistance);
    }

    public double computeTotalDistance() {
        double distance = 0;
        if (customers.isEmpty()) return distance;
        Customer prev = null;
        for (Customer customer : customers) {
            if (prev == null) {
                distance += depot.distanceTo(customer);
            } else {
                distance += prev.distanceTo(customer);
            }
            prev = customer;
        }
        if (prev != null) {
            distance += prev.distanceTo(depot);
        }
        this.distance = distance;
        return distance;
    }

	public void evaluateTimeWindows() {
		// TODO Auto-generated method stub
		
	}
}