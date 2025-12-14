package paper.pss.exp.jackson_project.metamorphicRelations;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import paper.pss.exp.jackson_project.model.MetamorphicRelation;
import paper.pss.exp.jackson_project.model.MetamorphicGroup;
import paper.pss.exp.jackson_project.model.TestCase;

/**
 * MR3: Truncation Relation
 * Tests whether removing digits from the end of a number produces the expected result
 * based on integer division by powers of 10
 */
public class MR3_relation implements MetamorphicRelation {
    private static final Random random = new Random();

    @Override
    public String getId() {
        return "MR3";
    }

    @Override
    public String getDescription() {
        return "Truncation Relation - Tests removing digits from the end";
    }

    @Override
    public List<TestCase> generateFollowupTests(TestCase sourceTest) {
        List<TestCase> followupTests = new ArrayList<>();
        String inputString = sourceTest.getInput();

        if (inputString == null || inputString.isEmpty()) {
            return followupTests;
        }

        try {
            // Check if it's a valid number
            Integer.parseInt(inputString);

            // Skip leading minus sign for truncation
            int startIndex = 0;
            boolean isNegative = false;
            if (inputString.startsWith("-")) {
                startIndex = 1;
                isNegative = true;
            }

            // If only 1 digit, cannot truncate
            if (inputString.length() - startIndex <= 1) {
                return followupTests;
            }

            // Generate 1-3 different truncation lengths
            int maxTruncations = Math.min(3, inputString.length() - startIndex - 1);
            List<Integer> truncationLengths = new ArrayList<>();

            for (int i = 0; i < maxTruncations; i++) {
                // Generate a truncation length (remove 1 to n-1 digits)
                int length;
                do {
                    length = 1 + random.nextInt(inputString.length() - startIndex - 1);
                } while (truncationLengths.contains(length));

                truncationLengths.add(length);

                // Create follow-up test by removing 'length' digits from the end
                String truncatedString = inputString.substring(0, inputString.length() - length);

                // Ensure the follow-up is a valid number
                try {
                    Integer.parseInt(truncatedString);
                    followupTests.add(new TestCase(truncatedString, sourceTest.getPartitionId()));
                } catch (NumberFormatException e) {
                    // Skip invalid follow-up tests
                }
            }
        } catch (NumberFormatException e) {
            // Not applicable if the source is not a valid number
        }

        return followupTests;
    }

    @Override
    public boolean verifyRelation(TestCase sourceTest, TestCase followupTest, int sourceResult, int followupResult, String sourceExecution, String followupExecution) {
        // Get input strings
        String sourceString = sourceTest.getInput();
        String followupString = followupTest.getInput();

        // Calculate how many digits were removed
        int digitsRemoved = sourceString.length() - followupString.length();

        // If no digits were removed, the results should be equal
        if (digitsRemoved == 0) {
            return sourceResult == followupResult;
        }

        // Calculate the expected follow-up result by truncating the source result
        long expectedFollowup = sourceResult / (long) Math.pow(10, digitsRemoved);

        // Check for integer overflow
        if (expectedFollowup > Integer.MAX_VALUE || expectedFollowup < Integer.MIN_VALUE) {
            return false;
        }

        // Compare the actual follow-up result with the expected result
        return followupResult == (int) expectedFollowup;
    }

    @Override
    public boolean isApplicableTo(TestCase testCase) {
        String inputString = testCase.getInput();

        // Not applicable to null inputs
        if (inputString == null) {
            return false;
        }

        try {
            // Check if it's a valid number
            Integer.parseInt(inputString);

            // Skip leading minus sign for length calculation
            int startIndex = inputString.startsWith("-") ? 1 : 0;

            // Only applicable if there are at least 2 digits to truncate
            return inputString.length() - startIndex >= 2;
        } catch (NumberFormatException e) {
            return false;
        }
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
        System.out.println("=== MR3 截断关系测试 ===");
        MR3_relation mr3 = new MR3_relation();
        
        // 测试用例
        String[] testInputs = {"12345", "987654321", "123", "12", "1", "0", "-12345", "abc123", "", null};
        
        for (String input : testInputs) {
            System.out.println("\n测试输入: " + (input == null ? "null" : "\"" + input + "\""));
            
            TestCase sourceTest = new TestCase(input, 1);
            
            // 检查是否适用
            boolean applicable = mr3.isApplicableTo(sourceTest);
            System.out.println("  适用性: " + applicable);
            
            if (!applicable) {
                continue;
            }
            
            // 生成后续测试
            List<TestCase> followupTests = mr3.generateFollowupTests(sourceTest);
            System.out.println("  生成的后续测试数量: " + followupTests.size());
            
            for (TestCase followup : followupTests) {
                System.out.println("    后续测试: \"" + followup.getInput() + "\"");
                
                // 尝试验证关系
                try {
                    int sourceResult = Integer.parseInt(input);
                    int followupResult = Integer.parseInt(followup.getInput());
                    
                    boolean verified = mr3.verifyRelation(sourceTest, followup, sourceResult, followupResult, "", "");
                    
                    System.out.println("      源结果: " + sourceResult + ", 后续结果: " + followupResult);
                    System.out.println("      关系验证: " + (verified ? "✓ 通过" : "✗ 失败"));
                    
                    // 验证截断逻辑
                    String expectedTruncation = input.substring(0, input.length() - 1);
                    System.out.println("      预期截断: \"" + expectedTruncation + "\"");
                    System.out.println("      实际后续: \"" + followup.getInput() + "\"");
                    System.out.println("      截断匹配: " + (expectedTruncation.equals(followup.getInput()) ? "✓" : "✗"));
                    
                    // 验证数学关系
                    int expectedResult = sourceResult / 10;
                    System.out.println("      数学验证: " + sourceResult + " / 10 = " + expectedResult);
                    System.out.println("      结果匹配: " + (expectedResult == followupResult ? "✓" : "✗"));
                    
                } catch (NumberFormatException e) {
                    System.out.println("      解析错误，测试错误处理");
                    boolean verified = mr3.verifyRelation(sourceTest, followup, 0, 0, "NumberFormatException", "NumberFormatException");
                    System.out.println("      错误处理验证: " + (verified ? "✓ 通过" : "✗ 失败"));
                }
            }
            
            // 测试蜕变组创建
            List<MetamorphicGroup> groups = mr3.createGroups(sourceTest);
            System.out.println("  创建的蜕变组数量: " + groups.size());
        }
        
        System.out.println("\n=== MR3测试完成 ===");
    }
}