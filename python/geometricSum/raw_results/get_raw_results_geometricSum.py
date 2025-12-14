"""
测试用例生成方法有效性实验脚本

该脚本用于探究 geometricSum/generation/phase1 中不同测试用例生成方法的有效性。
通过对每个突变体在不同测试用例数量下的 P-measure 值进行评估，
分析各种生成策略的效果。
"""

import importlib
import importlib.util
import json
from pathlib import Path
import random
import time
from typing import Any, Dict, List

from tqdm import tqdm

from python.geometricSum.model.metamorphic_group import MetamorphicGroup
from python.geometricSum.model.test_case import TestCase
from python.geometricSum.mutants.mutants import mutants
from python.geometricSum.utils.get_path_utils import get_config_path
from python.geometricSum.utils.load_mrs_utils import load_all_metamorphic_relations
from python.geometricSum.utils.mg_domain_construction_utils import MGDomainGenerator
from python.geometricSum.utils.pharsing_config_utils import GeometricSumConfigExtractor


class TestCaseGenerationEffectivenessExperiment:
    """
    测试用例生成方法有效性实验类
    """

    def __init__(
        self,
        program: str,
        phase: str,
        config_path=None,
        file_path=None,
        internal_iteration=1000,
        external_iteration=50,
        max_tcs_num=35,
        min_tcs_num=1,
    ):
        """
        初始化实验

        Args:
            config_path: 配置文件路径
            file_path: 测试用例生成器文件路径
            internal_iteration: 内部迭代次数
            external_iteration: 外层重复次数
            max_tcs_num: 最大测试用例数量
            min_tcs_num: 最小测试用例数量
        """
        if config_path is None:
            config_path = get_config_path()

        self.config_extractor = GeometricSumConfigExtractor(config_path)
        self.mutants_instance = mutants()
        self.metamorphic_relations = load_all_metamorphic_relations()

        # 获取分区数量
        self.partitions_num = len(self.config_extractor.partitions)

        # 绑定实验参数到self
        self.file_path = file_path
        self.internal_iteration = internal_iteration
        self.external_iteration = external_iteration
        self.max_tcs_num = max_tcs_num
        self.min_tcs_num = min_tcs_num
        self.phase = phase
        self.program = program

        # 创建结果目录
        self.results_dir = Path(f"RQs/RQ1/raw_results/{program}/{phase}")
        self.results_dir.mkdir(parents=True, exist_ok=True)

        # 保存参数配置
        self.save_experiment_config()

    def save_experiment_config(self):
        """
        保存实验参数配置到文件
        """
        config = {
            "file_path": self.file_path,
            "internal_iteration": self.internal_iteration,
            "external_iteration": self.external_iteration,
            "max_tcs_num": self.max_tcs_num,
            "min_tcs_num": self.min_tcs_num,
            "partitions_num": self.partitions_num,
        }

        config_file = (
            self.results_dir / f"experiment_config_{Path(self.file_path).stem}.json"
        )
        with open(config_file, "w", encoding="utf-8") as f:
            json.dump(config, f, indent=2, ensure_ascii=False)

        print(f"实验配置已保存到: {config_file}")

    def load_test_generator(self, file_path: str):
        """
        动态加载测试用例生成器

        Args:
            file_path: 生成器文件路径

        Returns:
            生成器实例
        """
        spec = importlib.util.spec_from_file_location("generator", file_path)
        module = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(module)

        # 查找生成器类（假设类名以Generator结尾）
        for attr_name in dir(module):
            attr = getattr(module, attr_name)
            if (
                isinstance(attr, type)
                and (
                    attr_name.endswith("Generator") or attr_name.endswith("GeneratorMG")
                )
                and hasattr(attr, "generate")
            ):
                return attr()

        raise ValueError(f"在文件 {file_path} 中未找到有效的生成器类")

    def generate_metamorphic_groups(
        self, source_tests: List[TestCase]
    ) -> List[MetamorphicGroup]:
        """
        每个源测试用例只生成 1 个 MG（随机选一个 MR）

        Args:
            source_tests: 源测试用例列表

        Returns:
            MG 列表
        """
        metamorphic_groups = []

        for source_test in source_tests:
            # 随机选择一个 MR
            mr = random.choice(self.metamorphic_relations)
            followup_tests = mr.generate_followup_tests(source_test)

            if not followup_tests:
                continue

            # 只选第一个 follow-up（通常只有一个）
            followup_test = random.choice(followup_tests)

            mg = MetamorphicGroup(
                mr_id=mr.get_id(),
                description=mr.get_description(),
                source_test=source_test,
                followup_test=followup_test,
            )
            metamorphic_groups.append(mg)

        return metamorphic_groups

    def execute_metamorphic_test(self, mutant_func, mg: MetamorphicGroup) -> bool:
        """
        执行蜕变测试

        Args:
            mutant_func: 突变体函数
            mg: 蜕变测试组

        Returns:
            是否触发缺陷
        """
        try:
            source_result = mutant_func(mg.get_source_test().get_x_value())

            # 执行后续测试用例
            followup_result = mutant_func(mg.get_followup_test().get_x_value())

            # 获取对应的蜕变关系
            mr = None
            for relation in self.metamorphic_relations:
                if relation.get_id() == mg.get_mr_id():
                    mr = relation
                    break

            if mr is None:
                return False

            # 验证蜕变关系
            return not mr.verify_relation(
                mg.get_source_test(),
                mg.get_followup_test(),
                source_result,
                followup_result,
            )

        except Exception:
            # 如果执行出错，认为触发了缺陷
            return True

    def calculate_p_measure(
        self,
        mg_domain,
        mutant_func,
        test_cases_num: int,
        generator,
        internal_iteration: int = 1000,
    ) -> float:
        """
        计算 P-measure 值

        Args:
            mutant_name: 突变体名称
            mutant_func: 突变体函数
            test_cases_num: 测试用例数量
            generator: 测试用例生成器
            internal_iteration: 内部迭代次数

        Returns:
            P-measure 值
        """
        defect_detected_count = 0

        for _ in range(internal_iteration):
            # 生成源测试用例
            if self.phase == "phase1":
                source_tests = generator.generate(test_cases_num)
                # 生成蜕变测试组
                metamorphic_groups = self.generate_metamorphic_groups(source_tests)
            elif self.phase == "phase2":
                # 从 MG 域中选择 MG

                metamorphic_groups = generator.generate(
                    test_cases_num, mg_domain=mg_domain
                )
            else:
                raise ValueError("phase 错误")

            for mg in metamorphic_groups:
                if self.execute_metamorphic_test(mutant_func, mg):
                    defect_detected_count += 1
                    break

        return (
            defect_detected_count / internal_iteration
            if internal_iteration > 0
            else 0.0
        )

    def run_experiment(
        self,
        file_path: str = None,
        internal_iteration: int = None,
        external_iteration: int = None,
        max_tcs_num: int = None,
        min_tcs_num: int = None,
    ) -> tuple[Dict[str, Any], Dict[str, Any]]:
        """
        运行完整实验

        Args:
            file_path: 测试用例生成策略脚本路径（可选，默认使用self中的值）
            internal_iteration: 内部采样次数（可选，默认使用self中的值）
            external_iteration: 外层重复次数（可选，默认使用self中的值）
            max_tcs_num: 最大的测试用例数目（可选，默认使用self中的值）
            min_tcs_num: 最小的测试用例数目（可选，默认使用self中的值）

        Returns:
            实验结果字典和时间记录字典的元组
        """
        # 使用传入参数或self中的默认值
        file_path = file_path or self.file_path
        internal_iteration = internal_iteration or self.internal_iteration
        external_iteration = external_iteration or self.external_iteration
        max_tcs_num = max_tcs_num or self.max_tcs_num
        min_tcs_num = min_tcs_num or self.min_tcs_num

        print(f"开始实验: {file_path}")
        print(f"内部迭代次数: {internal_iteration}")
        print(f"外层重复次数: {external_iteration}")
        print(f"测试用例数量范围: {min_tcs_num} - {max_tcs_num}")

        # 加载测试用例生成器
        generator = self.load_test_generator(file_path)

        results = {}
        time_records = {}
        mg_domain = MGDomainGenerator().generate_domain()

        # 遍历每个突变体
        for mutant_name, mutant_func in self.mutants_instance.test_subject.items():
            print(f"\n处理突变体: {mutant_name}")
            results[mutant_name] = {}
            time_records[mutant_name] = {}

            # 遍历不同的测试用例数量
            for test_cases_num in range(min_tcs_num, max_tcs_num + 1):
                print(f"  测试用例数量: {test_cases_num}")
                p_measures = []
                iteration_times = []

                # 重复 external_iteration 次
                for _ in tqdm(
                    range(external_iteration), desc=f"self.file_path:{self.file_path}"
                ):
                    # 记录开始时间
                    start_time = time.time()

                    p_measure = self.calculate_p_measure(
                        mg_domain,
                        mutant_func,
                        test_cases_num,
                        generator,
                        internal_iteration,
                    )

                    # 记录结束时间并计算耗时
                    end_time = time.time()
                    iteration_time = end_time - start_time

                    p_measures.append(p_measure)
                    iteration_times.append(iteration_time)

                results[mutant_name][str(test_cases_num)] = p_measures
                time_records[mutant_name][str(test_cases_num)] = iteration_times

        return results, time_records

    def save_results(self, results: Dict[str, Any], method_name: str):
        """
        保存实验结果

        Args:
            results: 实验结果
            method_name: 方法名称
        """
        output_file = self.results_dir / f"P-measure_{method_name}.json"

        with open(output_file, "w", encoding="utf-8") as f:
            json.dump(results, f, indent=2, ensure_ascii=False)

        print(f"\n结果已保存到: {output_file}")

    def save_time_records(self, time_records: Dict[str, Any], method_name: str):
        """
        保存时间记录

        Args:
            time_records: 时间记录
            method_name: 方法名称
        """
        output_file = self.results_dir / f"time_consumption_{method_name}.json"

        with open(output_file, "w", encoding="utf-8") as f:
            json.dump(time_records, f, indent=2, ensure_ascii=False)

        print(f"时间记录已保存到: {output_file}")

    def print_summary(self, results: Dict[str, Any]):
        """
        打印实验结果摘要

        Args:
            results: 实验结果
        """
        print("\n=== 实验结果摘要 ===")

        for mutant_name, mutant_results in results.items():
            print(f"\n突变体: {mutant_name}")

            for test_cases_num, p_measures in mutant_results.items():
                avg_p_measure = sum(p_measures) / len(p_measures)
                min_p_measure = min(p_measures)
                max_p_measure = max(p_measures)

                print(
                    f"  测试用例数 {test_cases_num}: "
                    f"平均={avg_p_measure:.4f}, "
                    f"最小={min_p_measure:.4f}, "
                    f"最大={max_p_measure:.4f}"
                )

    def print_time_summary(self, time_records: Dict[str, Any]):
        """
        打印时间记录摘要

        Args:
            time_records: 时间记录
        """
        print("\n=== 时间记录摘要 ===")

        for mutant_name, mutant_times in time_records.items():
            print(f"\n突变体: {mutant_name}")

            for test_cases_num, iteration_times in mutant_times.items():
                avg_time = sum(iteration_times) / len(iteration_times)
                min_time = min(iteration_times)
                max_time = max(iteration_times)
                total_time = sum(iteration_times)

                print(
                    f"  测试用例数 {test_cases_num}: "
                    f"平均={avg_time:.4f}s, "
                    f"最小={min_time:.4f}s, "
                    f"最大={max_time:.4f}s, "
                    f"总计={total_time:.4f}s"
                )


