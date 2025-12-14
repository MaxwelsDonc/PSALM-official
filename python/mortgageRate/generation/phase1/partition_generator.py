import os
import random
from typing import List, Dict

from python.mortgageRate.model.test_case import TestCase
from python.mortgageRate.utils.pharsing_config_utils import MortgageConfigExtractor
from python.mortgageRate.utils.get_path_utils import get_project_root

class PartitionGenerator:
    """
    抵押贷款测试的分区测试生成器（适配新版 config）
    使用最大最小(maximin)算法从各分区中选择测试用例，
    以确保测试用例在不同分区间的合理分布
    """
    def __init__(self, config_path=get_project_root()+'/mortgageRate/mortgage_config.json'):
        # random.seed(os.path.basename(__file__))
        self.config_extractor = MortgageConfigExtractor(config_path)
        self.input_range = self.config_extractor.input_range
        self.partitions = self.config_extractor.partitions  # list of dict [{min, max, left, right}]
        self.partition_ratios = self.config_extractor.partition_ratio  # list
        self.selected_counts = {i + 1: 0 for i in range(len(self.partitions))}


    def _validate_config(self):
        """验证配置是否有效"""
        if not self.input_range or "min" not in self.input_range or "max" not in self.input_range:
            raise ValueError("输入区间配置无效")
        if not self.partitions or not all("min" in p and "max" in p for p in self.partitions):
            raise ValueError("分区配置无效")
        if not self.partition_ratios or len(self.partition_ratios) != len(self.partitions):
            raise ValueError("分区比例配置无效或与分区数量不匹配")

    def _get_partitions(self) -> List[Dict]:
        """直接返回分区结构体列表"""
        return self.partitions

    def generate(self, count: int) -> List[TestCase]:
        """使用最大最小算法生成指定数量的测试用例"""
        return self._allocate_test_cases(count)

    def _allocate_test_cases(self, count: int) -> List[TestCase]:
        """使用最大最小算法分配测试用例到各分区"""
        for partition_id in self.selected_counts:
            self.selected_counts[partition_id] = 0

        sampling_rates = {i + 1: 0.0 for i in range(len(self.partitions))}
        test_cases = []
        for _ in range(count):
            selected_partition = self._find_lowest_sampling_rate_partition(sampling_rates)
            test_case = self._generate_test_case_in_partition(selected_partition)
            test_cases.append(test_case)
            self.selected_counts[selected_partition] += 1
            sampling_rates[selected_partition] = (
                self.selected_counts[selected_partition] /
                self.partition_ratios[selected_partition - 1]
            )
        return test_cases

    def _find_lowest_sampling_rate_partition(self, sampling_rates: Dict[int, float]) -> int:
        selected_partition = 1
        lowest_rate = float('inf')
        for partition_id, rate in sampling_rates.items():
            if rate < lowest_rate:
                lowest_rate = rate
                selected_partition = partition_id
            elif rate == lowest_rate and partition_id <= len(self.partition_ratios):
                current_size = self.partition_ratios[partition_id - 1]
                selected_size = self.partition_ratios[selected_partition - 1]
                if current_size > selected_size:
                    selected_partition = partition_id
        return selected_partition

    def _generate_test_case_in_partition(self, partition_id: int) -> TestCase:
        if partition_id < 1 or partition_id > len(self.partitions):
            raise ValueError(f"无效的分区ID: {partition_id}")
        part = self.partitions[partition_id - 1]
        min_value = part["min"]
        max_value = part["max"]
        value = random.uniform(min_value, max_value)
        return TestCase(value, partition_id)

    def generate_test_case(self, partition_id: int) -> TestCase:
        return self._generate_test_case_in_partition(partition_id)

    def get_statistics(self, test_cases):
        """
        统计每个分区的测试用例数量和占比（简洁版）
        """
        partition_count = {i + 1: 0 for i in range(len(self.partitions))}
        total = len(test_cases)
        for tc in test_cases:
            pid = tc.get_partition_id()
            if pid in partition_count:
                partition_count[pid] += 1
        report = [f"测试用例总数: {total}"]
        for pid in sorted(partition_count.keys()):
            part = self.partitions[pid - 1]
            desc = f"分区{pid}: [{part['min']},{part['max']}] ({part['left']},{part['right']})"
            count = partition_count[pid]
            percent = 100.0 * count / total if total else 0
            report.append(f"{desc}: {count} ({percent:.2f}%)")
        return "\n".join(report)

if __name__ == "__main__":
    generator = PartitionGenerator()
    test_cases = generator.generate(100)
    print(generator.get_statistics(test_cases))
    print("\n示例测试用例:")
    for i in range(min(5, len(test_cases))):
        print(f"测试用例 #{i + 1}: {test_cases[i]}")