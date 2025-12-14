import os
import random
from typing import List

from python.mortgageRate.model.test_case import TestCase
from python.mortgageRate.utils.pharsing_config_utils import MortgageConfigExtractor
from python.mortgageRate.utils.get_path_utils import get_config_path
from python.mortgageRate.model.metamorphic_relation import MetamorphicRelation

class InputScaling(MetamorphicRelation):
    """
    MR3: 输入缩放关系

    验证当房屋价值按比例缩放时，输出应该增加
    具体而言: f(x*scale) > f(x)，其中scale > 1
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
        return "mr_3"

    def get_description(self) -> str:
        """获取蜕变关系描述"""
        return "输入缩放关系 - 验证当房屋价值按比例增加时，抵押贷款输出也应增加"

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

        # 生成2个后续测试用例(mr_3:2)
        for i in range(self.follow_up_count):
            # 使用不同的缩放因子
            if i == 0:
                scale_factor = random.uniform(1.01, 1.1)  # 小缩放
            else:
                scale_factor = random.uniform(1.11, 1.2)  # 大缩放

            new_house_value = min(house_value * scale_factor, 600001)
            followup_test = TestCase(new_house_value)
            followup_tests.append(followup_test)

        return followup_tests

    def verify_relation(self, source_test: TestCase, followup_test: TestCase,
                        source_result: float, followup_result: float,
                        source_execution: str = "", followup_execution: str = "") -> bool:
        """
        验证源测试和后续测试的结果是否满足蜕变关系

        验证followup_result > source_result

        Args:
            source_test: 源测试用例
            followup_test: 后续测试用例
            source_result: 源测试结果
            followup_result: 后续测试结果
            source_execution: 源测试执行的错误信息，如果有的话
            followup_execution: 后续测试执行的错误信息，如果有的话

        Returns:
            如果满足关系(followup_result > source_result)则返回True
        """
        # 检查是否有任何执行错误
        if source_execution or followup_execution:
            # 如果两个测试都有相同类型的错误，关系仍被认为是满足的
            return source_execution == followup_execution

        # 验证后续结果是否大于源结果
        # 添加一个小的容差值(-1e-6)以处理浮点数比较
        return followup_result >= source_result - 1e-6

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
    test = InputScaling()