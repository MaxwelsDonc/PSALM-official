from abc import ABC, abstractmethod
from typing import List

from python.mortgageRate.model.metamorphic_group import MetamorphicGroup
from python.mortgageRate.utils.load_mrs_utils import load_all_metamorphic_relations
from python.mortgageRate.model.test_case import TestCase


class MetamorphicRelation(ABC):
    """
    蜕变关系的抽象基类

    定义了蜕变关系应实现的接口，包括后续测试用例生成和关系验证

    所有具体的蜕变关系类都应该继承这个基类并实现其抽象方法
    """

    @abstractmethod
    def get_id(self) -> str:
        """
        获取蜕变关系的ID

        Returns:
            str: 蜕变关系ID (例如 "mr_1")
        """
        pass

    @abstractmethod
    def get_description(self) -> str:
        """
        获取蜕变关系的描述

        Returns:
            str: 蜕变关系的人类可读描述
        """
        pass

    @abstractmethod
    def generate_followup_tests(self, source_test: TestCase) -> List[TestCase]:
        """
        基于源测试用例生成后续测试用例

        Args:
            source_test (test_case): 源测试用例

        Returns:
            List[TestCase]: 生成的后续测试用例列表
        """
        pass

    @abstractmethod
    def verify_relation(self, source_test: TestCase, followup_test: TestCase,
                        source_result: float, followup_result: float,
                        source_execution: str = "", followup_execution: str = "") -> bool:
        """
        验证源测试和后续测试的结果是否满足蜕变关系

        Args:
            source_test (test_case): 源测试用例
            followup_test (test_case): 后续测试用例
            source_result (float): 源测试结果
            followup_result (float): 后续测试结果
            source_execution (str, optional): 源测试执行的错误信息，如果有的话
            followup_execution (str, optional): 后续测试执行的错误信息，如果有的话

        Returns:
            bool: 如果满足蜕变关系返回True，否则返回False
        """
        pass

    @abstractmethod
    def is_applicable_to(self, test_case: TestCase) -> bool:
        """
        检查蜕变关系是否适用于给定的测试用例

        Args:
            test_case (test_case): 要检查的测试用例

        Returns:
            bool: 如果蜕变关系适用于给定的测试用例返回True，否则返回False
        """
        pass

    def create_groups(self, source_test: TestCase) -> List[MetamorphicGroup]:
        """
        基于源测试用例创建蜕变组

        Args:
            source_test (test_case): 源测试用例

        Returns:
            List[metamorphic_group]: 创建的蜕变组列表
        """
        groups = []

        # 检查蜕变关系是否适用于源测试用例
        if not self.is_applicable_to(source_test):
            return groups

        # 生成后续测试用例
        followup_tests = self.generate_followup_tests(source_test)

        # 为每个后续测试用例创建一个蜕变组
        for followup_test in followup_tests:
            group = MetamorphicGroup(
                self.get_id(),
                self.get_description(),
                source_test,
                followup_test
            )
            groups.append(group)

        return groups


class MetamorphicRelationFactory:
    """
    蜕变关系工厂类

    提供用于创建和管理蜕变关系的静态方法
    """

    @staticmethod
    def create_all_relations() -> List[MetamorphicRelation]:
        """
        创建所有支持的蜕变关系

        Returns:
            List[MetamorphicRelation]: 所有蜕变关系的列表
        """
        # 创建并返回所有蜕变关系的实例
        return load_all_metamorphic_relations()

    @staticmethod
    def generate_metamorphic_groups(test_case: TestCase) -> List[MetamorphicGroup]:
        """
        为测试用例生成所有适用的蜕变组

        Args:
            test_case (test_case): 源测试用例

        Returns:
            List[metamorphic_group]: 生成的蜕变组列表
        """
        groups = []

        for relation in MetamorphicRelationFactory.create_all_relations():
            if relation.is_applicable_to(test_case):
                groups.extend(relation.create_groups(test_case))

        return groups