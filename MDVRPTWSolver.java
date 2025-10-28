import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * ${user}blackcontractor@farid
 */
public class MDVRPTWSolver extends Application {

    private BorderPane root;
    private CanvasPane canvasPane;
    private VBox controlPanel;
    private LineChart<Number, Number> convergenceChart;
    private XYChart.Series<Number, Number> convergenceSeries;
    private TextArea logArea;
    private Label statsLabel;
    private Label timeWindowStatsLabel;
    private RoutingSolver solver;

    // Problem Data
    private List<Depot> depots = new ArrayList<>();
    List<Customer> customers = new ArrayList<>();
    private List<Route> solutionRoutes = new ArrayList<>();

    // Algorithm Parameters
    private int populationSize = 50;
    private double crossoverRate = 0.9;
    private double scalingFactor = 0.8;
    private int maxGenerations = 200;
    private int vehicleCapacity = 200;
    double penaltyWeight = 1000;
    double bestFitness = Double.MAX_VALUE;

    private boolean enableRelocationLocalSearch = true;

    // Visualization settings
    private boolean showDepotLabels = true;
    private boolean showCustomerLabels = true;
    private boolean showRouteLabels = true;
    private boolean showTimeWindows = true;

    // Colors for visualization
    private final Color[] routeColors = {
            Color.RED, Color.BLUE, Color.GREEN, Color.PURPLE, Color.ORANGE,
            Color.CYAN, Color.MAGENTA, Color.DARKGREEN, Color.DARKBLUE, Color.DARKRED,
            Color.DARKORANGE, Color.DARKCYAN, Color.DARKMAGENTA, Color.LIGHTGREEN
    };
	public int customerCount;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("MDVRP-Time Windows with Differential Evolution");

        root = new BorderPane();
        canvasPane = new CanvasPane(800, 600);
        root.setCenter(canvasPane);

        solver = new RoutingSolver();
        solver.setCanvasPane(canvasPane);

        createControlPanel();
        createChartsAndLogs();

        MenuBar menuBar = createMenuBar(primaryStage);
        root.setTop(menuBar);

