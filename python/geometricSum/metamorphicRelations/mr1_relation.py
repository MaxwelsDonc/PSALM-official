from typing import List

from geometricSum.model.metamorphic_relation import MetamorphicRelation
from geometricSum.model.test_case import TestCase


class MR1Relation(MetamorphicRelation):
    """
    MR1: 平方关系
    
    给定源测试用例x，生成后续测试用例 x * x / 2
    验证 sut(ft) <= sut(st) + 1e-6
    """
    
    def get_id(self) -> str:
        """获取蜕变关系ID"""
        return "mr_1"
    
    def get_description(self) -> str:
        """获取蜕变关系描述"""
        return "平方关系 - 给定源测试用例x，生成后续测试用例 x * x / 2，验证 sut(ft) <= sut(st) + 1e-6"
    
    def generate_followup_tests(self, source_test: TestCase) -> List[TestCase]:
        """
        基于源测试用例生成后续测试用例
        
        创建一个新的输入值: x * x / 2
        
        Args:
            source_test: 源测试用例
            
        Returns:
            包含一个后续测试用例的列表
        """
        followup_tests = []
        
        # 获取源测试的x值
        x_value = source_test.get_x_value()
        
        # 计算后续测试的x值
        new_x_value = x_value * x_value / 2
        
        # 创建新的测试用例
        followup_test = TestCase(new_x_value)
        followup_tests.append(followup_test)
        
        return followup_tests
    
    def verify_relation(self, source_test: TestCase, followup_test: TestCase,
                        source_result: int, followup_result: int) -> bool:
        """
        验证源测试和后续测试的结果是否满足蜕变关系
        
        验证 sut(ft) <= sut(st) + 1e-6
        
        Args:
            source_test: 源测试用例
            followup_test: 后续测试用例
            source_result: 源测试结果
            followup_result: 后续测试结果
            
        Returns:
            如果满足关系则返回True
        """
        # 验证后续结果是否小于等于源结果加上一个小的容差值
        return followup_result <= source_result + 1e-6
    
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