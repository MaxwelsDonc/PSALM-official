"""
Multi-threaded Test Case Generation Effectiveness Experiment Script

This script implements a multi-threaded architecture based on the Java version to evaluate
the effectiveness of different test case generation methods in incomeTax/generation.
It uses parallel processing to assess P-measure values for each mutant across various
test case quantities, analyzing the performance of different generation strategies
while improving experimental efficiency.

Python implementation based on get_raw_results_multithreaded.java
"""

import importlib
import importlib.util
import json
import logging
import random
import time
import os
import threading
from concurrent.futures import ThreadPoolExecutor, as_completed
from datetime import datetime
from pathlib import Path
from typing import Any, Dict, List, Optional, Tuple
from dataclasses import dataclass
from threading import Lock
import argparse

from tqdm import tqdm

from python.incomeTax.model.metamorphic_group import MetamorphicGroup
from python.incomeTax.model.test_case import TestCase
from python.incomeTax.mutants.mutants import mutants
from python.incomeTax.utils.get_path_utils import get_config_path
from python.incomeTax.utils.load_mrs_utils import load_all_metamorphic_relations
from python.incomeTax.utils.mg_domain_construction_utils import MGDomainGenerator
from python.incomeTax.utils.pharsing_config_utils import IncomeTaxConfigExtractor


@dataclass
class ExperimentResult:
    """Thread-safe experiment result container"""

    def __init__(self):
        self.p_measures: Dict[str, Dict[str, List[float]]] = {}
        self.lock = Lock()

    def add_result(
        self, mutant_name: str, test_cases_num: str, p_measures: List[float]
    ):
        """Thread-safe method to add results"""
        with self.lock:
            if mutant_name not in self.p_measures:
                self.p_measures[mutant_name] = {}
            self.p_measures[mutant_name][test_cases_num] = p_measures


@dataclass
class MutantResult:
    """Result container for a single mutant"""

    def __init__(self, mutant_name: str):
        self.mutant_name = mutant_name
        self.p_measures: Dict[str, List[float]] = {}


class MutantTask:
    """Experimental task for a single mutant"""

    def __init__(self, mutant_name: str, mutant_func, strategy: str, experiment):
        self.mutant_name = mutant_name
        self.mutant_func = mutant_func
        self.strategy = strategy
        self.experiment = experiment

    def execute(self) -> MutantResult:
        """Execute the mutant task"""
        thread_id = threading.current_thread().name
        self.experiment.log_thread_safe(
            f"[{thread_id}] Processing mutant: {self.mutant_name}"
        )

        result = MutantResult(self.mutant_name)

        try:
            # Iterate through different test case quantities
            for test_cases_num in range(
                self.experiment.min_tcs_num, self.experiment.max_tcs_num + 1
            ):
                p_measures = []

                # Update progress
                self.experiment.update_progress(
                    self.strategy, self.mutant_name, test_cases_num
                )

                # Repeat external_iteration times
                for iter_count in range(self.experiment.external_iteration):
                    p_measure = self.experiment.calculate_p_measure(
                        self.mutant_func, test_cases_num, self.strategy
                    )
                    p_measures.append(p_measure)

                result.p_measures[str(test_cases_num)] = p_measures

            self.experiment.log_thread_safe(
                f"[{thread_id}] Completed mutant {self.mutant_name}"
            )
            return result

        except Exception as e:
            self.experiment.log_thread_safe(
                f"[{thread_id}] Error: Mutant {self.mutant_name} execution failed - {str(e)}"
            )
            return result