        Scene scene = new Scene(root, 1200, 800);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void createControlPanel() {
        controlPanel = new VBox(10);
        controlPanel.setPadding(new Insets(15));
        controlPanel.setStyle("-fx-background-color: #f5f5f5; -fx-border-color: #ddd; -fx-border-width: 0 0 0 1;");
        controlPanel.setPrefWidth(350);

        Label titleLabel = new Label("MDVRP with Time Windows");
        titleLabel.setFont(Font.font("Arial", 18));
        titleLabel.setStyle("-fx-font-weight: bold;");

        statsLabel = new Label("No solution generated");
        statsLabel.setFont(Font.font("Arial", 12));
        statsLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        timeWindowStatsLabel = new Label("Time window violations: 0");
        timeWindowStatsLabel.setFont(Font.font("Arial", 12));
        timeWindowStatsLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #d35400;");

        Label paramLabel = new Label("Algorithm Parameters:");
        paramLabel.setFont(Font.font("Arial", 14));
        paramLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        Spinner<Integer> popSizeSpinner = createSpinner(10, 200, populationSize, 10, "Population Size");
        Spinner<Double> crSpinner = createSpinner(0.1, 1.0, crossoverRate, 0.05, "Crossover Rate");
        Spinner<Double> fSpinner = createSpinner(0.1, 1.0, scalingFactor, 0.05, "Scaling Factor");
        Spinner<Integer> genSpinner = createSpinner(10, 1000, maxGenerations, 10, "Max Generations");
        Spinner<Integer> capacitySpinner = createSpinner(10, 500, vehicleCapacity, 10, "Vehicle Capacity");
        Spinner<Double> penaltySpinner = createSpinner(100, 10000, penaltyWeight, 100, "Time Window Penalty");

        CheckBox relocationCheck = new CheckBox("Enable Route Relocation Local Search");
        relocationCheck.setSelected(enableRelocationLocalSearch);
        relocationCheck.selectedProperty().addListener((obs, oldVal, newVal) -> enableRelocationLocalSearch = newVal);

        Button runButton = new Button("Run Differential Evolution");
        runButton.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold;");
        runButton.setMaxWidth(Double.MAX_VALUE);
        runButton.setOnAction(e -> runAlgorithm());

        Button analyzeButton = new Button("Analyze Metrics Summary");
        analyzeButton.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-font-weight: bold;");
        analyzeButton.setMaxWidth(Double.MAX_VALUE);
        analyzeButton.setOnAction(e -> {
            MetricsAnalyzer.analyzeLatestSummary(message ->
                    Platform.runLater(() -> log(message))
            );
            File latest = MetricsAnalyzer.getLatestSummaryFile();
            if (latest != null) {
                MetricsChartViewer.show(latest.getAbsolutePath());
            } else {
                log("No summary file found to display charts.");
            }
        });

        Label visLabel = new Label("Visualization Options:");
        visLabel.setFont(Font.font("Arial", 14));
        visLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        CheckBox depotLabelCheck = new CheckBox("Show Depot Labels");
        depotLabelCheck.setSelected(showDepotLabels);
        depotLabelCheck.selectedProperty().addListener((obs, oldVal, newVal) -> {
            showDepotLabels = newVal;
            canvasPane.setShowDepotLabels(newVal);
            canvasPane.draw();
        });

        CheckBox customerLabelCheck = new CheckBox("Show Customer Labels");
        customerLabelCheck.setSelected(showCustomerLabels);
        customerLabelCheck.selectedProperty().addListener((obs, oldVal, newVal) -> {
            showCustomerLabels = newVal;
            canvasPane.setShowCustomerLabels(newVal);
            canvasPane.draw();
        });

        CheckBox routeLabelCheck = new CheckBox("Show Route Labels");
        routeLabelCheck.setSelected(showRouteLabels);
        routeLabelCheck.selectedProperty().addListener((obs, oldVal, newVal) -> {
            showRouteLabels = newVal;
            canvasPane.setShowRouteLabels(newVal);
            canvasPane.draw();
        });

        CheckBox timeWindowCheck = new CheckBox("Show Time Windows");
        timeWindowCheck.setSelected(showTimeWindows);
        timeWindowCheck.selectedProperty().addListener((obs, oldVal, newVal) -> {
            showTimeWindows = newVal;
            canvasPane.setShowTimeWindows(newVal);
            canvasPane.draw();
        });

        controlPanel.getChildren().addAll(
                titleLabel,
                statsLabel,
                timeWindowStatsLabel,
                new Separator(),
                paramLabel,
                popSizeSpinner,
                crSpinner,
                fSpinner,
                genSpinner,
                capacitySpinner,
                penaltySpinner,
                relocationCheck,
                runButton,
                analyzeButton,
                new Separator(),
                visLabel,
                depotLabelCheck,
                customerLabelCheck,
                routeLabelCheck,
                timeWindowCheck
        );
        root.setRight(controlPanel);

        popSizeSpinner.getValueFactory().valueProperty().addListener((obs, oldVal, newVal) -> populationSize = newVal);
        crSpinner.getValueFactory().valueProperty().addListener((obs, oldVal, newVal) -> crossoverRate = newVal);
        fSpinner.getValueFactory().valueProperty().addListener((obs, oldVal, newVal) -> scalingFactor = newVal);
        genSpinner.getValueFactory().valueProperty().addListener((obs, oldVal, newVal) -> maxGenerations = newVal);
        capacitySpinner.getValueFactory().valueProperty().addListener((obs, oldVal, newVal) -> vehicleCapacity = newVal);
        penaltySpinner.getValueFactory().valueProperty().addListener((obs, oldVal, newVal) -> penaltyWeight = newVal);
    }

    private Spinner createSpinner(double min, double max, double initial, double step, String label) {
        Spinner spinner;
        if (step == (int) step) {
            spinner = new Spinner<>(new SpinnerValueFactory.IntegerSpinnerValueFactory((int) min, (int) max, (int) initial, (int) step));
        } else {
            spinner = new Spinner<>(new SpinnerValueFactory.DoubleSpinnerValueFactory(min, max, initial, step));
        }
        spinner.setEditable(true);
        spinner.setPrefWidth(250);

        Label spinnerLabel = new Label(label);
        spinnerLabel.setStyle("-fx-font-weight: bold;");
        VBox container = new VBox(5, spinnerLabel, spinner);
        container.setPadding(new Insets(5, 0, 5, 0));
        controlPanel.getChildren().add(container);
        return spinner;
    }

