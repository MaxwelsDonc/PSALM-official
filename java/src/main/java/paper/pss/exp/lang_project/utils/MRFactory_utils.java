package paper.pss.exp.lang_project.utils;

import java.util.ArrayList;
import java.util.List;

import paper.pss.exp.lang_project.model.MetamorphicRelation;
import paper.pss.exp.lang_project.metamorphicRelations.MR1_relation;
import paper.pss.exp.lang_project.metamorphicRelations.MR2_relation;
import paper.pss.exp.lang_project.metamorphicRelations.MR3_relation;
import paper.pss.exp.lang_project.metamorphicRelations.MR4_relation;
import paper.pss.exp.lang_project.metamorphicRelations.MR5_relation;
import paper.pss.exp.lang_project.model.MetamorphicGroup;
import paper.pss.exp.lang_project.model.TestCase;

/**
 * Factory class for creating and managing metamorphic relations
 */
public class MRFactory_utils {
    private static final List<MetamorphicRelation> ALL_RELATIONS = new ArrayList<>();

    static {
        // Initialize all metamorphic relations
        ALL_RELATIONS.add(new MR1_relation());
        ALL_RELATIONS.add(new MR2_relation());
        ALL_RELATIONS.add(new MR3_relation());
        ALL_RELATIONS.add(new MR4_relation());
        ALL_RELATIONS.add(new MR5_relation());
    }

    /**
     * Get all available metamorphic relations
     * @return List of all metamorphic relations
     */
    public static List<MetamorphicRelation> getAllRelations() {
        return new ArrayList<>(ALL_RELATIONS);
    }

    /**
     * Get a specific metamorphic relation by ID
     * @param id The ID of the relation (e.g., "MR1", "MR2", etc.)
     * @return The metamorphic relation with the given ID, or null if not found
     */
    public static MetamorphicRelation getRelationById(String id) {
        for (MetamorphicRelation relation : ALL_RELATIONS) {
            if (relation.getId().equals(id)) {
                return relation;
            }
        }
        return null;
    }

    /**
     * Get all applicable metamorphic relations for a given test case
     * @param testCase The test case to check
     * @return List of applicable metamorphic relations
     */
    public static List<MetamorphicRelation> getApplicableRelations(TestCase testCase) {
        List<MetamorphicRelation> applicableRelations = new ArrayList<>();
        for (MetamorphicRelation relation : ALL_RELATIONS) {
            if (relation.isApplicableTo(testCase)) {
                applicableRelations.add(relation);
            }
        }
        return applicableRelations;
    }

    /**
     * Generate all metamorphic groups for a given test case
     * @param sourceTest The source test case
     * @return List of all metamorphic groups generated from applicable relations
     */
    public static List<MetamorphicGroup> generateAllGroups(TestCase sourceTest) {
        List<MetamorphicGroup> allGroups = new ArrayList<>();
        List<MetamorphicRelation> applicableRelations = getApplicableRelations(sourceTest);
        
        for (MetamorphicRelation relation : applicableRelations) {
            allGroups.addAll(relation.createGroups(sourceTest));
        }
        
        return allGroups;
    }

    /**
     * Get the count of available metamorphic relations
     * @return The number of available relations
     */
    public static int getRelationCount() {
        return ALL_RELATIONS.size();
    }

    /**
     * Get the IDs of all available metamorphic relations
     * @return List of relation IDs
     */
    public static List<String> getAllRelationIds() {
        List<String> ids = new ArrayList<>();
        for (MetamorphicRelation relation : ALL_RELATIONS) {
            ids.add(relation.getId());
        }
        return ids;
    }

    /**
     * 测试和调试MRFactory_utils的主方法
     */
    public static void main(String[] args) {
        System.out.println("=== MRFactory_utils 测试 ===");
        
        // 测试获取所有关系
        List<MetamorphicRelation> allRelations = getAllRelations();
        System.out.println("总共有 " + allRelations.size() + " 个蜕变关系:");
        
        for (MetamorphicRelation relation : allRelations) {
            System.out.println("  " + relation.getId() + ": " + relation.getDescription());
        }
        
        // 测试通过ID获取关系
        System.out.println("\n通过ID获取关系测试:");
        String[] testIds = {"MR1", "MR3", "MR6", "INVALID"};
        
        for (String id : testIds) {
            MetamorphicRelation relation = getRelationById(id);
            if (relation != null) {
                System.out.println("  " + id + " -> " + relation.getDescription());
            } else {
                System.out.println("  " + id + " -> 未找到");
            }
        }
        
        // 测试获取关系数量和ID列表
        System.out.println("\n关系数量: " + getRelationCount());
        System.out.println("所有关系ID: " + getAllRelationIds());
        
        // 测试适用性检查
        System.out.println("\n适用性测试:");
        java.util.Date date1 = new java.util.Date(2023, 5, 15);
        java.util.Date date2 = new java.util.Date(2023, 5, 16);
        TestCase testCase = new TestCase(date1, date2, 1);
        
        List<MetamorphicRelation> applicableRelations = getApplicableRelations(testCase);
        System.out.println("对于测试用例 (" + date1 + ", " + date2 + "):");
        System.out.println("  适用的关系数量: " + applicableRelations.size());
        
        for (MetamorphicRelation relation : applicableRelations) {
            System.out.println("    " + relation.getId() + ": " + relation.getDescription());
        }
        
        // 测试生成蜕变组
        List<MetamorphicGroup> allGroups = generateAllGroups(testCase);
        System.out.println("\n生成的蜕变组数量: " + allGroups.size());
        
        for (MetamorphicGroup group : allGroups) {
            System.out.println("  组ID: " + group.getMRId() + ", 描述: " + group.getDescription());
        }
        
        System.out.println("\n=== MRFactory_utils测试完成 ===");
    }
}