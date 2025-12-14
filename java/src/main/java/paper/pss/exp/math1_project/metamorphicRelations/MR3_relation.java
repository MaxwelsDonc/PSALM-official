package paper.pss.exp.math1_project.metamorphicRelations;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.apache.commons.math3.util.MathArrays;

import paper.pss.exp.math1_project.model.MetamorphicRelation;
import paper.pss.exp.math1_project.model.MetamorphicGroup;
import paper.pss.exp.math1_project.model.TestCase;

/**
 * MR3: Distributive Relation
 * 验证卷积操作的分配律特性：
 * convolve(x, add(h1, h2)) == add(convolve(x, h1), convolve(x, h2))
 * 
 * 对于输入数组x、h1和h2，对h1和h2求和后再与x卷积的结果，
 * 应等于分别与x卷积后再求和的结果（在浮点计算误差范围内）。
 */
public class MR3_relation implements MetamorphicRelation {

    // 浮点比较允许的误差范围
    private static final double EPSILON = 1e-5;

    @Override
    public String getId() {
        return "MR3";
    }

    @Override
    public String getDescription() {
        return "分配律关系 - 验证对输入数组x、h1和h2，convolve(x, add(h1, h2)) == add(convolve(x, h1), convolve(x, h2))";
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

            // 生成一个简单的h2数组
            double[] h2 = {0.5, 1.0}; // 使用固定的简单数组

            // 计算add(h1, h2)
            double[] h1_plus_h2 = addArrays(h1, h2);

            // 创建后续测试用例：使用原始x和计算得到的h1_plus_h2
            TestCase followupTest = new TestCase(
                    x.clone(),
                    h1_plus_h2,
                    sourceTest.getPartitionId()
            );

            followupTests.add(followupTest);

        } catch (Exception e) {
            // 如果发生异常，记录并返回空列表
            System.err.println("生成MR3后续测试时出错: " + e.getMessage());
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
            double[] h2 = {0.5, 1.0};
            double[] x = sourceTest.getX();
            double[] h1 = sourceTest.getH();

            // 计算convolve(x, h1)和convolve(x, h2)
            double[] x_conv_h1 = sourceResult; // 这就是源测试的结果
            double[] x_conv_h2 = MathArrays.convolve(x, h2);

            // 计算add(convolve(x, h1), convolve(x, h2))
            double[] expected = addArrays(x_conv_h1, x_conv_h2);

            // 验证数组长度是否相等
            if (expected.length != followupResult.length) {
                return false;
            }

            // 验证数组元素是否在误差范围内相等
            return arraysEqualWithinEpsilon(expected, followupResult, EPSILON);

        } catch (Exception e) {
            System.err.println("验证MR3关系时出错: " + e.getMessage());
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

    /**
     * 将两个数组相加，如果长度不同，较短的数组用0填充
     */
    private double[] addArrays(double[] arr1, double[] arr2) {
        int maxLength = Math.max(arr1.length, arr2.length);
        double[] result = new double[maxLength];
        
        for (int i = 0; i < maxLength; i++) {
            double val1 = (i < arr1.length) ? arr1[i] : 0.0;
            double val2 = (i < arr2.length) ? arr2[i] : 0.0;
            result[i] = val1 + val2;
        }
        
        return result;
    }

    @Override
    public boolean isApplicableTo(TestCase testCase) {
        // 分配律适用于所有有效的测试用例
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