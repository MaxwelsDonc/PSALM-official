import random
import time
from typing import List, Set, Tuple, Dict

import numpy as np
from scipy.spatial import cKDTree

from geometricSum.model.metamorphic_group import MetamorphicGroup
from geometricSum.utils.get_path_utils import get_project_root
from geometricSum.utils.mg_domain_construction_utils import MGDomainGenerator
from geometricSum.utils.pharsing_config_utils import GeometricSumConfigExtractor


class OptimizedPartitionMTARTGenerator:
    """
    完全优化的分区预分组MT-ART生成器

    主要优化：
    1. 预先按分区对MG进行分组，避免重复查找
    2. 固定分区策略，避免重复划分
    3. 移除不必要的空间索引和二分查找
    4. 简化数据结构，提高内存效率
    """

    def __init__(
        self,
        mg_domain=None,
        config_path=get_project_root() + "/geometricSum/geometricsum_config.json",
    ):
        self.config_extractor = GeometricSumConfigExtractor(config_path)
        self.mg_domain = mg_domain or []
        self.removed_mg_ids = set()

        # 分区相关属性
        self.partition_groups: Dict[int, Set[MetamorphicGroup]] = {}
        self.partitions: List[Tuple[float, float]] = []

    def _partition_domain(
        self, domain_range: np.ndarray, num_partitions: int
    ) -> List[Tuple[float, float]]:
        """将一维输入域划分为num_partitions个均匀分区"""
        min_val, max_val = domain_range
        step = (max_val - min_val) / num_partitions
        return [
            (min_val + i * step, min_val + (i + 1) * step)
            for i in range(num_partitions)
        ]

    def _find_partition(
        self, point: float, partitions: List[Tuple[float, float]]
    ) -> int:
        """确定点属于哪个分区"""
        for idx, (low, high) in enumerate(partitions):
            if low <= point < high:
                return idx
        return len(partitions) - 1  # 处理边界情况

    def _build_partition_groups(
        self, partitions: List[Tuple[float, float]]
    ) -> Dict[int, Set[MetamorphicGroup]]:
        """预先将MG按分区分组，这是核心优化"""
        partition_groups = {i: set() for i in range(len(partitions))}


        for mg in self.mg_domain:
            # 找到STC所属分区
            stc_partition = self._find_partition(mg.source_test.x_value, partitions)
            partition_groups[stc_partition].add(mg)
        return partition_groups

    def _get_uncovered_partition_candidates(
        self, uncovered_partitions: Set[int]
    ) -> Set[MetamorphicGroup]:
        """从未覆盖分区中快速获取所有候选MG"""
        candidates = set()
        for partition_idx in uncovered_partitions:
            candidates |= self.partition_groups.get(partition_idx, set())
        return candidates

    @staticmethod
    def _internal_distance(mg: MetamorphicGroup) -> float:
        """计算MG内部STC与FTC的距离"""
        return abs(mg.source_test.x_value - mg.followup_test.x_value)

    def _evaluate_candidates(
        self,
        candidates: List[MetamorphicGroup],
        kdtree: cKDTree,
        selected_points_count: int,
    ) -> List[Tuple[MetamorphicGroup, float, float]]:
        """批量评估候选MG"""
        scored = []
        for mg in candidates:
            # 准备候选点
            candidate_points = np.array(
                [mg.source_test.x_value, mg.followup_test.x_value]
            ).reshape(-1, 1)

            # 使用KD树计算平均距离
            distances, _ = kdtree.query(candidate_points, k=selected_points_count)
            avg_distance = np.mean(distances) if distances.size > 0 else float("inf")

            # 计算内部距离
            internal_distance = self._internal_distance(mg)
            scored.append((mg, avg_distance, internal_distance))

        return scored

    def _update_partition_coverage(
        self,
        mg: MetamorphicGroup,
        covered_partitions: Set[int],
        uncovered_partitions: Set[int],
    ) -> None:
        """
        更新分区覆盖状态
        """
        # 检查STC点覆盖的分区
        stc_partition = self._find_partition(mg.source_test.x_value, self.partitions)
        if stc_partition not in covered_partitions:
            covered_partitions.add(stc_partition)
            uncovered_partitions.discard(stc_partition)

        # 检查FTC点覆盖的分区
        ftc_partition = self._find_partition(mg.followup_test.x_value, self.partitions)
        if ftc_partition not in covered_partitions:
            covered_partitions.add(ftc_partition)
            uncovered_partitions.discard(ftc_partition)

    def generate(
        self,
        num_samples: int,
        candidates_per_iter: int = 10,
        mg_domain=None,
        verbose: bool = False,
    ) -> List[MetamorphicGroup]:
        """
        优化的主生成流程

        Args:
            num_samples: 需要生成的样本数
            candidates_per_iter: 每次迭代评估的候选数
            mg_domain: MG域（可选，用于更新）
            verbose: 是否输出详细信息
        """
        if mg_domain is not None:
            self.mg_domain = mg_domain
            self.removed_mg_ids = set()

        if not self.mg_domain:
            return []

        if num_samples > len(self.mg_domain):
            raise ValueError(f"样本数 {num_samples} 超过MG域大小 {len(self.mg_domain)}")

        # 计算域范围
        all_x = [mg.source_test.x_value for mg in self.mg_domain]
        domain_range = np.array([min(all_x), max(all_x)])

        # 一次性划分固定分区
        num_partitions = int(num_samples * 1.5)
        self.partitions = self._partition_domain(domain_range, num_partitions)

        if verbose:
            print(f"域范围: [{domain_range[0]:.4f}, {domain_range[1]:.4f}]")
            print(f"划分 {num_partitions} 个均匀分区")

        # 预构建分区分组 - 核心优化
        start_time = time.time()
        self.partition_groups = self._build_partition_groups(self.partitions)
        grouping_time = time.time() - start_time

        if verbose:
            print(f"分区分组构建耗时: {grouping_time:.4f}s")

        # 初始化覆盖状态
        covered_partitions = set()
        uncovered_partitions = set(range(num_partitions))

        # 随机选择第一个MG
        first_mg = random.choice(self.mg_domain)
        selected = [first_mg]
        self.removed_mg_ids.add(id(first_mg))

        # 更新第一个MG的覆盖状态
        self._update_partition_coverage(
            first_mg, covered_partitions, uncovered_partitions
        )

        # 初始化KD树
        selected_points = [first_mg.source_test.x_value, first_mg.followup_test.x_value]
        selected_points_array = np.array(selected_points).reshape(-1, 1)
        kdtree = cKDTree(selected_points_array)

        # 主迭代循环
        while len(selected) < num_samples:
            if verbose:
                print(f"\n=== 迭代 {len(selected)+1} ===")
                print(f"已选择: {len(selected)}/{num_samples}")
                print(f"未覆盖分区数: {len(uncovered_partitions)}")

            # 从未覆盖分区获取候选
            valid_candidates = self._get_uncovered_partition_candidates(
                uncovered_partitions
            )

            if verbose:
                print(f"未覆盖分区候选数: {len(valid_candidates)}")

            if not valid_candidates:
                if verbose:
                    print("没有可用候选，提前结束")
                break

            # 采样并评估候选
            k = min(candidates_per_iter, len(valid_candidates))
            candidates = random.sample(list(valid_candidates), k)

            # 批量评估并选择最佳候选
            scored_candidates = self._evaluate_candidates(
                candidates, kdtree, len(selected_points_array)
            )
            best_mg, best_avg_dist, best_internal_dist = max(
                scored_candidates, key=lambda x: (x[1], x[2])
            )

            # 添加到选中集合
            selected.append(best_mg)
            self.removed_mg_ids.add(id(best_mg))

            # 更新覆盖状态
            self._update_partition_coverage(
                best_mg, covered_partitions, uncovered_partitions
            )

            if verbose:
                print(
                    f"选中: STC={best_mg.source_test.x_value:.4f}, FTC={best_mg.followup_test.x_value:.4f}, 平均距离={best_avg_dist:.4f}, 内部距离={best_internal_dist:.4f}"
                )

            # 更新KD树
            new_points = np.array(
                [best_mg.source_test.x_value, best_mg.followup_test.x_value]
            ).reshape(-1, 1)
            selected_points_array = np.vstack([selected_points_array, new_points])
            kdtree = cKDTree(selected_points_array)

        if verbose:
            coverage_rate = len(covered_partitions) / num_partitions * 100
            print(f"\n{'='*50}")
            print(f"生成完成！最终选择: {len(selected)} 个MG")
            print(
                f"分区覆盖率: {len(covered_partitions)}/{num_partitions} ({coverage_rate:.1f}%)"
            )
            if uncovered_partitions:
                print(f"未覆盖分区: {sorted(uncovered_partitions)}")

        return selected

    def get_coverage_stats(self) -> Dict:
        """获取覆盖统计信息"""
        if not hasattr(self, "partitions") or not self.partitions:
            return {}

        return {
            "total_partitions": len(self.partitions),
            "partition_sizes": [len(group) for group in self.partition_groups.values()],
            "domain_range": (
                (self.partitions[0][0], self.partitions[-1][1])
                if self.partitions
                else None
            ),
        }


