import java.util.List;
import javafx.scene.chart.XYChart;

/**
 * Common interface for solver contexts (GUI and headless).
 * Allows DifferentialEvolution to work with both MDVRPTWSolver and HeadlessSolverContext.
 *
 * ${user}blackcontractor@farid
 */
public interface SolverContext {
    // Logging
    void log(String message);

    // Solution evaluation
    void evaluateSolution(Solution solution);
    List<Route> decodeSolution(Solution solution);
    double[] evaluateDepotOnly(Depot depot, List<Customer> orderedCustomers);

    // State access
    List<Depot> getDepots();
    List<Customer> getCustomers();
    double getPenaltyWeight();
    void setPenaltyWeight(double weight);
    double getBasePenaltyWeight();
    void setBasePenaltyWeight(double weight);
    double getBestFitness();
    void setBestFitness(double fitness);
    String getCurrentDatasetName();

    // Visualization (no-op in headless)
    void animateSolution(Solution solution, int generation);
    void drawSolution(Solution sol);
}
