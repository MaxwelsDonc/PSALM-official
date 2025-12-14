from typing import List

from python.mortgageRate.model.test_case import TestCase
from python.mortgageRate.model.metamorphic_relation import MetamorphicRelation


class EquivalentInputs(MetamorphicRelation):
    """
    MR5: 等价输入关系

    验证某些"特殊"输入应该产生相同的结果
    具体而言: f(x) = f(x/205000*x)

    这是基于抵押贷款计算中的阈值逻辑设计的特殊关系
    """

    def get_id(self) -> str:
        """获取蜕变关系ID"""
        return "mr_5"

    def get_description(self) -> str:
        """获取蜕变关系描述"""
        return "等价输入关系 - 验证特定的函数输入应该产生相同的结果"

    def generate_followup_tests(self, source_test: TestCase) -> List[TestCase]:
        """
        基于源测试用例生成后续测试用例

        创建一个新的输入值: x/205000*x

        Args:
            source_test: 源测试用例

        Returns:
            包含一个后续测试用例的列表
        """
        followup_tests = []

        # 获取源测试的房屋价值
        house_value = source_test.get_house_value()

        # 计算特殊的等价输入值
        new_house_value = house_value / 205000 * house_value

        # 创建新的测试用例
        followup_test = TestCase(new_house_value)
        followup_tests.append(followup_test)

        return followup_tests

    def verify_relation(self, source_test: TestCase, followup_test: TestCase,
                        source_result: float, followup_result: float,
                        source_execution: str = "", followup_execution: str = "") -> bool:
        """
        验证源测试和后续测试的结果是否满足蜕变关系

        验证source_result = followup_result

        Args:
            source_test: 源测试用例
            followup_test: 后续测试用例
            source_result: 源测试结果
            followup_result: 后续测试结果
            source_execution: 源测试执行的错误信息，如果有的话
            followup_execution: 后续测试执行的错误信息，如果有的话

        Returns:
            如果两个结果相等则返回True
        """
        # 检查是否有任何执行错误
        if source_execution or followup_execution:
            # 如果两个测试都有相同类型的错误，关系仍被认为是满足的
            return source_execution == followup_execution

        # 验证结果是否相等(考虑浮点精度)
        return abs(followup_result - source_result) < 1e-6

    def is_applicable_to(self, test_case: TestCase) -> bool:
        """
        检查蜕变关系是否适用于给定的测试用例

        这个关系对所有测试用例都适用

        Args:
            test_case: 要检查的测试用例

        Returns:
            通常返回True，表示这个关系对所有测试用例都适用
        """
        return True