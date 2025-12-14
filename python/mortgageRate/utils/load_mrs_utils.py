from python.mortgageRate.utils.pharsing_config_utils import MortgageConfigExtractor
from python.mortgageRate.utils.get_path_utils import get_config_path
def load_all_metamorphic_relations():
    """
    手动加载所有蜕变关系

    Returns:
        List: 所有蜕变关系的实例
    """
    # 显式导入所有蜕变关系类
    from python.mortgageRate.metamorphicRelations.input_increase import InputIncrease
    from python.mortgageRate.metamorphicRelations.input_output_ratio import InputOutputRatio
    from python.mortgageRate.metamorphicRelations.input_scaling import InputScaling
    from python.mortgageRate.metamorphicRelations.scaled_ratio import ScaledRatio
    from python.mortgageRate.metamorphicRelations.equivalent_inputs import EquivalentInputs
    from python.mortgageRate.metamorphicRelations.ratio_upper_bound import RatioUpperBound

    # 创建实例并返回
    relations = [
        InputIncrease(),
        InputOutputRatio(),
        InputScaling(),
        ScaledRatio(),
        EquivalentInputs(),
        RatioUpperBound()
    ]

    # 过滤掉不在配置中的关系
    config_path = get_config_path()
    config_extractor = MortgageConfigExtractor(config_path)
    mr_ids = set(config_extractor.mr_types.keys())
    filtered_relations = [r for r in relations if r.get_id() in mr_ids]

    return filtered_relations