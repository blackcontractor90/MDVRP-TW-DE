# MDVRP-TW-DE

A JavaFX/Java solver for the **Multi-Depot Vehicle Routing Problem with Time Windows (MDVRPTW)**, implementing Differential Evolution (DE) with three independently toggleable constraint-handling mechanisms: an adaptive penalty system, Large Neighborhood Search (LNS) reinsertion, and Tabu memory.

This repository accompanies the accepted manuscript:

> Morsidi, F. (2026). An Ablation Study of Constraint-Handling Mechanisms in Differential Evolution for Multi-Depot Vehicle Routing with Time Windows. *Malaysian Journal of Science and Advanced Technology*. https://doi.org/10.56532/mjsat.v3i3.163

**Headline finding:** Large Neighborhood Search is the dominant contributor to cost reduction (35–45% lower mean route distance than baseline DE). Tabu memory contributes little on its own. The adaptive penalty mechanism, the component this study originally hypothesized would help, **substantially worsens both route cost and time-window feasibility** when layered on top of LNS and Tabu. This is reported as an intentional negative result: isolating each mechanism's individual contribution, rather than reporting only a single bundled hybrid, is the paper's main methodological point.

---

## Ablation Results

Five configurations were compared on four MDVRPTW benchmark instances from Vidal et al. (2013) — `pr11a`, `pr11b` (360 customers each), `pr12a`, `pr12b` (480 customers each) — with 30 independent repetitions per configuration per instance (150 runs per instance, 600 runs total).

### Mean route distance (± SD), averaged across the four instances

| Configuration | Mean Distance | Change vs. baseline DE |
|---|---|---|
| DE (baseline) | 27,496.3 | — |
| DE+LNS | 16,765.7 | **−39.0%** |
| DE+Tabu | 27,471.4 | −0.1% (no meaningful effect) |
| DE+LNS+Tabu | 16,852.0 | −38.7% (≈ same as LNS alone) |
| DE+LNS+Tabu+Adaptive | 20,712.4 | −24.7% (worse than DE+LNS+Tabu by +22.9%) |

Full per-instance distance, penalized fitness, and on-time service rate are in the manuscript's Tables 1–3.

### Statistical tests

- **Friedman test** across all five configurations: χ² = 14.80, *df* = 4, *p* = 0.0051 — statistically significant difference in configuration ranking. DE+LNS (mean rank 1.25) and DE+LNS+Tabu (1.75) ranked consistently best; DE (4.75) and DE+Tabu (4.25) consistently worst.
- **Pairwise exact Wilcoxon tests** (LNS-containing configurations vs. baseline; DE+LNS+Tabu vs. the adaptive hybrid) were directionally unanimous across all four instances, but with only four paired observations the smallest attainable two-sided *p*-value is 0.125. These pairwise results are reported as *consistent* rather than independently significant — the Friedman test is the basis for the significance claim.
- **Adding the adaptive penalty** to DE+LNS+Tabu increased mean fitness by 74.2–90.6% across the four instances while simultaneously *reducing* on-time service rate (e.g. 77.4% → 62.1% on `pr12b`) — the mechanism degrades cost and feasibility together, rather than trading one for the other.
- **No configuration achieved a fully feasible solution** (zero time-window violations) on any instance, across all 600 runs.

---

## Constraint-Handling Mechanisms

- **Adaptive penalty system.** Adjusted every 10 generations based on the population's feasibility ratio (target 0.5): penalty weight ×1.05 if feasibility falls below target, ×0.8 if above, clamped to [1000, 100000].
- **Large Neighborhood Search (LNS).** Applied to every trial solution each generation, plus a refinement pass on the incumbent best every 30 generations. Destroys `min(25, max(3, ⌊0.4×n⌋))` customers at random and greedily reinserts each at its lowest-cost position within its (fixed) depot's route.
- **Tabu memory.** A fixed-size list (15 entries, FIFO) of complete chromosomes previously found to be the incumbent best. A candidate is tabu only on an exact chromosome match; no aspiration criterion is implemented.

DE itself uses NP=50, 200 generations, self-adaptive F/CR (initialized at F=0.8, CR=0.9, adjusted per generation via recorded successful trial values), order-crossover (OX), and swap-based mutation. See the manuscript (Section 3) for full algorithmic detail and pseudocode.

---

## Features

### Core Algorithm
- Differential Evolution (DE) with self-adaptive F/CR control
- Three independently toggleable constraint-handling components (see above), enabling the ablation configurations `DE`, `DE+LNS`, `DE+Tabu`, `DE+LNS+Tabu`, `DE+LNS+Tabu+Adaptive`
- Fitness = total route distance + penalty weight × time-window violations

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

### Headless Mode (reproducing the ablation study)

Compile as above, then run `HeadlessHarness` in place of `MDVRPTWSolver`. See `HeadlessHarness.java` and `HeadlessSolverContext.java` for selecting an ablation configuration, dataset instance, and output location. The manuscript's results used the `pr11a`, `pr11b`, `pr12a`, `pr12b` instances from Vidal et al. (2013), 30 repetitions per configuration per instance.

### Dataset

Vidal et al. (2013) MDVRPTW benchmark instances (`pr11a`, `pr11b`, `pr12a`, `pr12b`), Cordeau-style format. Best-known solutions for these instances are maintained in the [PyVRP/Instances repository](https://github.com/PyVRP/VRPLIB); this study reports mechanism-isolation results under a fixed 200-generation budget rather than a percentage-gap comparison to best-known solutions, as the two are not directly comparable (see manuscript Section 4.4).

---

## Citation

If you use this code, please cite:

```bibtex
@article{morsidi2026ablation,
  author  = {Morsidi, Farid},
  title   = {An Ablation Study of Constraint-Handling Mechanisms in Differential Evolution for Multi-Depot Vehicle Routing with Time Windows},
  journal = {Malaysian Journal of Science and Advanced Technology},
  year    = {2026},
  doi     = {10.56532/mjsat.v3i3.163}
}
```

A citable, versioned archive of this repository is available via Zenodo:
https://doi.org/10.5281/zenodo.22933165

---

## License

MIT License.

---

## Contact

**Farid Morsidi**
farid.mors90@gmail.com
Computing Department, Faculty of Computing & Meta-Technology
Universiti Pendidikan Sultan Idris, 35900 Tanjong Malim, Perak, Malaysia
