import json
from python.incomeTax.utils.get_path_utils import get_config_path


class IncomeTaxConfigExtractor:
    def __init__(self, config_path):
        with open(config_path, "r") as file:
            self.config = json.load(file)

        self.input_range = self._extract_input_range()
        self.partition_ratio = self._extract_partition_ratio()
        self.partitions = self._extract_partitions()
        self.mr_types = self._extract_mr_types()

    def _extract_input_range(self):
        return self.config.get("inputRange", {})

    def _extract_partition_ratio(self):
        return self.config.get("partitionRatio", [])

    def _extract_partitions(self):
        # 返回每个分区的结构体 [{min, max, left, right}, ...]
        return self.config.get("partitionRange", [])

    def _extract_mr_types(self):
        # 直接返回字典，无需正则
        return self.config.get("mrType", {})

    def summary(self):
        return {
            "input_range": self.input_range,
            "partition_ratio": self.partition_ratio,
            "partitions": self.partitions,
            "mr_types": self.mr_types,
        }


if __name__ == "__main__":
    config_path = incomeTax.utils.get_path_utils.get_config_path()
    extractor = IncomeTaxConfigExtractor(config_path)
    print(extractor.summary())
