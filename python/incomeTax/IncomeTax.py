import random
from collections import defaultdict

import numpy as np
import os
import openpyxl as opxl
import datetime


class IncomeTax(object):
    """
    [1] F. T. Chan, T. Y. Chen, I. K. Mak, and Y. T. Yu, “Proportional sampling strategy: Guidelines for software tes
    ting practitioners,” Information and Software Technology, vol. 38, no. 12, pp. 775–782, 1996, doi: 10.1016/0950-5849
    (96)01103-2.

    input_domain=[1,1400000]
    """

    def __init__(self):
        self.part_info = {'part_1': {'size': 2, 'range': (1, 20000), 'selected_num': 0},
                          'part_2': {'size': 1, 'range': (20000, 30000), 'selected_num': 0},
                          'part_3': {'size': 1, 'range': (30000, 40000), 'selected_num': 0},
                          'part_4': {'size': 1, 'range': (40000, 50000), 'selected_num': 0},
                          'part_5': {'size': 45, 'range': (50000, 500000), 'selected_num': 0},
                          'part_6': {'size': 90, 'range': (500000, 1400000), 'selected_num': 0}}
        self.input_domain = [1, 1400000]
        # get test_subject
        self.test_subject = {}
        for i in range(1, 32):
            key_name = 'main_' + str(i)
            if key_name in ['main_1', 'main_2', 'main_4', 'main_5', 'main_9', 'main_10', 'main_11', 'main_12',
                            'main_16', 'main_17', 'main_18', 'main_19', 'main_24', 'main_25', 'main_26']:
                continue
            if hasattr(self, 'main_' + str(i)):
                self.test_subject[key_name] = getattr(self, 'main_' + str(i))

    def initial_part_info(self):
        # 把part info里面的selected num都置为0
        for key in self.part_info.keys():
            self.part_info[key]['selected_num'] = 0

    @staticmethod
    def main(income):
        if income <= 20000:
            tax = 0
        elif income <= 30000:
            tax = 0.12 * (income - 20000)
        elif income <= 40000:
            tax = 1200 + 0.14 * (income - 30000)
        elif income <= 50000:
            tax = 2600 + 0.17 * (income - 40000)
        elif income <= 500000:
            tax = 0.15 * income
        else:
            tax = 0.165 * income
        return tax

    @staticmethod
    def main_1(income):
        if income < 20000:  # 将 <= 修改为 <
            tax = 0
        elif income <= 30000:
            tax = 0.12 * (income - 20000)
        elif income <= 40000:
            tax = 1200 + 0.14 * (income - 30000)
        elif income <= 50000:
            tax = 2600 + 0.17 * (income - 40000)
        elif income <= 500000:
            tax = 0.15 * income
        else:
            tax = 0.165 * income
        return tax

    @staticmethod
    def main_2(income):
        if income <= 20001:  # 将 20000 修改为 20001
            tax = 0
        elif income <= 30000:
            tax = 0.12 * (income - 20000)
        elif income <= 40000:
            tax = 1200 + 0.14 * (income - 30000)
        elif income <= 50000:
            tax = 2600 + 0.17 * (income - 40000)
        elif income <= 500000:
            tax = 0.15 * income
        else:
            tax = 0.165 * income
        return tax

    @staticmethod
    def main_3(income):
        if income <= 20000:
            tax = 1  # change from 0 to 1
        elif income <= 30000:
            tax = 0.12 * (income - 20000)
        elif income <= 40000:
            tax = 1200 + 0.14 * (income - 30000)
        elif income <= 50000:
            tax = 2600 + 0.17 * (income - 40000)
        elif income <= 500000:
            tax = 0.15 * income
        else:
            tax = 0.165 * income
        return tax

    @staticmethod
    def main_4(income):
        if income <= 20000:
            tax = 0
        elif income < 30000:  # change from <= into <
            tax = 0.12 * (income - 20000)
        elif income <= 40000:
            tax = 1200 + 0.14 * (income - 30000)
        elif income <= 50000:
            tax = 2600 + 0.17 * (income - 40000)
        elif income <= 500000:
            tax = 0.15 * income
        else:
            tax = 0.165 * income
        return tax

    @staticmethod
    def main_5(income):
        if income <= 20000:
            tax = 0
        elif income <= 30001:  # change from 30000 into 30001
            tax = 0.12 * (income - 20000)
        elif income <= 40000:
            tax = 1200 + 0.14 * (income - 30000)
        elif income <= 50000:
            tax = 2600 + 0.17 * (income - 40000)
        elif income <= 500000:
            tax = 0.15 * income
        else:
            tax = 0.165 * income
        return tax

    @staticmethod
    def main_6(income):
        if income <= 20000:
            tax = 0
        elif income <= 30000:
            tax = 1.12 * (income - 20000)  # change from 0.12 into 1.12
        elif income <= 40000:
            tax = 1200 + 0.14 * (income - 30000)
        elif income <= 50000:
            tax = 2600 + 0.17 * (income - 40000)
        elif income <= 500000:
            tax = 0.15 * income
        else:
            tax = 0.165 * income
        return tax

    @staticmethod
    def main_7(income):
        if income <= 20000:
            tax = 0
        elif income <= 30000:
            tax = 0.12 / (income - 20000)  # change from * into /
        elif income <= 40000:
            tax = 1200 + 0.14 * (income - 30000)
        elif income <= 50000:
            tax = 2600 + 0.17 * (income - 40000)
        elif income <= 500000:
            tax = 0.15 * income
        else:
            tax = 0.165 * income
        return tax

    @staticmethod
    def main_8(income):
        if income <= 20000:
            tax = 0
        elif income <= 30000:
            tax = 0.12 * (income + 20000)  # change from - into +
        elif income <= 40000:
            tax = 1200 + 0.14 * (income - 30000)
        elif income <= 50000:
            tax = 2600 + 0.17 * (income - 40000)
        elif income <= 500000:
            tax = 0.15 * income
        else:
            tax = 0.165 * income
        return tax

    @staticmethod
    def main_9(income):
        if income <= 20000:
            tax = 0
        elif income <= 30000:
            tax = 0.12 * (income - 20001)  # change from 20000 into 20001
        elif income <= 40000:
            tax = 1200 + 0.14 * (income - 30000)
        elif income <= 50000:
            tax = 2600 + 0.17 * (income - 40000)
        elif income <= 500000:
            tax = 0.15 * income
        else:
            tax = 0.165 * income
        return tax

    @staticmethod
    def main_10(income):
        if income <= 20000:
            tax = 0
        elif income <= 30000:
            tax = 0.12 * (income - 20000)
        elif income < 40000:  # change from <= into <
            tax = 1200 + 0.14 * (income - 30000)
        elif income <= 50000:
            tax = 2600 + 0.17 * (income - 40000)
        elif income <= 500000:
            tax = 0.15 * income
        else:
            tax = 0.165 * income
        return tax

    @staticmethod
    def main_11(income):
        if income <= 20000:
            tax = 0
        elif income <= 30000:
            tax = 0.12 * (income - 20000)
        elif income <= 40001:  # change from 40000 into 40001
            tax = 1200 + 0.14 * (income - 30000)
        elif income <= 50000:
            tax = 2600 + 0.17 * (income - 40000)
        elif income <= 500000:
            tax = 0.15 * income
        else:
            tax = 0.165 * income
        return tax

    @staticmethod
    def main_12(income):
        if income <= 20000:
            tax = 0
        elif income <= 30000:
            tax = 0.12 * (income - 20000)
        elif income <= 40000:
            tax = 1201 + 0.14 * (income - 30000)  # change 1200 into 1201
        elif income <= 50000:
            tax = 2600 + 0.17 * (income - 40000)
        elif income <= 500000:
            tax = 0.15 * income
        else:
            tax = 0.165 * income
        return tax

    @staticmethod
    def main_13(income):
        if income <= 20000:
            tax = 0
        elif income <= 30000:
            tax = 0.12 * (income - 20000)
        elif income <= 40000:
            tax = 1200 + 1.14 * (income - 30000)  # change 0.14 into 1.14
        elif income <= 50000:
            tax = 2600 + 0.17 * (income - 40000)
        elif income <= 500000:
            tax = 0.15 * income
        else:
            tax = 0.165 * income
        return tax

    @staticmethod
    def main_14(income):
        if income <= 20000:
            tax = 0
        elif income <= 30000:
            tax = 0.12 * (income - 20000)
        elif income <= 40000:
            tax = 1200 + 0.14 / (income - 30000)  # change from * into /
        elif income <= 50000:
            tax = 2600 + 0.17 * (income - 40000)
        elif income <= 500000:
            tax = 0.15 * income
        else:
            tax = 0.165 * income
        return tax

    @staticmethod
    def main_15(income):
        if income <= 20000:
            tax = 0
        elif income <= 30000:
            tax = 0.12 * (income - 20000)
        elif income <= 40000:
            tax = 1200 + 0.14 * (income + 30000)  # change from - into +
        elif income <= 50000:
            tax = 2600 + 0.17 * (income - 40000)
        elif income <= 500000:
            tax = 0.15 * income
        else:
            tax = 0.165 * income
        return tax

    @staticmethod
    def main_16(income):
        if income <= 20000:
            tax = 0
        elif income <= 30000:
            tax = 0.12 * (income - 20000)
        elif income <= 40000:
            tax = 1200 + 0.14 * (income - 30001)  # change from 30000 into 30001
        elif income <= 50000:
            tax = 2600 + 0.17 * (income - 40000)
        elif income <= 500000:
            tax = 0.15 * income
        else:
            tax = 0.165 * income
        return tax

    @staticmethod
    def main_17(income):
        if income <= 20000:
            tax = 0
        elif income <= 30000:
            tax = 0.12 * (income - 20000)
        elif income <= 40000:
            tax = 1200 + 0.14 * (income - 30000)
        elif income < 50000:  # change from <= into <
            tax = 2600 + 0.17 * (income - 40000)
        elif income <= 500000:
            tax = 0.15 * income
        else:
            tax = 0.165 * income
        return tax

    @staticmethod
    def main_18(income):
        if income <= 20000:
            tax = 0
        elif income <= 30000:
            tax = 0.12 * (income - 20000)
        elif income <= 40000:
            tax = 1200 + 0.14 * (income - 30000)
        elif income <= 50001:  # change from 50000 into 50001
            tax = 2600 + 0.17 * (income - 40000)
        elif income <= 500000:
            tax = 0.15 * income
        else:
            tax = 0.165 * income
        return tax

    @staticmethod
    def main_19(income):
        if income <= 20000:
            tax = 0
        elif income <= 30000:
            tax = 0.12 * (income - 20000)
        elif income <= 40000:
            tax = 1200 + 0.14 * (income - 30000)
        elif income <= 50000:
            tax = 2601 + 0.17 * (income - 40000)  # change from 2600 into 2600
        elif income <= 500000:
            tax = 0.15 * income
        else:
            tax = 0.165 * income
        return tax

    @staticmethod
    def main_20(income):
        if income <= 20000:
            tax = 0
        elif income <= 30000:
            tax = 0.12 * (income - 20000)
        elif income <= 40000:
            tax = 1200 + 0.14 * (income - 30000)
        elif income <= 50000:
            tax = 2600 - 0.17 * (income - 40000)  # change from + into -
        elif income <= 500000:
            tax = 0.15 * income
        else:
            tax = 0.165 * income
        return tax

    @staticmethod
    def main_21(income):
        if income <= 20000:
            tax = 0
        elif income <= 30000:
            tax = 0.12 * (income - 20000)
        elif income <= 40000:
            tax = 1200 + 0.14 * (income - 30000)
        elif income <= 50000:
            tax = 2600 + 1.17 * (income - 40000)  # change from 0.17 into 1.17
        elif income <= 500000:
            tax = 0.15 * income
        else:
            tax = 0.165 * income
        return tax

    @staticmethod
    def main_22(income):
        if income <= 20000:
            tax = 0
        elif income <= 30000:
            tax = 0.12 * (income - 20000)
        elif income <= 40000:
            tax = 1200 + 0.14 * (income - 30000)
        elif income <= 50000:
            tax = 2600 + 0.17 / (income - 40000)  # change from * into /
        elif income <= 500000:
            tax = 0.15 * income
        else:
            tax = 0.165 * income
        return tax

    @staticmethod
    def main_23(income):
        if income <= 20000:
            tax = 0
        elif income <= 30000:
            tax = 0.12 * (income - 20000)
        elif income <= 40000:
            tax = 1200 + 0.14 * (income - 30000)
        elif income <= 50000:
            tax = 2600 + 0.17 * (income + 40000)  # change from - into +
        elif income <= 500000:
            tax = 0.15 * income
        else:
            tax = 0.165 * income
        return tax

    @staticmethod
    def main_24(income):
        if income <= 20000:
            tax = 0
        elif income <= 30000:
            tax = 0.12 * (income - 20000)
        elif income <= 40000:
            tax = 1200 + 0.14 * (income - 30000)
        elif income <= 50000:
            tax = 2600 + 0.17 * (income - 40001)  # change from 40000 into 40001
        elif income <= 500000:
            tax = 0.15 * income
        else:
            tax = 0.165 * income
        return tax

    @staticmethod
    def main_25(income):
        if income <= 20000:
            tax = 0
        elif income <= 30000:
            tax = 0.12 * (income - 20000)
        elif income <= 40000:
            tax = 1200 + 0.14 * (income - 30000)
        elif income <= 50000:
            tax = 2600 + 0.17 * (income - 40000)
        elif income < 500000:  # change from <= into <
            tax = 0.15 * income
        else:
            tax = 0.165 * income
        return tax

    @staticmethod
    def main_26(income):
        if income <= 20000:
            tax = 0
        elif income <= 30000:
            tax = 0.12 * (income - 20000)
        elif income <= 40000:
            tax = 1200 + 0.14 * (income - 30000)
        elif income <= 50000:
            tax = 2600 + 0.17 * (income - 40000)
        elif income <= 500001:  # change from 500000 into 500001
            tax = 0.15 * income
        else:
            tax = 0.165 * income
        return tax

    @staticmethod
    def main_27(income):
        if income <= 20000:
            tax = 0
        elif income <= 30000:
            tax = 0.12 * (income - 20000)
        elif income <= 40000:
            tax = 1200 + 0.14 * (income - 30000)
        elif income <= 50000:
            tax = 2600 + 0.17 * (income - 40000)
        elif income <= 500000:
            tax = 1.15 * income  # change form 0.15 into 1.15
        else:
            tax = 0.165 * income
        return tax

    @staticmethod
    def main_28(income):
        if income <= 20000:
            tax = 0
        elif income <= 30000:
            tax = 0.12 * (income - 20000)
        elif income <= 40000:
            tax = 1200 + 0.14 * (income - 30000)
        elif income <= 50000:
            tax = 2600 + 0.17 * (income - 40000)
        elif income <= 500000:
            tax = 0.15 / income  # change from * into /
        else:
            tax = 0.165 * income
        return tax

    @staticmethod
    def main_29(income):
        if income <= 20000:
            tax = 0
        elif income <= 30000:
            tax = 0.12 * (income - 20000)
        elif income <= 40000:
            tax = 1200 + 0.14 * (income - 30000)
        elif income <= 50000:
            tax = 2600 + 0.17 * (income - 40000)
        elif income <= 500000:
            tax = 0.15 * income
        else:
            tax = 1.165 * income  # change from 0.165 into 1.165
        return tax

    @staticmethod
    def main_30(income):
        if income <= 20000:
            tax = 0
        elif income <= 30000:
            tax = 0.12 * (income - 20000)
        elif income <= 40000:
            tax = 1200 + 0.14 * (income - 30000)
        elif income <= 50000:
            tax = 2600 + 0.17 * (income - 40000)
        elif income <= 500000:
            tax = 0.15 * income
        else:
            tax = 0.165 / income  # change from * into /
        return tax

    @staticmethod
    def main_31(income):
        if income <= 20000:
            tax = 0
        elif income <= 30000:
            tax = 0.12 * (income - 20000)
        elif income <= 40000:
            tax = 1200 - 0.14 * (income - 30000)  # change form + into -
        elif income <= 50000:
            tax = 2600 + 0.17 * (income - 40000)
        elif income <= 500000:
            tax = 0.15 * income
        else:
            tax = 0.165 * income
        return tax

    def random_selection_st(self, test_cases_num, **kwargs):
        test_cases = []
        for i in range(test_cases_num):
            test_cases.append(random.randint(self.input_domain[0], self.input_domain[1]))
        return test_cases

    def partition_selection_st(self, test_cases_num):
        test_cases = []
        # # 首先分配一个
        # 剩下的按照进行比例分配
        # 首先分配商
        quotient, remainder = divmod(test_cases_num, sum([info['size'] for info in self.part_info.values()]))
        for part_name in self.part_info.keys():
            current_part = self.part_info[part_name]
            for _ in range(quotient):
                for size in range(current_part['size']):
                    test_cases.append(random.randint(current_part['range'][0], current_part['range'][1]))
                    current_part['selected_num'] += 1
        # 输出每个分区挑选测试用例的数目

        # 然后分配余数
        for _ in range(remainder):
            current_part_tuple = min(self.part_info.items(),
                                     key=lambda x: (x[1]['selected_num'] / x[1]['size'], random.random()))
            current_part = self.part_info[current_part_tuple[0]]
            test_cases.append(random.randint(current_part['range'][0], current_part['range'][1]))
            current_part['selected_num'] += 1

        # 返回挑选的测试用例
        self.initial_part_info()
        return test_cases

    def random_selection_mg(self, test_cases_num,**kwargs):
        sampling_st = np.linspace(self.input_domain[0], self.input_domain[1], 1000)
        sampling_mg = self.mg_generation(sampling_st, mode='mg')
        test_mg = [random.choice(sampling_mg) for _ in range(test_cases_num)]
        return test_mg

    def partition_selection_mg(self, test_cases_num,**kwargs):
        """
        根据给定的测试用例数量和参数N，在不同分区中选择测试点。

        :param test_cases_num: 需要选择的测试用例总数
        :return: 一个包含选定测试点的列表
        """
        # 在输入域内等距采样1000个点
        sampling_st = np.linspace(self.input_domain[0], self.input_domain[1], 1000)
        # 初始化用于存储采样点的字典
        sampling_partition_st = defaultdict(list)
        sampling_partition_mg = defaultdict(list)

        # 遍历采样点，并根据分区信息将其分配到对应的分区中
        for st in sampling_st:
            for partition_name in self.part_info.keys():
                # if sampling_partition_st[partition_name] is None:
                #     sampling_partition_st[partition_name] = []
                # 将采样点添加到对应的分区中
                range_max = self.part_info[partition_name]['range'][1]
                range_min = self.part_info[partition_name]['range'][0]
                if range_min <= st < range_max:
                    sampling_partition_st[partition_name].append(st)
                    break

        # 对每个分区的采样点生成基于'mg'模式的测试点
        for partition_name in sampling_partition_st:
            current_partition_st = sampling_partition_st[partition_name]
            current_partition_mg = self.mg_generation(current_partition_st, mode='mg')
            sampling_partition_mg[partition_name] = current_partition_mg

        test_mg = []
        total_size = sum([info['size'] for info in self.part_info.values()])
        quotient, remainder = divmod(test_cases_num, total_size)
        # 根据测试用例总数，在各分区中均匀选择测试点
        for part_name in self.part_info.keys():
            current_part = self.part_info[part_name]
            for _ in range(quotient):
                for size in range(current_part['size']):
                    test_mg.append(random.choice(sampling_partition_mg[part_name]))
                    current_part['selected_num'] += 1
        # 然后分配余数
        for _ in range(remainder):
            current_part_tuple = min(self.part_info.items(),
                                     key=lambda x: (x[1]['selected_num'] / x[1]['size'], random.random()))
            current_part = self.part_info[current_part_tuple[0]]
            test_mg.append(random.choice(sampling_partition_mg[current_part_tuple[0]]))
            current_part['selected_num'] += 1

        # 重置分区信息
        self.initial_part_info()
        return test_mg

    def mg_generation(self, source_test_cases_list, mode='st', **kwargs):
        """
        mr=1 :given x,  f(x+1000)>f(x)
        mr=2 :given x, 2.27<=(f(x+10)-f(x))/10<=2.75
        """
        """
        优化后的函数，用于评估测试用例的有效性。
        :param source_test_cases_list: 测试用例列表
        :return: 包含评估结果和分析结果的列表
        """
        if mode == 'st':
            # evaluation_result_mr = []
            mg_list = []
            mr_index_list = [1, 2, 3]
            for source_test_case in source_test_cases_list:
                mr_index = random.choice(mr_index_list)

                if mr_index == 1:
                    follow_up_test_case = min(source_test_case + random.randint(20, 100), self.input_domain[1] + 1)
                    mg_list.append(('mr_1', source_test_case, follow_up_test_case))

                elif mr_index == 2:
                    follow_up_test_case = min(source_test_case + random.randint(20000, 50000), self.input_domain[1] + 1)
                    mg_list.append(('mr_2', source_test_case, follow_up_test_case))

                elif mr_index == 3:
                    follow_up_test_case = source_test_case * random.uniform(1, 2)
                    mg_list.append(('mr_3', source_test_case, follow_up_test_case))


        elif mode == 'mg':
            mg_list = []
            for source_test_case in source_test_cases_list:
                # mr-1
                for _ in range(1):
                    follow_up_test_case = min(source_test_case + random.randint(20, 100), self.input_domain[1] + 1)
                    mg_list.append(('mr_1', source_test_case, follow_up_test_case))
                # mr-2
                for _ in range(1):
                    follow_up_test_case = min(source_test_case + random.randint(20000, 50000), self.input_domain[1] + 1)
                    mg_list.append(('mr_2', source_test_case, follow_up_test_case))
                # mr-3
                for _ in range(1):
                    follow_up_test_case = source_test_case * random.uniform(1, 2)
                    mg_list.append(('mr_3', source_test_case, follow_up_test_case))

        else:
            print('mode value is Invalid')
        return mg_list

    def mg_verification(self, mg_list, sut) -> bool:
        verification_list = []
        for mg in mg_list:
            mr_str, st, ft = mg
            if mr_str == 'mr_1':
                verification_list.append(sut(ft) > sut(st) - 1e-6)
            elif mr_str == 'mr_2':
                verification_list.append(
                    (sut(ft) - sut(st)) / (ft - st) > 0.12 - 1e-6
                )
            elif mr_str == 'mr_3':

                verification_list.append(sut(ft) > sut(st) * ft / st - 1e-6)
            if False in verification_list:
                break

        return False in verification_list