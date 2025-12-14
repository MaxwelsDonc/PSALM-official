package paper.pss.exp.jackson_project.metamorphicRelations;

import java.util.ArrayList;
import java.util.List;

import paper.pss.exp.jackson_project.model.MetamorphicRelation;
import paper.pss.exp.jackson_project.model.MetamorphicGroup;
import paper.pss.exp.jackson_project.model.TestCase;

/**
 * MR1: Sign Symmetry Relation
 * Tests the property that parseInt("-" + n) == -parseInt(n) for valid positive integers
 * and parseInt(n.substring(1)) == -parseInt(n) for valid negative integers
 */
public class MR1_relation implements MetamorphicRelation {

    @Override
    public String getId() {
        return "MR1";
    }

    @Override
    public String getDescription() {
        return "Sign Symmetry Relation - Tests sign conversion properties";
    }

    @Override
    public List<TestCase> generateFollowupTests(TestCase sourceTest) {
        List<TestCase> followupTests = new ArrayList<>();
        String inputString = sourceTest.getInput();

        if (inputString != null) {
            String followupString;

            if (inputString.startsWith("-")) {
                // For negative numbers, remove the minus sign
                followupString = inputString.substring(1);
            } else {
                // For non-negative numbers, add a minus sign
                followupString = "-" + inputString;
            }

            // Create the follow-up test case with the same partition ID
            // Note: We don't check if it's a valid integer here, as we want to test
            // behavior for invalid inputs too (they should produce similar errors)
            followupTests.add(new TestCase(followupString, sourceTest.getPartitionId()));
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
            // This is inconsistent behavior - one input parsed successfully while the other failed
            return false;
        }

        // Case 3: Both tests executed successfully (no errors)
        // Check the numeric relationship: the sum of the source and follow-up results should be zero
        // (e.g., parseInt("123") + parseInt("-123") = 0)
        return sourceResult + followupResult == 0;
    }

    @Override
    public boolean isApplicableTo(TestCase testCase) {
        String inputString = testCase.getInput();

        // Only check if the input is not null
        return inputString != null;
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
        System.out.println("=== MR1 符号对称关系测试 ===");
        MR1_relation mr1 = new MR1_relation();
        
        // 测试用例
        String[] testInputs = {"123", "-456", "0", "789", "-12", "abc", "", null};
        
        for (String input : testInputs) {
            System.out.println("\n测试输入: " + (input == null ? "null" : "\"" + input + "\""));
            
            TestCase sourceTest = new TestCase(input, 1);
            
            // 检查是否适用
            boolean applicable = mr1.isApplicableTo(sourceTest);
            System.out.println("  适用性: " + applicable);
            
            if (!applicable) {
                continue;
            }
            
            // 生成后续测试
            List<TestCase> followupTests = mr1.generateFollowupTests(sourceTest);
            System.out.println("  生成的后续测试数量: " + followupTests.size());
            
            for (TestCase followup : followupTests) {
                System.out.println("    后续测试: \"" + followup.getInput() + "\"");
                
                // 尝试验证关系
                try {
                    int sourceResult = Integer.parseInt(input);
                    int followupResult = Integer.parseInt(followup.getInput());
                    
                    boolean verified = mr1.verifyRelation(sourceTest, followup, sourceResult, followupResult, "", "");
                    
                    System.out.println("      源结果: " + sourceResult + ", 后续结果: " + followupResult);
                    System.out.println("      关系验证: " + (verified ? "✓ 通过" : "✗ 失败"));
                    System.out.println("      数学验证: " + sourceResult + " + " + followupResult + " = " + (sourceResult + followupResult));
                    
                } catch (NumberFormatException e) {
                    System.out.println("      解析错误，测试错误处理");
                    boolean verified = mr1.verifyRelation(sourceTest, followup, 0, 0, "NumberFormatException", "NumberFormatException");
                    System.out.println("      错误处理验证: " + (verified ? "✓ 通过" : "✗ 失败"));
                }
            }
            
            // 测试蜕变组创建
            List<MetamorphicGroup> groups = mr1.createGroups(sourceTest);
            System.out.println("  创建的蜕变组数量: " + groups.size());
        }
        
        System.out.println("\n=== MR1测试完成 ===");
    }
}