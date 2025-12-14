#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Partition-based Test Generator for Income Tax Testing

This module implements a partition-based test generator that uses the maximin algorithm
to select test cases from different partitions, ensuring reasonable distribution
of test cases across different income tax brackets.
"""

import random
from typing import List, Dict
from python.incomeTax.model.test_case import TestCase
from python.incomeTax.utils.pharsing_config_utils import IncomeTaxConfigExtractor
from python.incomeTax.utils.get_path_utils import get_config_path


class PartitionGenerator:
    """
    收入税测试的分区测试生成器

    使用最大最小(maximin)算法从各分区中选择测试用例，
    以确保测试用例在不同收入税级别间的合理分布。
    """

    def __init__(self, config_path=None):
        """
        初始化分区生成器

        Args:
            config_path (str, optional): 配置文件路径，默认使用标准配置
        """
        if config_path is None:
            config_path = get_config_path()

        self.config_extractor = IncomeTaxConfigExtractor(config_path)
        self.input_range = self.config_extractor.input_range
        self.partitions = (
            self.config_extractor.partitions
        )  # list of dict [{min, max, left, right}]
        self.partition_size = self.config_extractor.partition_ratio  # list
        self.selected_counts = {i + 1: 0 for i in range(len(self.partitions))}

        self._validate_config()

    def _validate_config(self):
        """
        验证配置是否有效

        Raises:
            ValueError: 当配置无效时抛出异常
        """
        if (
            not self.input_range
            or "min" not in self.input_range
            or "max" not in self.input_range
        ):
            raise ValueError("输入区间配置无效")
        if not self.partitions or not all(
            "min" in p and "max" in p for p in self.partitions
        ):
            raise ValueError("分区配置无效")
        if not self.partition_size or len(self.partition_size) != len(self.partitions):
            raise ValueError("分区比例配置无效或与分区数量不匹配")

    def _get_partitions(self) -> List[Dict]:
        """
        获取分区结构体列表

        Returns:
            List[Dict]: 分区配置列表
        """
        return self.partitions

    def generate(self, count: int) -> List[TestCase]:
        """
        使用最大最小算法生成指定数量的测试用例

        Args:
            count (int): 要生成的测试用例数量

        Returns:
            List[TestCase]: 生成的测试用例列表
        """
        return self._allocate_test_cases(count)

    def _allocate_test_cases(self, count: int) -> List[TestCase]:
        """
        使用最大最小算法分配测试用例到各分区

        Args:
            count (int): 要分配的测试用例数量

        Returns:
            List[TestCase]: 分配的测试用例列表
        """
        # 重置计数器
        for partition_id in self.selected_counts:
            self.selected_counts[partition_id] = 0

        sampling_rates = {i + 1: 0.0 for i in range(len(self.partitions))}
        test_cases = []

        for _ in range(count):
            # 选择采样率最低的分区
            selected_partition = self._find_lowest_sampling_rate_partition(
                sampling_rates
            )

            # 在选定分区中生成测试用例
            test_case = self._generate_test_case_in_partition(selected_partition)
            test_cases.append(test_case)

            # 更新计数和采样率
            self.selected_counts[selected_partition] += 1
            sampling_rates[selected_partition] = (
                self.selected_counts[selected_partition]
                / self.partition_size[selected_partition - 1]
            )

        return test_cases

    def _find_lowest_sampling_rate_partition(
        self, sampling_rates: Dict[int, float]
    ) -> int:
        """
        找到采样率最低的分区

        Args:
            sampling_rates (Dict[int, float]): 各分区的采样率

        Returns:
            int: 采样率最低的分区ID
        """
        selected_partition = 1
        lowest_rate = float("inf")

        for partition_id, rate in sampling_rates.items():
            if rate < lowest_rate:
                lowest_rate = rate
                selected_partition = partition_id
            elif rate == lowest_rate and partition_id <= len(self.partition_size):
                # 如果采样率相同，选择分区大小更大的
                current_size = self.partition_size[partition_id - 1]
                selected_size = self.partition_size[selected_partition - 1]
                if current_size > selected_size:
                    selected_partition = partition_id

        return selected_partition

    def _generate_test_case_in_partition(self, partition_id: int) -> TestCase:
        """
        在指定分区中生成测试用例

        Args:
            partition_id (int): 分区ID

        Returns:
            TestCase: 生成的测试用例

        Raises:
            ValueError: 当分区ID无效时抛出异常
        """
        if partition_id < 1 or partition_id > len(self.partitions):
            raise ValueError(f"无效的分区ID: {partition_id}")

        part = self.partitions[partition_id - 1]
        min_value = part["min"]
        max_value = part["max"]

        # 处理开闭区间
        if part.get("left") == "open":
            min_value += 0.01  # 稍微增加以避开开区间边界
        if part.get("right") == "open":
            max_value -= 0.01  # 稍微减少以避开开区间边界

        value = random.uniform(min_value, max_value)
        return TestCase(value, partition_id)

    def generate_test_case(self, partition_id: int) -> TestCase:
        """
        在指定分区中生成单个测试用例

        Args:
            partition_id (int): 分区ID

        Returns:
            TestCase: 生成的测试用例
        """
        return self._generate_test_case_in_partition(partition_id)

    def get_statistics(self, test_cases: List[TestCase]) -> str:
        """
        统计每个分区的测试用例数量和占比

        Args:
            test_cases (List[TestCase]): 测试用例列表

        Returns:
            str: 统计信息字符串
        """
        partition_count = {i + 1: 0 for i in range(len(self.partitions))}
        total = len(test_cases)

        for tc in test_cases:
            pid = tc.get_partition_id()
            if pid in partition_count:
                partition_count[pid] += 1

        report = [f"分区测试用例总数: {total}"]

        for pid in sorted(partition_count.keys()):
            part = self.partitions[pid - 1]
            desc = f"分区{pid}: [{part['min']},{part['max']}] ({part['left']},{part['right']})"
            count = partition_count[pid]
            percent = 100.0 * count / total if total else 0
            expected_percent = self.partition_size[pid - 1] * 100
            report.append(
                f"{desc}: {count} ({percent:.2f}%, 期望: {expected_percent:.2f}%)"
            )

        return "\n".join(report)


# 示例用法
if __name__ == "__main__":
    generator = PartitionGenerator()
    test_cases = generator.generate(50000)

    print("=== 分区测试用例生成器示例 ===")
    print(generator.get_statistics(test_cases))
    print("\n前5个测试用例:")
    for i in range(min(5, len(test_cases))):
        print(f"测试用例 #{i + 1}: {test_cases[i]}")
