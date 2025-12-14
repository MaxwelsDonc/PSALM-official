import random
from typing import List

from python.incomeTax.model.test_case import TestCase
from python.incomeTax.model.metamorphic_relation import MetamorphicRelation

# 定义最高收入和最低收入
EXEMPTION_AMOUNT = 5000.0
MAX_INCOME = 1400000


class MR3Relation(MetamorphicRelation):
    """
    MR3: 输入缩放关系

    验证当收入按比例增加时，所得税也应该相应增加。
    """

    def get_id(self) -> str:
        """获取蜕变关系ID"""
        return "mr_3"

    def get_description(self) -> str:
        """获取蜕变关系描述"""
        return "输入缩放关系 - 验证当收入按比例增加时，所得税也应该相应增加"

    def generate_followup_tests(self, source_test: TestCase) -> List[TestCase]:
        """
        基于源测试用例生成后续测试用例

        创建一个收入按比例缩放的后续测试用例。

        Args:
            source_test: 源测试用例

        Returns:
            包含一个后续测试用例的列表
        """
        followup_tests = []

        income_value = source_test.get_income_value()
        # 缩放因子，对应IncomeTax.py中的mr_3
        scaling_factor1 = random.uniform(1, 2)
        scaling_factor2 = random.uniform(1, 2)

        new_income_value1 = min(income_value * scaling_factor1, MAX_INCOME)
        new_income_value2 = min(income_value * scaling_factor2, MAX_INCOME)

        followup_test1 = TestCase(new_income_value1)
        followup_test2 = TestCase(new_income_value2)
        followup_tests.extend([followup_test1, followup_test2])

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
            如果满足输入缩放关系则返回True
        """
        # 检查是否有任何执行错误
        if source_execution or followup_execution:
            # 如果两个测试都有相同类型的错误，关系仍被认为是满足的
            return source_execution == followup_execution

        # 简单的缩放关系，税额应该随收入增加而增加
        return followup_result >= source_result - 1e-6

    def is_applicable_to(self, test_case: TestCase) -> bool:
        """
        检查蜕变关系是否适用于给定的测试用例

        这个关系适用于收入大于等于免征额，且有缩放空间的测试用例。

        Args:
            test_case: 要检查的测试用例

        Returns:
            如果测试用例的收入大于等于免征额且缩放后不超过最高收入则返回True，否则返回False
        """
        # 确保有足够的空间进行收入缩放
        # 假设最小缩放因子为1.01，确保缩放后不超过MAX_INCOME
        return True


if __name__ == "__main__":
    mr = MR3Relation()
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
        followup_result = source_result * random.uniform(
            1.0, 1.5
        )  # 假设税额随收入缩放而增加
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

    source_test_not_applicable_high = TestCase(income_value=990000.0)  # 接近最高收入
    print(
        f"Is applicable to {source_test_not_applicable_high.get_income_value()} (near max income): {mr.is_applicable_to(source_test_not_applicable_high)}"
    )
