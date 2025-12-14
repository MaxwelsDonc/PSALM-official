import os
import random
from typing import List, Dict, Tuple
from collections import defaultdict

from python.mortgageRate.model.metamorphic_group import MetamorphicGroup
from python.mortgageRate.utils.pharsing_config_utils import MortgageConfigExtractor
from python.mortgageRate.utils.get_path_utils import get_project_root
from python.mortgageRate.utils.mg_domain_construction_utils import MGDomainGenerator


class PartitionGeneratorMG:
    """
    Partition-based MG sampler:
    Each round, allocate one MG to the partition-MR with lowest current sampling rate.
    """

    def __init__(
        self,
        mg_domain: List[MetamorphicGroup] = None,
        config_path=get_project_root() + "/mortgageRate/mortgage_config.json",
    ):
        # random.seed(os.path.basename(__file__))
        self.config_extractor = MortgageConfigExtractor(config_path)
        self.mg_domain = mg_domain
        self.mr_types = self.config_extractor.mr_types

        # 定义MR分组映射
        self.mr_to_group = {
            "mr_1": "group1",
            "mr_2": "group1",  # mr1 + mr2 -> group1
            "mr_3": "group2",
            "mr_4": "group2",  # mr3 + mr4 -> group2
            "mr_5": "group3",
            "mr_6": "group3",  # mr5 + mr6 -> group3
        }

        # Build (partition_id, group_name) → [MGs]
        self.partition_mg_map: Dict[Tuple[int, str], List[MetamorphicGroup]] = (
            defaultdict(list)
        )

        # 只有在 mg_domain 不为 None 时才构建映射
        if self.mg_domain is not None:
            for mg in self.mg_domain:
                # 边界检查：确保mg和其属性存在
                if not mg or not hasattr(mg, "mr_id") or not hasattr(mg, "source_test"):
                    continue
                if not hasattr(mg.source_test, "partition_id"):
                    continue

                # 将MR映射到对应的组
                group_name = self.mr_to_group.get(mg.mr_id)
                if group_name:
                    # 边界检查：确保partition_id有效
                    partition_id = mg.source_test.partition_id
                    if (
                        partition_id is None
                        or not isinstance(partition_id, int)
                        or partition_id < 1
                    ):
                        continue

                    key = (partition_id, group_name)
                    self.partition_mg_map[key].append(mg)

        # Get the (partition_id, group_name) → float
        self.partition_size = self._generate_partition_size()
        self.partition_selected_counts = defaultdict(int)
        self.partition_selected_ratio = defaultdict(float)

    def _generate_partition_size(self):
        """
        Generate the expected size/weight for each (partition_id, mr_group) combination.
        MR groups: mr1+mr2 -> group1, mr3+mr4 -> group2, mr5+mr6 -> group3
        For each pair, value = partition_ratio[partition_id-1] * sum(mr_types in group)
        Returns:
            dict[(int, str), float]: mapping from (partition_id, mr_group) to weight
        """
        # 每次重新获取partition_ratio，避免并行时的竞争关系
        source_partition_ratios = self.config_extractor.partition_ratio

        # 定义MR分组
        mr_groups = {
            "group1": ["mr_1", "mr_2"],  # mr1 + mr2
            "group2": ["mr_3", "mr_4"],  # mr3 + mr4
            "group3": ["mr_5", "mr_6"],  # mr5 + mr6
        }

        size_dict = {}
        for idx, part_ratio in enumerate(source_partition_ratios):
            partition_id = idx + 1
            for group_name, mr_list in mr_groups.items():
                # 计算该组内所有MR类型值的总和
                group_weight = sum(
                    int(self.mr_types.get(mr_id, 0)) for mr_id in mr_list
                )
                size_dict[(partition_id, group_name)] = part_ratio * group_weight
        return size_dict

    def _find_lowest_sampling_rate_partition(self):
        """
        Find the (partition_id, mr_id) key with the lowest selected_ratio.
        If multiple have the same min ratio, pick the one with largest partition_size.
        Returns:
            tuple: (partition_id, mr_id) or None if no valid partitions
        """
        keys = list(self.partition_size.keys())

        # 边界检查：确保有可用的分区
        if not keys:
            return None

        # 边界检查：确保所有分区都有对应的MG
        valid_keys = [
            k for k in keys if k in self.partition_mg_map and self.partition_mg_map[k]
        ]
        if not valid_keys:
            return None

        # 1. 找所有采样率最小的 key
        try:
            min_ratio = min(self.partition_selected_ratio[k] for k in valid_keys)
            min_keys = [
                k for k in valid_keys if self.partition_selected_ratio[k] == min_ratio
            ]
        except (ValueError, KeyError):
            # 如果出现异常，返回第一个有效的key
            return valid_keys[0] if valid_keys else None

        if not min_keys:
            return None

        # 2. 如果多个采样率相同，选 size 最大的
        if len(min_keys) == 1:
            return min_keys[0]

        # 有多个，选 size 最大
        try:
            max_size = max(self.partition_size[k] for k in min_keys)
            candidates = [k for k in min_keys if self.partition_size[k] == max_size]
            # 如还有平局，随便取一个
            return candidates[0] if candidates else min_keys[0]
        except (ValueError, KeyError):
            # 如果出现异常，返回第一个min_key
            return min_keys[0]

    def generate(self, total_samples: int, mg_domain=None) -> List[MetamorphicGroup]:
        """
        Dynamically allocate one MG at a time to the (partition, group) with the lowest current sampling rate.
        """
        if mg_domain is not None:
            self.mg_domain = mg_domain
            # 重新构建 partition_mg_map
            self.partition_mg_map.clear()
            for mg in self.mg_domain:
                # 边界检查：确保mg和其属性存在
                if not mg or not hasattr(mg, "mr_id") or not hasattr(mg, "source_test"):
                    continue
                if not hasattr(mg.source_test, "partition_id"):
                    continue

                # 将MR映射到对应的组
                group_name = self.mr_to_group.get(mg.mr_id)
                if group_name:
                    # 边界检查：确保partition_id有效
                    partition_id = mg.source_test.partition_id
                    if (
                        partition_id is None
                        or not isinstance(partition_id, int)
                        or partition_id < 1
                    ):
                        continue

                    key = (partition_id, group_name)
                    self.partition_mg_map[key].append(mg)

        # 检查是否有可用的分区
        if not self.partition_mg_map:
            return []

        # 边界检查：确保total_samples有效
        if total_samples <= 0:
            return []

        # Prepare sampling
        sampled = []
        # Init - 只初始化有MG的分区
        for key in self.partition_mg_map.keys():
            if key in self.partition_size:
                self.partition_selected_counts[key] = 0
                self.partition_selected_ratio[key] = 0.0

        for _ in range(total_samples):
            target_partition_key = self._find_lowest_sampling_rate_partition()

            # 边界检查：确保找到了有效的分区
            if target_partition_key is None:
                break

            # 边界检查：确保分区中有可用的MG
            if (
                target_partition_key not in self.partition_mg_map
                or not self.partition_mg_map[target_partition_key]
            ):
                break

            try:
                target_mg = random.choice(self.partition_mg_map[target_partition_key])
                sampled.append(target_mg)
                self.partition_selected_counts[target_partition_key] += 1

                # 边界检查：防止除零错误
                if (
                    target_partition_key in self.partition_size
                    and self.partition_size[target_partition_key] > 0
                ):
                    self.partition_selected_ratio[target_partition_key] = (
                        self.partition_selected_counts[target_partition_key]
                        / self.partition_size[target_partition_key]
                    )
            except (IndexError, KeyError, ValueError):
                # 如果出现异常，跳过这次采样
                break

        return sampled

    def get_partition_statistics(self, mg_list: List[MetamorphicGroup]) -> str:
        """
        Get statistics for each (partition_id, group_name) combination.
        """
        if not mg_list:
            return "No MGs provided for statistics"

        # 统计每个(partition_id, group_name)的数量
        partition_counts = defaultdict(int)
        total_count = 0

        for mg in mg_list:
            # 边界检查：确保mg和其属性存在
            if not mg or not hasattr(mg, "mr_id") or not hasattr(mg, "source_test"):
                continue
            if not hasattr(mg.source_test, "partition_id"):
                continue

            # 将MR映射到对应的组
            group_name = self.mr_to_group.get(mg.mr_id)
            if group_name:
                # 边界检查：确保partition_id有效
                partition_id = mg.source_test.partition_id
                if (
                    partition_id is None
                    or not isinstance(partition_id, int)
                    or partition_id < 1
                ):
                    continue

                key = (partition_id, group_name)
                partition_counts[key] += 1
                total_count += 1

        if total_count == 0:
            return "No valid MGs found for statistics"

        # 生成统计报告
        result = [f"Total sampled MGs: {total_count}"]

        # 按分区和组排序
        sorted_keys = sorted(partition_counts.keys())
        for key in sorted_keys:
            partition_id, group_name = key
            count = partition_counts[key]
            percentage = (count / total_count) * 100 if total_count > 0 else 0
            result.append(
                f"Partition {partition_id}, Group {group_name}: {count} ({percentage:.1f}%)"
            )

        return "\n".join(result)


if __name__ == "__main__":
    mg_domain = MGDomainGenerator().generate_domain()
    partition_gen = PartitionGeneratorMG(mg_domain)  # 传入 mg_domain 参数
    sampled = partition_gen.generate(total_samples=12, mg_domain=mg_domain)
    print(partition_gen.get_partition_statistics(sampled))
