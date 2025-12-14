package paper.pss.exp.jackson_project.metamorphicRelations;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import paper.pss.exp.jackson_project.model.MetamorphicRelation;
import paper.pss.exp.jackson_project.model.MetamorphicGroup;
import paper.pss.exp.jackson_project.model.TestCase;

/**
 * MR2: Number Concatenation Relation
 * Tests whether a number can be correctly split into parts and recombined
 * based on positional value principles
 */
public class MR2_relation implements MetamorphicRelation {
    private static final Random random = new Random();

    @Override
    public String getId() {
        return "MR2";
    }

    @Override
    public String getDescription() {
        return "Number Concatenation Relation - Tests splitting and combining numbers";
    }

    @Override
    public List<TestCase> generateFollowupTests(TestCase sourceTest) {
        List<TestCase> followupTests = new ArrayList<>();
        String inputString = sourceTest.getInput();

        if (inputString == null || inputString.isEmpty()) {
            return followupTests;
        }

        try {
            // Skip leading minus sign for splitting
            int startIndex = 0;
            boolean isNegative = false;
            if (inputString.startsWith("-")) {
                startIndex = 1;
                isNegative = true;
            }

            // Check if it's a valid number
            Integer.parseInt(inputString);

            // If only 1 digit, return the original string
            if (inputString.length() - startIndex <= 1) {
                followupTests.add(new TestCase(inputString, sourceTest.getPartitionId()));
                return followupTests;
            }

            // Generate 1-3 different split positions
            int maxSplits = Math.min(2, inputString.length() - startIndex - 1);
            List<Integer> splitPositions = new ArrayList<>();

            for (int i = 0; i < maxSplits; i++) {
                // Generate a split position (at least 1 digit in each part)
                int position;
                do {
                    position = startIndex + 1 + random.nextInt(inputString.length() - startIndex - 1);
                } while (splitPositions.contains(position));

                splitPositions.add(position);

                // Create follow-up test with the first part only
                String firstPart = inputString.substring(startIndex, position);

                // Add the sign back if needed
                String followupString = isNegative ? "-" + firstPart : firstPart;

                // Ensure the follow-up is a valid number
                try {
                    Integer.parseInt(followupString);
                    followupTests.add(new TestCase(followupString, sourceTest.getPartitionId()));
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
        // 获取输入字符串
        String sourceString = sourceTest.getInput();
        String followupString = followupTest.getInput();

        // 如果输入字符串完全相同，直接比较结果
        if (sourceString.equals(followupString)) {
            return sourceResult == followupResult;
        }

        // 处理负数情况
        boolean isNegative = sourceString.startsWith("-");
        boolean followupNegative = followupString.startsWith("-");

        // 提取实际部分（去掉负号）
        String firstPart = followupNegative ? followupString.substring(1) : followupString;
        int startIndex = isNegative ? 1 : 0;

        // 检查 follow-up 字符串是否是 source 字符串的前缀
        if (!sourceString.substring(startIndex).startsWith(firstPart)) {
            return false;
        }

        // 提取 source 字符串的第二部分
        String secondPart = sourceString.substring(startIndex + firstPart.length());

        // 如果第二部分为空，返回 false
        if (secondPart.isEmpty()) {
            return false;
        }

        // 解析第二部分为整数
        int secondPartValue = Integer.parseInt(secondPart);

        // 计算期望的 source 值
        long expectedSource;
        if (isNegative) {
            expectedSource = -1L * (Math.abs((long)followupResult) * (long)Math.pow(10, secondPart.length()) + secondPartValue);
        } else {
            expectedSource = (long)followupResult * (long)Math.pow(10, secondPart.length()) + secondPartValue;
        }

        // 检查整数溢出
        if (expectedSource > Integer.MAX_VALUE || expectedSource < Integer.MIN_VALUE) {
            return false;
        }

        // 比较实际结果和期望结果
        return sourceResult == (int)expectedSource;
    }

    @Override
    public boolean isApplicableTo(TestCase testCase) {
        String inputString = testCase.getInput();

        // Not applicable to null inputs
        if (inputString == null) {
            return false;
        }
        return true;
    }

    @Override
    public List<MetamorphicGroup> createGroups(TestCase sourceTest) {
        List<MetamorphicGroup> groups = new ArrayList<>();
        List<TestCase> followupTests = generateFollowupTests(sourceTest);

        // Create a group for each follow-up test (first part only)
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
    public static void main(String[] args) {
        System.out.println("=== MR2 数字连接关系测试 ===");
        MR2_relation mr2 = new MR2_relation();
        
        // 测试用例
        String[] testInputs = {"123", "456", "0", "789", "12", "abc", "", null, "999"};
        
        for (String input : testInputs) {
            System.out.println("\n测试输入: " + (input == null ? "null" : "\"" + input + "\""));
            
            TestCase sourceTest = new TestCase(input, 1);
            
            // 检查是否适用
            boolean applicable = mr2.isApplicableTo(sourceTest);
            System.out.println("  适用性: " + applicable);
            
            if (!applicable) {
                continue;
            }
            
            // 生成后续测试
            List<TestCase> followupTests = mr2.generateFollowupTests(sourceTest);
            System.out.println("  生成的后续测试数量: " + followupTests.size());
            
            for (TestCase followup : followupTests) {
                System.out.println("    后续测试: \"" + followup.getInput() + "\"");
                
                // 尝试验证关系
                try {
                    int sourceResult = Integer.parseInt(input);
                    int followupResult = Integer.parseInt(followup.getInput());
                    
                    boolean verified = mr2.verifyRelation(sourceTest, followup, sourceResult, followupResult, "", "");
                    
                    System.out.println("      源结果: " + sourceResult + ", 后续结果: " + followupResult);
                    System.out.println("      关系验证: " + (verified ? "✓ 通过" : "✗ 失败"));
                    
                    // 验证连接逻辑
                    String expectedConcatenation = input + input;
                    System.out.println("      预期连接: \"" + expectedConcatenation + "\"");
                    System.out.println("      实际后续: \"" + followup.getInput() + "\"");
                    System.out.println("      连接匹配: " + (expectedConcatenation.equals(followup.getInput()) ? "✓" : "✗"));
                    
                } catch (NumberFormatException e) {
                    System.out.println("      解析错误，测试错误处理");
                    boolean verified = mr2.verifyRelation(sourceTest, followup, 0, 0, "NumberFormatException", "NumberFormatException");
                    System.out.println("      错误处理验证: " + (verified ? "✓ 通过" : "✗ 失败"));
                }
            }
            
            // 测试蜕变组创建
            List<MetamorphicGroup> groups = mr2.createGroups(sourceTest);
            System.out.println("  创建的蜕变组数量: " + groups.size());
        }
        
        System.out.println("\n=== MR2测试完成 ===");
    }
}