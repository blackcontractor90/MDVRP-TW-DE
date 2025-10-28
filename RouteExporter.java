import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/**
 * ${user}blackcontractor@farid
 */
public class RouteExporter {

    public static void exportToCSV(List<Route> routes, String filename) throws IOException {
        try (FileWriter writer = new FileWriter(filename)) {
            writer.write("RouteID,Depot,CustomerID,X,Y,Ready,Due,Service,RouteDuration,Penalty\n");

            int routeNum = 1;
            for (Route route : routes) {
                double routeDuration = route.getRouteDuration();

                for (Customer c : route.customers) {
                    writer.write(String.format(
                        "%d,%s,%s,%.2f,%.2f,%.1f,%.1f,%.1f,%.1f,%.1f\n",
                        routeNum,
                        route.depot.name,
                        c.name,
                        c.x, c.y,
                        c.readyTime,
                        c.dueTime,
                        c.serviceTime,
                        routeDuration,
                        route.penalty
                    ));
                }
                routeNum++;
            }
        }
    }
}
