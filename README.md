# MDVRP-TW-DE

A JavaFX/Java application implementing a Hybrid Adaptive Differential Evolution algorithm for the **Multi-Depot Vehicle Routing Problem with Time Windows (MDVRPTW)**, extended with Large Neighborhood Search (LNS) reinsertion, Tabu memory, an adaptive time-window penalty, and route-relocation local search.

This repository accompanies a manuscript restructured as a **controlled ablation study across four Differential Evolution variants**, submitted to the *Malaysian Journal of Science and Advanced Technology (MJSAT)* and currently under revision (previously submitted to the *Malaysian Journal of Computing*, retracted and resubmitted).

The headline finding: **LNS drives most of the performance gain, while the adaptive penalty mechanism consistently makes results worse.** This is reported as an intentional negative result, not a limitation to be downplayed, see [Ablation Results](#ablation-results) below.

---

## Ablation Results

Five configurations are supported by the headless harness, isolating the contribution of each component:

- `DE` — baseline Differential Evolution only
- `DE+LNS` — adds Large Neighborhood Search reinsertion
- `DE+Tabu` — adds Tabu memory
- `DE+LNS+Tabu` — adds both
- `DE+LNS+Tabu+Adaptive` — adds an adaptive time-window penalty on top of all of the above

Across these configurations, LNS reinsertion is the primary driver of solution-quality improvement, while adding the adaptive time-window penalty consistently makes results worse rather than better. Full per-configuration results, the instance set, and statistical treatment are reported in the manuscript.

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
- Supports the five ablation configurations listed above
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

### GUI Mode (recommended for exploration)

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

### Headless Mode (batch experiments)

Compile as above, then run `HeadlessHarness` in place of `MDVRPTWSolver`. See `HeadlessHarness.java` and `HeadlessSolverContext.java` for the available configuration options (ablation variant selection, dataset path, seeding, and output location).

---

## Citation

This work is currently under revision at the *Malaysian Journal of Science and Advanced Technology (MJSAT)*. A full citation, including volume, issue, and DOI, will be added here once the manuscript is accepted.

A citable, versioned archive of this repository will be made available via Zenodo upon acceptance.

---

## License

MIT License.

---

## Contact

**Farid Morsidi**
farid.mors90@gmail.com
Computing Department, Faculty of Computing & Meta-Technology
Universiti Pendidikan Sultan Idris, 35900 Tanjong Malim, Perak, Malaysia
