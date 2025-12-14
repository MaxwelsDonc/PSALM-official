package paper.pss.exp.jackson_project.metamorphicRelations;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import paper.pss.exp.jackson_project.model.MetamorphicRelation;
import paper.pss.exp.jackson_project.model.MetamorphicGroup;
import paper.pss.exp.jackson_project.model.TestCase;

/**
 * MR5: Trailing Zeros Relation
 * Tests whether adding trailing zeros to a number multiplies its parsed value by the appropriate power of 10
 */
public class MR5_relation implements MetamorphicRelation {
    private static final Random random = new Random();

    @Override
    public String getId() {
        return "MR5";
    }

    @Override
    public String getDescription() {
        return "Trailing Zeros Relation - Tests adding trailing zeros";
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

            // Generate 1-3 different numbers of trailing zeros
            int maxVariations = 3;
            List<Integer> zerosCounts = new ArrayList<>();

            for (int i = 0; i < maxVariations; i++) {
                // Generate a number of trailing zeros (1 to 3)
                int zerosCount;
                do {
                    zerosCount = 1 + random.nextInt(3);
                } while (zerosCounts.contains(zerosCount));

                zerosCounts.add(zerosCount);

                // Create follow-up test by adding trailing zeros
                String followupString = inputString + "0".repeat(zerosCount);

                followupTests.add(new TestCase(followupString, sourceTest.getPartitionId()));
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

        // Calculate how many trailing zeros were added
        int zerosAdded = followupString.length() - sourceString.length();

        // If no zeros were added, the results should be equal
        if (zerosAdded == 0) {
            return sourceResult == followupResult;
        }

        // Calculate the expected follow-up result
        long expectedFollowup = (long) sourceResult * (long) Math.pow(10, zerosAdded);

        // Check for integer overflow
        if (expectedFollowup > Integer.MAX_VALUE || expectedFollowup < Integer.MIN_VALUE) {
            // If overflow occurs, both should fail with the same error or handle overflow consistently
            // For this implementation, we'll consider it a failure if overflow occurs
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
            int value = Integer.parseInt(inputString);

            // Not applicable if the number is 0 (adding trailing zeros to 0 still gives 0)
            if (value == 0) {
                return false;
            }
            if (Math.abs(value) >= Integer.MAX_VALUE / 1000) {
                return false;
            }

            return true;
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
     * 测试和调试MR5关系的主方法
     */
    public static void main(String[] args) {
        System.out.println("=== MR5 尾随零关系测试 ===");
        MR5_relation mr5 = new MR5_relation();
        
        // 测试用例
        String[] testInputs = {"123", "456", "0", "789", "12", "1", "-123", "abc", "", null};
        
        for (String input : testInputs) {
            System.out.println("\n测试输入: " + (input == null ? "null" : "\"" + input + "\""));
            
            TestCase sourceTest = new TestCase(input, 1);
            
            // 检查是否适用
            boolean applicable = mr5.isApplicableTo(sourceTest);
            System.out.println("  适用性: " + applicable);
            
            if (!applicable) {
                continue;
            }
            
            // 生成后续测试
            List<TestCase> followupTests = mr5.generateFollowupTests(sourceTest);
            System.out.println("  生成的后续测试数量: " + followupTests.size());
            
            for (TestCase followup : followupTests) {
                System.out.println("    后续测试: \"" + followup.getInput() + "\"");
                
                // 尝试验证关系
                try {
                    int sourceResult = Integer.parseInt(input);
                    int followupResult = Integer.parseInt(followup.getInput());
                    
                    boolean verified = mr5.verifyRelation(sourceTest, followup, sourceResult, followupResult, "", "");
                    
                    System.out.println("      源结果: " + sourceResult + ", 后续结果: " + followupResult);
                    System.out.println("      关系验证: " + (verified ? "✓ 通过" : "✗ 失败"));
                    
                    // 验证尾随零逻辑
                    String expectedWithTrailingZeros = input + "0";
                    System.out.println("      预期尾随零: \"" + expectedWithTrailingZeros + "\"");
                    System.out.println("      实际后续: \"" + followup.getInput() + "\"");
                    System.out.println("      尾随零匹配: " + (expectedWithTrailingZeros.equals(followup.getInput()) ? "✓" : "✗"));
                    
                    // 验证数学关系（应该是10倍）
                    int expectedResult = sourceResult * 10;
                    System.out.println("      数学验证: " + sourceResult + " * 10 = " + expectedResult);
                    System.out.println("      结果匹配: " + (expectedResult == followupResult ? "✓" : "✗"));
                    
                } catch (NumberFormatException e) {
                    System.out.println("      解析错误，测试错误处理");
                    boolean verified = mr5.verifyRelation(sourceTest, followup, 0, 0, "NumberFormatException", "NumberFormatException");
                    System.out.println("      错误处理验证: " + (verified ? "✓ 通过" : "✗ 失败"));
                }
            }
            
            // 测试蜕变组创建
            List<MetamorphicGroup> groups = mr5.createGroups(sourceTest);
            System.out.println("  创建的蜕变组数量: " + groups.size());
        }
        
        System.out.println("\n=== MR5测试完成 ===");
    }
}