class TestCaseGenerationEffectivenessExperimentMT:
    """
    Multi-threaded test case generation effectiveness experiment class
    """

    def __init__(
        self,
        program: str,
        phase: str,
        strategies: List[str],
        config_path: Optional[str] = None,
        internal_iteration: int = 1000,
        external_iteration: int = 50,
        max_tcs_num: int = 18,
        min_tcs_num: int = 6,
        thread_pool_size: Optional[int] = None,
        log_enabled: bool = True,
    ):
        """
        Initialize multi-threaded experiment

        Args:
            program: Program name
            phase: Experiment phase (phase1 or phase2)
            strategies: List of strategies
            config_path: Configuration file path
            internal_iteration: Number of internal iterations
            external_iteration: Number of external repetitions
            max_tcs_num: Maximum number of test cases
            min_tcs_num: Minimum number of test cases
            thread_pool_size: Thread pool size
            log_enabled: Whether to enable logging
        """
        if config_path is None:
            config_path = get_config_path()

        self.program = program
        self.phase = phase
        self.strategies = strategies
        self.config_extractor = IncomeTaxConfigExtractor(config_path)
        self.mutants_instance = mutants()
        self.metamorphic_relations = load_all_metamorphic_relations()

        # Experiment parameters
        self.internal_iteration = internal_iteration
        self.external_iteration = external_iteration
        self.max_tcs_num = max_tcs_num
        self.min_tcs_num = min_tcs_num
        self.log_enabled = log_enabled

        # Thread pool configuration
        if thread_pool_size is None:
            # Determine thread count based on CPU cores and mutant count
            mutant_count = len(self.mutants_instance.test_subject)
            cpu_count = os.cpu_count()
            cpu_count = min(mutant_count, cpu_count)  # Limit maximum thread count
            self.thread_pool_size = cpu_count
        else:
            self.thread_pool_size = thread_pool_size

        # Get partition count
        self.partitions_num = len(self.config_extractor.partitions)

        # Create results directory
        self.results_dir = Path(f"python/{program}/raw_results/{phase}")
        self.results_dir.mkdir(parents=True, exist_ok=True)

        # Logging setup
        self.logger = None
        self.log_lock = Lock()
        if self.log_enabled:
            self.setup_logger()

        # Generator cache
        self.generators_cache = {}

        # Generate MetamorphicGroup domain for Phase2
        if phase == "phase2":
            try:
                print("[DEBUG] Starting MetamorphicGroup domain generation...")
                if self.log_enabled and self.logger:
                    self.logger.info("Generating MetamorphicGroup domain...")
                self.mg_domain = MGDomainGenerator().generate_domain()
                if self.log_enabled and self.logger:
                    self.logger.info(
                        f"MG domain generation completed, {len(self.mg_domain)} MetamorphicGroups created"
                    )
                print(
                    f"[DEBUG] MG domain generation completed, {len(self.mg_domain)} MetamorphicGroups created"
                )
            except Exception as e:
                print(f"[ERROR] Failed to initialize MetamorphicGroup domain: {str(e)}")
                raise RuntimeError(
                    f"Failed to initialize MetamorphicGroup domain: {str(e)}"
                )
        else:
            self.mg_domain = None

        # Progress tracking
        self.total_experiments = self.calculate_total_experiments()
        self.completed_experiments = 0
        self.progress_lock = Lock()

        print(
            f"[DEBUG] Multi-threaded experiment initialization completed, thread pool size: {self.thread_pool_size}"
        )

    def setup_logger(self):
        """Setup logging configuration"""
        try:
            # Create log directory
            log_dir = self.results_dir / "log"
            log_dir.mkdir(exist_ok=True)

            # Set log filename with timestamp
            timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
            log_file = log_dir / f"experiment_{timestamp}.log"

            # Create logger
            self.logger = logging.getLogger(f"{self.__class__.__name__}_{timestamp}")
            self.logger.setLevel(logging.INFO)

            # Create file handler
            file_handler = logging.FileHandler(log_file, encoding="utf-8")
            file_handler.setLevel(logging.INFO)

            # Create console handler
            console_handler = logging.StreamHandler()
            console_handler.setLevel(logging.INFO)

            # Create formatter
            formatter = logging.Formatter(
                "%(asctime)s - %(name)s - %(levelname)s - %(message)s"
            )
            file_handler.setFormatter(formatter)
            console_handler.setFormatter(formatter)

            # Add handlers to logger
            self.logger.addHandler(file_handler)
            self.logger.addHandler(console_handler)

            self.logger.info(
                f"Multi-threaded experiment started - Log file: {log_file}"
            )

        except Exception as e:
            print(f"Error setting up logger: {str(e)}")

    def log_thread_safe(self, message: str):
        """Thread-safe logging method"""
        if not self.log_enabled or not self.logger:
            return

        with self.log_lock:
            self.logger.info(message)

    def calculate_total_experiments(self) -> int:
        """Calculate total number of experiments"""
        mutants_count = len(self.mutants_instance.test_subject)
        test_case_range_count = self.max_tcs_num - self.min_tcs_num + 1
        total = mutants_count * test_case_range_count

        if self.log_enabled and self.logger:
            self.logger.info(
                f"Total experiment configuration: {mutants_count} mutants × {test_case_range_count} test case quantities = {total} experiments"
            )

        return total

    def update_progress(self, strategy: str, mutant: str, test_case_num: int):
        """Update progress - thread-safe version"""
        with self.progress_lock:
            self.completed_experiments += 1
            progress = (self.completed_experiments / self.total_experiments) * 100

            progress_msg = f"Overall progress: {progress:.1f}% ({self.completed_experiments}/{self.total_experiments}) - Current: {mutant}"
            self.log_thread_safe(progress_msg)

    def load_test_generator(self, file_path: str):
        """
        Dynamically load test case generator with caching

        Args:
            file_path: Generator file path

        Returns:
            Generator instance
        """
        if file_path in self.generators_cache:
            return self.generators_cache[file_path]

        try:
            spec = importlib.util.spec_from_file_location("generator", file_path)
            module = importlib.util.module_from_spec(spec)
            spec.loader.exec_module(module)

            # Find generator class (assuming class name ends with Generator)
            for attr_name in dir(module):
                attr = getattr(module, attr_name)
                if (
                    isinstance(attr, type)
                    and (
                        attr_name.endswith("Generator")
                        or attr_name.endswith("GeneratorMG")
                    )
                    and hasattr(attr, "generate")
                ):
                    generator = attr()
                    self.generators_cache[file_path] = generator
                    return generator

            raise ValueError(f"No valid generator class found in file {file_path}")

        except Exception as e:
            raise RuntimeError(f"Failed to load generator: {str(e)}")

    def generate_test_cases(self, count: int, strategy: str):
        """
        Generate test cases - thread-safe version
        """
        try:
            file_path = f"python/{self.program}/generation/{self.phase}/{strategy}.py"
            generator = self.load_test_generator(file_path)

            if self.phase == "phase1":
                return generator.generate(count)
            else:  # phase2
                return generator.generate(count, mg_domain=self.mg_domain)

        except Exception as e:
            print(f"Error generating test cases: {str(e)}")
            return None

    def generate_metamorphic_groups(
        self, source_tests: List[TestCase]
    ) -> List[MetamorphicGroup]:
        """
        Generate only 1 MG per source test case (randomly select one MR)

        Args:
            source_tests: List of source test cases

        Returns:
            List of MGs
        """
        metamorphic_groups = []

        for source_test in source_tests:
            # Randomly select an MR
            mr = random.choice(self.metamorphic_relations)
            followup_tests = mr.generate_followup_tests(source_test)

            if not followup_tests:
                continue

            # Select only the first follow-up (usually there's only one)
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
        Execute metamorphic test

        Args:
            mutant_func: Mutant function
            mg: Metamorphic test group

        Returns:
            Whether a defect is triggered
        """
        try:
            # Get source and follow-up test results
            source_result = mutant_func(mg.get_source_test().get_income_value())
            followup_result = mutant_func(mg.get_followup_test().get_income_value())

            # Get corresponding metamorphic relation
            mr = None
            for relation in self.metamorphic_relations:
                if relation.get_id() == mg.get_mr_id():
                    mr = relation
                    break

            if mr is None:
                return False

            # Verify metamorphic relation - defect detected if relation is NOT satisfied
            return not mr.verify_relation(
                mg.get_source_test(),
                mg.get_followup_test(),
                source_result,
                followup_result,
            )
        except Exception as e:
            self.log_thread_safe(f"Error in metamorphic test execution: {str(e)}")
            return False

    def calculate_p_measure(
        self, mutant_func, test_cases_num: int, strategy: str
    ) -> float:
        """
        Calculate P-measure value

        Args:
            mutant_func: Mutant function
            test_cases_num: Number of test cases
            strategy: Strategy name

        Returns:
            P-measure value
        """
        defect_detected_count = 0

        for _ in range(self.internal_iteration):
            defect_detected = False

            if self.phase == "phase1":
                # Phase1: Generate source test cases, then generate metamorphic test groups
                source_tests = self.generate_test_cases(test_cases_num, strategy)
                if source_tests is None:
                    continue
                metamorphic_groups = self.generate_metamorphic_groups(source_tests)
            else:
                # Phase2: Directly select MGs from MG domain
                metamorphic_groups = self.generate_test_cases(test_cases_num, strategy)
                if metamorphic_groups is None:
                    continue

            for mg in metamorphic_groups:
                if self.execute_metamorphic_test(mutant_func, mg):
                    defect_detected = True
                    break

            if defect_detected:
                defect_detected_count += 1

        return (
            defect_detected_count / self.internal_iteration
            if self.internal_iteration > 0
            else 0.0
        )

    def run_experiment_multithreaded(self, strategy: str) -> ExperimentResult:
        """
        Run complete experiment with multi-threading
        """
        result = ExperimentResult()

        self.log_thread_safe(
            f"=== Starting multi-threaded processing for strategy: {strategy} ==="
        )
        self.log_thread_safe(
            f"Thread pool configuration: {self.thread_pool_size} threads processing {len(self.mutants_instance.test_subject)} mutants in parallel"
        )

        # Create task list
        tasks = []
        for mutant_name, mutant_func in self.mutants_instance.test_subject.items():
            task = MutantTask(mutant_name, mutant_func, strategy, self)
            tasks.append(task)

        self.log_thread_safe("All tasks created, starting parallel execution...")

        # Execute tasks using thread pool
        with ThreadPoolExecutor(max_workers=self.thread_pool_size) as executor:
            # Submit all tasks
            future_to_task = {executor.submit(task.execute): task for task in tasks}

            # Collect results
            for future in as_completed(future_to_task):
                task = future_to_task[future]
                try:
                    mutant_result = future.result()
                    # Thread-safe result addition
                    for test_cases_num, p_measures in mutant_result.p_measures.items():
                        result.add_result(
                            mutant_result.mutant_name, test_cases_num, p_measures
                        )

                except Exception as e:
                    self.log_thread_safe(
                        f"Error: Exception occurred while processing mutant {task.mutant_name} - {str(e)}"
                    )

        self.log_thread_safe(f"=== Strategy {strategy} completed ===")
        return result

    def save_results(self, result: ExperimentResult, method_name: str):
        """
        Save experiment results

        Args:
            result: Experiment results
            method_name: Method name
        """
        try:
            # Save P-measure results
            output_file = self.results_dir / f"P-measure_{method_name}.json"

            with open(output_file, "w", encoding="utf-8") as f:
                json.dump(result.p_measures, f, indent=2, ensure_ascii=False)

            # Statistics on results
            total_mutants = len(result.p_measures)
            total_test_case_configs = (
                len(next(iter(result.p_measures.values()))) if result.p_measures else 0
            )

            self.log_thread_safe(f"Results saved successfully:")
            self.log_thread_safe(f"  P-measure file: {output_file.name}")
            self.log_thread_safe(
                f"  Contains data: {total_mutants} mutants × {total_test_case_configs} test case configurations"
            )

            print(f"Results saved to: {output_file}")

        except Exception as e:
            self.log_thread_safe(f"Error: Failed to save results - {str(e)}")
            print(f"Failed to save results: {str(e)}")

    def save_experiment_config(self, strategy: str):
        """
        Save experiment configuration parameters to file
        """
        config = {
            "program": self.program,
            "phase": self.phase,
            "strategy": strategy,
            "internal_iteration": self.internal_iteration,
            "external_iteration": self.external_iteration,
            "max_tcs_num": self.max_tcs_num,
            "min_tcs_num": self.min_tcs_num,
            "thread_pool_size": self.thread_pool_size,
            "partitions_num": self.partitions_num,
        }

        config_file = self.results_dir / f"experiment_config_{strategy}.json"
        with open(config_file, "w", encoding="utf-8") as f:
            json.dump(config, f, indent=2, ensure_ascii=False)

        print(f"Experiment configuration saved to: {config_file}")

    def print_summary(self, result: ExperimentResult):
        """
        Print experiment results summary

        Args:
            result: Experiment results
        """
        print("\n=== Experiment Results Summary ===")

        for mutant_name, mutant_results in result.p_measures.items():
            print(f"\nMutant: {mutant_name}")

            for test_cases_num, p_measures in mutant_results.items():
                avg_p_measure = sum(p_measures) / len(p_measures)
                min_p_measure = min(p_measures)
                max_p_measure = max(p_measures)

                print(
                    f"  Test cases {test_cases_num}: "
                    f"avg={avg_p_measure:.4f}, "
                    f"min={min_p_measure:.4f}, "
                    f"max={max_p_measure:.4f}"
                )


def run_experiment(
    program: str = "incomeTax",
    phase: str = "phase1",
    internal_iteration: int = 1000,
    external_iteration: int = 50,
    max_tcs_num: int = 6,
    min_tcs_num: int = 6,
    thread_pool_size: Optional[int] = None,
    log_enabled: bool = True,
    strategies: Optional[List[str]] = None,
) -> None:
    """
    Run the multi-threaded experiment with specified parameters

    Args:
        program: Program name (default: "incomeTax")
        phase: Experiment phase ("phase1" or "phase2") (default: "phase1")
        internal_iteration: Number of internal iterations (default: 1000)
        external_iteration: Number of external repetitions (default: 50)
        max_tcs_num: Maximum number of test cases (default: 6)
        min_tcs_num: Minimum number of test cases (default: 6)
        thread_pool_size: Thread pool size (default: None, auto-determined)
        log_enabled: Whether to enable logging (default: True)
        strategies: List of strategies (default: None, auto-determined based on phase)
    """
    print("[DEBUG] Multi-threaded experiment program starting")

    # Auto-determine strategies if not provided
    if strategies is None:
        if phase == "phase1":
            strategies = ["art_generator", "random_generator", "partition_generator"]
        else:  # phase2
            strategies = ["random_generator", "partition_generator", "mtart_generator"]

    print(f"[DEBUG] Parameter configuration complete: phase={phase}, program={program}")
    print(f"[DEBUG] Strategy configuration complete: {', '.join(strategies)}")

    # Create experiment instance
    print("[DEBUG] Starting to create experiment instance...")
    try:
        experiment = TestCaseGenerationEffectivenessExperimentMT(
            program=program,
            phase=phase,
            strategies=strategies,
            internal_iteration=internal_iteration,
            external_iteration=external_iteration,
            max_tcs_num=max_tcs_num,
            min_tcs_num=min_tcs_num,
            thread_pool_size=thread_pool_size,
            log_enabled=log_enabled,
        )
        print("[DEBUG] Experiment instance created successfully")
    except Exception as e:
        print(f"[ERROR] Failed to create experiment instance: {str(e)}")
        return

    # Log experiment information
    experiment.log_thread_safe(
        "=== Multi-threaded Test Case Generation Effectiveness Experiment Started ==="
    )
    experiment.log_thread_safe(f"Experiment configuration:")
    experiment.log_thread_safe(f"  Program: {program}")
    experiment.log_thread_safe(f"  Phase: {phase}")
    experiment.log_thread_safe(f"  Thread pool size: {experiment.thread_pool_size}")
    experiment.log_thread_safe(f"  Internal iterations: {internal_iteration}")
    experiment.log_thread_safe(f"  External repetitions: {external_iteration}")
    experiment.log_thread_safe(
        f"  Test case count range: {min_tcs_num} - {max_tcs_num}"
    )
    experiment.log_thread_safe(
        f"  Target mutant count: {len(experiment.mutants_instance.test_subject)}"
    )
    experiment.log_thread_safe(f"  Strategy list: {', '.join(strategies)}")

    # Run experiments
    for i, strategy in enumerate(strategies):
        experiment.log_thread_safe(
            f"\n=== Starting strategy {i + 1}/{len(strategies)}: {strategy} ==="
        )
        print(f"\nRunning strategy: {strategy}")

        # Reset progress counter
        experiment.completed_experiments = 0

        # Run multi-threaded experiment
        result = experiment.run_experiment_multithreaded(strategy)

        # Save results
        experiment.save_results(result, strategy)

        # Save configuration
        # experiment.save_experiment_config(strategy)

        # Print summary
        experiment.print_summary(result)

        experiment.log_thread_safe(f"Strategy {strategy} completed")
        print(f"Strategy {strategy} completed")

    experiment.log_thread_safe("\n=== All multi-threaded experiments completed! ===")
    print("\n=== All multi-threaded experiments completed! ===")


def parse_args():
    parser = argparse.ArgumentParser(description="Income Tax Experiment Runner")

    parser.add_argument(
        "--phase", choices=["phase1", "phase2"], help="Experiment phase"
    )
    parser.add_argument("--min_tcs_num", type=int, help="Minimum number of test cases")
    parser.add_argument("--max_tcs_num", type=int, help="Maximum number of test cases")

    return parser.parse_args()


if __name__ == "__main__":
    # === CONFIGURATION PARAMETERS ===
    EXPERIMENT_CONFIG = {
        "program": "incomeTax",
        "phase": "phase1",
        "internal_iteration": 1000,
        "external_iteration": 50,
        "min_tcs_num": 6,
        "max_tcs_num": 18,
        "thread_pool_size": None,
        "log_enabled": True,
        "strategies": None,
    }

    # === PARSE COMMAND LINE ===
    args = parse_args()
    if args.phase:
        EXPERIMENT_CONFIG["phase"] = args.phase
    if args.min_tcs_num:
        EXPERIMENT_CONFIG["min_tcs_num"] = args.min_tcs_num
    if args.max_tcs_num:
        EXPERIMENT_CONFIG["max_tcs_num"] = args.max_tcs_num

    # === RUN EXPERIMENT ===
    print("=" * 60)
    print("INCOME TAX EXPERIMENT CONFIGURATION")
    print("=" * 60)
    for key, value in EXPERIMENT_CONFIG.items():
        print(f"{key:20}: {value}")
    print("=" * 60)

    run_experiment(**EXPERIMENT_CONFIG)
