package paper.pss.exp.jackson_project.metamorphicRelations;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import paper.pss.exp.jackson_project.model.MetamorphicRelation;
import paper.pss.exp.jackson_project.model.MetamorphicGroup;
import paper.pss.exp.jackson_project.model.TestCase;

/**
 * MR4: Leading Zeros Relation
 * Tests whether adding leading zeros to a number doesn't change its parsed value
 */
public class MR4_relation implements MetamorphicRelation {
    private static final Random random = new Random();

    @Override
    public String getId() {
        return "MR4";
    }

    @Override
    public String getDescription() {
        return "Leading Zeros Relation - Tests adding leading zeros";
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

            // Generate 1-3 different numbers of leading zeros
            int maxVariations = 3;
            List<Integer> zerosCounts = new ArrayList<>();

            for (int i = 0; i < maxVariations; i++) {
                // Generate a number of leading zeros (1 to 5)
                int zerosCount;
                do {
                    zerosCount = 1 + random.nextInt(5);
                } while (zerosCounts.contains(zerosCount));

                zerosCounts.add(zerosCount);

                // Create follow-up test by adding leading zeros
                String followupString;
                if (inputString.startsWith("-")) {
                    // For negative numbers, add zeros after the minus sign
                    followupString = "-" + "0".repeat(zerosCount) + inputString.substring(1);
                } else {
                    // For non-negative numbers, add zeros at the beginning
                    followupString = "0".repeat(zerosCount) + inputString;
                }

                followupTests.add(new TestCase(followupString, sourceTest.getPartitionId()));
            }
        } catch (NumberFormatException e) {
            // Not applicable if the source is not a valid number
        }

        return followupTests;
    }

    @Override
    public boolean verifyRelation(TestCase sourceTest, TestCase followupTest, int sourceResult, int followupResult, String sourceExecution, String followupExecution) {
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
        // The results should be exactly the same
        return sourceResult == followupResult;
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
     * 测试和调试MR4关系的主方法
     */
    public static void main(String[] args) {
        System.out.println("=== MR4 前导零关系测试 ===");
        MR4_relation mr4 = new MR4_relation();
        
        // 测试用例
        String[] testInputs = {"123", "456", "0", "789", "12", "1", "-123", "abc", "", null};
        
        for (String input : testInputs) {
            System.out.println("\n测试输入: " + (input == null ? "null" : "\"" + input + "\""));
            
            TestCase sourceTest = new TestCase(input, 1);
            
            // 检查是否适用
            boolean applicable = mr4.isApplicableTo(sourceTest);
            System.out.println("  适用性: " + applicable);
            
            if (!applicable) {
                continue;
            }
            
            // 生成后续测试
            List<TestCase> followupTests = mr4.generateFollowupTests(sourceTest);
            System.out.println("  生成的后续测试数量: " + followupTests.size());
            
            for (TestCase followup : followupTests) {
                System.out.println("    后续测试: \"" + followup.getInput() + "\"");
                
                // 尝试验证关系
                try {
                    int sourceResult = Integer.parseInt(input);
                    int followupResult = Integer.parseInt(followup.getInput());
                    
                    boolean verified = mr4.verifyRelation(sourceTest, followup, sourceResult, followupResult, "", "");
                    
                    System.out.println("      源结果: " + sourceResult + ", 后续结果: " + followupResult);
                    System.out.println("      关系验证: " + (verified ? "✓ 通过" : "✗ 失败"));
                    
                    // 验证前导零逻辑
                    String expectedWithLeadingZeros = "0" + input;
                    System.out.println("      预期前导零: \"" + expectedWithLeadingZeros + "\"");
                    System.out.println("      实际后续: \"" + followup.getInput() + "\"");
                    System.out.println("      前导零匹配: " + (expectedWithLeadingZeros.equals(followup.getInput()) ? "✓" : "✗"));
                    
                    // 验证数学关系（应该相等）
                    System.out.println("      数学验证: " + sourceResult + " == " + followupResult + " ? " + (sourceResult == followupResult ? "✓" : "✗"));
                    
                } catch (NumberFormatException e) {
                    System.out.println("      解析错误，测试错误处理");
                    boolean verified = mr4.verifyRelation(sourceTest, followup, 0, 0, "NumberFormatException", "NumberFormatException");
                    System.out.println("      错误处理验证: " + (verified ? "✓ 通过" : "✗ 失败"));
                }
            }
            
            // 测试蜕变组创建
            List<MetamorphicGroup> groups = mr4.createGroups(sourceTest);
            System.out.println("  创建的蜕变组数量: " + groups.size());
        }
        
        System.out.println("\n=== MR4测试完成 ===");
    }
}