from python.geometricSum.utils.pharsing_config_utils import GeometricSumConfigExtractor
from python.geometricSum.utils.get_path_utils import get_config_path


def load_all_metamorphic_relations():
    """
    手动加载所有蜕变关系

    Returns:
        List: 所有蜕变关系的实例
    """
    # 显式导入所有蜕变关系类
    from python.geometricSum.metamorphicRelations.mr1_relation import MR1Relation
    from python.geometricSum.metamorphicRelations.mr2_relation import MR2Relation
    from python.geometricSum.metamorphicRelations.mr3_relation import MR3Relation

    # 创建实例并返回
    relations = [MR1Relation(), MR2Relation(), MR3Relation()]

    # 过滤掉不在配置中的关系
    config_path = get_config_path()
    config_extractor = GeometricSumConfigExtractor(config_path)
    mr_ids = set(config_extractor.mr_types.keys())
    filtered_relations = [r for r in relations if r.get_id() in mr_ids]

    return filtered_relations
