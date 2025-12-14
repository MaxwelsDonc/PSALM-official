class TestCase:
    """
    表示抵押贷款测试中的测试用例

    该类保存测试用例的输入参数(房屋价值)和分区信息

    Attributes:
        house_value (float): 房屋价值，抵押贷款计算的输入
        partition_id (int): 测试用例所属的分区ID
    """

    def __init__(self, house_value, partition_id=None):
        """
        创建一个新的测试用例

        Args:
            house_value (float): 房屋价值
            partition_id (int, optional): 测试用例所属的分区ID。如果未提供，将自动确定。
        """
        self.house_value = round(house_value,2)

        # 如果未提供分区ID，则根据房屋价值自动确定
        if partition_id is None:
            self.partition_id = self._determine_partition()
        else:
            self.partition_id = partition_id

    def _determine_partition(self):
        """
        根据房屋价值确定分区ID

        对于抵押贷款测试，通常基于阈值(例如205000)将测试用例分为不同分区

        Returns:
            int: 分区ID (1表示低于阈值，2表示高于或等于阈值)
        """
        if self.house_value < 205000:
            return 1  # 分区1: 低于阈值的房屋价值
        else:
            return 2  # 分区2: 高于或等于阈值的房屋价值

    def get_house_value(self):
        """获取房屋价值"""
        return self.house_value

    def get_partition_id(self):
        """获取分区ID"""
        return self.partition_id

    def __str__(self):
        """
        返回测试用例的字符串表示

        Returns:
            str: 格式化的测试用例字符串
        """
        return f"Partition {self.partition_id}: house_value={self.house_value}"

    def __repr__(self):
        """
        返回测试用例的代码表示形式

        Returns:
            str: 可用于重建对象的字符串表示
        """
        return f"TestCase({self.house_value}, {self.partition_id})"