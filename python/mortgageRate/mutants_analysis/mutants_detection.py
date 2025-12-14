# -*- coding: utf-8 -*-
from dataclasses import dataclass, field
from enum import Enum
import json
import logging
import os
import signal
from typing import Any, Dict, List, Optional, Set, Tuple

from python.mortgageRate.generation.phase1.random_generator import RandomGenerator
from python.mortgageRate.model.test_case import TestCase
from python.mortgageRate.mutants.mutants import mutants
from python.mortgageRate.mutants.origin import mortgage_rate
from python.mortgageRate.utils.get_path_utils import get_config_path
from python.mortgageRate.utils.load_mrs_utils import load_all_metamorphic_relations

# # 添加项目根目录到Python路径
# project_root = Path(__file__).parent.parent.parent.parent
# sys.path.insert(0, str(project_root))

# 导入所需模块


# 配置日志记录器
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - %(levelname)s - %(message)s",
    handlers=[logging.StreamHandler()],
)
logger = logging.getLogger(__name__)


class MutantType(Enum):
    """突变体类型枚举"""

    NORMAL = "normal"
    EQUIVALENT = "equivalent"
    SUBSUMED = "subsumed"
    ALLKILLED = "allkilled"
    ERROR = "error"
    TIMEOUT = "timeout"


@dataclass
class MutantResult:
    """突变体结果封装类"""

    results: Dict[str, Any] = field(default_factory=dict)  # 测试输入 -> 结果
    statuses: Dict[str, str] = field(default_factory=dict)  # 测试输入 -> 状态
    killed_by: Set[str] = field(default_factory=set)  # 被哪些测试用例kill
    subsumed_by: Set[str] = field(default_factory=set)  # 被哪些突变体包含
    subsumes: Set[str] = field(default_factory=set)  # 包含哪些突变体
    type: MutantType = MutantType.NORMAL


