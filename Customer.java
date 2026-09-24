/**
 * ${user}blackcontractor@farid
 */
public class Customer {
    public final double x;
    public final double y;
    public final String name;
    public final int demand;
    public final double readyTime;
    public final double dueTime;
    public final double serviceTime;

    public int assignedDepotId = -1;
    public double arrivalTime = -1.0;
	public int id;

    public Customer(double x, double y, String name, int demand,
                    double readyTime, double dueTime, double serviceTime) {
        this.x = x;
        this.y = y;
        this.name = name;
        this.demand = demand;
        this.readyTime = readyTime;
        this.dueTime = dueTime;
        this.serviceTime = serviceTime;
    }

    public Customer(int id, double x, double y, int demand,
                    double readyTime, double dueTime, double serviceTime) {
        this(x, y, "C" + id, demand, readyTime, dueTime, serviceTime);
        this.id = id;
    }

    /**
     * Deep-copy constructor. Needed because Solution's copy constructor previously
     * shared Customer instances across cloned solutions/routes (only the List was
     * copied, not its contents) - meaning mutable fields like arrivalTime and
     * assignedDepotId written by one solution's evaluation could bleed into a
     * different "cloned" solution that should have been independent.
     */
    public Customer(Customer other) {
        this.x = other.x;
        this.y = other.y;
        this.name = other.name;
        this.demand = other.demand;
        this.readyTime = other.readyTime;
        this.dueTime = other.dueTime;
        this.serviceTime = other.serviceTime;
        this.id = other.id;
        this.assignedDepotId = other.assignedDepotId;
        this.arrivalTime = other.arrivalTime;
    }

	public double distanceTo(Customer customer) {
        double dx = this.x - customer.x;
        double dy = this.y - customer.y;
        return Math.hypot(dx, dy);
    }

    public double distanceTo(Depot depot) {
        double dx = this.x - depot.x;
        double dy = this.y - depot.y;
        return Math.hypot(dx, dy);
    }

    public boolean isWithinTimeWindow(double arrivalTime) {
        return arrivalTime >= readyTime && arrivalTime <= dueTime;
    }

    public double slackTime() {
        return Math.max(0.0, dueTime - arrivalTime);
    }

    @Override
    public String toString() {
        return String.format("%s (%.2f, %.2f), demand=%d, window=[%.2f - %.2f]",
                name, x, y, demand, readyTime, dueTime);
    }
}