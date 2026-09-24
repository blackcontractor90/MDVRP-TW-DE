import java.io.*;
import java.util.*;

/**
 * ${user}blackcontractor@farid
 */
public class DataLoader {
    public int numberOfVehicles;
    public int vehicleCapacity;
    public List<Depot> depots = new ArrayList<>();
    public List<Customer> customers = new ArrayList<>();

    public void loadData(String filename) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(filename));
        String line;

        // First line: vehicle info
        line = reader.readLine();
        if (line == null) throw new IOException("Empty file");
        String[] firstLine = line.trim().split("\\s+");
        numberOfVehicles = Integer.parseInt(firstLine[0]);
        vehicleCapacity = Integer.parseInt(firstLine[1]);

        // Read depot + customer data
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty()) continue;

            String[] tokens = line.split("\\s+");
            int id = Integer.parseInt(tokens[0]);
            double x = Double.parseDouble(tokens[1]);
            double y = Double.parseDouble(tokens[2]);
            int demand = Integer.parseInt(tokens[3]);
            int readyTime = Integer.parseInt(tokens[4]);
            int dueDate = Integer.parseInt(tokens[5]);
            int serviceTime = Integer.parseInt(tokens[6]);

            if (demand == 0) {
                // Bug fix: this used to pass the depot row's readyTime/dueDate
                // positionally into Depot's (vehicleCapacity, maxVehicles) params,
                // which for a typical Solomon/Cordeau depot row means capacity=0
                // (readyTime is usually 0) and maxVehicles=huge (dueDate is usually
                // the planning horizon). A capacity of 0 makes every route
                // infeasible for any customer with positive demand - i.e. this
                // silently broke routing for any dataset loaded through this path.
                // Depot capacity/fleet size come from the file's first line, already
                // parsed into vehicleCapacity/numberOfVehicles above.
                depots.add(new Depot(id, x, y, vehicleCapacity, numberOfVehicles));
            } else {
                customers.add(new Customer(id, x, y, demand, readyTime, dueDate, serviceTime));
            }
        }

        reader.close();
        System.out.println("Loaded " + depots.size() + " depot(s) and " + customers.size() + " customer(s).");
    }
}