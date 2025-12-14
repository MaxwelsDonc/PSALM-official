"""
This module contains the TestCase class for income tax calculation testing.

The TestCase class represents test cases used in income tax calculations,
storing input parameters (income values) and partition information.
"""

class TestCase:
    """
    表示所得税计算中的测试用例

    该类保存测试用例的输入参数(收入值)和分区信息

    Attributes:
        income_value (float): 收入值，所得税计算的输入
        partition_id (int): 测试用例所属的分区ID
    """

    def __init__(self, income_value, partition_id=None):
        """
        创建一个新的测试用例

        Args:
            income_value (float): 收入值
            partition_id (int, optional): 测试用例所属的分区ID。如果未提供，将自动确定。
        """
        self.income_value = round(income_value, 2)

        # 如果未提供分区ID，则根据收入值自动确定
        if partition_id is None:
            self.partition_id = self._determine_partition()
        else:
            self.partition_id = partition_id

    def _determine_partition(self):
        """
        根据收入值确定分区ID

        对应 partitionRange 中的区间：
        - 注意区间是左开右闭，除了第一个是闭区间。
        - 返回的分区ID从 1 开始。
        
        Returns:
            int: 分区ID（1 ~ 6）
        """
        income = self.income_value

        if 1 <= income <= 20000:
            return 1
        elif 20000 < income <= 30000:
            return 2
        elif 30000 < income <= 40000:
            return 3
        elif 40000 < income <= 50000:
            return 4
        elif 50000 < income <= 500000:
            return 5
        elif 500000 < income <= 1400000:
            return 6
        else:
            raise ValueError(f"收入值 {income} 超出支持范围 [1, 1,400,000]")


    def get_income_value(self):
        """获取收入值"""
        return self.income_value

    def get_partition_id(self):
        """获取分区ID"""
        return self.partition_id

    def __str__(self):
        """
        返回测试用例的字符串表示

        Returns:
            str: 格式化的测试用例字符串
        """
        return f"Partition {self.partition_id}: income_value={self.income_value}"

    def __repr__(self):
        """
        返回测试用例的代码表示形式

        Returns:
            str: 可用于重建对象的字符串表示
        """
        return f"TestCase({self.income_value}, {self.partition_id})"


if __name__ == "__main__":
    # 测试不同收入值的分区
    test_cases = [
        TestCase(income_value=10000),   # Should be in partition 1
        TestCase(income_value=25000),   # Should be in partition 2
        TestCase(income_value=35000),   # Should be in partition 3
        TestCase(income_value=45000),   # Should be in partition 4
        TestCase(income_value=100000),  # Should be in partition 5
        TestCase(income_value=800000),  # Should be in partition 6
    ]

    print("\n--- 测试 TestCase 类的分区逻辑 ---")
    for tc in test_cases:
        print(f"收入: {tc.get_income_value()}, 分区ID: {tc.get_partition_id()}")

    # 测试指定分区ID的创建
    tc_with_id = TestCase(income_value=10000, partition_id=99)
    print(f"\n--- 测试指定分区ID的 TestCase ---")
    print(f"收入: {tc_with_id.get_income_value()}, 指定分区ID: {tc_with_id.get_partition_id()}")

    # 测试 __str__ 和 __repr__ 方法
    print("\n--- 测试 __str__ 和 __repr__ 方法 ---")
    print(f"__str__ 结果: {test_cases[0]}")
    print(f"__repr__ 结果: {repr(test_cases[0])}")
        