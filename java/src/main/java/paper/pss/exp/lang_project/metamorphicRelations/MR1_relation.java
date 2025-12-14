package paper.pss.exp.lang_project.metamorphicRelations;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import paper.pss.exp.lang_project.model.MetamorphicRelation;
import paper.pss.exp.lang_project.model.MetamorphicGroup;
import paper.pss.exp.lang_project.model.TestCase;

/**
 * MR1: Symmetry Relation
 * Tests the property that isSameDay(date1, date2) == isSameDay(date2, date1)
 */
public class MR1_relation implements MetamorphicRelation {

    @Override
    public String getId() {
        return "MR1";
    }

    @Override
    public String getDescription() {
        return "Symmetry Relation - isSameDay(date1, date2) == isSameDay(date2, date1)";
    }

    @Override
    public List<TestCase> generateFollowupTests(TestCase sourceTest) {
        List<TestCase> followupTests = new ArrayList<>();
        
        // Swap the two dates
        TestCase followupTest = new TestCase(
            sourceTest.getDate2(),
            sourceTest.getDate1(),
            sourceTest.getPartitionId()
        );
        
        followupTests.add(followupTest);
        return followupTests;
    }

    @Override
    public boolean verifyRelation(TestCase sourceTest, TestCase followupTest, 
                                boolean sourceResult, boolean followupResult, 
                                String sourceExecution, String followupExecution) {
        // Case 1: Both tests resulted in errors
        if (!sourceExecution.isEmpty() && !followupExecution.isEmpty()) {
            // Check if both errors are of the same type
            return sourceExecution.equals(followupExecution);
        }

        // Case 2: One test resulted in error, the other didn't
        if (!sourceExecution.isEmpty() || !followupExecution.isEmpty()) {
            // This is inconsistent behavior
            return false;
        }

        // Case 3: Both tests executed successfully (no errors)
        // The results should be the same due to symmetry
        return sourceResult == followupResult;
    }

    @Override
    public boolean isApplicableTo(TestCase testCase) {
        // Applicable to all test cases with two dates
        return true;
    }

    @Override
    public List<MetamorphicGroup> createGroups(TestCase sourceTest) {
        List<MetamorphicGroup> groups = new ArrayList<>();
        List<TestCase> followupTests = generateFollowupTests(sourceTest);

        for (TestCase followupTest : followupTests) {
            groups.add(new MetamorphicGroup(
                    getId(),
                    getDescription(),
                    sourceTest,
                    followupTest));
        }

        return groups;
    }

    /**
     * 测试和调试MR1关系的主方法
     */
    public static void main(String[] args) {
        System.out.println("=== MR1 对称关系测试 ===");
        MR1_relation mr1 = new MR1_relation();
        
        // 创建测试用例
        Date date1 = new Date(2023, 5, 15);
        Date date2 = new Date(2023, 5, 16);
        
        TestCase sourceTest = new TestCase(date1, date2, 1);
        
        System.out.println("源测试: date1=" + date1 + ", date2=" + date2);
        
        // 检查是否适用
        boolean applicable = mr1.isApplicableTo(sourceTest);
        System.out.println("适用性: " + applicable);
        
        if (applicable) {
            // 生成后续测试
            List<TestCase> followupTests = mr1.generateFollowupTests(sourceTest);
            System.out.println("生成的后续测试数量: " + followupTests.size());
            
            for (TestCase followup : followupTests) {
                System.out.println("后续测试: date1=" + followup.getDate1() + ", date2=" + followup.getDate2());
                
                // 模拟验证关系
                boolean sourceResult = true; // 假设结果
                boolean followupResult = true; // 假设结果
                
                boolean verified = mr1.verifyRelation(sourceTest, followup, sourceResult, followupResult, "", "");
                System.out.println("关系验证: " + (verified ? "✓ 通过" : "✗ 失败"));
            }
            
            // 测试蜕变组创建
            List<MetamorphicGroup> groups = mr1.createGroups(sourceTest);
            System.out.println("创建的蜕变组数量: " + groups.size());
        }
        
        System.out.println("\n=== MR1测试完成 ===");
    }
}