import os
import random
import sys
from typing import List, Dict, Tuple
from collections import defaultdict

# 使用相对导入路径
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), '../../../')))
from geometricSum.model.metamorphic_group import MetamorphicGroup
from geometricSum.utils.pharsing_config_utils import GeometricSumConfigExtractor
from geometricSum.utils.get_path_utils import get_project_root
from geometricSum.utils.mg_domain_construction_utils import MGDomainGenerator

class PartitionGeneratorMG:
    """
    Partition-based MG sampler:
    Each round, allocate one MG to the partition-MR with lowest current sampling rate.
    """

    def __init__(self, mg_domain: List[MetamorphicGroup]=None,
                 config_path=get_project_root()+'/geometricSum/geometricsum_config.json'):
        # random.seed(os.path.basename(__file__))
        self.config_extractor = GeometricSumConfigExtractor(config_path)
        self.source_partition_ratios = self.config_extractor.partition_ratio  # List
        self.mg_domain = mg_domain
        self.mr_types = self.config_extractor.mr_types

        # Build (partition_id, mr_id) → [MGs]
        self.partition_mg_map: Dict[Tuple[int, str], List[MetamorphicGroup]] = defaultdict(list)
        # Get the (partition_id, mr_id) → float
        self.partition_size = self._generate_partition_size()
        self.partition_selected_counts = defaultdict(int)
        self.partition_selected_ratio = defaultdict(float)

    def _generate_partition_size(self):
        """
        Generate the expected size/weight for each (partition_id, mr_id) combination.
        For each pair, value = partition_ratio[partition_id-1] * mr_types[mr_id]
        Returns:
            dict[(int, str), float]: mapping from (partition_id, mr_id) to weight
        """
        size_dict = {}
        for idx, part_ratio in enumerate(self.source_partition_ratios):
            partition_id = idx + 1
            for mr_id, mr_type_val in self.mr_types.items():
                size_dict[(partition_id, mr_id)] = part_ratio * int(mr_type_val)
        return size_dict

    def _find_lowest_sampling_rate_partition(self):
        """
        Find the (partition_id, mr_id) key with the lowest selected_ratio.
        If multiple have the same min ratio, pick the one with largest partition_size.
        Returns:
            tuple: (partition_id, mr_id)
        """
        keys = self.partition_size.keys()
        # 1. 找所有采样率最小的 key
        min_ratio = min(self.partition_selected_ratio[k] for k in keys)
        min_keys = [k for k in keys if self.partition_selected_ratio[k] == min_ratio]
        # 2. 如果多个采样率相同，选 size 最大的
        if len(min_keys) == 1:
            return min_keys[0]
        # 有多个，选 size 最大
        max_size = max(self.partition_size[k] for k in min_keys)
        candidates = [k for k in min_keys if self.partition_size[k] == max_size]
        # 如还有平局，随便取一个
        return candidates[0]

    def generate(self, total_samples: int, mg_domain=None) -> List[MetamorphicGroup]:
        """
        Dynamically allocate one MG at a time to the (partition, MR) with the lowest current sampling rate.
        """
        # Prepare sampling
        sampled = []
        # Init
        if mg_domain is None:
            for mg in self.mg_domain:
                key = (mg.source_test.partition_id, mg.mr_id)
                self.partition_mg_map[key].append(mg)
        else:
            for mg in mg_domain:
                key = (mg.source_test.partition_id, mg.mr_id)
                self.partition_mg_map[key].append(mg)

        for key, _ in self.partition_size.items():
            self.partition_selected_counts[key] = 0
            self.partition_selected_ratio[key] = 0.0

        for _ in range(total_samples):
            target_partition_key = self._find_lowest_sampling_rate_partition()
            target_mg = random.choice(self.partition_mg_map[target_partition_key])
            sampled.append(target_mg)
            self.partition_selected_counts[target_partition_key] += 1
            self.partition_selected_ratio[target_partition_key] = self.partition_selected_counts[target_partition_key] / self.partition_size[target_partition_key]
        return sampled

    def get_partition_statistics(self, mg_list: List[MetamorphicGroup]) -> str:
        """
        Return a string summary of the number of sampled MGs in each (partition_id, mr_id).
        Shows both absolute count and percent of total.
        """
        from collections import defaultdict

        stats = defaultdict(int)
        total = len(mg_list)
        for mg in mg_list:
            key = (mg.source_test.partition_id, mg.mr_id)
            stats[key] += 1

        # Format the output
        result = "Partition Statistics:\n"
        result += "-------------------\n"
        result += "(Partition, MR) -> Count (Percentage)\n"
        for key, count in sorted(stats.items()):
            percentage = (count / total) * 100 if total > 0 else 0
            result += f"({key[0]}, {key[1]}) -> {count} ({percentage:.2f}%)\n"

        return result


if __name__ == "__main__":
    print("开始生成分区")
    generator = MGDomainGenerator()
    mg_domain = generator.generate_domain()
    print("分区生成完成")
    sampler = PartitionGeneratorMG(mg_domain)
    cases = sampler.generate(100)
    print(sampler.get_partition_statistics(cases))
    for i in range(min(5, len(cases))):
        print(f"BMA测试用例 #{i + 1}: {cases[i]}")