from collections import deque
from typing import List, Set

import numpy as np

from numba import njit
from python.incomeTax.model.metamorphic_group import MetamorphicGroup
from python.incomeTax.utils.get_path_utils import get_project_root
from python.incomeTax.utils.pharsing_config_utils import IncomeTaxConfigExtractor


@njit
def compute_score_numba(stc, ftc, coords):
    """
    Numba JIT 加速版分数计算
    coords: numpy array of shape (N, 2)
    """
    dists = np.abs(coords[:, 0] - stc) + np.abs(coords[:, 1] - ftc)
    avg_dist = dists.mean()
    internal_dist = abs(stc - ftc)
    return avg_dist + internal_dist * 0.001


class MTARTGenerator:
    """
    两段随机构建候选集的 MT-ART 生成器（加速版）
    """

    def __init__(
        self,
        mg_domain=None,
        config_path=get_project_root() + "/incomeTax/incomeTax_config.json",
    ):
        self.config_extractor = IncomeTaxConfigExtractor(config_path)

    def _create_partitions(
        self, mg_domain: List[MetamorphicGroup], num_partitions: int
    ) -> List[List[MetamorphicGroup]]:
        """基于 source_test.income_value 创建等距分区"""
        values = np.array([mg.source_test.income_value for mg in mg_domain])
        min_val, max_val = values.min(), values.max()
        step = (max_val - min_val) / num_partitions

        partitions = [[] for _ in range(num_partitions)]
        for mg in mg_domain:
            idx = min(
                int((mg.source_test.income_value - min_val) / step), num_partitions - 1
            )
            partitions[idx].append(mg)

        return partitions

    def _compute_score(self, mg: MetamorphicGroup, recent_coords: deque) -> float:
        """计算单个 MG 的分数（加速版）"""
        if not recent_coords:
            return float("inf")

        stc = mg.source_test.income_value
        ftc = mg.followup_test.income_value
        coords = np.array(recent_coords)

        return compute_score_numba(stc, ftc, coords)

    def _select_candidates_by_two_stage_random(
        self,
        partitions: List[List[MetamorphicGroup]],
        covered_partitions: Set[int],
        candidates_per_iter: int,
    ) -> (List[MetamorphicGroup], List[int]):
        """两段随机选择候选集（加速版，用 numpy）"""
        candidates = []
        partition_idx_list = []

        available_partitions = [
            (i, p)
            for i, p in enumerate(partitions)
            if i not in covered_partitions and p
        ]

        if not available_partitions:
            return candidates, partition_idx_list

        attempts = 0
        while (
            len(candidates) < candidates_per_iter and attempts < candidates_per_iter * 3
        ):
            idx = np.random.randint(len(available_partitions))
            partition_idx, partition = available_partitions[idx]

            mg_idx = np.random.randint(len(partition))
            selected_mg = partition[mg_idx]

            candidates.append(selected_mg)
            partition_idx_list.append(partition_idx)

            attempts += 1

        return candidates, partition_idx_list

    def generate(
        self,
        num_samples: int,
        mg_domain: List[MetamorphicGroup],
        candidates_per_iter: int = 5,
    ) -> List[MetamorphicGroup]:
        """生成 MT-ART 采样序列"""
        if num_samples > len(mg_domain):
            raise ValueError(f"样本数 {num_samples} 超过 MG 域大小 {len(mg_domain)}")

        if not mg_domain:
            return []

        num_partitions = max(1, int(num_samples * 1.5))
        partitions = self._create_partitions(mg_domain, num_partitions)

        selected = []
        recent_coords = deque(maxlen=5)
        covered_partitions = set()

        for _ in range(num_samples):
            candidates, partition_idx_list = (
                self._select_candidates_by_two_stage_random(
                    partitions, covered_partitions, candidates_per_iter
                )
            )

            if not candidates:
                break

            best_mg = max(
                candidates, key=lambda mg: self._compute_score(mg, recent_coords)
            )

            selected.append(best_mg)
            recent_coords.append(
                (best_mg.source_test.income_value, best_mg.followup_test.income_value)
            )

            # 标记对应分区为已覆盖
            best_idx = candidates.index(best_mg)
            covered_partitions.add(partition_idx_list[best_idx])

        return selected
