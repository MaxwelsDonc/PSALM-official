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