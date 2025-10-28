# MDVRP-TW Solver (MDVRP-TW-DE)

Multi-Depot Vehicle Routing Problem with Time Windows (MDVRPTW) — a JavaFX GUI application that uses Differential Evolution (DE) with local search and LNS operators to find route plans. The tool visualizes depots, customers, routes and time windows, plots convergence, and supports Cordeau-style datasets. It can export routes to CSV and save solution snapshots.

## Key features
- Differential Evolution-based metaheuristic for MDVRPTW
- Local search: route relocation operator
- Large Neighborhood Search (LNS) reinsertion operator
- JavaFX interactive GUI with:
  - Canvas visualization of depots, customers and routes
  - Toggleable labels and time window display
  - Convergence chart (fitness per generation)
  - Run/export controls (CSV, PNG snapshots)
- Supports Cordeau-style dataset parsing and a fallback DataLoader

## Repository expectations (required supporting classes)
MDVRPTWSolver.java references many classes and utilities that must exist in the project for the application to compile and run. Make sure the following classes are implemented and available in the same package or adjusted package structure:

- CanvasPane — drawing, snapshot, and add-mode handling (setDepots, setCustomers, setSolutionRoutes, draw, drawWithGenerationOverlay, saveSnapshot, setShowDepotLabels(), setShowCustomerLabels(), setShowRouteLabels(), setShowTimeWindows(), setAddDepotMode(), setAddCustomerMode()).
- Depot — fields x, y, name/id, maxVehicles, vehicleCapacity and helper method distanceTo(Customer).
- Customer — fields x, y, id/name label, demand, readyTime, dueTime, serviceTime, assignedDepotId.
- Route — fields depot, List<Customer> customers, double distance, double penalty, int timeWindowViolations, Color color; methods computeTotalDistance(), distanceTo(depot/customer) or helpers as needed.
- Solution — stores chromosome (int[]), decoded routes (List<Route>), fitness/distance/penalty/timeWindowViolations, and a copy constructor.
- DifferentialEvolution — the DE implementation used by MDVRPTWSolver (constructor with parameters: solver reference, populationSize, scalingFactor, crossoverRate, maxGenerations), run() returning a Solution, and setConvergenceSeries(XYChart.Series).
- DataLoader — optional structured data loader with fields `depots`, `customers`, and `vehicleCapacity`.
- RouteExporter — exports routes to CSV (exportToCSV method).
- SolutionMetrics — saveRunToCSV(...) for recording run metadata and results.
- MetricsAnalyzer / MetricsChartViewer — optional utilities for analyzing and plotting saved metrics.

## Requirements
- Java 11+ (Java 17+ recommended)
- JavaFX (matching your JDK version). If using JDK without bundled JavaFX, add OpenJFX SDK or use Maven/Gradle dependencies (org.openjfx).
- Build tool (recommended): Maven or Gradle for dependency and runtime configuration.

## Build & run

### In an IDE (recommended)
1. Import project into IntelliJ IDEA / Eclipse / NetBeans.
2. Add JavaFX SDK as a library or configure the build tool (Maven/Gradle).
3. Ensure the main class is `MDVRPTWSolver`.
4. Run the application.

### From command line (example)
1. Download JavaFX SDK and set JAVAFX_LIB to its lib directory.
2. Compile:
   javac --module-path $JAVAFX_LIB --add-modules javafx.controls,javafx.fxml -d out $(find src -name '*.java')
3. Run:
   java --module-path $JAVAFX_LIB --add-modules javafx.controls,javafx.fxml -cp out MDVRPTWSolver

Note: exact commands depend on your project layout. Using Maven/Gradle is recommended to avoid manual module path setup.

## User interface & usage
- Menu > File:
  - New Problem — clear current data
  - Load MDVRPTW Dataset — opens file chooser to load dataset (Cordeau-style or DataLoader)
  - Export Routes to CSV — save current routes
  - Save Solution as Image — save PNG snapshot of the canvas
- Menu > Edit:
  - Add Depot / Add Customer — switch to canvas add mode, then click to place
  - Clear All — clear everything
- Menu > View:
  - Toggle depot/customer/route/time-window labels
- Control panel:
  - Algorithm parameters: population size, crossover rate, scaling factor, max generations, vehicle capacity, time-window penalty
  - Checkbox to enable route-relocation local search
  - Run Differential Evolution — start optimization (background thread)
  - Analyze Metrics Summary — open saved metrics chart viewer (if available)

## Dataset format (Cordeau-compatible)
MDVRPTWSolver includes a Cordeau-style parser that expects a space-separated file with the following structure:

1. Header line:
   totalVehicles depots customers vehicleCapacity

2. Next `depotCount` lines: (skipped/time-window lines for depots in the parser)

3. Next `customerCount` lines: each customer line must contain at least:
   id x y serviceTime demand [readyTime] [dueTime]
   - readyTime and dueTime are optional; defaults used if missing (0 and large value).

4. Next `depotCount` lines: depot coordinates:
   x y

If you prefer simpler formats, implement DataLoader to accept JSON/CSV/formatted text and set MDVRPTWSolver to use it.

## Suggestions for improvements
- Add Maven/Gradle build file to manage JavaFX dependencies and enable easy packaging (fat JAR).
- Provide a headless CLI mode for batch experiments (no JavaFX).
- Add sample datasets (Cordeau instances) under `data/` and include test cases.
- Add logging (slf4j) with a log file output for long runs.
- Add automated tests and a CI pipeline.

## Contact
Maintainer: blackcontractor90 (GitHub)
---
