package paper.pss.exp.math1_project.metamorphicRelations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import org.apache.commons.math3.util.MathArrays;

import paper.pss.exp.math1_project.model.MetamorphicRelation;
import paper.pss.exp.math1_project.model.MetamorphicGroup;
import paper.pss.exp.math1_project.model.TestCase;

/**
 * MR2: Associative Relation
 * 验证卷积操作的结合律特性：
 * convolve(convolve(x, h1), h2) == convolve(x, convolve(h1, h2))
 * 
 * 对于三个输入数组x、h1和h2，无论先执行哪两个数组的卷积，
 * 最终结果应保持一致（在浮点计算误差范围内）。
 */
public class MR2_relation implements MetamorphicRelation {

    // 浮点比较允许的误差范围
    private static final double EPSILON = 1e-5;

    // 随机数生成器，用于生成h2数组
    private static final Random random = new Random();

    @Override
    public String getId() {
        return "MR2";
    }

    @Override
    public String getDescription() {
        return "结合律关系 - 验证convolve(convolve(x, h1), h2) == convolve(x, convolve(h1, h2))";
    }

    @Override
    public List<TestCase> generateFollowupTests(TestCase sourceTest) {
        List<TestCase> followupTests = new ArrayList<>();

        // 如果源测试用例不可用，返回空列表
        if (sourceTest == null) {
            return followupTests;
        }

        try {
            // 获取源测试中的输入数组
            double[] x = sourceTest.getX();
            double[] h1 = sourceTest.getH();

            // 生成一个简单的h2数组（使用固定模式以便验证）
            double[] h2 = {1.0, 0.5}; // 使用固定的简单数组

            // 计算convolve(h1, h2)
            double[] h1_conv_h2 = MathArrays.convolve(h1, h2);

            // 创建后续测试用例：使用原始x和计算得到的h1_conv_h2
            TestCase followupTest = new TestCase(
                    x.clone(),
                    h1_conv_h2,
                    sourceTest.getPartitionId()
            );

            followupTests.add(followupTest);

        } catch (Exception e) {
            // 如果发生异常，记录并返回空列表
            System.err.println("生成MR2后续测试时出错: " + e.getMessage());
        }

        return followupTests;
    }

    @Override
    public boolean verifyRelation(TestCase sourceTest, TestCase followupTest,
            double[] sourceResult, double[] followupResult,
            String sourceExecution, String followupExecution) {

        // 检查是否有任何执行错误
        if (!sourceExecution.isEmpty() || !followupExecution.isEmpty()) {
            // 如果两个测试都有相同类型的错误，关系仍被认为是满足的
            return sourceExecution.equals(followupExecution);
        }

        // 检查结果是否为空
        if (sourceResult == null || followupResult == null ||
                sourceResult.length == 0 || followupResult.length == 0) {
            return false;
        }

        try {
            // 使用固定的h2数组（与生成时相同）
            double[] h2 = {1.0, 0.5};

            // 计算convolve(sourceResult, h2)，这应该等于followupResult
            double[] expected = MathArrays.convolve(sourceResult, h2);

            // 验证数组长度是否相等
            if (expected.length != followupResult.length) {
                return false;
            }

            // 验证数组元素是否在误差范围内相等
            return arraysEqualWithinEpsilon(expected, followupResult, EPSILON);

        } catch (Exception e) {
            System.err.println("验证MR2关系时出错: " + e.getMessage());
            return false;
        }
    }

    /**
     * 检查两个数组是否在指定误差范围内相等
     */
    private boolean arraysEqualWithinEpsilon(double[] arr1, double[] arr2, double epsilon) {
        if (arr1.length != arr2.length) {
            return false;
        }

        for (int i = 0; i < arr1.length; i++) {
            if (Math.abs(arr1[i] - arr2[i]) > epsilon) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean isApplicableTo(TestCase testCase) {
        // 结合律适用于所有有效的测试用例
        return testCase != null && testCase.getX() != null && testCase.getH() != null
                && testCase.getX().length > 0 && testCase.getH().length > 0;
    }

    @Override
    public List<MetamorphicGroup> createGroups(TestCase sourceTest) {
        List<MetamorphicGroup> groups = new ArrayList<>();
        
        if (isApplicableTo(sourceTest)) {
            List<TestCase> followupTests = generateFollowupTests(sourceTest);
            for (TestCase followupTest : followupTests) {
                MetamorphicGroup group = new MetamorphicGroup(
                    getId(),
                    getDescription(),
                    sourceTest,
                    followupTest
                );
                groups.add(group);
            }
        }
        
        return groups;
    }
}