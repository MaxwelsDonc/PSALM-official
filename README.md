# PSALM

This repository contains the experimental code for the paper:

**PSALM: applying Proportional SAmpLing strategy in Metamorphic Testing**

The code is research-oriented and intended to support the experiments reported in the paper.

---

## What this repository is for

- Implementing **PSALM** (a proportional sampling strategy) and several baselines (Random, ART, MT-ART).
- Running **metamorphic testing experiments** on multiple Java and Python subject programs.
- Comparing PSALM with baseline strategies under metamorphic testing.
- Producing experimental results, tables and figures for **RQ1–RQ3** in the paper.
---


## High-level structure

- `java/`  
  Java subject programs, metamorphic relations, generators, mutant integration, and experiment drivers.

- `python/`  
  Python subject programs with the same experimental structure as the Java side.

- `RQ1/`, `RQ2/`, `RQ3/`  
  Analysis scripts that aggregate raw results and generate tables/figures for the paper.

---

## Subjects

- **Java subjects**  
  `jackson_project`, `jfreeChart_project`, `lang_project`, `math1_project`, `math2_project`

- **Python subjects**  
  `geometricSum`, `incomeTax`, `mortgageRate`

Each subject follows a similar layout:

- `generation/phase1`, `generation/phase2` – test or MG sampling strategies  
- `metamorphicRelations/` – subject-specific MRs  
- `model/` – test case and MR abstractions  
- `mutants/` – externally generated mutants  
- `raw_results/` – experiment drivers and JSON outputs  
- `mutants_analysis/` – optional mutant classification and subsumption analysis

---

## Experimental workflow

1. **Prepare mutants externally**  
   Generate mutants using external tools ([`mutmut`](https://github.com/boxed/mutmut) and [`Major`](https://mutation-testing.org/)) and place them into the expected directory structure.

2. **Run per-subject experiments**  
   Use subject-specific raw result scripts (e.g., `get_raw_results_*.java` or `get_raw_results_*.py`) to run PSALM and baseline strategies.

3. **Aggregate results**  
   Run scripts under `RQ1/`, `RQ2/`, and `RQ3/` to compute statistics and generate tables/figures.

---
## Quick start

- Java experiments: run `java/<subject>/raw_results/get_raw_results_*.java`.
- Python experiments: run `python/<subject>/raw_results/get_raw_results_*.py`.
- Mutants must be generated externally before running experiments.
---

## Notes on usage

- This is **research code** meant to accompany the paper.
- Reproducing results requires:
  - Reading the paper for experimental settings.
  - Preparing mutants in advance.
  - Adjusting paths and configurations to your environment.
- Code-level understanding is expected.
- The mutants files are not included in this repository due to their size. Please generate them using the external tools.
---

<!-- ## Citation

If you use this code in academic work, please cite the paper:

**PSALM: applying Proportional SAmpLing strategy in Metamorphic Testing**

(Use the official publication details.) -->
