"""
Random Test Generator for Income Tax Testing

This module implements a pure random test generator that uniformly samples
test cases from the entire input domain without considering partitions.
This serves as a baseline for comparison with other testing strategies.
"""

import random
from typing import List
from python.incomeTax.model.test_case import TestCase
from python.incomeTax.utils.pharsing_config_utils import IncomeTaxConfigExtractor
from python.incomeTax.utils.get_path_utils import get_config_path


class RandomGenerator:
    """
    全局均匀采样的随机测试用例生成器（标准Random Testing，RT Baseline）

    直接在input domain范围内均匀采样，不依赖分区。
    这是最基础的测试用例生成策略，用作其他策略的基准对比。
    """

    def __init__(self, config_path=None):
        """
        初始化随机生成器

        Args:
            config_path (str, optional): 配置文件路径，默认使用标准配置
        """
        if config_path is None:
            config_path = get_config_path()

        self.config_extractor = IncomeTaxConfigExtractor(config_path)
        self.input_range = self.config_extractor.input_range

    def generate(self, num_cases: int = 10) -> List[TestCase]:
        """
        生成指定数量的随机测试用例

        Args:
            num_cases (int): 要生成的测试用例数量

        Returns:
            List[TestCase]: 生成的测试用例列表
        """
        lower, upper = self.input_range["min"], self.input_range["max"]
        test_cases = []

        for _ in range(num_cases):
            value = random.uniform(lower, upper)
            # TestCase构造函数会自动确定分区归属
            test_cases.append(TestCase(value))

        return test_cases

    def get_statistics(self, test_cases: List[TestCase]) -> str:
        """
        获取测试用例的统计信息

        Args:
            test_cases (List[TestCase]): 测试用例列表

        Returns:
            str: 统计信息字符串
        """
        if not test_cases:
            return "无测试用例"

        income_values = [tc.get_income_value() for tc in test_cases]

        stats = [
            f"随机测试用例总数: {len(test_cases)}",
            f"收入范围: [{min(income_values):.2f}, {max(income_values):.2f}]",
            f"平均收入: {sum(income_values) / len(income_values):.2f}",
        ]

        # 分区统计
        partition_count = {}
        for tc in test_cases:
            pid = tc.get_partition_id()
            partition_count[pid] = partition_count.get(pid, 0) + 1

        stats.append("分区分布:")
        partitions = self.config_extractor.partitions
        for pid in sorted(partition_count.keys()):
            count = partition_count[pid]
            percent = 100.0 * count / len(test_cases)
            part = partitions[pid - 1]
            desc = f"分区{pid} [{part['min']},{part['max']}]"
            stats.append(f"  {desc}: {count} ({percent:.2f}%)")

        return "\n".join(stats)

    def generate_in_range(
        self, num_cases: int, min_income: float, max_income: float
    ) -> List[TestCase]:
        """
        在指定收入范围内生成随机测试用例

        Args:
            num_cases (int): 要生成的测试用例数量
            min_income (float): 最小收入值
            max_income (float): 最大收入值

        Returns:
            List[TestCase]: 生成的测试用例列表

        Raises:
            ValueError: 当收入范围无效时抛出异常
        """
        if min_income >= max_income:
            raise ValueError("最小收入必须小于最大收入")

        # 确保范围在配置的输入域内
        domain_min = self.input_range["min"]
        domain_max = self.input_range["max"]

        if min_income < domain_min or max_income > domain_max:
            raise ValueError(
                f"指定范围 [{min_income}, {max_income}] 超出输入域 [{domain_min}, {domain_max}]"
            )

        test_cases = []
        for _ in range(num_cases):
            value = random.uniform(min_income, max_income)
            test_cases.append(TestCase(value))

        return test_cases

    def generate_by_partition(
        self, num_cases: int, partition_id: int
    ) -> List[TestCase]:
        """
        在指定分区内生成随机测试用例

        Args:
            num_cases (int): 要生成的测试用例数量
            partition_id (int): 分区ID

        Returns:
            List[TestCase]: 生成的测试用例列表

        Raises:
            ValueError: 当分区ID无效时抛出异常
        """
        partitions = self.config_extractor.partitions

        if partition_id < 1 or partition_id > len(partitions):
            raise ValueError(f"无效的分区ID: {partition_id}")

        part = partitions[partition_id - 1]
        min_value = part["min"]
        max_value = part["max"]

        # 处理开闭区间
        if part.get("left") == "open":
            min_value += 0.01
        if part.get("right") == "open":
            max_value -= 0.01

        return self.generate_in_range(num_cases, min_value, max_value)


# 示例用法
if __name__ == "__main__":
    generator = RandomGenerator()

    print("=== 随机测试用例生成器示例 ===")

    # 生成全域随机测试用例
    cases = generator.generate(num_cases=50000)
    print(generator.get_statistics(cases))
    print("\n前5个测试用例:")
    for i in range(min(5, len(cases))):
        print(f"测试用例 #{i + 1}: {cases[i]}")

    # 生成特定分区的测试用例
    print("\n=== 分区3的随机测试用例 ===")
    partition_cases = generator.generate_by_partition(10, 3)
    for i, case in enumerate(partition_cases):
        print(f"分区3测试用例 #{i + 1}: {case}")