    private void createChartsAndLogs() {
        convergenceSeries = new XYChart.Series<>();
        convergenceSeries.setName("Best Fitness");
        NumberAxis xAxis = new NumberAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Generation");
        yAxis.setLabel("Total Cost (Distance + Penalties)");
        convergenceChart = new LineChart<>(xAxis, yAxis);
        convergenceChart.setTitle("Fitness Convergence");
        convergenceChart.setPrefHeight(250);
        convergenceChart.setCreateSymbols(false);
        convergenceChart.setLegendVisible(false);
        convergenceChart.setAnimated(false);
        convergenceChart.getData().add(convergenceSeries);

        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setFont(Font.font("Consolas", 12));
        logArea.setStyle("-fx-control-inner-background: #f8f8f8;");
        logArea.setPrefHeight(150);

        VBox bottomBox = new VBox(10, convergenceChart, logArea);
        bottomBox.setPadding(new Insets(10));
        root.setBottom(bottomBox);
    }

    private MenuBar createMenuBar(Stage stage) {
        MenuBar menuBar = new MenuBar();

        Menu fileMenu = new Menu("File");
        MenuItem newItem = new MenuItem("New Problem");
        MenuItem loadCordeauItem = new MenuItem("Load Cordeau Dataset");
        MenuItem exportCSVItem = new MenuItem("Export Routes to CSV");
        MenuItem saveImageItem = new MenuItem("Save Solution as Image");
        MenuItem exitItem = new MenuItem("Exit");

        newItem.setOnAction(e -> resetProblem());
        loadCordeauItem.setOnAction(e -> loadDataset(stage));
        exportCSVItem.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save Routes as CSV");
            fileChooser.setInitialFileName("routes.csv");
            File file = fileChooser.showSaveDialog(stage);
            if (file != null) {
                try {
                    RouteExporter.exportToCSV(solver.getRoutes(), file.getAbsolutePath());
                    log("Routes exported to: " + file.getName());
                } catch (IOException ex) {
                    log("Export failed: " + ex.getMessage());
                }
            }
        });
        saveImageItem.setOnAction(e -> saveImage(stage));
        exitItem.setOnAction(e -> Platform.exit());

        fileMenu.getItems().addAll(
                newItem,
                loadCordeauItem,
                exportCSVItem,
                saveImageItem,
                new SeparatorMenuItem(),
                exitItem
        );

        Menu editMenu = new Menu("Edit");
        MenuItem addDepotItem = new MenuItem("Add Depot");
        MenuItem addCustomerItem = new MenuItem("Add Customer");
        MenuItem clearItem = new MenuItem("Clear All");

        addDepotItem.setOnAction(e -> setAddDepotMode());
        addCustomerItem.setOnAction(e -> setAddCustomerMode());
        clearItem.setOnAction(e -> clearProblem());

        editMenu.getItems().addAll(addDepotItem, addCustomerItem, new SeparatorMenuItem(), clearItem);

        Menu viewMenu = new Menu("View");
        CheckMenuItem showDepotLabelsItem = new CheckMenuItem("Show Depot Labels");
        CheckMenuItem showCustomerLabelsItem = new CheckMenuItem("Show Customer Labels");
        CheckMenuItem showRouteLabelsItem = new CheckMenuItem("Show Route Labels");
        CheckMenuItem showTimeWindowsItem = new CheckMenuItem("Show Time Windows");

        showDepotLabelsItem.setSelected(showDepotLabels);
        showCustomerLabelsItem.setSelected(showCustomerLabels);
        showRouteLabelsItem.setSelected(showRouteLabels);
        showTimeWindowsItem.setSelected(showTimeWindows);

        showDepotLabelsItem.setOnAction(e -> {
            showDepotLabels = showDepotLabelsItem.isSelected();
            canvasPane.setShowDepotLabels(showDepotLabels);
            canvasPane.draw();
        });
        showCustomerLabelsItem.setOnAction(e -> {
            showCustomerLabels = showCustomerLabelsItem.isSelected();
            canvasPane.setShowCustomerLabels(showCustomerLabels);
            canvasPane.draw();
        });
        showRouteLabelsItem.setOnAction(e -> {
            showRouteLabels = showRouteLabelsItem.isSelected();
            canvasPane.setShowRouteLabels(showRouteLabels);
            canvasPane.draw();
        });
        showTimeWindowsItem.setOnAction(e -> {
            showTimeWindows = showTimeWindowsItem.isSelected();
            canvasPane.setShowTimeWindows(showTimeWindows);
            canvasPane.draw();
        });

        viewMenu.getItems().addAll(
                showDepotLabelsItem,
                showCustomerLabelsItem,
                showRouteLabelsItem,
                showTimeWindowsItem
        );

        Menu helpMenu = new Menu("Help");
        MenuItem aboutItem = new MenuItem("About");
        aboutItem.setOnAction(e -> showAboutDialog());
        helpMenu.getItems().add(aboutItem);

        menuBar.getMenus().addAll(fileMenu, editMenu, viewMenu, helpMenu);
        return menuBar;
    }

    private void showAboutDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("About MDVRPTW Solver");
        alert.setHeaderText("Multi-Depot Vehicle Routing Problem with Time Windows");
        alert.setContentText("This application uses Differential Evolution to solve\n" +
                "the MDVRP with Time Windows (MDVRPTW).\n\n" +
                "Features include:\n" +
                "- Supports Cordeau benchmark datasets\n" +
                "- Time window constraints with penalty handling\n" +
                "- Interactive visualization of routes and time windows\n" +
                "- Performance metrics and convergence tracking\n\n" +
                "Developed with JavaFX\nVersion 2.0");
        alert.showAndWait();
    }

    private void setAddDepotMode() {
        canvasPane.setAddDepotMode();
        log("Click on the canvas to add a depot");
    }

    private void setAddCustomerMode() {
        canvasPane.setAddCustomerMode();
        log("Click on the canvas to add a customer");
    }

    private void resetProblem() {
        depots.clear();
        customers.clear();
        solutionRoutes.clear();
        convergenceSeries.getData().clear();
        logArea.clear();
        statsLabel.setText("No solution generated");
        timeWindowStatsLabel.setText("Time window violations: 0");
        canvasPane.setDepots(depots);
        canvasPane.setCustomers(customers);
        canvasPane.setSolutionRoutes(solutionRoutes);
        canvasPane.draw();
        log("New problem created");
    }

    private void clearProblem() {
        resetProblem();
        log("Problem cleared");
    }

    private void saveImage(Stage stage) {
        DirectoryChooser dirChooser = new DirectoryChooser();
        dirChooser.setTitle("Choose Folder to Save Image");
        File selectedDir = dirChooser.showDialog(stage);

        if (selectedDir != null) {
            String baseName = "solution";
            String extension = ".png";
            int index = 1;
            File imageFile;
            do {
                imageFile = new File(selectedDir, baseName + "_" + index + extension);
                index++;
            } while (imageFile.exists());

            canvasPane.saveSnapshot(imageFile.getName().replace(".png", ""));
            log("Solution saved as image: " + imageFile.getName());
        }
    }
    
    /*
    private void loadCordeauDataset(Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Load Cordeau MDVRPTW Dataset");
        File file = fileChooser.showOpenDialog(stage);

        if (file != null) {
            try {
                resetProblem();
                parseCordeauFile(file);

                canvasPane.setDepots(depots);
                canvasPane.setCustomers(customers);
                canvasPane.draw();

                solver.setDepots(depots);
                solver.setCustomers(customers);
                solver.solve();

                log("Loaded Cordeau dataset: " + file.getName());
                log("Depots: " + depots.size() + ", Customers: " + customers.size());
            } catch (IOException e) {
                log("Error loading dataset: " + e.getMessage());
            }
        }
    }
	*/
    
    private void loadDataset(Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Load MDVRPTW Dataset");
        File file = fileChooser.showOpenDialog(stage);

        if (file != null) {
            resetProblem(); // clear any existing data
            boolean loaded = false;

            // Try loading with DataLoader (for simple/structured text files)
            try {
                DataLoader loader = new DataLoader();
                loader.loadData(file.getAbsolutePath());
                depots = loader.depots;
                customers = loader.customers;
                vehicleCapacity = loader.vehicleCapacity;
                loaded = true;
                log("Successfully loaded using DataLoader.");
            } catch (Exception e) {
                log("DataLoader failed: " + e.getMessage());
            }

            // Try fallback Cordeau-style parser
            if (!loaded) {
                try {
                    parseCordeauFile(file);
                    loaded = true;
                    log("Successfully loaded using parseCordeauFile.");
                } catch (IOException e) {
                    log("Cordeau parser failed: " + e.getMessage());
                }
            }

            if (loaded) {
                canvasPane.setDepots(depots);
                canvasPane.setCustomers(customers);
                canvasPane.draw();

                solver.setDepots(depots);
                solver.setCustomers(customers);
                solver.solve();

                log("Loaded dataset: " + file.getName());
                log("Depots: " + depots.size() + ", Customers: " + customers.size());
            } else {
                log("Failed to load dataset: unsupported or malformed file.");
            }
        }
    }



    /*
    private void parseCordeauFile(File file) throws IOException {
        customers.clear();
        depots.clear();
        try (Scanner scanner = new Scanner(file)) {
            if (!scanner.hasNextLine()) throw new IOException("Empty file or missing header line.");
            String headerLine = scanner.nextLine();
            String[] tokens = headerLine.trim().split("\\s+");
            if (tokens.length < 4) throw new IOException("Header must contain at least 4 values: vehicles, depots, customers, capacity.");

            int totalVehicles = Integer.parseInt(tokens[0]);
            int depotCount = Integer.parseInt(tokens[1]);
            int customerCount = Integer.parseInt(tokens[2]);
            vehicleCapacity = Integer.parseInt(tokens[3]);

            for (int i = 0; i < depotCount; i++) scanner.nextLine(); // skip depot time window lines

            for (int i = 0; i < customerCount; i++) {
                if (!scanner.hasNextLine()) throw new IOException("Missing customer data at line " + (i + 2 + depotCount));
                String line = scanner.nextLine();
                tokens = line.trim().split("\\s+");
                if (tokens.length < 7) throw new IOException("Malformed customer line: " + line);

                int id = Integer.parseInt(tokens[0]);
                double x = Double.parseDouble(tokens[1]);
                double y = Double.parseDouble(tokens[2]);
                double serviceTime = Double.parseDouble(tokens[3]);
                int demand = Integer.parseInt(tokens[4]);
                double readyTime = Double.parseDouble(tokens[5]);
                double dueTime = Double.parseDouble(tokens[6]);

                customers.add(new Customer(x, y, "C" + id, demand, readyTime, dueTime, serviceTime));
            }

            for (int i = 0; i < depotCount; i++) {
                if (!scanner.hasNextLine()) throw new IOException("Missing depot location at line " + (i + 2 + depotCount + customerCount));
                String line = scanner.nextLine();
                tokens = line.trim().split("\\s+");
                if (tokens.length < 2) throw new IOException("Malformed depot line: " + line);

                double x = Double.parseDouble(tokens[0]);
                double y = Double.parseDouble(tokens[1]);
                Depot depot = new Depot(x, y, "D" + (i + 1));
                depot.maxVehicles = totalVehicles;
                depot.vehicleCapacity = vehicleCapacity;
                depots.add(depot);
            }
        }
        log("Vehicle capacity set to: " + vehicleCapacity);
    }
	*/
    
    private void parseCordeauFile(File file) throws IOException {
        customers.clear();
        depots.clear();

        try (Scanner scanner = new Scanner(file)) {
            if (!scanner.hasNextLine()) throw new IOException("File is empty or missing header line.");

            String headerLine = scanner.nextLine().trim();
            String[] headerTokens = headerLine.split("\\s+");

            if (headerTokens.length < 4)
                throw new IOException("Header must include: [totalVehicles depots customers vehicleCapacity]");

            int totalVehicles = Integer.parseInt(headerTokens[0]);
            int depotCount = Integer.parseInt(headerTokens[1]);
            int customerCount = Integer.parseInt(headerTokens[2]);
            vehicleCapacity = Integer.parseInt(headerTokens[3]);

            // Skip depot time window lines (usually 1 per depot)
            for (int i = 0; i < depotCount; i++) {
                if (!scanner.hasNextLine())
                    throw new IOException("Missing depot time window line at line " + (i + 2));
                scanner.nextLine();
            }

            // Load customers
            for (int i = 0; i < customerCount; i++) {
                if (!scanner.hasNextLine())
                    throw new IOException("Missing customer data at line " + (i + 2 + depotCount));

                String line = scanner.nextLine().trim();
                String[] tokens = line.split("\\s+");

                if (tokens.length < 5)
                    throw new IOException("Malformed customer line (must contain at least 5 values): " + line);

                int id = Integer.parseInt(tokens[0]);
                double x = Double.parseDouble(tokens[1]);
                double y = Double.parseDouble(tokens[2]);
                double serviceTime = Double.parseDouble(tokens[3]);
                int demand = Integer.parseInt(tokens[4]);

                double readyTime = tokens.length > 5 ? Double.parseDouble(tokens[5]) : 0;
                double dueTime = tokens.length > 6 ? Double.parseDouble(tokens[6]) : 1000;

                customers.add(new Customer(x, y, "C" + id, demand, readyTime, dueTime, serviceTime));
            }

            // Load depot coordinates
            for (int i = 0; i < depotCount; i++) {
                if (!scanner.hasNextLine())
                    throw new IOException("Missing depot coordinate at line " + (i + 2 + depotCount + customerCount));

                String line = scanner.nextLine().trim();
                String[] tokens = line.split("\\s+");

                if (tokens.length < 2)
                    throw new IOException("Malformed depot line (expected x y): " + line);

                double x = Double.parseDouble(tokens[0]);
                double y = Double.parseDouble(tokens[1]);

                Depot depot = new Depot(x, y, "D" + (i + 1));
                depot.maxVehicles = totalVehicles;
                depot.vehicleCapacity = vehicleCapacity;

                depots.add(depot);
            }

            log("Cordeau-compatible file loaded: " + file.getName());
            log("Vehicle capacity: " + vehicleCapacity);
            log("Depots: " + depots.size() + ", Customers: " + customers.size());

        } catch (NumberFormatException e) {
            throw new IOException("Invalid number format in file: " + e.getMessage(), e);
        }
    }


 
	
    public void log(String message) {
        Platform.runLater(() -> logArea.appendText(message + "\n"));
    }

    public void drawSolution(Solution sol) {
        if (canvasPane != null && sol != null && sol.routes != null) {
            Platform.runLater(() -> {
                canvasPane.setSolutionRoutes(sol.routes);
                canvasPane.draw();
            });
        }
    }

    public void animateSolution(Solution solution, int generation) {
        if (canvasPane != null) {
            Platform.runLater(() -> {
                canvasPane.setSolutionRoutes(solution.routes);
                canvasPane.drawWithGenerationOverlay(solution.routes, generation);
            });
        }
    }

    private void runAlgorithm() {
        if (depots.isEmpty() || customers.isEmpty()) {
            log("Error: Add at least one depot and one customer");
            return;
        }
        log("Starting Differential Evolution for MDVRP with Time Windows...");
        log("Population: " + populationSize + ", Generations: " + maxGenerations);
        log("Crossover Rate: " + crossoverRate + ", Scaling Factor: " + scalingFactor);
        log("Vehicle Capacity: " + vehicleCapacity + ", Time Window Penalty: " + penaltyWeight);

        solutionRoutes.clear();
        convergenceSeries.getData().clear();
        canvasPane.draw();

        new Thread(() -> {
            try {
                String runId = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
                DifferentialEvolution de = new DifferentialEvolution(
                        this, populationSize, scalingFactor, crossoverRate, maxGenerations
                );
                de.setConvergenceSeries(convergenceSeries);

                Solution bestSolution = de.run();

                if (enableRelocationLocalSearch) {
                    log("Applying local search (route relocation)...");
                    routeRelocationLocalSearch(bestSolution);
                }

                SolutionMetrics.saveRunToCSV(
                        bestSolution, "DifferentialEvolution", populationSize, maxGenerations,
                        scalingFactor, crossoverRate, runId
                );

                Platform.runLater(() -> {
                    solutionRoutes = bestSolution.routes;
                    canvasPane.setSolutionRoutes(solutionRoutes);
                    canvasPane.draw();
                    canvasPane.saveSnapshot("final_" + runId);

                    double totalDistance = bestSolution.totalDistance;
                    double totalPenalty = bestSolution.totalPenalty;
                    int numRoutes = solutionRoutes.size();
                    int customersServed = solutionRoutes.stream().mapToInt(r -> r.customers.size()).sum();
                    int timeWindowViolations = solutionRoutes.stream().mapToInt(r -> r.timeWindowViolations).sum();

                    statsLabel.setText(String.format(
                            "Solution Stats: Distance=%.2f | Penalties=%.2f | Total Cost=%.2f | Routes=%d",
                            totalDistance, totalPenalty, bestFitness, numRoutes
                    ));
                    timeWindowStatsLabel.setText(String.format("Time window violations: %d", timeWindowViolations));
                    log("  Optimization completed! Total cost: " + String.format("%.2f", bestFitness));
                    log("  - Total distance: " + String.format("%.2f", totalDistance));
                    log("  - Time window penalties: " + String.format("%.2f", totalPenalty));
                    log("  - Routes: " + numRoutes + ", Customers served: " + customersServed + "/" + customers.size());
                    log("  - Time window violations: " + timeWindowViolations);
                });

            } catch (Exception e) {
                log(" Error during optimization: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }

    // --- Local Search: Route Relocation ---
    private void routeRelocationLocalSearch(Solution solution) {
        boolean improved = true;
        int moves = 0;
        while (improved) {
            improved = false;
            outer:
            for (int fromRouteIdx = 0; fromRouteIdx < solution.routes.size(); fromRouteIdx++) {
                Route fromRoute = solution.routes.get(fromRouteIdx);
                for (int custIdx = 0; custIdx < fromRoute.customers.size(); custIdx++) {
                    Customer customerToMove = fromRoute.customers.get(custIdx);
                    for (int toRouteIdx = 0; toRouteIdx < solution.routes.size(); toRouteIdx++) {
                        Route toRoute = solution.routes.get(toRouteIdx);
                        for (int insertPos = 0; insertPos <= toRoute.customers.size(); insertPos++) {
                            if (fromRoute == toRoute && (insertPos == custIdx || insertPos == custIdx + 1)) continue;
                            Solution trialSol = cloneSolution(solution);
                            Route trialFromRoute = trialSol.routes.get(fromRouteIdx);
                            Route trialToRoute = trialSol.routes.get(toRouteIdx);

                            Customer moved = trialFromRoute.customers.remove(custIdx);
                            if (trialFromRoute == trialToRoute && custIdx < insertPos) {
                                trialToRoute.customers.add(insertPos - 1, moved);
                            } else {
                                trialToRoute.customers.add(insertPos, moved);
                            }
                            evaluateSolution(trialSol);
                            if (trialSol.fitness < solution.fitness) {
                                solution.routes = trialSol.routes;
                                solution.fitness = trialSol.fitness;
                                solution.totalDistance = trialSol.totalDistance;
                                solution.totalPenalty = trialSol.totalPenalty;
                                improved = true;
                                moves++;
                                break outer;
                            }
                        }
                    }
                }
            }
        }
        log("Local search completed with " + moves + " improvements");
    }

    private Solution cloneSolution(Solution sol) {
        return new Solution(sol); // Uses Solution's copy constructor
    }

    // Evaluate a solution (sets routes, fitness, and penalty)
    void evaluateSolution(Solution solution) {
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

    // Decode chromosome into actual routes
    List<Route> decodeSolution(Solution solution) {
        List<Route> routes = new ArrayList<>();
        int colorIndex = 0;

        for (Customer customer : customers) {
            if (customer.assignedDepotId <= 0) {
                Depot nearest = findNearestDepot(customer);
                customer.assignedDepotId = depots.indexOf(nearest) + 1;
            }
        }

        Map<Depot, List<Customer>> depotCustomers = new HashMap<>();
        for (Depot depot : depots) depotCustomers.put(depot, new ArrayList<>());

        for (Customer customer : customers) {
            int depotIdx = customer.assignedDepotId - 1;
            if (depotIdx < 0 || depotIdx >= depots.size()) continue;
            Depot depot = depots.get(depotIdx);
            depotCustomers.get(depot).add(customer);
        }

        for (Depot depot : depots) {
            List<Customer> depotCustList = depotCustomers.get(depot);
            if (depotCustList.isEmpty()) continue;

            List<Customer> sortedCustomers = new ArrayList<>();
            for (int gene : solution.chromosome) {
                if (gene < 0 || gene >= customers.size()) continue;
                Customer cust = customers.get(gene);
                if (depotCustList.contains(cust) && !sortedCustomers.contains(cust))
                    sortedCustomers.add(cust);
            }
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

    private double distance(Customer prev, Depot depot) {
		// TODO Auto-generated method stub
		return 0;
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
    
    // --- LNS Implementation ---
    public void applyLNS(Solution solution) {
        double destroyFraction = 0.2; // Remove 20% of customers
        int numCustomers = customers.size();
        int numToRemove = Math.max(1, (int)(destroyFraction * numCustomers));

        // 1. Select random customers to remove
        Set<Customer> toRemove = new HashSet<>();
        List<Customer> shuffledCustomers = new ArrayList<>(customers);
        Collections.shuffle(shuffledCustomers, new Random());
        for (int i = 0; i < numToRemove; i++) {
            toRemove.add(shuffledCustomers.get(i));
        }

        // 2. Remove from routes
        for (Route route : solution.routes) {
            route.customers.removeIf(toRemove::contains);
        }

        // 3. Reinsert each removed customer
        for (Customer customer : toRemove) {
            double bestDelta = Double.POSITIVE_INFINITY;
            Route bestRoute = null;
            int bestPos = -1;

            for (Route route : solution.routes) {
                for (int pos = 0; pos <= route.customers.size(); pos++) {
                    // Try inserting customer at pos
                    route.customers.add(pos, customer);
                    double oldDistance = route.computeTotalDistance();
                    boolean feasible = checkTimeWindowFeasibility(route, vehicleCapacity);
                    double newDistance = route.computeTotalDistance();
                    double delta = newDistance - oldDistance;
                    route.customers.remove(pos);

                    if (feasible && delta < bestDelta) {
                        bestDelta = delta;
                        bestRoute = route;
                        bestPos = pos;
                    }
                }
            }
            // If found a feasible insertion, insert it
            if (bestRoute != null && bestPos != -1) {
                bestRoute.customers.add(bestPos, customer);
            } else {
                // If not feasible anywhere, insert in shortest route at end
                Route minRoute = solution.routes.stream()
                        .min(Comparator.comparingInt(r -> r.customers.size()))
                        .orElse(solution.routes.get(0));
                minRoute.customers.add(customer);
            }
        }

        // After LNS, update routes
        for (Route route : solution.routes) {
            evaluateRoute(route);
        }
    }

    // Checks time window feasibility for a route and capacity
    public boolean checkTimeWindowFeasibility(Route route, int vehicleCapacity) {
        double currentTime = 0.0;
        int load = 0;
        Depot depot = route.depot;
        if (route.customers.isEmpty()) return true;
        Customer first = route.customers.get(0);

        currentTime += depot.distanceTo(first);
        if (currentTime < first.readyTime) currentTime = first.readyTime;
        else if (currentTime > first.dueTime) return false;
        currentTime += first.serviceTime;
        load += first.demand;

        Customer prev = first;
        for (int i = 1; i < route.customers.size(); i++) {
            Customer current = route.customers.get(i);
            currentTime += prev.distanceTo(current);
            if (currentTime < current.readyTime) currentTime = current.readyTime;
            else if (currentTime > current.dueTime) return false;
            currentTime += current.serviceTime;
            load += current.demand;
            prev = current;
        }
        // Final trip to depot
        currentTime += prev.distanceTo(depot);
        return load <= vehicleCapacity;
    }

	public Solution createRandomSolution() {
		// TODO Auto-generated method stub
		return null;
	}
}