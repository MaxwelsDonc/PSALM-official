package paper.pss.exp.lang_project.metamorphicRelations;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Random;

import paper.pss.exp.lang_project.model.MetamorphicRelation;
import paper.pss.exp.lang_project.model.MetamorphicGroup;
import paper.pss.exp.lang_project.model.TestCase;

/**
 * MR5: Date Addition Relation
 * Tests the property that adding the same number of days to both dates
 * should not change the result of isSameDay
 */
public class MR5_relation implements MetamorphicRelation {
    private static final Random random = new Random();
    private static final int MAX_DAYS_TO_ADD = 365; // Maximum days to add/subtract

    @Override
    public String getId() {
        return "MR5";
    }

    @Override
    public String getDescription() {
        return "Date Addition Relation - Adding same days to both dates should preserve isSameDay result";
    }

    @Override
    public List<TestCase> generateFollowupTests(TestCase sourceTest) {
        List<TestCase> followupTests = new ArrayList<>();
        
        Date originalDate1 = sourceTest.getDate1();
        Date originalDate2 = sourceTest.getDate2();
        
        if (originalDate1 == null || originalDate2 == null) {
            return followupTests;
        }
        
        // Generate a random number of days to add (can be negative)
        int daysToAdd = random.nextInt(2 * MAX_DAYS_TO_ADD + 1) - MAX_DAYS_TO_ADD;
        
        // Add the same number of days to both dates
        Date modifiedDate1 = addDays(originalDate1, daysToAdd);
        Date modifiedDate2 = addDays(originalDate2, daysToAdd);
        
        // Generate follow-up test with modified dates
        TestCase followupTest = new TestCase(
            modifiedDate1,
            modifiedDate2,
            sourceTest.getPartitionId()
        );
        
        followupTests.add(followupTest);
        return followupTests;
    }
    
    /**
     * Adds the specified number of days to a date
     */
    private Date addDays(Date originalDate, int daysToAdd) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(originalDate);
        calendar.add(Calendar.DAY_OF_MONTH, daysToAdd);
        return calendar.getTime();
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
        // The results should be the same since we added the same number of days to both dates
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
     * 测试和调试MR5关系的主方法
     */
    @SuppressWarnings("deprecation")
    public static void main(String[] args) {
        System.out.println("=== MR5 日期加法关系测试 ===");
        MR5_relation mr5 = new MR5_relation();
        
        // 创建测试用例
        Date date1 = new Date(2023, 5, 15, 10, 30, 45);
        Date date2 = new Date(2023, 5, 16, 14, 20, 10);
        
        TestCase sourceTest = new TestCase(date1, date2, 1);
        
        System.out.println("源测试: date1=" + date1 + ", date2=" + date2);
        
        // 检查是否适用
        boolean applicable = mr5.isApplicableTo(sourceTest);
        System.out.println("适用性: " + applicable);
        
        if (applicable) {
            // 生成后续测试
            List<TestCase> followupTests = mr5.generateFollowupTests(sourceTest);
            System.out.println("生成的后续测试数量: " + followupTests.size());
            
            for (TestCase followup : followupTests) {
                System.out.println("后续测试: date1=" + followup.getDate1() + ", date2=" + followup.getDate2());
                
                // 模拟验证关系 - 不同天的日期加上相同天数后应该仍然不同
                boolean sourceResult = false; // 假设不是同一天
                boolean followupResult = false; // 应该仍然不是同一天
                
                boolean verified = mr5.verifyRelation(sourceTest, followup, sourceResult, followupResult, "", "");
                System.out.println("关系验证: " + (verified ? "✓ 通过" : "✗ 失败"));
                
                // 计算添加的天数
                long daysDiff1 = (followup.getDate1().getTime() - date1.getTime()) / (24 * 60 * 60 * 1000);
                long daysDiff2 = (followup.getDate2().getTime() - date2.getTime()) / (24 * 60 * 60 * 1000);
                
                System.out.println("  添加的天数: " + daysDiff1 + " (两个日期应该相同: " + (daysDiff1 == daysDiff2) + ")");
                
                // 显示日期变化
                System.out.println("  原始日期1: " + date1.getDate() + "/" + (date1.getMonth() + 1) + "/" + (date1.getYear() + 1900));
                System.out.println("  修改日期1: " + followup.getDate1().getDate() + "/" + (followup.getDate1().getMonth() + 1) + "/" + (followup.getDate1().getYear() + 1900));
            }
            
            // 测试蜕变组创建
            List<MetamorphicGroup> groups = mr5.createGroups(sourceTest);
            System.out.println("创建的蜕变组数量: " + groups.size());
        }
        
        System.out.println("\n=== MR5测试完成 ===");
    }
}