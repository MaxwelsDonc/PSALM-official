package paper.pss.exp.math1_project.metamorphicRelations;

import java.util.ArrayList;
import java.util.List;

import paper.pss.exp.math1_project.model.MetamorphicRelation;
import paper.pss.exp.math1_project.model.MetamorphicGroup;
import paper.pss.exp.math1_project.model.TestCase;

/**
 * MR4: Linear Scaling Relation
 * 验证卷积操作的线性缩放特性：
 * convolve(k*x, h) == k*convolve(x, h)
 * convolve(x, k*h) == k*convolve(x, h)
 * 
 * 对于输入数组x和h，当对其中一个数组进行线性缩放(乘以常数k)后，
 * 卷积结果也会按相同系数k缩放（在浮点计算误差范围内）。
 */
public class MR4_relation implements MetamorphicRelation {

    // 浮点比较允许的误差范围
    private static final double EPSILON = 1e-5;

    // 缩放因子
    private static final double SCALING_FACTOR = 2.0;

    @Override
    public String getId() {
        return "MR4";
    }

    @Override
    public String getDescription() {
        return "线性缩放关系 - 验证convolve(k*x, h) == k*convolve(x, h) 且 convolve(x, k*h) == k*convolve(x, h)";
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
            double[] h = sourceTest.getH();

            // 生成第一个后续测试：缩放x数组
            double[] scaledX = scaleArray(x, SCALING_FACTOR);
            TestCase followupTest1 = new TestCase(
                    scaledX,
                    h.clone(),
                    sourceTest.getPartitionId()
            );
            followupTests.add(followupTest1);

            // 生成第二个后续测试：缩放h数组
            double[] scaledH = scaleArray(h, SCALING_FACTOR);
            TestCase followupTest2 = new TestCase(
                    x.clone(),
                    scaledH,
                    sourceTest.getPartitionId()
            );
            followupTests.add(followupTest2);

        } catch (Exception e) {
            // 如果发生异常，记录并返回空列表
            System.err.println("生成MR4后续测试时出错: " + e.getMessage());
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

        // 验证数组长度是否相等
        if (sourceResult.length != followupResult.length) {
            return false;
        }

        // 计算期望的缩放结果
        double[] expectedResult = scaleArray(sourceResult, SCALING_FACTOR);

        // 验证数组元素是否在误差范围内相等
        return arraysEqualWithinEpsilon(expectedResult, followupResult, EPSILON);
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
     * 将数组中的每个元素乘以缩放因子k
     */
    private double[] scaleArray(double[] array, double k) {
        double[] scaledArray = new double[array.length];
        for (int i = 0; i < array.length; i++) {
            scaledArray[i] = array[i] * k;
        }
        return scaledArray;
    }

    @Override
    public boolean isApplicableTo(TestCase testCase) {
        // 线性缩放适用于所有有效的测试用例
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