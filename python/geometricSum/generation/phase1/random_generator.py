import random
from typing import List
from python.geometricSum.model.test_case import TestCase
from python.geometricSum.utils.pharsing_config_utils import GeometricSumConfigExtractor
from python.geometricSum.utils.get_path_utils import get_config_path


class RandomGenerator:
    """
    全局均匀采样的随机测试用例生成器（标准Random Testing，RT Baseline）
    直接在input domain范围内均匀采样，不依赖分区。
    """

    def __init__(self, config_path=get_config_path()):
        # seed = os.path.basename(__file__)
        # random.seed(seed)
        self.config_extractor = GeometricSumConfigExtractor(config_path)
        self.input_range = (
            self.config_extractor.input_range
        )  # dict, {'min':..., 'max':...}

    def generate(self, num_cases: int = 10) -> List[TestCase]:
        lower, upper = self.input_range["min"], self.input_range["max"]
        test_cases = []
        for _ in range(num_cases):
            value = random.uniform(lower, upper)
            # 如果你要统计分区归属，可以在此处后处理
            test_cases.append(TestCase(value))
        return test_cases


# 示例用法
if __name__ == "__main__":
    generator = RandomGenerator()
    cases = generator.generate(num_cases=100)
    for i in range(min(5, len(cases))):
        print(f"RT测试用例 #{i + 1}: {cases[i]}")
