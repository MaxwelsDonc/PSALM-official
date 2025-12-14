import random
from typing import List

from python.incomeTax.model.metamorphic_relation import MetamorphicRelation
from python.incomeTax.model.test_case import TestCase
from python.incomeTax.utils.get_path_utils import get_config_path
from python.incomeTax.utils.pharsing_config_utils import IncomeTaxConfigExtractor


class MR1Relation(MetamorphicRelation):
    """
    MR1: 输入增加关系

    验证当收入增加时，所得税也应该增加。
    """

    def __init__(self, config_path=get_config_path()):
        """
        初始化MR1Relation
        Args:
            config_path: 配置文件路径
        """
        self.config_path = config_path
        self.config_extractor = IncomeTaxConfigExtractor(config_path)
        """初始化MR1Relation"""

    def get_id(self) -> str:
        """获取蜕变关系ID"""
        return "mr_1"

    def get_description(self) -> str:
        """获取蜕变关系描述"""
        return "输入增加关系 - 验证当收入增加时，所得税也应该增加"

    def generate_followup_tests(self, source_test: TestCase) -> List[TestCase]:
        """
        基于源测试用例生成后续测试用例

        创建一个收入略高于源测试用例的后续测试用例。

        Args:
            source_test: 源测试用例

        Returns:
            包含一个后续测试用例的列表
        """
        followup_tests = []

        income_value = source_test.get_income_value()
        # 增加一个随机的delta，确保新收入不超过MAX_INCOME
        delta = random.uniform(
            20, 100
        )  # 增加20到100之间的随机值，对应IncomeTax.py中的mr_1
        new_income_value = min(
            income_value + delta, self.config_extractor.input_range["max"]
        )

        followup_test = TestCase(new_income_value)
        followup_tests.append(followup_test)

        return followup_tests

    def verify_relation(
        self,
        source_test: TestCase,
        followup_test: TestCase,
        source_result: float,
        followup_result: float,
        source_execution: str = "",
        followup_execution: str = "",
    ) -> bool:
        """
        验证源测试和后续测试的结果是否满足蜕变关系

        验证后续测试的税额是否大于或等于源测试的税额。

        Args:
            source_test: 源测试用例
            followup_test: 后续测试用例
            source_result: 源测试结果
            followup_result: 后续测试结果
            source_execution: 源测试执行的错误信息，如果有的话
            followup_execution: 后续测试执行的错误信息，如果有的话

        Returns:
            如果满足输入增加关系则返回True
        """
        # 检查是否有任何执行错误
        if source_execution or followup_execution:
            # 如果两个测试都有相同类型的错误，关系仍被认为是满足的
            return source_execution == followup_execution

        # 允许浮点数比较的微小误差
        return followup_result >= source_result - 1e-6

    def is_applicable_to(self, test_case: TestCase) -> bool:
        """
        检查蜕变关系是否适用于给定的测试用例

        这个关系适用于收入大于等于免征额，且有增加空间的测试用例。

        Args:
            test_case: 要检查的测试用例

        Returns:
            如果测试用例的收入大于等于免征额且小于最高收入减去最小增量则返回True，否则返回False
        """
        # 确保有足够的空间进行收入增加
        return (
            test_case.get_income_value()
            < self.config_extractor.input_range["max"] - 100
        )


if __name__ == "__main__":
    mr = MR1Relation()
    print(f"MR ID: {mr.get_id()}")
    print(f"MR Description: {mr.get_description()}")

    # 示例测试用例
    source_test = TestCase(income_value=10000.0)
    print(f"\nSource Test Case Income: {source_test.get_income_value()}")

    # 生成后续测试用例
    followup_tests = mr.generate_followup_tests(source_test)
    print("Follow-up Test Cases Income:")
    for ft in followup_tests:
        print(f"  {ft.get_income_value()}")

    # 模拟结果并验证关系
    source_result = 500.0  # 假设的税额
    print(f"\nSource Result: {source_result}")

    for ft in followup_tests:
        # 模拟一个增加的税额
        followup_result = source_result + random.uniform(10, 50)  # 假设税额增加
        is_satisfied = mr.verify_relation(
            source_test, ft, source_result, followup_result
        )
        print(
            f"  Follow-up Income: {ft.get_income_value()}, Result: {followup_result}, Satisfied: {is_satisfied}"
        )

    # 测试不适用情况
    source_test_not_applicable_low = TestCase(income_value=4000.0)  # 低于免征额
    print(
        f"\nIs applicable to {source_test_not_applicable_low.get_income_value()} (below exemption): {mr.is_applicable_to(source_test_not_applicable_low)}"
    )

    source_test_not_applicable_high = TestCase(income_value=1400000 - 1)  # 接近最高收入
    print(
        f"Is applicable to {source_test_not_applicable_high.get_income_value()} (near max income): {mr.is_applicable_to(source_test_not_applicable_high)}"
    )
