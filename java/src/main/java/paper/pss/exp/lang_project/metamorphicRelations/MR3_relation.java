package paper.pss.exp.lang_project.metamorphicRelations;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import paper.pss.exp.lang_project.model.MetamorphicRelation;
import paper.pss.exp.lang_project.model.MetamorphicGroup;
import paper.pss.exp.lang_project.model.TestCase;

/**
 * MR3: Calendar Conversion Consistency
 * Tests the property that using Calendar objects to represent the same dates
 * should produce the same result as using Date objects directly
 */
public class MR3_relation implements MetamorphicRelation {

    @Override
    public String getId() {
        return "MR3";
    }

    @Override
    public String getDescription() {
        return "Calendar Conversion Consistency - Calendar and Date representations should be consistent";
    }

    @Override
    public List<TestCase> generateFollowupTests(TestCase sourceTest) {
        List<TestCase> followupTests = new ArrayList<>();
        
        Date originalDate1 = sourceTest.getDate1();
        Date originalDate2 = sourceTest.getDate2();
        
        if (originalDate1 == null || originalDate2 == null) {
            return followupTests;
        }
        
        // Convert dates to Calendar and back to Date
        Date calendarDate1 = convertThroughCalendar(originalDate1);
        Date calendarDate2 = convertThroughCalendar(originalDate2);
        
        // Generate follow-up test with calendar-converted dates
        TestCase followupTest = new TestCase(
            calendarDate1,
            calendarDate2,
            sourceTest.getPartitionId()
        );
        
        followupTests.add(followupTest);
        return followupTests;
    }
    
    /**
     * Converts a Date through Calendar to ensure consistency
     */
    private Date convertThroughCalendar(Date originalDate) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(originalDate);
        
        // Extract date components
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        int second = calendar.get(Calendar.SECOND);
        int millisecond = calendar.get(Calendar.MILLISECOND);
        
        // Create a new Calendar with the same components
        Calendar newCalendar = Calendar.getInstance();
        newCalendar.clear();
        newCalendar.set(year, month, day, hour, minute, second);
        newCalendar.set(Calendar.MILLISECOND, millisecond);
        
        return newCalendar.getTime();
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
        // The results should be the same since the dates represent the same moments
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
     * 测试和调试MR3关系的主方法
     */
    public static void main(String[] args) {
        System.out.println("=== MR3 日历转换一致性测试 ===");
        MR3_relation mr3 = new MR3_relation();
        
        // 创建测试用例
        Date date1 = new Date(2023, 5, 15, 10, 30, 45);
        Date date2 = new Date(2023, 5, 16, 14, 20, 10);
        
        TestCase sourceTest = new TestCase(date1, date2, 1);
        
        System.out.println("源测试: date1=" + date1 + ", date2=" + date2);
        
        // 检查是否适用
        boolean applicable = mr3.isApplicableTo(sourceTest);
        System.out.println("适用性: " + applicable);
        
        if (applicable) {
            // 生成后续测试
            List<TestCase> followupTests = mr3.generateFollowupTests(sourceTest);
            System.out.println("生成的后续测试数量: " + followupTests.size());
            
            for (TestCase followup : followupTests) {
                System.out.println("后续测试: date1=" + followup.getDate1() + ", date2=" + followup.getDate2());
                
                // 模拟验证关系
                boolean sourceResult = false; // 假设不是同一天
                boolean followupResult = false; // 应该仍然不是同一天
                
                boolean verified = mr3.verifyRelation(sourceTest, followup, sourceResult, followupResult, "", "");
                System.out.println("关系验证: " + (verified ? "✓ 通过" : "✗ 失败"));
                
                // 显示日期转换的一致性
                System.out.println("  原始日期1: " + date1.getTime());
                System.out.println("  转换日期1: " + followup.getDate1().getTime());
                System.out.println("  时间差: " + Math.abs(date1.getTime() - followup.getDate1().getTime()) + "ms");
            }
            
            // 测试蜕变组创建
            List<MetamorphicGroup> groups = mr3.createGroups(sourceTest);
            System.out.println("创建的蜕变组数量: " + groups.size());
        }
        
        System.out.println("\n=== MR3测试完成 ===");
    }
}