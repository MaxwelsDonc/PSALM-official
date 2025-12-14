from typing import List

from python.geometricSum.generation.phase1.partition_generator import PartitionGenerator
from python.geometricSum.model.metamorphic_group import MetamorphicGroup
from python.geometricSum.model.metamorphic_relation import MetamorphicRelationFactory
from python.geometricSum.utils.get_path_utils import get_config_path
from python.geometricSum.utils.load_mrs_utils import load_all_metamorphic_relations
from python.geometricSum.utils.pharsing_config_utils import GeometricSumConfigExtractor


class MGDomainGenerator:
    """
    蜕变组域生成器

    该类负责构建一个大型的蜕变组域，供后续根据不同策略选择测试用例
    """

    def __init__(self):
        """
        初始化蜕变组域生成器

        Args:
            domain_size: 域的大小(源测试用例数量)
        """
        # 加载配置
        self.config_path = get_config_path()
        self.config_extractor = GeometricSumConfigExtractor(self.config_path)

        # 设置域大小
        self.domain_size = self.calculate_domain_size()

        # 分区测试生成器(用于生成源测试用例)
        self.partition_generator = PartitionGenerator(self.config_path)

        # 加载蜕变关系
        self.all_relations = self._load_all_metamorphic_relations()

    def _load_all_metamorphic_relations(self):
        """手动加载所有蜕变关系"""
        return load_all_metamorphic_relations()

    def generate_domain(self) -> List[MetamorphicGroup]:
        """
        生成蜕变组域
        首先生成一组源测试用例，然后为每个测试用例应用所有适用的蜕变关系，
        生成蜕变组
        Returns:
            List[MetamorphicGroup]: 生成的蜕变组域
        """
        # 1. 生成源测试用例
        source_tests = self.partition_generator.generate_test_cases(self.domain_size)

        # 2. 为每个源测试用例应用所有适用的蜕变关系
        mg_domain = []

        for source_test in source_tests:
            mgs = MetamorphicRelationFactory.generate_metamorphic_groups(source_test)
            mg_domain.extend(mgs)

        return mg_domain

    def calculate_domain_size(self, min_samples_per_category: int = 5) -> int:
        """
        计算合适的域大小，确保每个类别(MR×分区组合)至少有指定数量的样本

        Args:
            min_samples_per_category: 每个类别最少的样本数

        Returns:
            int: 建议的域大小
        """
        # 获取分区比例
        partition_ratios = self.config_extractor.partition_ratio
        # 获取MR类型
        mr_types = self.config_extractor.mr_types
        # 计算类别数
        total_categories = len(partition_ratios) * len(mr_types)
        # 推荐域大小（每类最少min_samples_per_category，考虑安全系数1.5）
        suggested_size = int(min_samples_per_category * total_categories * 1.5)
        return min(max(suggested_size, 1000), 10000)  # 最小1000，最大10000

    def get_domain_statistics(self, mg_domain: List[MetamorphicGroup]) -> str:
        """
        输出MR × 分区交叉表，缺失处自动填0，保证每行每列长度一致
        """
        if not mg_domain:
            return "蜕变组域为空，无统计信息可用"

        # Get all MR types from config instead of mg_domain
        mr_list = self.config_extractor.mr_types

        # Get partitions from domain
        partition_set = set()
        for mg in mg_domain:
            partition_set.add(mg.get_source_test().get_partition_id())
        partition_list = sorted(partition_set)

        # 构造交叉表 {(mr_id, partition_id): count}
        cross_table = {}
        for mg in mg_domain:
            mr_id = mg.get_mr_id()
            partition_id = mg.get_source_test().get_partition_id()
            key = (mr_id, partition_id)
            cross_table[key] = cross_table.get(key, 0) + 1

        # 表头
        stats = []
        header = ["MR \\ 分区"] + [f"P{pid}" for pid in partition_list] + ["合计"]
        stats.append("| " + " | ".join(header) + " |")
        stats.append("|" + "|".join(["---"] * len(header)) + "|")

        # 行：每个MR
        total_per_row = {}
        total_per_col = {pid: 0 for pid in partition_list}
        grand_total = 0
        for mr_id in mr_list:
            row = [mr_id]
            row_total = 0
            for pid in partition_list:
                count = cross_table.get((mr_id, pid), 0)
                row.append(str(count))
                row_total += count
                total_per_col[pid] += count
                grand_total += count
            row.append(str(row_total))
            total_per_row[mr_id] = row_total
            stats.append("| " + " | ".join(row) + " |")

        # 最后一行：各分区合计
        total_row = ["合计"]
        for pid in partition_list:
            total_row.append(str(total_per_col[pid]))
        total_row.append(str(grand_total))
        stats.append("| " + " | ".join(total_row) + " |")

        return "\n".join(stats)


# 测试代码
if __name__ == "__main__":
    generator = MGDomainGenerator()
    print("正在生成蜕变组域...")
    mg_domain = generator.generate_domain()
    print("蜕变组域生成完成")
    print(generator.get_domain_statistics(mg_domain))
