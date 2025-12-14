import random
from typing import List

from python.incomeTax.model.test_case import TestCase
from python.incomeTax.model.metamorphic_relation import MetamorphicRelation
from python.incomeTax.mutants.origin import income_tax

MAX_INCOME = 1400000
MIN_INCOME = 1


class MR2Relation(MetamorphicRelation):
    """
    MR2: 合成型蜕变关系（Composite Metamorphic Relation）

    验证 income_tax(a) + income_tax(b) <= income_tax(a + b)
    """

    def get_id(self) -> str:
        """获取蜕变关系ID"""
        return "mr_2"

    def get_description(self) -> str:
        """获取蜕变关系描述"""
        return (
            "合成型蜕变关系 - 验证 income_tax(a) + income_tax(b) <= income_tax(a + b)"
        )

    def generate_followup_tests(self, source_test: TestCase) -> List[TestCase]:
        """
        随机生成一个比 source income 更大的 followup income（b > a），
        用于验证 income_tax(a) + income_tax(b - a) <= income_tax(b)
        """
        followup_tests = []

        income_value = source_test.get_income_value()
        # Generate two follow-up test cases
        for _ in range(2):
            # b 不要太小，避免0，保证一定的跨度
            followup_test_value = random.uniform(income_value, MAX_INCOME)
            followup_test = TestCase(followup_test_value)
            followup_tests.append(followup_test)

        return followup_tests

    def verify_relation(
        self,
        source_test: TestCase,  # a
        followup_test: TestCase,  # b
        source_result: float,  # income_tax(a)
        followup_result: float,  # income_tax(b)
        source_execution: str = "",
        followup_execution: str = "",
    ) -> bool:
        """
        验证 income_tax(a) + income_tax(b - a) <= income_tax(b)
        """
        # 检查是否有任何执行错误
        if source_execution or followup_execution:
            # 如果三者都报一样的错，算满足
            return source_execution == followup_execution

        # 允许极小浮点误差
        epsilon = 1e-6
        # 计算合成后的税额
        delta_income_value = (
            followup_test.get_income_value() - source_test.get_income_value()
        )
        delta_result = income_tax(delta_income_value)
        return source_result + delta_result - epsilon < followup_result

    def is_applicable_to(self, test_case: TestCase) -> bool:
        """
        检查蜕变关系是否适用于给定的测试用例

        适用于能与另外一个b合并后不超过MAX_INCOME的a。

        Args:
            test_case: a

        Returns:
            bool
        """
        return test_case.get_income_value() < MAX_INCOME - 1


if __name__ == "__main__":
    mr = MR2Relation()
    print(f"MR ID: {mr.get_id()}")
    print(f"MR Description: {mr.get_description()}")

    # 示例测试用例 a
    source_test = TestCase(income_value=60000.0)
    print(f"\nSource Test Case Income (a): {source_test.get_income_value()}")

    # 生成后续测试用例 b
    followup_tests = mr.generate_followup_tests(source_test)
    print("Follow-up Test Cases Income (b):")
    for ft in followup_tests:
        print(f"  {ft.get_income_value()}")

    # 验证合成关系
    source_result = income_tax(source_test.get_income_value())
    followup_result = income_tax(followup_tests[0].get_income_value())
    print(
        f"\nVerification Result: {mr.verify_relation(source_test, followup_tests[0], source_result, followup_result)}"
    )

    # 测试不适用情况
    source_test_not_applicable_low = TestCase(income_value=MAX_INCOME)  # 无法再合成
    print(
        f"\nIs applicable to {source_test_not_applicable_low.get_income_value()} (MAX_INCOME): {mr.is_applicable_to(source_test_not_applicable_low)}"
    )
