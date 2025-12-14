import pandas as pd
import numpy as np
import matplotlib.pyplot as plt

# ========= 基础配置 =========
file_path = "RQ1/RQ1.xlsx"
output_prefix = "fig:rq1"
p_threshold = 0.05

# ========= 项目顺序配置 =========
PROJECT_ORDER = ['MoR', 'GeS', 'InT', 'CopyS', 'ParseI', 'CLine', 'Conv', 'IsDay']

# ========= 图形样式全局设置（统一字体） =========
plt.rcParams.update(
    {
        "font.family": "Times New Roman",
        "font.size": 12,
        "axes.titlesize": 12,  # 图标题
        "axes.labelsize": 12,  # 坐标轴标题
        "xtick.labelsize": 12,  # X轴刻度
        "ytick.labelsize": 12,  # Y轴刻度
        "legend.fontsize": 12,  # 图例
        "axes.linewidth": 0.8,
        "pdf.fonttype": 42,  # 嵌入矢量字体
    }
)

# ========= 读取数据 =========
xls = pd.ExcelFile(file_path)
all_data = []

for sheet in xls.sheet_names:
    df = pd.read_excel(xls, sheet_name=sheet)
    df["project"] = sheet.replace("_project", "")
    df["mutant"] = df["mutant"].astype(str).str.strip().str.lower()
    df["phase"] = df["phase"].astype(str).str.strip().str.lower()
    df = df[df["mutant"] != "summary"]
    all_data.append(df)

data = pd.concat(all_data, ignore_index=True)


# ========= 分类（3类） =========
def categorize(row, eps=1e-9):
    part = float(row["partition"])
    rnd = float(row["random"])
    pval = row.get("p-value(partition vs random)", np.nan)

    # 如果缺失或无效，直接算为 No difference
    if np.isnan(part) or np.isnan(rnd) or np.isnan(pval):
        return "No difference"

    if (part > rnd + eps) and (pval < p_threshold):
        return "PSALM better"
    elif (rnd > part + eps) and (pval < p_threshold):
        return "RS better"
    else:
        return "No difference"


# ========= 绘图函数 =========
def plot_phase(df, phase, out_pdf):
    if df.empty:
        print(f"[skip] no data for {phase}")
        return

    df = df.copy()
    df["category"] = df.apply(categorize, axis=1)

    cats = ["PSALM better", "No difference", "RS better"]
    summary = (
        df.groupby(["project", "category"])
        .size()
        .unstack(fill_value=0)
        .reindex(columns=cats, fill_value=0)
    )
    
    # ======== 按指定顺序重新排列项目 ========
    # 只保留存在于数据中的项目，并按指定顺序排列
    existing_projects = [proj for proj in PROJECT_ORDER if proj in summary.index]
    summary = summary.reindex(existing_projects)

    # ======== 添加总体汇总行 ========
    total_row = summary.sum(axis=0).to_frame().T
    total_row.index = ["Overall"]
    summary = pd.concat([summary, total_row])

    # ======== 精确计算百分比并强制总和为100% ========
    perc = summary.div(summary.sum(axis=1), axis=0) * 100
    perc = perc.round(0)

    # 直接用“100 - 其他两类”修正最后一类，避免偏差
    perc["RS better"] = 100 - perc["PSALM better"] - perc["No difference"]

    totals = summary.sum(axis=1)

    # ======== 绘制堆叠条形图 ========
    fig, ax = plt.subplots(figsize=(max(7, 1.0 * len(summary) + 2), 4))
    bottom = np.zeros(len(summary))
    colors = ["#2b83ba", "#f0f0f0", "#d7191c"]
    edgecolor = "white"

    for i, col in enumerate(cats):
        values = perc[col].values
        bars = ax.bar(
            summary.index,
            values,
            bottom=bottom,
            color=colors[i],
            edgecolor=edgecolor,
            linewidth=0.5,
            label=col,
        )
        for j, v in enumerate(values):
            if v > 5:
                y_pos = bottom[j] + v / 2
                ax.text(
                    j,
                    y_pos,
                    f"{v:.0f}%",
                    ha="center",
                    va="center",
                    fontsize=12,
                    family="Times New Roman",
                    color="black",
                )
        bottom += values

    # ======== 柱顶标总数 ========
    for i, proj in enumerate(summary.index):
        ax.text(
            i,
            106,
            f"n={int(totals.iloc[i])}",
            ha="center",
            va="bottom",
            fontsize=12,
            family="Times New Roman",
            fontweight="semibold",
            color="#333333",
        )

    # ======== 其他设置保持不变 ========
    ax.set_ylabel("Proportion of mutants (%)", labelpad=6, family="Times New Roman")
    ax.set_xticks(range(len(summary.index)))
    ax.set_xticklabels(summary.index, rotation=20, ha="right", family="Times New Roman")
    ax.set_ylim(0, 115)
    ax.grid(axis="y", linestyle="--", alpha=0.3)

    ax.legend(
        cats,
        ncol=3,
        frameon=False,
        loc="upper center",
        bbox_to_anchor=(0.5, -0.15),
        fontsize=12,
    )

    plt.tight_layout()
    plt.savefig(out_pdf, bbox_inches="tight")
    plt.close()
    print(f"[ok] saved {out_pdf}")


# ========= 生成两张图 =========
for ph in ["phase1", "phase2"]:
    dfp = data[data["phase"] == ph]
    plot_phase(dfp, ph, f"RQ1/{output_prefix}{ph}.pdf")
