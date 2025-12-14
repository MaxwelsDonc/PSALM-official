package paper.pss.exp.lang_project.metamorphicRelations;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import paper.pss.exp.lang_project.model.MetamorphicRelation;
import paper.pss.exp.lang_project.model.MetamorphicGroup;
import paper.pss.exp.lang_project.model.TestCase;

/**
 * MR4: Time Zone Invariance
 * Tests the property that converting dates to different time zones
 * should not affect the result of isSameDay when the dates represent
 * the same calendar day in their respective time zones
 */
public class MR4_relation implements MetamorphicRelation {
    private static final String[] TIME_ZONES = {
        "UTC", "GMT", "America/New_York", "Europe/London", 
        "Asia/Tokyo", "Australia/Sydney", "America/Los_Angeles"
    };

    @Override
    public String getId() {
        return "MR4";
    }

    @Override
    public String getDescription() {
        return "Time Zone Invariance - Time zone conversion should not affect same-day comparison";
    }

    @Override
    public List<TestCase> generateFollowupTests(TestCase sourceTest) {
        List<TestCase> followupTests = new ArrayList<>();
        
        Date originalDate1 = sourceTest.getDate1();
        Date originalDate2 = sourceTest.getDate2();
        
        if (originalDate1 == null || originalDate2 == null) {
            return followupTests;
        }
        
        // Convert dates to different time zones
        String targetTimeZone = TIME_ZONES[1]; // Use GMT as target
        Date convertedDate1 = convertToTimeZone(originalDate1, targetTimeZone);
        Date convertedDate2 = convertToTimeZone(originalDate2, targetTimeZone);
        
        // Generate follow-up test with time zone converted dates
        TestCase followupTest = new TestCase(
            convertedDate1,
            convertedDate2,
            sourceTest.getPartitionId()
        );
        
        followupTests.add(followupTest);
        return followupTests;
    }
    
    /**
     * Converts a Date to a different time zone representation
     * Note: This creates a conceptual conversion for testing purposes
     */
    private Date convertToTimeZone(Date originalDate, String timeZoneId) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(originalDate);
        
        // Get the original time zone offset
        TimeZone originalTz = calendar.getTimeZone();
        int originalOffset = originalTz.getOffset(originalDate.getTime());
        
        // Get the target time zone offset
        TimeZone targetTz = TimeZone.getTimeZone(timeZoneId);
        int targetOffset = targetTz.getOffset(originalDate.getTime());
        
        // Calculate the time difference
        long timeDifference = targetOffset - originalOffset;
        
        // Create a new date with the time zone adjustment
        // Note: For same-day testing, we want to preserve the calendar day
        // so we adjust to maintain the same local time in the new zone
        Calendar targetCalendar = Calendar.getInstance(targetTz);
        targetCalendar.setTime(originalDate);
        
        // Preserve the local date/time components in the new time zone
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        int second = calendar.get(Calendar.SECOND);
        int millisecond = calendar.get(Calendar.MILLISECOND);
        
        targetCalendar.clear();
        targetCalendar.set(year, month, day, hour, minute, second);
        targetCalendar.set(Calendar.MILLISECOND, millisecond);
        
        return targetCalendar.getTime();
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
        // The results should be the same since we preserved the calendar day
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
     * 测试和调试MR4关系的主方法
     */
    public static void main(String[] args) {
        System.out.println("=== MR4 时区不变性测试 ===");
        MR4_relation mr4 = new MR4_relation();
        
        // 创建测试用例
        Date date1 = new Date(2023, 5, 15, 10, 30, 45);
        Date date2 = new Date(2023, 5, 15, 14, 20, 10);
        
        TestCase sourceTest = new TestCase(date1, date2, 1);
        
        System.out.println("源测试: date1=" + date1 + ", date2=" + date2);
        
        // 检查是否适用
        boolean applicable = mr4.isApplicableTo(sourceTest);
        System.out.println("适用性: " + applicable);
        
        if (applicable) {
            // 生成后续测试
            List<TestCase> followupTests = mr4.generateFollowupTests(sourceTest);
            System.out.println("生成的后续测试数量: " + followupTests.size());
            
            for (TestCase followup : followupTests) {
                System.out.println("后续测试: date1=" + followup.getDate1() + ", date2=" + followup.getDate2());
                
                // 模拟验证关系 - 同一天的不同时间应该返回true
                boolean sourceResult = true; // 假设是同一天
                boolean followupResult = true; // 应该仍然是同一天
                
                boolean verified = mr4.verifyRelation(sourceTest, followup, sourceResult, followupResult, "", "");
                System.out.println("关系验证: " + (verified ? "✓ 通过" : "✗ 失败"));
                
                // 显示时区转换的效果
                System.out.println("  原始时间戳1: " + date1.getTime());
                System.out.println("  转换时间戳1: " + followup.getDate1().getTime());
                System.out.println("  时间差: " + (followup.getDate1().getTime() - date1.getTime()) + "ms");
            }
            
            // 测试蜕变组创建
            List<MetamorphicGroup> groups = mr4.createGroups(sourceTest);
            System.out.println("创建的蜕变组数量: " + groups.size());
        }
        
        System.out.println("\n=== MR4测试完成 ===");
    }
}