class MutantAnalysis:
    """
    主要功能：
    1. 生成测试用例
    2. 批量执行突变体测试
    3. 分析突变体类型和包含关系
    4. 计算最大独立集
    5. 生成分析报告
    """

    def __init__(self, timeout_seconds: int = 3):
        """
        初始化突变体分析器

        Args:
            timeout_seconds: 单个测试用例的超时时间（秒）
        """
        self.timeout_seconds = timeout_seconds
        self.mutant_names: List[str] = []
        self.mutant_results: Dict[str, MutantResult] = {}
        self.test_cases: List[TestCase] = []
        self.mutants_instance = mutants()
        self.metamorphic_relations = load_all_metamorphic_relations()
        self.config = self._load_config()

    def _load_config(self) -> Dict:
        """
        加载配置文件

        Returns:
            dict: 配置信息
        """
        config_path = get_config_path()
        try:
            with open(config_path, "r", encoding="utf-8") as f:
                return json.load(f)
        except FileNotFoundError:
            logger.warning("配置文件未找到: %s，使用默认配置", config_path)
            return {"partitionRange": []}

    def generate_test_cases(self, count: int = 1000) -> List[TestCase]:
        """
        生成测试用例（使用分区生成器）

        Args:
            count: 要生成的测试用例数量

        Returns:
            List[TestCase]: 生成的测试用例列表
        """
        try:
            # 使用分区生成器生成测试用例
            generator = RandomGenerator()
            self.test_cases = generator.generate(count)
            logger.info(f"生成了 {len(self.test_cases)} 个测试用例")
        except Exception as e:
            raise RuntimeError(f"使用随机生成器失败: {e}") from e

        return self.test_cases

    def load_mutants(self) -> None:
        """
        发现并加载突变体
        """
        self.mutant_names.clear()

        # 从test_subject字典获取突变体
        mutant_methods = []
        for name in self.mutants_instance.test_subject:
            if name.startswith("mutant_"):
                mutant_methods.append(name)

        # 按数字排序
        def extract_number(name):
            try:
                return int(name.split("_")[1])
            except (IndexError, ValueError):
                return 0

        mutant_methods.sort(key=extract_number)
        self.mutant_names = mutant_methods

        logger.info(f"发现 {len(self.mutant_names)} 个突变体: {self.mutant_names}")

    def _execute_with_timeout(self, func, test_case: TestCase) -> Tuple[Any, str]:
        """
        带超时的执行方法

        Args:
            func: 要执行的函数
            test_case: 测试用例

        Returns:
            Tuple[Any, str]: (结果, 状态)
        """
        x_value = test_case.get_house_value()

        def timeout_handler(signum, frame):
            raise TimeoutError("函数执行超时")

        try:
            # 设置超时信号
            signal.signal(signal.SIGALRM, timeout_handler)
            signal.alarm(self.timeout_seconds)

            # 执行函数
            result = func(x_value)

            # 取消超时信号
            signal.alarm(0)

            return result, "success"
        except TimeoutError:
            signal.alarm(0)  # 确保取消超时信号
            return None, "timeout"
        except Exception as e:
            signal.alarm(0)  # 确保取消超时信号
            logger.error(f"执行函数时发生异常: {str(e)}")
            return str(e), "error"

    def execute_all_tests(self) -> None:
        """
        批量执行所有测试（核心优化）
        """
        # 确保测试用例和突变体已加载
        if not self.test_cases:
            logger.info("警告：没有测试用例，使用默认生成方法")
            self.generate_test_cases()

        if not self.mutant_names:
            logger.info("警告：没有突变体，尝试加载突变体")
            self.load_mutants()

        logger.info("批量执行突变体测试...")
        logger.info(f"可用的突变体: {list(self.mutants_instance.test_subject.keys())}")
        logger.info(f"要测试的突变体: {self.mutant_names}")

        # 获取原始函数
        original_func = mortgage_rate

        for mutant_name in self.mutant_names:
            result = MutantResult()
            self.mutant_results[mutant_name] = result

            try:
                # 获取突变体函数
                if mutant_name not in self.mutants_instance.test_subject:
                    logger.error(f"突变体 {mutant_name} 不在 test_subject 字典中")
                    logger.debug(
                        f"可用的突变体: {list(self.mutants_instance.test_subject.keys())}"
                    )
                    result.type = MutantType.ERROR
                    continue

                mutant_func = self.mutants_instance.test_subject[mutant_name]

                # 执行所有测试用例
                for test_case in self.test_cases:
                    input_str = str(test_case.get_house_value())

                    # 执行原始函数
                    orig_result, orig_status = self._execute_with_timeout(
                        original_func, test_case
                    )

                    # 执行突变体
                    mut_result, mut_status = self._execute_with_timeout(
                        mutant_func, test_case
                    )

                    # 记录结果和状态
                    result.results[input_str] = mut_result
                    result.statuses[input_str] = mut_status

                    # 判断是否被kill
                    if mut_status == "timeout":
                        result.type = MutantType.TIMEOUT
                        break
                    elif mut_status == "error":
                        result.type = MutantType.ERROR
                    elif orig_status == "success" and mut_status == "success":
                        # 使用蜕变关系验证
                        if orig_result != mut_result:
                            result.killed_by.add(input_str)
                    elif orig_status == "success" and mut_status != "success":
                        result.killed_by.add(input_str)
                    elif orig_status != "success" and mut_status == "success":
                        result.killed_by.add(input_str)

                logger.info(f"{mutant_name}: {len(result.killed_by)} kills")

            except KeyError:
                logger.error(f"无法找到突变体方法: {mutant_name}")
                result.type = MutantType.ERROR
            except Exception as e:
                logger.error(f"突变体执行失败: {mutant_name} -> {str(e)}")
                result.type = MutantType.ERROR

    def analyze_mutants(self) -> None:
        """
        分析突变体类型和包含关系
        """
        logger.info("分析突变体类型...")

        # 第一步：基于killed_by集合判断突变体类型
        for mutant_name in self.mutant_names:
            result = self.mutant_results[mutant_name]

            if result.type == MutantType.NORMAL:
                # 检查是否为等价突变体
                if not result.killed_by:
                    result.type = MutantType.EQUIVALENT
                # 检查是否被所有测试用例杀死
                elif len(result.killed_by) == len(self.test_cases):
                    result.type = MutantType.ALLKILLED

        # 第二步：多轮迭代检查包含关系
        logger.info("检查包含关系...")
        found_new_subsumption = True
        iteration = 0

        while found_new_subsumption:
            iteration += 1
            logger.info(f"包含关系检查第 {iteration} 轮...")
            found_new_subsumption = False

            for i, mutant_a in enumerate(self.mutant_names):
                result_a = self.mutant_results[mutant_a]

                # 跳过已经确定为等价、错误或超时的突变体
                if result_a.type in [
                    MutantType.EQUIVALENT,
                    MutantType.ERROR,
                    MutantType.TIMEOUT,
                ]:
                    continue

                for j, mutant_b in enumerate(self.mutant_names):
                    if i == j:
                        continue

                    result_b = self.mutant_results[mutant_b]

                    # 跳过已经确定为等价、错误或超时的突变体
                    if result_b.type in [
                        MutantType.EQUIVALENT,
                        MutantType.ERROR,
                        MutantType.TIMEOUT,
                    ]:
                        continue

                    # 检查包含关系：如果A的所有kill都被B包含，且A不为空，则A包含B
                    if (
                        result_a.killed_by
                        and result_b.killed_by.issuperset(result_a.killed_by)
                        and len(result_a.killed_by) < len(result_b.killed_by)
                    ):

                        # 如果B之前不是SUBSUMED类型，现在变成SUBSUMED
                        if result_b.type != MutantType.SUBSUMED:
                            result_b.type = MutantType.SUBSUMED
                            found_new_subsumption = True
                        # 如果B之前已经是SUBSUMED类型，但发现了新的包含关系
                        elif (
                            result_b.type == MutantType.SUBSUMED
                            and mutant_a not in result_b.subsumed_by
                        ):
                            found_new_subsumption = True

                        result_b.subsumed_by.add(mutant_a)
                        result_a.subsumes.add(mutant_b)

                        logger.info(
                            f"发现包含关系: {mutant_a} 包含 {mutant_b} "
                            f"({mutant_a} kill_count: {len(result_a.killed_by)}, "
                            f"{mutant_b} kill_count: {len(result_b.killed_by)})"
                        )

        logger.info(f"包含关系检查完成，共进行了 {iteration} 轮")

    def calculate_maximum_independent_set(self) -> List[str]:
        """
        计算最大独立集

        Returns:
            List[str]: 最大独立集中的突变体列表
        """
        # 获取所有NORMAL类型和SUBSUMED类型的突变体
        maximum_mutants = []
        for mutant_name in self.mutant_names:
            result = self.mutant_results[mutant_name]
            if result.type in [MutantType.NORMAL, MutantType.SUBSUMED]:
                maximum_mutants.append(mutant_name)

        if not maximum_mutants:
            return []

        # 使用贪心算法计算最大独立集
        return self._find_maximum_independent_set_greedy(maximum_mutants)

    def _find_maximum_independent_set_greedy(self, candidates: List[str]) -> List[str]:
        """
        使用贪心算法寻找最大独立集

        Args:
            candidates: 候选突变体列表

        Returns:
            List[str]: 最大独立集
        """
        independent_set = []
        remaining = set(candidates)

        while remaining:
            # 找到度数最小的节点（被包含关系最少的突变体）
            min_degree_node = None
            min_degree = float("inf")

            for mutant in remaining:
                result = self.mutant_results[mutant]
                degree = 0

                # 计算与其他剩余节点的连接数（包含关系）
                for other in remaining:
                    if mutant != other:
                        other_result = self.mutant_results[other]
                        # 如果存在包含关系，则它们之间有边
                        if (
                            other in result.subsumed_by
                            or other in result.subsumes
                            or mutant in other_result.subsumed_by
                            or mutant in other_result.subsumes
                        ):
                            degree += 1

                if degree < min_degree:
                    min_degree = degree
                    min_degree_node = mutant

            if min_degree_node is not None:
                # 将度数最小的节点加入独立集
                independent_set.append(min_degree_node)
                remaining.remove(min_degree_node)

                # 移除所有与该节点相邻的节点
                selected_result = self.mutant_results[min_degree_node]
                to_remove = set()

                for other in remaining:
                    other_result = self.mutant_results[other]
                    # 如果存在包含关系，则移除相邻节点
                    if (
                        other in selected_result.subsumed_by
                        or other in selected_result.subsumes
                        or min_degree_node in other_result.subsumed_by
                        or min_degree_node in other_result.subsumes
                    ):
                        to_remove.add(other)

                remaining -= to_remove
            else:
                break

        return independent_set

    def generate_report(self, filename: Optional[str] = None) -> Dict:
        """
        生成JSON格式的分析报告

        Args:
            filename: 保存报告的文件名，如果为None则不保存文件

        Returns:
            Dict: 分析报告
        """
        self.analyze_mutants()

        # 统计各类型突变体数量
        type_groups = {mutant_type: [] for mutant_type in MutantType}

        for mutant_name in self.mutant_names:
            result = self.mutant_results[mutant_name]
            type_groups[result.type].append(mutant_name)

        # 构建报告
        report = {
            "statistics": {
                "total_mutants": len(self.mutant_names),
                "normal_mutants": len(type_groups[MutantType.NORMAL]),
                "equivalent_mutants": len(type_groups[MutantType.EQUIVALENT]),
                "subsumed_mutants": len(type_groups[MutantType.SUBSUMED]),
                "allkilled_mutants": len(type_groups[MutantType.ALLKILLED]),
                "timeout_mutants": len(type_groups[MutantType.TIMEOUT]),
                "error_mutants": len(type_groups[MutantType.ERROR]),
            }
        }
        # 计算并添加最大独立集信息
        max_independent_set = self.calculate_maximum_independent_set()
        report["maximum_independent_set"] = {
            "size": len(max_independent_set),
            "mutants": max_independent_set,
        }

        # 添加包含关系信息 - 合并相同subsumer的关系
        subsumption_dict = {}
        for mutant_name in self.mutant_names:
            result = self.mutant_results[mutant_name]
            if result.subsumes:  # 只有当该突变体包含其他突变体时才添加
                subsumption_dict[mutant_name] = list(result.subsumes)

        # 转换为新格式：每个subsumer对应一个包含所有被包含突变体的对象
        subsumption_relations = []
        for subsumer, subsumed_list in subsumption_dict.items():
            subsumption_relations.append(
                {"subsumer": subsumer, "subsumed": subsumed_list}
            )
        report["subsumption_relations"] = subsumption_relations

        # 按类型分组突变体
        mutants_by_type = {}
        for mutant_type in MutantType:
            mutants_by_type[mutant_type.value] = type_groups[mutant_type]
        report["mutants_by_type"] = mutants_by_type

        # 保存报告
        if filename:
            try:
                with open(filename, "w", encoding="utf-8") as f:
                    json.dump(report, f, indent=2, ensure_ascii=False)
                logger.info(f"JSON报告已保存到: {filename}")
            except Exception as e:
                logger.error(f"保存JSON报告失败: {str(e)}")
        else:
            logger.info(f"分析报告: {json.dumps(report, indent=2, ensure_ascii=False)}")

        return report

    def save_results(self, filename: str) -> None:
        """
        保存结果到CSV

        Args:
            filename: CSV文件名
        """
        try:
            import csv

            with open(filename, "w", newline="", encoding="utf-8") as csvfile:
                writer = csv.writer(csvfile)

                # 表头 - 第一列是TestCase，后面每两列是一个突变体的result和status
                header = ["TestCase"]
                for mutant_name in self.mutant_names:
                    header.extend([f"{mutant_name}_result", f"{mutant_name}_status"])
                writer.writerow(header)

                # 数据 - 每行是一个测试用例，每列是对应突变体的结果
                for test_case in self.test_cases:
                    input_str = str(test_case.get_house_value())
                    row = [f'"{input_str}"']

                    for mutant_name in self.mutant_names:
                        result = self.mutant_results[mutant_name]
                        res = result.results.get(input_str, "")
                        status = result.statuses.get(input_str, "unknown")
                        row.extend([f'"{res}"', status])

                    writer.writerow(row)

                # 额外添加突变体信息行
                type_row = ["MutantType"]
                for mutant_name in self.mutant_names:
                    result = self.mutant_results[mutant_name]
                    type_row.extend([result.type.value, ""])
                writer.writerow(type_row)

                kill_count_row = ["KillCount"]
                for mutant_name in self.mutant_names:
                    result = self.mutant_results[mutant_name]
                    kill_count_row.extend([len(result.killed_by), ""])
                writer.writerow(kill_count_row)

            logger.info(f"结果已保存到: {filename}")
        except Exception as e:
            logger.error(f"保存失败: {str(e)}")


if __name__ == "__main__":
    try:
        logger.info("🚀 开始简化版突变体分析...")

        analyzer = MutantAnalysis()
        test_case_count = 1000  # 默认值
        logger.info(f"将生成 {test_case_count} 个测试用例")

        # 生成指定数量的测试用例并执行测试
        analyzer.load_mutants()
        analyzer.generate_test_cases(test_case_count)
        analyzer.execute_all_tests()

        # 生成报告
        base_dir = os.path.dirname(__file__)
        analyzer.generate_report(os.path.join(base_dir, "mutant_analysis_report.json"))

        # 保存结果
        analyzer.save_results(os.path.join(base_dir, "simplified_mutant_analysis.csv"))

        logger.info("\n🎉 分析完成！")

    except Exception as e:
        logger.error(f"执行失败: {str(e)}")
        raise
