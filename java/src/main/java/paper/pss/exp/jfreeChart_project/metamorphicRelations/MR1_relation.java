package paper.pss.exp.jfreeChart_project.metamorphicRelations;

import java.awt.Shape;
import java.awt.geom.Line2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import paper.pss.exp.jfreeChart_project.model.MetamorphicRelation;
import paper.pss.exp.jfreeChart_project.model.MetamorphicGroup;
import paper.pss.exp.jfreeChart_project.model.TestCase;

/**
 * MR1_relation.java - 实现MR1: 宽度比例关系
 * 
 * 这个蜕变关系检查当保持线段不变，仅改变宽度参数时，
 * 生成的区域面积应该按相应的比例变化。
 * 
 * 具体而言，如果原始测试使用宽度w生成区域面积A，
 * 当宽度变为K*w时，生成的区域面积应接近K*A。
 * (考虑到端点区域和浮点计算误差，允许一定的误差范围)
 */
public class MR1_relation implements MetamorphicRelation {

    private static final Random random = new Random();

    // 默认的宽度比例系数
    private static final double DEFAULT_WIDTH_FACTOR = 2.0;

    // 面积比例验证允许的误差范围 (%)
    private static final double AREA_RATIO_TOLERANCE = 5.0;

    @Override
    public String getId() {
        return "MR1";
    }

    @Override
    public String getDescription() {
        return "宽度比例关系 - 当线段宽度增加K倍时，生成的区域面积应接近K倍";
    }

    @Override
    public List<TestCase> generateFollowupTests(TestCase sourceTest) {
        List<TestCase> followupTests = new ArrayList<>();

        // 如果源测试用例不可用，返回空列表
        if (sourceTest == null) {
            return followupTests;
        }

        try {
            Line2D line = sourceTest.getLine();
            float width = sourceTest.getWidth();

            // 生成2个不同宽度倍数的后续测试用例
            for (int i = 0; i < 2; i++) {
                // 默认使用2倍宽度，也可以随机生成在1.5-3.0之间的倍数
                double widthFactor = (i == 0) ? DEFAULT_WIDTH_FACTOR : 1.5 + random.nextDouble() * 1.5;

                // 创建新的测试用例，保持线段相同，仅改变宽度
                float newWidth = (float) (width * widthFactor);
                TestCase followupTest = new TestCase(
                        line, // 保持相同的线段
                        newWidth, // 新的宽度
                        sourceTest.getPartitionId() // 保持分区ID不变
                );

                followupTests.add(followupTest);
            }
        } catch (Exception e) {
            // 如果发生异常，记录并返回空列表
            System.err.println("生成MR1后续测试时出错: " + e.getMessage());
        }

        return followupTests;
    }

    @Override
    public boolean verifyRelation(TestCase sourceTest, TestCase followupTest,
            Shape sourceResult, Shape followupResult,
            String sourceExecution, String followupExecution) {
        // 检查是否有任何执行错误
        if (!sourceExecution.isEmpty() || !followupExecution.isEmpty()) {
            // 如果两个测试都有相同类型的错误，关系仍被认为是满足的
            return sourceExecution.equals(followupExecution);
        }

        // 如果结果都是null，则认为关系不满足
        if (sourceResult == null || followupResult == null) {
            return true;
        }

        try {
            // 从源测试和后续测试中获取宽度参数，计算比例因子
            float sourceWidth = sourceTest.getWidth();
            float followupWidth = followupTest.getWidth();
            double expectedFactor = followupWidth / sourceWidth;

            // 计算面积
            double sourceAreaSize = calculateAreaSize(sourceResult);
            double followupAreaSize = calculateAreaSize(followupResult);

            // 如果源面积几乎为零，无法进行有效的比例计算
            if (sourceAreaSize < 0.000001 || followupAreaSize < 0.000001) {
                return false;
            }

            // 计算实际的比例因子
            double actualFactor = followupAreaSize / sourceAreaSize;

            // 计算比例因子的偏差百分比
            double deviation = Math.abs(actualFactor - expectedFactor) / expectedFactor * 100.0;

            // 如果偏差在允许的范围内，认为关系满足
            return deviation <= AREA_RATIO_TOLERANCE;

        } catch (Exception e) {
            System.err.println("验证MR1时出错: " + e.getMessage());
            return false;
        }
    }

    /**
     * 计算Area对象的精确面积大小。
     * 使用PathIterator遍历形状边界点，采用Shoelace公式计算多边形面积。
     * 
     * @param area 要计算面积的Area对象
     * @return 计算得到的精确面积
     */
    private double calculateAreaSize(Shape result) {
        List<Point2D.Double> points = new ArrayList<>(4); // 已知是4个顶点

        // 提取路径中的顶点
        PathIterator iterator = result.getPathIterator(null);
        double[] coords = new double[6];
        while (!iterator.isDone()) {
            int type = iterator.currentSegment(coords);
            if (type == PathIterator.SEG_LINETO || type == PathIterator.SEG_MOVETO) {
                points.add(new Point2D.Double(coords[0], coords[1]));
            }
            iterator.next();
        }

        // 确保是四边形（原代码逻辑保证）
        if (points.size() != 4) {
            return -1;
        }

        // 计算向量叉积得到面积（适用于任意平行四边形）
        Point2D.Double p0 = points.get(0);
        Point2D.Double p1 = points.get(1);
        Point2D.Double p2 = points.get(2);

        double dx1 = p1.x - p0.x;
        double dy1 = p1.y - p0.y;
        double dx2 = p2.x - p0.x;
        double dy2 = p2.y - p0.y;

        // 面积 = |(dx1 * dy2) - (dx2 * dy1)|
        return Math.abs(dx1 * dy2 - dx2 * dy1);
    }

    @Override
    public boolean isApplicableTo(TestCase testCase) {
        // 此关系适用于所有有效的线段测试用例
        if (testCase == null || testCase.getLine() == null || testCase.getWidth() <= 0) {
            return false;
        }

        // 线段长度必须大于零
        Line2D line = testCase.getLine();
        return line.getP1().distance(line.getP2()) > 0.001;
    }

    @Override
    public List<MetamorphicGroup> createGroups(TestCase sourceTest) {
        List<MetamorphicGroup> groups = new ArrayList<>();

        // 生成后续测试用例
        List<TestCase> followupTests = generateFollowupTests(sourceTest);

        // 为每个后续测试创建一个蜕变组
        for (TestCase followupTest : followupTests) {
            groups.add(new MetamorphicGroup(
                    getId(),
                    getDescription(),
                    sourceTest,
                    followupTest));
        }

        return groups;
    }

}