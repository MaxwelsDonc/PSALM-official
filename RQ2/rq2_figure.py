import pandas as pd
import matplotlib.pyplot as plt
from pathlib import Path

# ======= 可修改参数 =======

# 输入 / 输出路径
INPUT_PATH = "RQ2/RQ2.xlsx"  # 输入 Excel 文件
OUTPUT_DIR = Path("RQ2")  # 输出目录
OUTPUT_FIG = OUTPUT_DIR / "fig_rq2.pdf"  # 输出图像文件

# 判定阈值
PVALUE_THRESHOLD = 0.05  # 显著性阈值

# 图形设置
FIG_SIZE = (4.5, 4.5)  # 图形尺寸 (英寸)
COLORS = ["#2b83ba", "#d7191c", "#f0f0f0"]  # 蓝、红、灰
LABELS = ["MG better", "ST better", "Similar"]
FONT_SIZE = 13
PCT_FONT_SIZE = 12
START_ANGLE = 90
PCT_DISTANCE = 0.72
DPI = 300  # 输出分辨率

# ===========================

OUTPUT_DIR.mkdir(exist_ok=True, parents=True)

# ===== 读取数据 =====
xls = pd.ExcelFile(INPUT_PATH)
rows = []
for sheet in xls.sheet_names:
    df = pd.read_excel(xls, sheet_name=sheet)
    df = df[df["mutant"].astype(str).str.lower() != "summary"]
    df["Program"] = sheet
    rows.append(df[["Program", "mutant", "MG", "st", "p(MGvsSrc)"]])
all_df = pd.concat(rows, ignore_index=True)


# ===== 分类判断 (基于 p 值) =====
def judge(row):
    p = row["p(MGvsSrc)"]
    if pd.isna(p):
        # 若两组结果完全相同，视为无显著差异
        if abs(row["MG"] - row["st"]) < 1e-10:
            return "Similar"
        return "Unknown"
    if p < PVALUE_THRESHOLD:
        if row["MG"] > row["st"]:
            return "MG better"
        elif row["MG"] < row["st"]:
            return "ST better"
    return "Similar"


all_df["Category"] = all_df.apply(judge, axis=1)

# ===== 汇总比例 =====
summary = all_df["Category"].value_counts().reindex(LABELS, fill_value=0)
total = summary.sum()
ratio = (summary / total * 100).round(2)

print(f"Total mutants: {total}")
for cat in LABELS:
    print(f"{cat}: {summary[cat]} ({ratio[cat]}%)")

# ===== 绘制饼图 =====
fig, ax = plt.subplots(figsize=FIG_SIZE)


def autopct_filter(pct):
    """仅显示 ≥5% 的扇区百分比"""
    return f"{pct:.1f}%" if pct >= 5 else ""


wedges, texts, autotexts = ax.pie(
    ratio.values,
    labels=None,
    autopct=autopct_filter,
    startangle=START_ANGLE,
    colors=COLORS,
    pctdistance=PCT_DISTANCE,
    textprops={"fontsize": PCT_FONT_SIZE, "color": "black"},
    wedgeprops={"edgecolor": "white", "linewidth": 0.8},
)

# ===== 调整布局并放大饼图区域 =====
ax.axis("equal")  # 保持圆形
ax.set_position([0.05, 0.05, 0.9, 0.9])  # 饼图占满画布

# ===== 图例放在底部居中 =====
plt.legend(
    wedges,
    LABELS,
    loc="upper center",
    bbox_to_anchor=(0.5, -0.02),
    fontsize=FONT_SIZE,
    ncol=3,
    frameon=False,
)

# ===== 保存文件：极小留白 =====
plt.savefig(OUTPUT_FIG, dpi=DPI, bbox_inches="tight", pad_inches=0.01)
plt.close()
print(f"Figure saved to: {OUTPUT_FIG}")
