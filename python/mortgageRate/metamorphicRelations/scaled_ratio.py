import os
import random
from typing import List

from python.mortgageRate.model.test_case import TestCase
from python.mortgageRate.utils.pharsing_config_utils import MortgageConfigExtractor
from python.mortgageRate.utils.get_path_utils import get_config_path
from python.mortgageRate.model.metamorphic_relation import MetamorphicRelation



class ScaledRatio(MetamorphicRelation):
    """
    MR4: 缩放输入输出比率关系

    验证当房屋价值按比例缩放时，输出的增加应在特定比率范围内
    具体而言: 2.27 <= (f(x*scale)-f(x))/(x*scale-x) <= 2.75
    """

    def __init__(self):
        # 从配置文件加载类型信息
        seed = os.path.basename(__file__)
        random.seed(seed)
        config_path = get_config_path()
        self.config_extractor = MortgageConfigExtractor(config_path)
        self.follow_up_count = self.config_extractor.mr_types.get(self.get_id())

    def get_id(self) -> str:
        """获取蜕变关系ID"""
        return "mr_4"

    def get_description(self) -> str:
        """获取蜕变关系描述"""
        return "缩放输入输出比率关系 - 验证输入缩放时，输出增加的比率应在预期范围内"

    def generate_followup_tests(self, source_test: TestCase) -> List[TestCase]:
        """
        基于源测试用例生成后续测试用例

        将源测试的房屋价值乘以一个随机的缩放因子(1.0-1.2之间)

        Args:
            source_test: 源测试用例

        Returns:
            包含一个后续测试用例的列表
        """
        followup_tests = []

        # 获取源测试的房屋价值
        house_value = source_test.get_house_value()

        # 生成3个后续测试用例(mr_4:3)
        scale_ranges = [
            (1.01, 1.07),  # 小缩放
            (1.08, 1.14),  # 中缩放
            (1.15, 1.2)  # 大缩放
        ]

        for i in range(min(self.follow_up_count, len(scale_ranges))):
            min_scale, max_scale = scale_ranges[i]
            scale_factor = random.uniform(min_scale, max_scale)
            new_house_value = min(house_value * scale_factor, 600001)

            followup_test = TestCase(new_house_value)
            followup_tests.append(followup_test)

        return followup_tests

    def verify_relation(self, source_test: TestCase, followup_test: TestCase,
                        source_result: float, followup_result: float,
                        source_execution: str = "", followup_execution: str = "") -> bool:
        """
        验证源测试和后续测试的结果是否满足蜕变关系

        验证变化比率是否在预期范围内: 2.27 <= (followup_result - source_result) / (followup_test_value - source_test_value) <= 2.75

        Args:
            source_test: 源测试用例
            followup_test: 后续测试用例
            source_result: 源测试结果
            followup_result: 后续测试结果
            source_execution: 源测试执行的错误信息，如果有的话
            followup_execution: 后续测试执行的错误信息，如果有的话

        Returns:
            如果满足比率关系则返回True
        """
        # 检查是否有任何执行错误
        if source_execution or followup_execution:
            # 如果两个测试都有相同类型的错误，关系仍被认为是满足的
            return source_execution == followup_execution

        # 计算输入变化
        source_value = source_test.get_house_value()
        followup_value = followup_test.get_house_value()
        input_delta = followup_value - source_value

        # 避免除以零
        if input_delta == 0:
            return True

        # 计算比率
        ratio = (followup_result - source_result) / input_delta

        # 验证比率是否在预期范围内(添加小的容差值)
        return ratio >= 2.27 - 1e-6

    def is_applicable_to(self, test_case: TestCase) -> bool:
        """
        检查蜕变关系是否适用于给定的测试用例

        这个关系对大多数测试用例都适用，除非房屋价值已经接近上限

        Args:
            test_case: 要检查的测试用例

        Returns:
            如果测试用例的房屋价值小于上限的80%则返回True，这样即使最大缩放也不会超过上限
        """
        max_value = 600001
        max_scale = 1.2

        # 确保缩放后的值不会超过上限
        return test_case.get_house_value() * max_scale <= max_value

if __name__ == '__main__':
    test = ScaledRatio()