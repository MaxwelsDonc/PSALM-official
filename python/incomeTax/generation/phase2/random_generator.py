import random
from typing import List

from python.incomeTax.model.metamorphic_group import MetamorphicGroup
from python.incomeTax.utils.pharsing_config_utils import IncomeTaxConfigExtractor
from python.incomeTax.utils.get_path_utils import get_config_path
from python.incomeTax.utils.mg_domain_construction_utils import MGDomainGenerator


class RandomGeneratorMG:
    """
    随机均匀抽取MetamorphicGroup（MG）的生成器
    支持加载config，便于后续扩展
    """

    def __init__(
        self,
        mg_domain: List[MetamorphicGroup] = None,
        config_path=get_config_path(),
    ):
        self.config_extractor = IncomeTaxConfigExtractor(config_path)
        self.mg_domain = mg_domain

    def generate(self, num_samples: int, mg_domain=None) -> List[MetamorphicGroup]:
        if mg_domain is not None:
            self.mg_domain = mg_domain
        if num_samples > len(self.mg_domain):
            raise ValueError("样本数量大于MGDomain总量")
        return random.sample(self.mg_domain, num_samples)


if __name__ == "__main__":
    print("开始生成分区")
    generator = MGDomainGenerator()
    mg_domain = generator.generate_domain()
    print("分区生成完成")
    sampler = RandomGeneratorMG(mg_domain)
    cases = sampler.generate(100)
    for i in range(min(5, len(cases))):
        print(f"RT测试用例 #{i + 1}: {cases[i]}")
