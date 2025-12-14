from typing import List

from geometricSum.model.metamorphic_relation import MetamorphicRelation
from geometricSum.model.test_case import TestCase
from geometricSum.mutants.origin import geometric_sum


class MR2Relation(MetamorphicRelation):
    """
    MR2: 减法关系
    
    给定源测试用例x，生成后续测试用例 x - p，其中 p = 1 / (2^(sut(x) - 2))
    验证 如果 0 < st < 2 且 0 < ft < 2，则 sut(ft) == sut(st) - 1
    """
    

    
    def get_id(self) -> str:
        """获取蜕变关系ID"""
        return "mr_2"
    
    def get_description(self) -> str:
        """获取蜕变关系描述"""
        return "减法关系 - 给定源测试用例x，生成后续测试用例 x - p，其中 p = 1 / (2^(sut(x) - 2))，验证特定条件下 sut(ft) == sut(st) - 1"
    
    def generate_followup_tests(self, source_test: TestCase) -> List[TestCase]:
        """
        基于源测试用例生成后续测试用例
        
        创建一个新的输入值: x - p，其中 p = 1 / (2^(sut(x) - 2))
        
        Args:
            source_test: 源测试用例
            
        Returns:
            包含一个后续测试用例的列表
        """
        followup_tests = []        
        # 获取源测试的x值
        x_value = source_test.get_x_value()
        source_result = geometric_sum(x_value)
        
        # 计算p值
        p = pow(2, source_result - 2)
        p = 1 / p
        
        # 计算后续测试的x值
        new_x_value = x_value - p
        
        # 创建新的测试用例
        followup_test = TestCase(new_x_value)
        followup_tests.append(followup_test)
        
        return followup_tests
    
    def verify_relation(self, source_test: TestCase, followup_test: TestCase,
                        source_result: int, followup_result: int) -> bool:
        """
        验证源测试和后续测试的结果是否满足蜕变关系
        
        验证 如果 0 < st < 2 且 0 < ft < 2，则 sut(ft) == sut(st) - 1
        
        Args:
            source_test: 源测试用例
            followup_test: 后续测试用例
            source_result: 源测试结果
            followup_result: 后续测试结果
            
        Returns:
            如果满足关系则返回True
        """
        # 获取源测试和后续测试的x值
        st = source_test.get_x_value()
        ft = followup_test.get_x_value()
        
        # 检查条件
        if 0 < st < 2 and 0 < ft < 2:
            # 验证关系
            return followup_result == source_result - 1
        else:
            # 如果不满足条件，则关系自动满足
            return True
    
    def is_applicable_to(self, test_case: TestCase) -> bool:
        """
        检查蜕变关系是否适用于给定的测试用例
        
        这个关系对所有测试用例都适用
        
        Args:
            test_case: 要检查的测试用例
            
        Returns:
            如果SUT函数已设置则返回True
        """
        return True