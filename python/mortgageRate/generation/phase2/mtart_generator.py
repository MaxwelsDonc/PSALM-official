import numpy as np
from collections import deque
from typing import List, Set, Tuple

from python.mortgageRate.model.metamorphic_group import MetamorphicGroup
from python.mortgageRate.utils.pharsing_config_utils import MortgageConfigExtractor
from python.mortgageRate.utils.get_path_utils import get_config_path

from numba import njit


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


class MTARTGeneratorMG:
    """
    MT-ART生成器（MortgageRate版），加速+精简实现
    """

    def __init__(self, config_path=get_config_path()):
        self.config_extractor = MortgageConfigExtractor(config_path)

    def _create_partitions(
        self, mg_domain: List[MetamorphicGroup], num_partitions: int
    ) -> List[List[MetamorphicGroup]]:
        """基于 source_test.house_value 创建等距分区"""
        values = np.array([mg.source_test.house_value for mg in mg_domain])
        min_val, max_val = values.min(), values.max()
        step = (max_val - min_val) / num_partitions if num_partitions > 0 else 1

        partitions = [[] for _ in range(num_partitions)]
        for mg in mg_domain:
            idx = min(
                int((mg.source_test.house_value - min_val) / step), num_partitions - 1
            )
            partitions[idx].append(mg)

        return partitions

    def _compute_score(self, mg: MetamorphicGroup, recent_coords: deque) -> float:
        """计算单个MG的分数"""
        if not recent_coords:
            return float("inf")

        stc = mg.source_test.house_value
        ftc = mg.followup_test.house_value
        coords = np.array(recent_coords, dtype=np.float64)

        return compute_score_numba(stc, ftc, coords)

    def _select_candidates(
        self,
        partitions: List[List[MetamorphicGroup]],
        covered_partitions: Set[int],
        candidates_per_iter: int,
    ) -> Tuple[List[MetamorphicGroup], List[int]]:
        """两段随机选择候选集"""
        candidates = []
        partition_idx_list = []

        available_indices = [
            i for i, p in enumerate(partitions) if i not in covered_partitions and p
        ]

        if not available_indices:
            return candidates, partition_idx_list

        sampled_indices = np.random.choice(
            available_indices,
            size=min(candidates_per_iter, len(available_indices)),
            replace=False,
        )

        for idx in sampled_indices:
            partition = partitions[idx]
            mg = partition[np.random.randint(len(partition))]
            candidates.append(mg)
            partition_idx_list.append(idx)

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
            candidates, partition_idx_list = self._select_candidates(
                partitions, covered_partitions, candidates_per_iter
            )

            if not candidates:
                break

            best_idx, best_mg = max(
                enumerate(candidates),
                key=lambda pair: self._compute_score(pair[1], recent_coords),
            )

            selected.append(best_mg)
            recent_coords.append(
                (best_mg.source_test.house_value, best_mg.followup_test.house_value)
            )
            covered_partitions.add(partition_idx_list[best_idx])

        return selected


# if __name__ == "__main__":
#     from python.mortgageRate.utils.mg_domain_construction_utils import MGDomainGenerator

#     print("开始生成 MGDomain...")
#     generator = MGDomainGenerator()
#     mg_domain = generator.generate_domain()
#     print("MGDomain生成完成，总候选数:", len(mg_domain))

#     sampler = MTARTGeneratorMG()

#     cases = sampler.generate(num_samples=50, mg_domain=mg_domain, candidates_per_iter=10)

#     print("生成的前5个 MT-ART 样本：")
#     for i, mg in enumerate(cases[:5]):
#         print(f"样本#{i+1}: STC={mg.source_test.house_value}, FTC={mg.followup_test.house_value}")
