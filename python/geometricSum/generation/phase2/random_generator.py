import os
import random
import sys
from typing import List

# 使用相对导入路径
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), '../../../')))
from geometricSum.model.metamorphic_group import MetamorphicGroup
from geometricSum.utils.pharsing_config_utils import GeometricSumConfigExtractor
from geometricSum.utils.get_path_utils import get_project_root
from geometricSum.utils.mg_domain_construction_utils import MGDomainGenerator

class RandomGeneratorMG:
    """
    随机均匀抽取MetamorphicGroup（MG）的生成器
    支持加载config，便于后续扩展
    """
    def __init__(self,  mg_domain: List[MetamorphicGroup]=None, config_path=get_project_root()+'/geometricSum/geometricsum_config.json'):
        # seed = os.path.basename(__file__)
        # random.seed(seed)
        self.config_extractor = GeometricSumConfigExtractor(config_path)
        self.mg_domain=mg_domain

    def generate(self, num_samples: int, mg_domain=None) -> List[MetamorphicGroup]:
        if mg_domain is None:
            mg_domain = self.mg_domain
        if num_samples > len(mg_domain):
            raise ValueError("Sample size exceeds MGDomain total size")
        return random.sample(mg_domain, num_samples)

if __name__ == "__main__":
    print("开始生成分区")
    generator = MGDomainGenerator()
    mg_domain = generator.generate_domain()
    print("分区生成完成")
    sampler = RandomGeneratorMG(mg_domain)
    cases = sampler.generate( 100)
    for i in range(min(5, len(cases))):
        print(f"RT测试用例 #{i + 1}: {cases[i]}")