if __name__ == "__main__":
    print("=== Test Case Generation Effectiveness Experiment ===")

    program = "geometricSum"
    phase = "phase2"

    # Experiment parameters configuration
    if phase == "phase1":
        file_path_list = [
            f"{program}/generation/{phase}/art_generator.py",
            f"{program}/generation/{phase}/random_generator.py",
            f"{program}/generation/{phase}/partition_generator.py",
        ]
    elif phase == "phase2":
        file_path_list = [
            # f"{program}/generation/{phase}/mtart_generator.py",
            # f"{program}/generation/{phase}/random_generator.py",
            f"{program}/generation/{phase}/partition_generator.py",
        ]
    else:
        raise ValueError(f"Unknown phase: {phase}")

    internal_iteration = 1000
    external_iteration = 50
    max_tcs_num = 35
    min_tcs_num = 7

    # Run experiments for each generator
    for file_path in file_path_list:
        print(f"\nRunning experiment for: {file_path}")

        # Create experiment instance
        experiment = TestCaseGenerationEffectivenessExperiment(
            program=program,
            phase=phase,
            file_path=file_path,
            internal_iteration=internal_iteration,
            external_iteration=external_iteration,
            max_tcs_num=max_tcs_num,
            min_tcs_num=min_tcs_num,
        )

        # Run experiment
        results, time_records = experiment.run_experiment()

        # Get method name from file path
        method_name = Path(experiment.file_path).stem

        # Save results
        experiment.save_results(results, method_name)

        # Save time records
        experiment.save_time_records(time_records, method_name)

        # Print summary
        experiment.print_summary(results)

        # Print time summary
        experiment.print_time_summary(time_records)

        print(f"\nExperiment completed for: {file_path}")

    print("\nAll experiments completed!")
