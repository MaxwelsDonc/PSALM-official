class TestCase:
    """
    表示几何和测试中的测试用例

    该类保存测试用例的输入参数(x值)和分区信息

    Attributes:
        x_value (float): x值，几何和计算的输入
        partition_id (int): 测试用例所属的分区ID
    """

    def __init__(self, x_value, partition_id=None):
        """
        创建一个新的测试用例

        Args:
            x_value (float): x值
            partition_id (int, optional): 测试用例所属的分区ID。如果未提供，将自动确定。
        """
        self.x_value = round(x_value, 4)

        # 如果未提供分区ID，则根据x值自动确定
        if partition_id is None:
            self.partition_id = self._determine_partition()
        else:
            self.partition_id = partition_id

    def _determine_partition(self):
        """
        根据x值确定分区ID

        对于几何和测试，基于预定义的范围将测试用例分为7个不同分区

        Returns:
            int: 分区ID (1-7)
        """
        # 分区范围定义，基于GeometricSum类中的定义
        part_ranges = {
            1: [(1 - 1) / 1, (2 - 1) / 1],                # [0, 1]
            2: [(2 - 1) / 1, (4 - 1) / 2],                # [1, 1.5]
            3: [(4 - 1) / 2, (8 - 1) / 4],                # [1.5, 1.75]
            4: [(8 - 1) / 4, (16 - 1) / 8],               # [1.75, 1.875]
            5: [(16 - 1) / 8, (32 - 1) / 16],             # [1.875, 1.9375]
            6: [(32 - 1) / 16, (64 - 1) / 32],            # [1.9375, 1.96875]
            7: [(64 - 1) / 32, 2 - 1e-4]                  # [1.96875, 2-1e-4]
        }

        for part_id, range_values in part_ranges.items():
            if range_values[0] <= self.x_value < range_values[1]:
                return part_id

        # 默认返回最后一个分区
        return 7

    def get_x_value(self):
        """获取x值"""
        return self.x_value

    def get_partition_id(self):
        """获取分区ID"""
        return self.partition_id

    def __str__(self):
        """
        返回测试用例的字符串表示

        Returns:
            str: 格式化的测试用例字符串
        """
        return f"Partition {self.partition_id}: x_value={self.x_value}"

    def __repr__(self):
        """
        返回测试用例的代码表示形式

        Returns:
            str: 可用于重建对象的字符串表示
        """
        return f"TestCase({self.x_value}, {self.partition_id})"