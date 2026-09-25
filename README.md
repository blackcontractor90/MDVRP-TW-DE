# MDVRP-TW-DE

A JavaFX/Java application implementing a Hybrid Adaptive Differential Evolution algorithm for the **Multi-Depot Vehicle Routing Problem with Time Windows (MDVRPTW)**, extended with Large Neighborhood Search (LNS) reinsertion, Tabu memory, an adaptive time-window penalty, and route-relocation local search.

This repository accompanies the manuscript [TODO: exact current title], submitted to the *Malaysian Journal of Science and Advanced Technology (MJSAT)* and currently under revision (previously submitted to the *Malaysian Journal of Computing*, retracted and resubmitted).

The manuscript restructures this work as a **controlled ablation study across four DE variants**, isolating the contribution of each component. The headline finding: **LNS drives most of the performance gain, while the adaptive penalty mechanism consistently makes results worse.** This is reported as an intentional negative result, not a limitation to be downplayed, see [Ablation Results](#ablation-results) below.

---

## Ablation Results

Four configurations were compared (see manuscript for full methodology and instance set):

| Configuration | What it adds | Effect on solution quality |
|---|---|---|
| `DE` | Baseline Differential Evolution only | Reference |
| `DE+LNS` | + Large Neighborhood Search reinsertion | **Primary driver of improvement** |
| `DE+Tabu` | + Tabu memory | [TODO: effect, e.g. marginal / neutral] |
| `DE+LNS+Tabu` | + both | [TODO: effect] |
| `DE+LNS+Tabu+Adaptive` | + adaptive time-window penalty | **Consistently worsens results relative to `DE+LNS+Tabu`** |

**Table 1.** Mean Route Distance ± SD (Best–Worst) Across 30 Repetitions

| Instance | Config | Mean | ± SD | Min | Max |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **pr11a** | DE | 23477.5 | 579.9 | 22705.0 | 25427.9 |
| | DE+LNS | 14991.5 | 307.5 | 14378.6 | 15568.5 |
| | DE+Tabu | 23441.7 | 497.9 | 22689.9 | 24339.3 |
| | DE+LNS+Tabu | 14865.5 | 470.2 | 13872.3 | 15591.2 |
| | +Adaptive | 17814.7 | 757.5 | 15825.1 | 19747.4 |
| **pr11b** | DE | 23436.8 | 568.8 | 21823.0 | 24550.0 |
| | DE+LNS | 12929.8 | 500.9 | 12054.9 | 13963.3 |
| | DE+Tabu | 23470.2 | 579.9 | 22014.7 | 24588.4 |
| | DE+LNS+Tabu | 13159.9 | 374.5 | 12387.1 | 13763.7 |
| | +Adaptive | 15761.4 | 679.4 | 14311.6 | 17192.6 |
| **pr12a** | DE | 31651.1 | 669.5 | 30280.4 | 32905.2 |
| | DE+LNS | 20556.1 | 479.5 | 19399.0 | 21311.8 |
| | DE+Tabu | 31645.9 | 597.0 | 30459.1 | 33161.3 |
| | DE+LNS+Tabu | 20745.9 | 565.7 | 19587.5 | 21702.8 |
| | +Adaptive | 25795.7 | 911.0 | 23885.9 | 27569.0 |
| **pr12b** | DE | 31419.7 | 679.0 | 30026.1 | 32805.8 |
| | DE+LNS | 18585.3 | 434.7 | 17443.8 | 19245.3 |
| | DE+Tabu | 31327.6 | 649.6 | 30463.7 | 32901.1 |
| | DE+LNS+Tabu | 18636.5 | 515.4 | 17418.5 | 19564.1 |
| | +Adaptive | 23477.8 | 1122.2 | 20817.4 | 25965.0 |




**Practical implication:** if you are using this code for your own routing experiments, `DE+LNS+Tabu` (without the adaptive penalty) is the configuration this study found most effective, not the full `DE+LNS+Tabu+Adaptive` combination its name might suggest is "most complete."

---

## Features

### Core Algorithm
- Differential Evolution (DE) metaheuristic tailored for MDVRPTW
- Optional components (toggleable for ablation studies):
  - Large Neighborhood Search (LNS) reinsertion
  - Tabu memory
  - Adaptive time-window penalty (see [Ablation Results](#ablation-results) — this component underperforms in our experiments)
  - Route relocation local search
- Adaptive F / CR parameter control
- Fitness = total distance + time-window penalties

### Graphical Interface (JavaFX)
- Interactive canvas visualization of depots, customers, routes, and time windows
- Toggleable labels
- Live convergence chart (best fitness per generation)
- Parameter controls (population size, F, CR, generations, capacity, penalty weight)
- Load Cordeau-style datasets
- Manually add depots / customers by clicking
- Export routes to CSV and save solution snapshots as PNG
- Metrics analysis and chart viewer

### Headless Mode
- `HeadlessHarness` — run large batches of experiments without the GUI
- Supports the five ablation configurations listed above via a single flag/config each
- Reproducible seeding
- Automatic CSV output of individual runs + summary statistics

---

## Requirements

- **Java 11+** (Java 17 recommended)
- **JavaFX** (must match your JDK version); use OpenJFX if your JDK does not bundle it
- No external build tool required (plain `.java` files, flat structure)

---

## Project Structure

All source files are in the root directory:

| File | Description |
|------|-------------|
| `MDVRPTWSolver.java` | Main JavaFX application |
| `DifferentialEvolution.java` | Core DE + LNS + Tabu + Adaptive logic |
| `HeadlessHarness.java` | Batch experiment runner (used to produce the manuscript's ablation results) |
| `HeadlessSolverContext.java` | Lightweight context for headless runs |
| `Decoder.java` / `RoutingSolver.java` | Solution decoding & evaluation |
| `CanvasPane.java` | Visualization component |
| `DataLoader.java`, `RouteExporter.java`, `SolutionMetrics.java`, etc. | Supporting utilities |

---

## How to Run

### 1. GUI Mode (recommended for exploration)

**Using an IDE (IntelliJ / Eclipse / NetBeans):**
1. Import the project.
2. Add the JavaFX SDK as a library.
3. Set the main class to `MDVRPTWSolver`.
4. Run.

**Command line:**
```bash
export JAVAFX_LIB=/path/to/javafx-sdk/lib

javac --module-path $JAVAFX_LIB --add-modules javafx.controls,javafx.fxml -d out *.java

java --module-path $JAVAFX_LIB --add-modules javafx.controls,javafx.fxml -cp out MDVRPTWSolver
```

### 2. Headless Mode (reproducing the manuscript's results)

```bash
javac --module-path $JAVAFX_LIB --add-modules javafx.controls,javafx.fxml -d out *.java

java --module-path $JAVAFX_LIB --add-modules javafx.controls,javafx.fxml -cp out HeadlessHarness 
```


---

## Dataset Format

Cordeau-style, same format as the AGTSP/hybridmemetic repos

---

## Citation

If you use this code, please cite the manuscript:

Morsidi, F. An Ablation Study of Constraint-Handling Mechanisms in Differential Evolution for Multi-Depot Vehicle Routing with Time Windows. *Malaysian Journal of Science and Advanced Technology (MJSAT)* (under revision).


A citable, versioned archive of this repository [TODO: will be / is] available via Zenodo:
https://doi.org/10.5281/zenodo.22933165

---

## License

MIT

---

## Contact

**Farid Morsidi**
farid.mors90@gmail.com
Computing Department, Faculty of Computing & Meta-Technology
Universiti Pendidikan Sultan Idris, 35900 Tanjong Malim, Perak, Malaysia
