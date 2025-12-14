import random
import heapq
from typing import List, Dict, Tuple
from collections import defaultdict

from python.incomeTax.model.metamorphic_group import MetamorphicGroup
from python.incomeTax.utils.pharsing_config_utils import IncomeTaxConfigExtractor
from python.incomeTax.utils.get_path_utils import get_config_path
from python.incomeTax.utils.mg_domain_construction_utils import MGDomainGenerator


class PartitionGeneratorMG:
    """
    Partition-based MG sampler with heap optimization.
    Each round, allocate one MG to the partition-MR with the lowest current sampling rate.
    """

    def __init__(
        self,
        mg_domain: List[MetamorphicGroup] = None,
        config_path=get_config_path(),
    ):
        self.config_extractor = IncomeTaxConfigExtractor(config_path)
        self.source_partition_ratios = self.config_extractor.partition_ratio  # List
        self.mg_domain = mg_domain
        self.mr_types = self.config_extractor.mr_types

        # Build (partition_id, mr_id) → [MGs]
        self.partition_mg_map: Dict[Tuple[int, str], List[MetamorphicGroup]] = (
            defaultdict(list)
        )
        self.partition_size = self._generate_partition_size()

        # Heap-related
        self.partition_selected_counts = defaultdict(int)
        self.heap = []

    def _generate_partition_size(self):
        """
        Generate the expected size/weight for each (partition_id, mr_id) combination.
        """
        size_dict = {}
        for idx, part_ratio in enumerate(self.source_partition_ratios):
            partition_id = idx + 1
            for mr_id, mr_type_val in self.mr_types.items():
                size_dict[(partition_id, mr_id)] = part_ratio * int(mr_type_val)
        return size_dict

    def _init_heap(self):
        """
        Initialize the heap with (sampling_rate, -partition_size, key)
        """
        self.heap = []
        for key in self.partition_size.keys():
            heapq.heappush(self.heap, (0.0, -self.partition_size[key], key))

    def generate(self, total_samples: int, mg_domain=None) -> List[MetamorphicGroup]:
        """
        Dynamically allocate one MG at a time using a min-heap for efficiency.
        """
        if mg_domain is not None:
            self.mg_domain = mg_domain

        # Build partition map
        for mg in self.mg_domain:
            key = (mg.source_test.partition_id, mg.mr_id)
            self.partition_mg_map[key].append(mg)

        sampled = []
        self.partition_selected_counts = defaultdict(int)
        self._init_heap()

        for _ in range(total_samples):
            # Pop the partition with the lowest sampling rate
            ratio, neg_size, key = heapq.heappop(self.heap)

            # Randomly pick an MG from this partition-MR
            target_mg = random.choice(self.partition_mg_map[key])
            sampled.append(target_mg)

            # Update counts and heap
            self.partition_selected_counts[key] += 1
            new_ratio = self.partition_selected_counts[key] / self.partition_size[key]
            heapq.heappush(self.heap, (new_ratio, neg_size, key))

        return sampled

    def get_partition_statistics(self, mg_list: List[MetamorphicGroup]) -> str:
        """
        Return a string summary of the number of sampled MGs in each (partition_id, mr_id).
        Shows both absolute count and percent of total.
        """
        stats = defaultdict(int)
        total = len(mg_list)
        for mg in mg_list:
            key = (mg.source_test.partition_id, mg.mr_id)
            stats[key] += 1

        result = "Partition Statistics:\n"
        result += "-------------------\n"
        result += "(Partition, MR) -> Count (Percentage)\n"
        for key, count in sorted(stats.items()):
            percentage = (count / total) * 100 if total > 0 else 0
            result += f"({key[0]}, {key[1]}) -> {count} ({percentage:.2f}%)\n"

        return result


if __name__ == "__main__":
    import time

    print("开始生成MG域")
    generator = MGDomainGenerator()
    mg_domain = generator.generate_domain()
    print("MG域生成完成")

    sampler = PartitionGeneratorMG(mg_domain)

    test_sizes = [10, 50, 100, 500]

    for size in test_sizes:
        start_time = time.time()
        samples = sampler.generate(size)
        elapsed = time.time() - start_time

        print(f"\n采样 {size} 个 MG 用时: {elapsed:.4f} 秒")
        print(sampler.get_partition_statistics(samples))

        for i in range(min(3, len(samples))):
            print(f"BMA测试用例 #{i + 1}: {samples[i]}")
