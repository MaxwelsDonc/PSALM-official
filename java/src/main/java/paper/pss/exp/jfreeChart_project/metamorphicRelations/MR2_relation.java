package paper.pss.exp.jfreeChart_project.metamorphicRelations;

import java.awt.Shape;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.awt.geom.PathIterator;

import paper.pss.exp.jfreeChart_project.model.MetamorphicRelation;
import paper.pss.exp.jfreeChart_project.model.MetamorphicGroup;
import paper.pss.exp.jfreeChart_project.model.TestCase;

/**
 * MR2_relation.java - 实现MR2: 线段长度比例关系
 * 
 * 这个蜕变关系检查当保持线段方向和宽度不变，仅改变线段长度时，
 * 生成的区域面积应该按相应的比例变化。
 * 
 * 具体而言，如果原始线段长度为L生成区域面积A，
 * 当线段长度变为K*L时，生成的区域面积应接近K*A
 * (考虑到端点区域和浮点计算误差，允许一定的误差范围)
 */
public class MR2_relation implements MetamorphicRelation {

    private static final Random random = new Random();

    // 默认的长度比例系数
    private static final double DEFAULT_LENGTH_FACTOR = 2.0;

    // 面积比例验证允许的误差范围 (%)
    private static final double AREA_RATIO_TOLERANCE = 10.0;

    @Override
    public String getId() {
        return "MR2";
    }

    @Override
    public String getDescription() {
        return "线段长度比例关系 - 当线段长度增加K倍时，生成的区域面积应接近K倍";
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

            // 源线段的起点和终点
            Point2D p1 = line.getP1();
            Point2D p2 = line.getP2();

            // 计算方向向量
            double dx = p2.getX() - p1.getX();
            double dy = p2.getY() - p1.getY();

            // 线段的原始长度
            double originalLength = line.getP1().distance(line.getP2());

            // 生成2个不同长度倍数的后续测试用例
            for (int i = 0; i < 2; i++) {
                // 默认使用2倍长度，也可以随机生成在1.5-3.0之间的倍数
                double lengthFactor = (i == 0) ? DEFAULT_LENGTH_FACTOR : 1.5 + random.nextDouble() * 1.5;

                // 创建新的线段，保持起点和方向，仅改变长度
                Point2D newP2 = new Point2D.Double(
                        p1.getX() + dx * lengthFactor,
                        p1.getY() + dy * lengthFactor);

                Line2D newLine = new Line2D.Double(p1, newP2);

                // 创建新的测试用例
                TestCase followupTest = new TestCase(
                        newLine, // 新的线段
                        width, // 保持相同的宽度
                        sourceTest.getPartitionId() // 保持分区ID不变
                );

                followupTests.add(followupTest);
            }
        } catch (Exception e) {
            // 如果发生异常，记录并返回空列表
            System.err.println("生成MR2后续测试时出错: " + e.getMessage());
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

        // 如果结果都是null，则认为关系满足
        if (sourceResult == null || followupResult == null) {
            return true;
        }

        try {
            // 计算原始线段和后续线段的长度
            Line2D sourceLine = sourceTest.getLine();
            Line2D followupLine = followupTest.getLine();

            double sourceLength = sourceLine.getP1().distance(sourceLine.getP2());
            double followupLength = followupLine.getP1().distance(followupLine.getP2());

            // 计算长度比例
            double expectedFactor = followupLength / sourceLength;

            // 计算面积
            double sourceAreaSize = calculateAreaSize(sourceResult);
            double followupAreaSize = calculateAreaSize(followupResult);

            if (sourceAreaSize < 0 || followupAreaSize < 0) {
                return false; // 无法计算面积，关系不满足
            }

            // 计算实际的比例因子
            double actualFactor = followupAreaSize / sourceAreaSize;

            // 计算比例因子的偏差百分比
            double deviation = Math.abs(actualFactor - expectedFactor) / expectedFactor * 100.0;

            // 如果偏差在允许的范围内，认为关系满足
            return deviation <= AREA_RATIO_TOLERANCE;

        } catch (Exception e) {
            System.err.println("验证MR2时出错: " + e.getMessage());
            return false;
        }
    }

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
            return -1; // 无法计算面积
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

        // 检查线段长度是否大于零
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