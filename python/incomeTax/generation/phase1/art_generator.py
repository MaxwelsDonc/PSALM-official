#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
ART (Adaptive Random Testing) Generator for Income Tax Testing

This module implements an ART generator that uses distance-based selection
to generate test cases with better diversity than pure random testing.
"""

import random
from typing import List
from python.incomeTax.model.test_case import TestCase
from python.incomeTax.utils.pharsing_config_utils import IncomeTaxConfigExtractor
from python.incomeTax.utils.get_path_utils import get_config_path


class ARTGenerator:
    """
    全局input domain下的距离型ART生成器

    使用自适应随机测试算法，通过距离度量来选择测试用例，
    确保生成的测试用例具有更好的分布性和多样性。
    """

    def __init__(self, config_path=None, candidate_num=10):
        """
        初始化ART生成器

        Args:
            config_path (str, optional): 配置文件路径，默认使用标准配置
            candidate_num (int): 候选测试用例数量，用于距离计算
        """
        if config_path is None:
            config_path = get_config_path()

        self.config_extractor = IncomeTaxConfigExtractor(config_path)
        self.input_range = self.config_extractor.input_range
        self.candidate_num = candidate_num

    @staticmethod
    def distance(x, y):
        """
        计算两个收入值之间的距离

        Args:
            x (float): 第一个收入值
            y (float): 第二个收入值

        Returns:
            float: 两个收入值之间的绝对距离
        """
        return abs(x - y)

    def generate(self, num_cases: int = 10) -> List[TestCase]:
        """
        使用ART算法生成指定数量的测试用例

        Args:
            num_cases (int): 要生成的测试用例数量

        Returns:
            List[TestCase]: 生成的测试用例列表
        """
        lower, upper = self.input_range["min"], self.input_range["max"]
        test_cases = []

        # 第一个测试用例随机生成
        value = random.uniform(lower, upper)
        test_cases.append(TestCase(value))

        # 后续测试用例使用ART算法生成
        for _ in range(1, num_cases):
            best_candidate = None
            best_min_dist = -1

            # 生成候选测试用例并选择距离最远的
            for _ in range(self.candidate_num):
                c_value = random.uniform(lower, upper)

                # 计算候选用例与已有用例的最小距离
                min_dist = min(
                    self.distance(c_value, tc.get_income_value()) for tc in test_cases
                )

                # 选择最小距离最大的候选用例
                if min_dist > best_min_dist:
                    best_min_dist = min_dist
                    best_candidate = c_value

            test_cases.append(TestCase(best_candidate))

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
            f"ART测试用例总数: {len(test_cases)}",
            f"收入范围: [{min(income_values):.2f}, {max(income_values):.2f}]",
            f"平均收入: {sum(income_values) / len(income_values):.2f}",
        ]

        # 分区统计
        partition_count = {}
        for tc in test_cases:
            pid = tc.get_partition_id()
            partition_count[pid] = partition_count.get(pid, 0) + 1

        stats.append("分区分布:")
        for pid in sorted(partition_count.keys()):
            count = partition_count[pid]
            percent = 100.0 * count / len(test_cases)
            stats.append(f"  分区{pid}: {count} ({percent:.2f}%)")

        return "\n".join(stats)


# 示例用法
if __name__ == "__main__":
    generator = ARTGenerator()
    cases = generator.generate(num_cases=20)

    print("=== ART测试用例生成器示例 ===")
    print(generator.get_statistics(cases))
    print("\n前5个测试用例:")
    for i in range(min(5, len(cases))):
        print(f"测试用例 #{i + 1}: {cases[i]}")
