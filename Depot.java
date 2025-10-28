
/**
 * ${user}blackcontractor@farid
 */
public class Depot {
    public final double x;
    public final double y;
    public final String name;
    public int maxVehicles;
    public double maxDuration;
    public int vehicleCapacity;
    public int id = -1;

    public Depot(double x, double y, String name) {
        this.x = x;
        this.y = y;
        this.name = name;
        this.vehicleCapacity = 100;
        this.maxVehicles = 1;
        this.maxDuration = 9999.0;
    }

    public Depot(int id, double x, double y, int vehicleCapacity, int maxVehicles) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.vehicleCapacity = vehicleCapacity;
        this.maxVehicles = maxVehicles;
        this.name = "D" + id;
        this.maxDuration = 9999.0;
    }

    public double distanceTo(Customer customer) {
        double dx = this.x - customer.x;
        double dy = this.y - customer.y;
        return Math.hypot(dx, dy);
    }

    @Override
    public String toString() {
        return String.format("%s (%.2f, %.2f), Capacity=%d, Vehicles=%d",
                name, x, y, vehicleCapacity, maxVehicles);
    }
}