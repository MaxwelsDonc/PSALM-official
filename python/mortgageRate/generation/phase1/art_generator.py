import os
import random
from typing import List
from python.mortgageRate.model.test_case import TestCase
from python.mortgageRate.utils.pharsing_config_utils import MortgageConfigExtractor
from python.mortgageRate.utils.get_path_utils import get_project_root



class ARTGenerator:
    """
    全局input domain下的距离型ART生成器
    """
    def __init__(self, config_path=get_project_root()+'/mortgageRate/mortgage_config.json', candidate_num=10):
        self.config_extractor = MortgageConfigExtractor(config_path)
        self.input_range = self.config_extractor.input_range
        self.candidate_num = candidate_num

    def distance(self,x, y):
        """默认一维距离。若未来扩展多维，可自动检测类型后切换。"""
        return abs(x - y)

    def generate(self, num_cases: int = 10) -> List[TestCase]:
        lower, upper = self.input_range["min"], self.input_range["max"]
        test_cases = []

        # 第一个随机
        value = random.uniform(lower, upper)
        test_cases.append(TestCase(value))

        for _ in range(1, num_cases):
            best_candidate = None
            best_min_dist = -1
            for _ in range(self.candidate_num):
                c_value = random.uniform(lower, upper)
                min_dist = min(
                    self.distance(c_value, tc.get_house_value())
                    for tc in test_cases
                )
                if min_dist > best_min_dist:
                    best_min_dist = min_dist
                    best_candidate = c_value
            test_cases.append(TestCase(best_candidate))
        return test_cases


# 示例用法
if __name__ == "__main__":
    generator = ARTGenerator()
    cases = generator.generate(num_cases=100)
    for i in range(min(5, len(cases))):
        print(f"ART测试用例 #{i + 1}: {cases[i]}")
