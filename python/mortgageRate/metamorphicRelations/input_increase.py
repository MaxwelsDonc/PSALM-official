import os
import random
from typing import List

from python.mortgageRate.model.metamorphic_relation import MetamorphicRelation
from python.mortgageRate.model.test_case import TestCase
from python.mortgageRate.utils.pharsing_config_utils import MortgageConfigExtractor
from python.mortgageRate.utils.get_path_utils import get_config_path


class InputIncrease(MetamorphicRelation):
    def __init__(self):
        seed = os.path.basename(__file__)
        random.seed(seed)
        # 从配置文件加载类型信息
        config_path = get_config_path()
        self.config_extractor = MortgageConfigExtractor(config_path)
        self.follow_up_count = self.config_extractor.mr_types.get(self.get_id())

    def get_id(self) -> str:
        return "mr_1"

    def get_description(self) -> str:
        return "输入增加关系 - 验证当房屋价值增加时，抵押贷款输出也应该增加"

    def generate_followup_tests(self, source_test: TestCase) -> List[TestCase]:
        followup_tests = []
        house_value = source_test.get_house_value()

        # 生成指定数量的后续测试用例
        for _ in range(self.follow_up_count):
            # 使用不同的增量值以创建不同的后续测试
            delta = random.randint(20, 100)
            new_house_value = min(house_value + delta, 600000)

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
        return followup_result > source_result - 1e-6

    def is_applicable_to(self, test_case: TestCase) -> bool:
        """
        检查蜕变关系是否适用于给定的测试用例

        这个关系对大多数测试用例都适用，除非房屋价值已经接近上限

        Args:
            test_case: 要检查的测试用例

        Returns:
            如果测试用例的房屋价值小于上限减去最大增量则返回True
        """
        max_delta = 100
        max_value = 600000

        # 如果房屋价值太接近上限，可能无法增加足够的量
        return test_case.get_house_value() < max_value - 20  # 20是最小增量

if __name__ == '__main__':
    test = InputIncrease()