import pandas as pd
from decimal import Decimal, ROUND_HALF_UP


# ========= 基础函数 =========
def round_half_up(x, ndigits):
    if pd.isna(x):
        return None
    q = Decimal(10) ** -ndigits
    return float(Decimal(str(x)).quantize(q, rounding=ROUND_HALF_UP))


def fmt_p(p):
    """p<0.05 显示 $<0.05$，避免 LaTeX 乱码"""
    if pd.isna(p):
        return "n/a"
    if p < 0.05:
        return r"$<0.05$"
    return rf"${round_half_up(p,3):.3f}$"


def fmt4(x):
    return f"{round_half_up(x,4):.4f}" if x is not None else "n/a"


def fmt3(x):
    return f"{round_half_up(x,3):.3f}" if x is not None else "n/a"


def fmt2(x):
    return f"{round_half_up(x,2):.2f}" if x is not None else "n/a"


# ========= 读取并提取 SUMMARY =========
file_path = "RQ1/RQ1.xlsx"  # 修改为你的实际路径
xls = pd.ExcelFile(file_path)
rows = []

for sheet in xls.sheet_names:
    df = pd.read_excel(xls, sheet_name=sheet)

    # 去除空格、大小写统一
    df["mutant"] = df["mutant"].astype(str).str.strip().str.lower()
    df["phase"] = df["phase"].astype(str).str.strip().str.lower()

    # 精确匹配 summary 行
    summary_rows_in_sheet = df[df["mutant"] == "summary"]
    if summary_rows_in_sheet.empty:
        continue

    # 遍历所有 SUMMARY 行（phase1 + phase2）
    for _, s in summary_rows_in_sheet.iterrows():
        part = s["partition"]
        rnd = s["random"]
        imp = (part - rnd) / rnd * 100 if rnd != 0 else float("nan")

        rows.append(
            {
                "Project": sheet.replace("_project", ""),
                "Phase": str(s["phase"]).strip().lower(),
                "P_PSALM": part,
                "P_RS": rnd,
                "Improvement(%)": imp,
                "p_value": s["p-value(partition vs random)"],
                "A12": s["A12(partition vs random)"],
            }
        )

all_df = pd.DataFrame(rows).sort_values(["Phase", "Project"]).reset_index(drop=True)

# ========= 生成按 phase 的 LaTeX 表格 =========
phase_map_caption = {
    "phase1": "Selecting source test cases",
    "phase2": "Selecting MGs",
}


def build_table_for_phase(df_phase, phase_key):
    if df_phase.empty:
        return None
    df_phase = df_phase.copy().sort_values("Project")
    out = df_phase.assign(
        **{
            r"$\overline{P}_{\mathrm{PSALM}}$": df_phase["P_PSALM"].map(fmt4),
            r"$\overline{P}_{\mathrm{RS}}$": df_phase["P_RS"].map(fmt4),
            "Improvement (\\%)": df_phase["Improvement(%)"].map(fmt2),
            "$p$": df_phase["p_value"].map(fmt_p),
            "$A_{12}$": df_phase["A12"].map(fmt3),
        }
    )[
        [
            "Project",
            r"$\overline{P}_{\mathrm{PSALM}}$",
            r"$\overline{P}_{\mathrm{RS}}$",
            "Improvement (\\%)",
            "$p$",
            "$A_{12}$",
        ]
    ]

    caption = (
        f"RQ1 ({phase_map_caption.get(phase_key, phase_key)}): "
        "Comparison between PSALM and RS across projects"
    )
    label = f"tab:RQ1_{phase_key}_results"

    latex = out.to_latex(
        index=False, escape=False, caption=caption, label=label, column_format="lccccc"
    )
    return latex


latex_p1 = build_table_for_phase(all_df[all_df["Phase"] == "phase1"], "phase1")
latex_p2 = build_table_for_phase(all_df[all_df["Phase"] == "phase2"], "phase2")

if latex_p1:
    with open("RQ1/RQ1_phase1_table.tex", "w", encoding="utf-8") as f:
        f.write(latex_p1)
if latex_p2:
    with open("RQ1/RQ1_phase2_table.tex", "w", encoding="utf-8") as f:
        f.write(latex_p2)

print(
    "Done. Generated:",
    "RQ1/RQ1_phase1_table.tex" if latex_p1 else "(no phase1)",
    "and",
    "RQ1/RQ1_phase2_table.tex" if latex_p2 else "(no phase2)",
)
