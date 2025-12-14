package paper.pss.exp.math1_project.metamorphicRelations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import paper.pss.exp.math1_project.model.MetamorphicRelation;
import paper.pss.exp.math1_project.model.MetamorphicGroup;
import paper.pss.exp.math1_project.model.TestCase;

/**
 * MR5: Shift Relation
 * 验证卷积操作的平移特性：
 * 当输入序列在时域中平移（通过在数组末尾添加零）时，
 * 输出结果也会按照相同的方式平移。
 */
public class MR5_relation implements MetamorphicRelation {

    // 浮点比较允许的误差范围
    private static final double EPSILON = 1e-5;

    // 添加零的数量
    private static final int SHIFT_AMOUNT = 2;

    @Override
    public String getId() {
        return "MR5";
    }

    @Override
    public String getDescription() {
        return "平移特性关系 - 验证当输入序列在时域中平移（通过添加零）时，输出结果也会相应平移";
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

            // 在x数组末尾添加零来实现平移
            double[] shiftedX = appendZeros(x, SHIFT_AMOUNT);

            // 创建后续测试用例
            TestCase followupTest = new TestCase(
                    shiftedX,
                    h.clone(),
                    sourceTest.getPartitionId()
            );

            followupTests.add(followupTest);

        } catch (Exception e) {
            // 如果发生异常，记录并返回空列表
            System.err.println("生成MR5后续测试时出错: " + e.getMessage());
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

        // 验证平移特性：followupResult应该是sourceResult在末尾添加零后的结果
        // 由于我们在输入x末尾添加了SHIFT_AMOUNT个零，输出也应该相应地扩展
        int expectedLength = sourceResult.length + SHIFT_AMOUNT;
        
        if (followupResult.length != expectedLength) {
            return false;
        }

        // 检查前面的元素是否相等
        for (int i = 0; i < sourceResult.length; i++) {
            if (Math.abs(sourceResult[i] - followupResult[i]) > EPSILON) {
                return false;
            }
        }

        // 检查后面添加的元素是否接近零（由于浮点误差，可能不完全为零）
        for (int i = sourceResult.length; i < followupResult.length; i++) {
            if (Math.abs(followupResult[i]) > EPSILON) {
                return false;
            }
        }

        return true;
    }

    /**
     * 在数组末尾添加指定数量的零
     */
    private double[] appendZeros(double[] array, int zeroCount) {
        double[] result = Arrays.copyOf(array, array.length + zeroCount);
        // Arrays.copyOf已经用0填充了新的元素
        return result;
    }

    @Override
    public boolean isApplicableTo(TestCase testCase) {
        // 平移特性适用于所有有效的测试用例
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