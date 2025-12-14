package paper.pss.exp.lang_project.metamorphicRelations;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

import paper.pss.exp.lang_project.model.MetamorphicRelation;
import paper.pss.exp.lang_project.model.MetamorphicGroup;
import paper.pss.exp.lang_project.model.TestCase;

/**
 * MR2: Time Component Invariance
 * Tests the property that changing time components (hours, minutes, seconds, milliseconds)
 * should not affect the result of isSameDay
 */
public class MR2_relation implements MetamorphicRelation {
    private static final Random random = new Random();

    @Override
    public String getId() {
        return "MR2";
    }

    @Override
    public String getDescription() {
        return "Time Component Invariance - Changing time components should not affect isSameDay result";
    }

    @Override
    public List<TestCase> generateFollowupTests(TestCase sourceTest) {
        List<TestCase> followupTests = new ArrayList<>();
        
        Date originalDate1 = sourceTest.getDate1();
        Date originalDate2 = sourceTest.getDate2();
        
        if (originalDate1 == null || originalDate2 == null) {
            return followupTests;
        }
        
        // Create modified dates with different time components
        Date modifiedDate1 = createModifiedTimeDate(originalDate1);
        Date modifiedDate2 = createModifiedTimeDate(originalDate2);
        
        // Generate follow-up test with modified time components
        TestCase followupTest = new TestCase(
            modifiedDate1,
            modifiedDate2,
            sourceTest.getPartitionId()
        );
        
        followupTests.add(followupTest);
        return followupTests;
    }
    
    /**
     * Creates a new Date with modified time components but same date
     */
    @SuppressWarnings("deprecation")
    private Date createModifiedTimeDate(Date originalDate) {
        Date modifiedDate = new Date(originalDate.getTime());
        
        // Randomly modify time components
        modifiedDate.setHours(random.nextInt(24));
        modifiedDate.setMinutes(random.nextInt(60));
        modifiedDate.setSeconds(random.nextInt(60));
        
        // Also modify milliseconds by creating a new date with different milliseconds
        long timeWithoutMs = (modifiedDate.getTime() / 1000) * 1000;
        modifiedDate = new Date(timeWithoutMs + random.nextInt(1000));
        
        return modifiedDate;
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
        // The results should be the same since only time components changed
        return sourceResult == followupResult;
    }

    @Override
    public boolean isApplicableTo(TestCase testCase) {
        // Applicable to all test cases with two valid dates
        return testCase.getDate1() != null && testCase.getDate2() != null;
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
     * 测试和调试MR2关系的主方法
     */
    @SuppressWarnings("deprecation")
    public static void main(String[] args) {
        System.out.println("=== MR2 时间组件不变性测试 ===");
        MR2_relation mr2 = new MR2_relation();
        
        // 创建测试用例
        Date date1 = new Date(2023, 5, 15, 10, 30, 45);
        Date date2 = new Date(2023, 5, 15, 14, 20, 10);
        
        TestCase sourceTest = new TestCase(date1, date2, 1);
        
        System.out.println("源测试: date1=" + date1 + ", date2=" + date2);
        
        // 检查是否适用
        boolean applicable = mr2.isApplicableTo(sourceTest);
        System.out.println("适用性: " + applicable);
        
        if (applicable) {
            // 生成后续测试
            List<TestCase> followupTests = mr2.generateFollowupTests(sourceTest);
            System.out.println("生成的后续测试数量: " + followupTests.size());
            
            for (TestCase followup : followupTests) {
                System.out.println("后续测试: date1=" + followup.getDate1() + ", date2=" + followup.getDate2());
                
                // 模拟验证关系 - 同一天的不同时间应该返回true
                boolean sourceResult = true; // 假设是同一天
                boolean followupResult = true; // 应该仍然是同一天
                
                boolean verified = mr2.verifyRelation(sourceTest, followup, sourceResult, followupResult, "", "");
                System.out.println("关系验证: " + (verified ? "✓ 通过" : "✗ 失败"));
                
                // 显示时间组件的变化
                System.out.println("  原始时间: " + date1.getHours() + ":" + date1.getMinutes() + ":" + date1.getSeconds());
                System.out.println("  修改时间: " + followup.getDate1().getHours() + ":" + followup.getDate1().getMinutes() + ":" + followup.getDate1().getSeconds());
            }
            
            // 测试蜕变组创建
            List<MetamorphicGroup> groups = mr2.createGroups(sourceTest);
            System.out.println("创建的蜕变组数量: " + groups.size());
        }
        
        System.out.println("\n=== MR2测试完成 ===");
    }
}