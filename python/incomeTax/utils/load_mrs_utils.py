def load_all_metamorphic_relations():
    """
    手动加载所有蜕变关系

    Returns:
        List: 所有蜕变关系的实例
    """
    from python.incomeTax.metamorphicRelations.mr1_relation import MR1Relation
    from python.incomeTax.metamorphicRelations.mr2_relation import MR2Relation
    from python.incomeTax.metamorphicRelations.mr3_relation import MR3Relation

    # 创建实例并返回
    relations = [MR1Relation(), MR2Relation(), MR3Relation()]

    return relations
