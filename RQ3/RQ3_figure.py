import pandas as pd
import numpy as np
import matplotlib.pyplot as plt

# ========= 基础配置 =========
phase1_file = "RQ3/RQ3_phase1.xlsx"
phase2_file = "RQ3/RQ3_phase2.xlsx"
output_prefix = "fig-rq3"
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

# ========= 读取数据函数 =========
def load_phase_data(file_path, phase_name):
    """读取指定阶段的Excel数据"""
    xls = pd.ExcelFile(file_path)
    all_data = []
    
    for sheet in xls.sheet_names:
        df = pd.read_excel(xls, sheet_name=sheet)
        df["project"] = sheet
        df["mutant"] = df["mutant"].astype(str).str.strip()
        # 过滤掉 summary 行
        df = df[~df["mutant"].str.contains("summary", case=False, na=False)]
        df["phase"] = phase_name
        all_data.append(df)
    
    return pd.concat(all_data, ignore_index=True)

# ========= 分类函数（3类） =========
def categorize_phase1(row, eps=1e-9):
    """Phase 1: PSALM vs ART 分类"""
    psalm = float(row["PSALM"])
    art = float(row["ART"])
    pval = row.get("p(PSALMvsBaseline)", np.nan)
    
    # 如果缺失或无效，直接算为 No difference
    if np.isnan(psalm) or np.isnan(art) or np.isnan(pval):
        return "No difference"
    
    if (psalm > art + eps) and (pval < p_threshold):
        return "PSALM better"
    elif (art > psalm + eps) and (pval < p_threshold):
        return "ART better"
    else:
        return "No difference"

def categorize_phase2(row, eps=1e-9):
    """Phase 2: PSALM vs MT-ART 分类"""
    psalm = float(row["PSALM"])
    mtart = float(row["MT-ART"])
    pval = row.get("p(PSALMvsBaseline)", np.nan)
    
    # 如果缺失或无效，直接算为 No difference
    if np.isnan(psalm) or np.isnan(mtart) or np.isnan(pval):
        return "No difference"
    
    if (psalm > mtart + eps) and (pval < p_threshold):
        return "PSALM better"
    elif (mtart > psalm + eps) and (pval < p_threshold):
        return "MT-ART better"
    else:
        return "No difference"

# ========= 绘图函数 =========
def plot_phase(df, phase_name, baseline_name, out_pdf):
    """绘制指定阶段的突变体分布图"""
    if df.empty:
        print(f"[skip] no data for {phase_name}")
        return
    
    df = df.copy()
    
    # 根据阶段选择分类函数
    if phase_name == "phase1":
        df["category"] = df.apply(categorize_phase1, axis=1)
        cats = ["PSALM better", "No difference", "ART better"]
    else:  # phase2
        df["category"] = df.apply(categorize_phase2, axis=1)
        cats = ["PSALM better", "No difference", "MT-ART better"]
    
    # 按项目和类别统计
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
    
    # ======== 输出统计数据到终端 ========
    print(f"\n========== {phase_name.upper()} (PSALM vs {baseline_name}) 统计数据 ==========")
    print(f"总突变体数量: {len(df)}")
    print("\n各项目突变体分类统计:")
    print(summary.to_string())
    
    # 计算总体百分比
    total_counts = summary.sum(axis=0)
    total_mutants = total_counts.sum()
    total_percentages = (total_counts / total_mutants * 100).round(1)
    
    print(f"\n总体分类统计:")
    for i, cat in enumerate(cats):
        count = total_counts.iloc[i]
        percentage = total_percentages.iloc[i]
        print(f"  {cat}: {count} ({percentage}%)")
    
    print(f"  总计: {total_mutants} (100.0%)")
    print("=" * 60)
    
    # ======== 添加总体汇总行 ========
    total_row = summary.sum(axis=0).to_frame().T
    total_row.index = ["Overall"]
    summary = pd.concat([summary, total_row])
    
    # ======== 精确计算百分比并强制总和为100% ========
    perc = summary.div(summary.sum(axis=1), axis=0) * 100
    perc = perc.round(0)
    
    # 直接用"100 - 其他两类"修正最后一类，避免偏差
    perc[cats[2]] = 100 - perc[cats[0]] - perc[cats[1]]
    
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

# ========= 主函数 =========
def main():
    # Phase 1: PSALM vs ART
    print("Processing Phase 1: PSALM vs ART...")
    phase1_data = load_phase_data(phase1_file, "phase1")
    plot_phase(phase1_data, "phase1", "ART", f"RQ3/{output_prefix}phase1.pdf")
    
    # Phase 2: PSALM vs MT-ART
    print("Processing Phase 2: PSALM vs MT-ART...")
    phase2_data = load_phase_data(phase2_file, "phase2")
    plot_phase(phase2_data, "phase2", "MT-ART", f"RQ3/{output_prefix}phase2.pdf")
    
    print("All figures generated successfully!")

if __name__ == "__main__":
    main()