if __name__ == "__main__":
    print("开始测试优化版分区预分组MT-ART生成器")

    # 生成测试数据
    generator = MGDomainGenerator()
    mg_domain = generator.generate_domain()
    print(f"MG域大小: {len(mg_domain)}")

    # 测试不同样本大小
    test_sizes = [10, 25, 50]

    for size in test_sizes:
        print(f"\n{'='*60}")
        print(f"测试样本数: {size}")
        print(f"{'='*60}")

        # 创建生成器并测试
        start_time = time.time()
        generator = OptimizedPartitionMTARTGenerator(mg_domain)

        # 生成样本
        samples = generator.generate(
            num_samples=size, candidates_per_iter=20, verbose=True
        )

        total_time = time.time() - start_time
        print(f"\n总耗时: {total_time:.4f}s")
        print(f"平均每样本: {total_time/size:.4f}s")

        # 显示样本质量统计
        if samples:
            stc_values = [mg.source_test.x_value for mg in samples]
            ftc_values = [mg.followup_test.x_value for mg in samples]
            internal_distances = [
                abs(mg.source_test.x_value - mg.followup_test.x_value) for mg in samples
            ]

            print(f"\n样本质量统计:")
            print(f"STC分布: [{min(stc_values):.4f}, {max(stc_values):.4f}]")
            print(f"FTC分布: [{min(ftc_values):.4f}, {max(ftc_values):.4f}]")
            print(f"平均内部距离: {np.mean(internal_distances):.4f}")
            print(f"内部距离标准差: {np.std(internal_distances):.4f}")
