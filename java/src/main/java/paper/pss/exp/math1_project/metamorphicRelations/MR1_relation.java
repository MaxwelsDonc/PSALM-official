package paper.pss.exp.math1_project.metamorphicRelations;

import java.util.ArrayList;
import java.util.List;

import paper.pss.exp.math1_project.model.MetamorphicRelation;
import paper.pss.exp.math1_project.model.MetamorphicGroup;
import paper.pss.exp.math1_project.model.TestCase;

/**
 * MR1: Commutative Relation
 * 验证卷积操作的交换律特性：convolve(x,h) == convolve(h,x)
 * 对于任意输入数组x和h，交换它们的位置后，卷积结果应保持不变
 * （在浮点计算误差范围内）。
 */
public class MR1_relation implements MetamorphicRelation {

    // 浮点比较允许的误差范围
    private static final double EPSILON = 1e-5;

    @Override
    public String getId() {
        return "MR1";
    }

    @Override
    public String getDescription() {
        return "交换律关系 - 验证对任意输入数组x和h，convolve(x,h) == convolve(h,x)";
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

            // 交换数组x和h来创建后续测试用例
            TestCase followupTest = new TestCase(
                    h.clone(), // 原数组h作为新数组x
                    x.clone(), // 原数组x作为新数组h
                    sourceTest.getPartitionId() // 保持分区ID不变
            );

            followupTests.add(followupTest);

        } catch (Exception e) {
            // 如果发生异常，记录并返回空列表
            System.err.println("生成MR1后续测试时出错: " + e.getMessage());
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

        // 如果结果都是null，则认为关系不满足
        // 检查这两个结果是否都为空
        if (sourceResult == null || followupResult == null ||
                sourceResult.length == 0 || followupResult.length == 0) {
            return false;
        }

        // 验证数组长度是否相等
        if (sourceResult.length != followupResult.length) {
            return false;
        }

        // 验证数组元素是否在误差范围内相等
        return arraysEqualWithinEpsilon(sourceResult, followupResult, EPSILON);
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
        // 交换律适用于所有有效的测试用例
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