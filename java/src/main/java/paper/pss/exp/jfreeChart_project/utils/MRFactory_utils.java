package paper.pss.exp.jfreeChart_project.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import paper.pss.exp.jfreeChart_project.model.MetamorphicRelation;
import paper.pss.exp.jfreeChart_project.model.MetamorphicGroup;
import paper.pss.exp.jfreeChart_project.model.TestCase;
import paper.pss.exp.jfreeChart_project.metamorphicRelations.MR1_relation;
import paper.pss.exp.jfreeChart_project.metamorphicRelations.MR2_relation;
import paper.pss.exp.jfreeChart_project.metamorphicRelations.MR3_relation;
import paper.pss.exp.jfreeChart_project.metamorphicRelations.MR4_relation;
import paper.pss.exp.jfreeChart_project.metamorphicRelations.MR5_relation;
import paper.pss.exp.jfreeChart_project.metamorphicRelations.MR6_relation;

/**
 * MRFactory_utils.java - 蜕变关系工厂类
 * 
 * 这个工厂类负责初始化和管理所有的蜕变关系实例，
 * 提供统一的接口来获取和操作蜕变关系。
 * 
 * 支持的蜕变关系：
 * - MR1: 宽度比例关系
 * - MR2: 线段长度比例关系
 * - MR3: 线段对称性关系
 * - MR4: 正交特性关系
 * - MR5: 旋转不变性关系
 * - MR6: 平移不变性关系
 */
public class MRFactory_utils {

    // 存储所有蜕变关系实例的列表
    private static final List<MetamorphicRelation> allRelations = new ArrayList<>();
    
    // 存储蜕变关系ID到实例的映射
    private static final Map<String, MetamorphicRelation> relationMap = new HashMap<>();

    // 静态初始化块，创建所有蜕变关系实例
    static {
        initializeRelations();
    }

    /**
     * 初始化所有蜕变关系实例
     */
    private static void initializeRelations() {
        // 创建所有蜕变关系实例
        MetamorphicRelation mr1 = new MR1_relation();
        MetamorphicRelation mr2 = new MR2_relation();
        MetamorphicRelation mr3 = new MR3_relation();
        MetamorphicRelation mr4 = new MR4_relation();
        MetamorphicRelation mr5 = new MR5_relation();
        MetamorphicRelation mr6 = new MR6_relation();

        // 添加到列表中
        allRelations.add(mr1);
        allRelations.add(mr2);
        allRelations.add(mr3);
        allRelations.add(mr4);
        allRelations.add(mr5);
        allRelations.add(mr6);

        // 添加到映射中
        relationMap.put(mr1.getId(), mr1);
        relationMap.put(mr2.getId(), mr2);
        relationMap.put(mr3.getId(), mr3);
        relationMap.put(mr4.getId(), mr4);
        relationMap.put(mr5.getId(), mr5);
        relationMap.put(mr6.getId(), mr6);
    }

    /**
     * 获取所有蜕变关系实例
     * 
     * @return 包含所有蜕变关系的列表
     */
    public static List<MetamorphicRelation> getAllRelations() {
        return new ArrayList<>(allRelations);
    }

    /**
     * 根据ID获取特定的蜕变关系
     * 
     * @param relationId 蜕变关系的ID（如"MR1", "MR2"等）
     * @return 对应的蜕变关系实例，如果不存在则返回null
     */
    public static MetamorphicRelation getRelationById(String relationId) {
        return relationMap.get(relationId);
    }

    /**
     * 获取适用于给定测试用例的所有蜕变关系
     * 
     * @param testCase 要检查的测试用例
     * @return 适用于该测试用例的蜕变关系列表
     */
    public static List<MetamorphicRelation> getApplicableRelations(TestCase testCase) {
        List<MetamorphicRelation> applicableRelations = new ArrayList<>();
        
        for (MetamorphicRelation relation : allRelations) {
            if (relation.isApplicableTo(testCase)) {
                applicableRelations.add(relation);
            }
        }
        
        return applicableRelations;
    }

    /**
     * 为给定的测试用例生成所有适用的蜕变组
     * 
     * @param sourceTest 源测试用例
     * @return 包含所有生成的蜕变组的列表
     */
    public static List<MetamorphicGroup> generateMetamorphicGroups(TestCase sourceTest) {
        List<MetamorphicGroup> allGroups = new ArrayList<>();
        
        // 获取适用于源测试用例的所有蜕变关系
        List<MetamorphicRelation> applicableRelations = getApplicableRelations(sourceTest);
        
        // 为每个适用的蜕变关系生成蜕变组
        for (MetamorphicRelation relation : applicableRelations) {
            List<MetamorphicGroup> groups = relation.createGroups(sourceTest);
            allGroups.addAll(groups);
        }
        
        return allGroups;
    }

    /**
     * 获取蜕变关系的数量
     * 
     * @return 总的蜕变关系数量
     */
    public static int getRelationCount() {
        return allRelations.size();
    }

    /**
     * 检查是否存在指定ID的蜕变关系
     * 
     * @param relationId 要检查的蜕变关系ID
     * @return 如果存在则返回true，否则返回false
     */
    public static boolean hasRelation(String relationId) {
        return relationMap.containsKey(relationId);
    }

    /**
     * 获取所有蜕变关系的ID列表
     * 
     * @return 包含所有蜕变关系ID的列表
     */
    public static List<String> getAllRelationIds() {
        List<String> ids = new ArrayList<>();
        for (MetamorphicRelation relation : allRelations) {
            ids.add(relation.getId());
        }
        return ids;
    }

    /**
     * 获取所有蜕变关系的描述信息
     * 
     * @return 包含所有蜕变关系描述的列表
     */
    public static List<String> getAllRelationDescriptions() {
        List<String> descriptions = new ArrayList<>();
        for (MetamorphicRelation relation : allRelations) {
            descriptions.add(relation.getId() + ": " + relation.getDescription());
        }
        return descriptions;
    }
}