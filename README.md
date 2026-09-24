# MDVRP-TW-DE

**Multi-Depot Vehicle Routing Problem with Time Windows (MDVRPTW)** solver using **Differential Evolution**, enhanced with Large Neighborhood Search (LNS), Tabu memory, adaptive penalty, and local search.

The project provides both:
- An interactive **JavaFX GUI** for visualization, parameter tuning, and single runs
- A **headless batch harness** for reproducible multi-run experiments and ablation studies

---

## Features

### Core Algorithm
- Differential Evolution (DE) metaheuristic tailored for MDVRPTW
- Optional components (toggleable for ablation studies):
  - Large Neighborhood Search (LNS) reinsertion
  - Tabu memory
  - Adaptive time-window penalty
  - Route relocation local search
- Adaptive F / CR parameter control
- Fitness = total distance + time-window penalties

### Graphical Interface (JavaFX)
- Interactive canvas visualization of depots, customers, routes, and time windows
- Toggleable labels
- Live convergence chart (best fitness per generation)
- Easy parameter controls (population size, F, CR, generations, capacity, penalty weight)
- Load Cordeau-style datasets
- Manually add depots / customers by clicking
- Export routes to CSV and save solution snapshots as PNG
- Metrics analysis and chart viewer

### Headless Mode
- `HeadlessHarness` – run large batches of experiments without GUI
- Supports multiple ablation configurations:
  - `DE`
  - `DE+LNS`
  - `DE+Tabu`
  - `DE+LNS+Tabu`
  - `DE+LNS+Tabu+Adaptive`
- Reproducible seeding
- Automatic CSV output of individual runs + summary statistics

---

## Requirements

- **Java 11+** (Java 17 recommended)
- **JavaFX** (must match your JDK version)
  - Use OpenJFX if your JDK does not include JavaFX
- No external build tool is currently required (plain `.java` files)

---

## Project Structure

All source files are in the root directory (flat structure):

| File | Description |
|------|-------------|
| `MDVRPTWSolver.java` | Main JavaFX application |
| `DifferentialEvolution.java` | Core DE + LNS + Tabu + Adaptive logic |
| `HeadlessHarness.java` | Batch experiment runner |
| `HeadlessSolverContext.java` | Lightweight context for headless runs |
| `Decoder.java` / `RoutingSolver.java` | Solution decoding & evaluation |
| `CanvasPane.java` | Visualization component |
| `DataLoader.java`, `RouteExporter.java`, `SolutionMetrics.java`, etc. | Supporting utilities |

---

## How to Run

### 1. GUI Mode (recommended for exploration)

**Using an IDE (IntelliJ / Eclipse / NetBeans):**
1. Import the project
2. Add the JavaFX SDK as a library (or configure via Maven/Gradle if you add one later)
3. Set the main class to `MDVRPTWSolver`
4. Run

**Command line example:**
```bash
# Set path to your JavaFX lib folder
export JAVAFX_LIB=/path/to/javafx-sdk/lib

# Compile
javac --module-path $JAVAFX_LIB --add-modules javafx.controls,javafx.fxml -d out *.java

# Run
java --module-path $JAVAFX_LIB --add-modules javafx.controls,javafx.fxml -cp out MDVRPTWSolver
