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


# ===== 读取并提取 SUMMARY 行 =====
file_path = "RQ2/RQ2.xlsx"  # 输入路径
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
            "Program": sheet.replace("_project", ""),
            "P_MG": s["MG"],
            "P_st": s["st"],
            "Improvement(%)": s["Improvement(%)"],
            "p": s["p(MGvsSrc)"],
            "A12": s["A12(MGvsSrc)"],
        }
    )

# ===== 汇总成 DataFrame =====
all_df = pd.DataFrame(rows).sort_values("Program").reset_index(drop=True)

# ===== 生成 LaTeX 表格 =====
if not all_df.empty:
    out = all_df.assign(
        **{
            r"$\overline{P}_{\mathrm{MG}}$": all_df["P_MG"].map(fmt4),
            r"$\overline{P}_{\mathrm{st}}$": all_df["P_st"].map(fmt4),
            "Improvement (\\%)": all_df["Improvement(%)"].map(fmt2),
            "$p$": all_df["p"].map(fmt_p),
            "$A_{12}$": all_df["A12"].map(fmt3),
        }
    )[
        [
            "Program",
            r"$\overline{P}_{\mathrm{MG}}$",
            r"$\overline{P}_{\mathrm{st}}$",
            "Improvement (\\%)",
            "$p$",
            "$A_{12}$",
        ]
    ]

    caption = (
        "RQ2: Comparison between PSALM applied to selecting source test cases "
        "and selecting MGs across projects"
    )
    label = "tab:rq2"

    Path("RQ2").mkdir(exist_ok=True, parents=True)
    latex = out.to_latex(
        index=False, escape=False, caption=caption, label=label, column_format="lccccc"
    )

    with open("RQ2/tab_rq2.tex", "w", encoding="utf-8") as f:
        f.write(latex)
    print("Done. Generated: RQ2/tab_rq2.tex")
else:
    print("No summary rows found.")
