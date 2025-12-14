"""
This module implements metamorphic testing group functionality.

It provides the MetamorphicGroup class which represents a test group in metamorphic
testing, containing a source test case and a follow-up test case. The class manages
metamorphic relation information and associated test cases.

Classes:
    MetamorphicGroup: Represents a metamorphic testing group with source and follow-up test cases.
"""


class MetamorphicGroup:
    """
    表示蜕变测试中的测试组，包含一个源测试用例和一个后续测试用例

    该类保存蜕变关系的信息以及相关的测试用例

    Attributes:
        mr_id (str): 蜕变关系ID (例如, "mr_1")
        description (str): 蜕变关系的描述
        source_test (TestCase): 源测试用例
        followup_test (TestCase): 后续测试用例
    """

    def __init__(self, mr_id, description, source_test, followup_test):
        """
        创建一个新的蜕变测试组

        Args:
            mr_id (str): 蜕变关系ID
            description (str): 蜕变关系的描述
            source_test (TestCase): 源测试用例
            followup_test (TestCase): 后续测试用例
        """
        self.mr_id = mr_id
        self.description = description
        self.source_test = source_test
        self.followup_test = followup_test

    def get_mr_id(self):
        """获取蜕变关系ID"""
        return self.mr_id

    def get_description(self):
        """获取蜕变关系描述"""
        return self.description

    def get_source_test(self):
        """获取源测试用例"""
        return self.source_test

    def get_followup_test(self):
        """获取后续测试用例"""
        return self.followup_test

    def __str__(self):
        """
        返回蜕变测试组的字符串表示

        Returns:
            str: 格式化的蜕变测试组字符串
        """
        return (
            f"MetamorphicGroup[{self.mr_id}]: {self.description}\n"
            f"  Source: {self.source_test}\n"
            f"  Followup: {self.followup_test}"
        )

    def __repr__(self):
        """
        返回蜕变测试组的代码表示形式

        Returns:
            str: 可用于重建对象的字符串表示
        """
        return (
            f"MetamorphicGroup('{self.mr_id}', '{self.description}', "
            f"{repr(self.source_test)}, {repr(self.followup_test)})"
        )


if __name__ == "__main__":
    # 导入 TestCase 类用于测试
    from incomeTax.model.test_case import TestCase

    # 创建一些测试用例
    source_tc = TestCase(income_value=50000)
    followup_tc = TestCase(income_value=100000)

    # 创建 MetamorphicGroup 实例
    mg1 = MetamorphicGroup(
        mr_id="MR_1",
        description="收入增加蜕变关系",
        source_test=source_tc,
        followup_test=followup_tc,
    )

    print("\n--- 测试 MetamorphicGroup 类的功能 ---")
    print(mg1)

    # 测试 getter 方法
    print(f"\nMR ID: {mg1.get_mr_id()}")
    print(f"Description: {mg1.get_description()}")
    print(f"Source Test: {mg1.get_source_test()}")
    print(f"Followup Test: {mg1.get_followup_test()}")

    # 测试 __repr__ 方法
    print("\n--- 测试 __repr__ 方法 ---")
    print(repr(mg1))

    # 创建另一个 MetamorphicGroup 实例
    source_tc2 = TestCase(income_value=200000)
    followup_tc2 = TestCase(income_value=250000)
    mg2 = MetamorphicGroup(
        mr_id="MR_2",
        description="税率变化蜕变关系",
        source_test=source_tc2,
        followup_test=followup_tc2,
    )
    print(f"\n另一个蜕变组:\n{mg2}")
