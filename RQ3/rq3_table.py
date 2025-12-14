import pandas as pd
from decimal import Decimal, ROUND_HALF_UP
from pathlib import Path


# ===== 基础格式化函数 =====
def round_half_up(x, ndigits):
    if pd.isna(x):
        return None
    q = Decimal(10) ** -ndigits
    return float(Decimal(str(x)).quantize(q, rounding=ROUND_HALF_UP))


def fmt_p(p):
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


# ===== 通用处理函数 =====
def extract_summary_from_excel(file_path, col_baseline_name):
    """读取 Excel 文件，提取 SUMMARY 行并格式化"""
    xls = pd.ExcelFile(file_path)
    rows = []

    for sheet in xls.sheet_names:
        df = pd.read_excel(xls, sheet_name=sheet)
        df["mutant"] = df["mutant"].astype(str).str.strip().str.lower()

        summary = df[df["mutant"] == "summary"]
        if summary.empty:
            continue
        s = summary.iloc[0]

        rows.append(
            {
                "Program": sheet,
                "PSALM": s["PSALM"],
                col_baseline_name: s[col_baseline_name],
                "Improvement(%)": s["Improvement(%)"],
                "p": s["p(PSALMvsBaseline)"],
                "A12": s["A12(PSALMvsBaseline)"],
            }
        )

    return pd.DataFrame(rows)


def export_latex(df, caption, label, out_path):
    """将结果导出为 LaTeX 表格"""
    if df.empty:
        print(f"No summary rows found in {out_path}")
        return

    out = df.assign(
        **{
            r"$\overline{P}_{\mathrm{PSALM}}$": df["PSALM"].map(fmt4),
            r"$\overline{P}_{\mathrm{Base}}$": df.iloc[:, 2].map(fmt4),
            "Improvement (\\%)": df["Improvement(%)"].map(fmt2),
            "$p$": df["p"].map(fmt_p),
            "$A_{12}$": df["A12"].map(fmt3),
        }
    )[
        [
            "Program",
            r"$\overline{P}_{\mathrm{PSALM}}$",
            r"$\overline{P}_{\mathrm{Base}}$",
            "Improvement (\\%)",
            "$p$",
            "$A_{12}$",
        ]
    ]

    Path(out_path).parent.mkdir(parents=True, exist_ok=True)
    latex = out.to_latex(
        index=False, escape=False, caption=caption, label=label, column_format="lccccc"
    )

    with open(out_path, "w", encoding="utf-8") as f:
        f.write(latex)
    print(f"Done. Generated: {out_path}")


# ===== Phase 1: PSALM vs ART =====
file_phase1 = "RQ3/RQ3_phase1.xlsx"
df1 = extract_summary_from_excel(file_phase1, "ART")
export_latex(
    df1,
    caption="RQ3 (Phase 1): Comparison between PSALM and ART when selecting source test cases",
    label="tab:rq3_phase1",
    out_path="RQ3/tab_rq3_phase1.tex",
)

# ===== Phase 2: PSALM vs MT-ART =====
file_phase2 = "RQ3/RQ3_phase2.xlsx"
df2 = extract_summary_from_excel(file_phase2, "MT-ART")
export_latex(
    df2,
    caption="RQ3 (Phase 2): Comparison between PSALM and MT-ART when selecting MGs",
    label="tab:rq3_phase2",
    out_path="RQ3/tab_rq3_phase2.tex",
)
