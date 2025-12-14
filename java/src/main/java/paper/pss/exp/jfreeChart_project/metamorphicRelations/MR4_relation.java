package paper.pss.exp.jfreeChart_project.metamorphicRelations;

import java.awt.Shape;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.awt.geom.PathIterator;

import paper.pss.exp.jfreeChart_project.model.MetamorphicRelation;
import paper.pss.exp.jfreeChart_project.model.MetamorphicGroup;
import paper.pss.exp.jfreeChart_project.model.TestCase;

/**
 * MR4_relation.java - 实现MR4: 正交特性关系
 * 
 * 这个蜕变关系生成与原始线段垂直的新线段，并验证它们生成的区域特性。
 * 垂直线段沿着原始线段的中点，与原始线段成90度角。
 * 两个线段生成的区域应具有特定的几何特性，特别是在它们交叉处。
 */
public class MR4_relation implements MetamorphicRelation {

    // 形状几何特性验证的误差范围 (%)
    private static final double SHAPE_PROPERTY_TOLERANCE = 1.0;

    @Override
    public String getId() {
        return "MR4";
    }

    @Override
    public String getDescription() {
        return "正交特性关系 - 生成与原始线段垂直的线段，验证它们形成的几何特性";
    }

    @Override
    public List<TestCase> generateFollowupTests(TestCase sourceTest) {
        List<TestCase> followupTests = new ArrayList<>();

        // 如果源测试用例不可用，返回空列表
        if (sourceTest == null) {
            return followupTests;
        }

        try {
            Line2D sourceLine = sourceTest.getLine();
            float width = sourceTest.getWidth();

            // 计算源线段的中点
            Point2D midPoint = new Point2D.Double(
                    (sourceLine.getX1() + sourceLine.getX2()) / 2,
                    (sourceLine.getY1() + sourceLine.getY2()) / 2);

            // 计算源线段的长度
            double sourceLength = sourceLine.getP1().distance(sourceLine.getP2());

            // 计算源线段的方向向量
            double dx = sourceLine.getX2() - sourceLine.getX1();
            double dy = sourceLine.getY2() - sourceLine.getY1();

            // 计算垂直向量 (正交向量: [-dy, dx] 或 [dy, -dx])
            double perpDx = -dy;
            double perpDy = dx;

            // 归一化垂直向量
            double perpLength = Math.sqrt(perpDx * perpDx + perpDy * perpDy);
            perpDx /= perpLength;
            perpDy /= perpLength;

            // 创建一个与源线段垂直的线段，长度与源线段相同
            // 计算垂直线段的两个端点
            Point2D perpStart = midPoint;
            Point2D perpEnd = new Point2D.Double(
                    midPoint.getX() + perpDx * sourceLength,
                    midPoint.getY() + perpDy * sourceLength);

            Line2D perpLine = new Line2D.Double(perpStart, perpEnd);

            // 创建新的测试用例
            TestCase followupTest = new TestCase(
                    perpLine, // 垂直线段
                    width, // 保持相同的宽度
                    sourceTest.getPartitionId() // 保持分区ID不变
            );

            followupTests.add(followupTest);

        } catch (Exception e) {
            // 如果发生异常，记录并返回空列表
            System.err.println("生成MR4后续测试时出错: " + e.getMessage());
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

            double sourceAreaSize = calculateAreaSize(sourceResult);
            double followupAreaSize = calculateAreaSize(followupResult);

            if (sourceAreaSize < 0 || followupAreaSize < 0) {
                return false; // 无法计算面积，关系不满足
            }

            // 计算面积比
            double areaRatio = followupAreaSize / Math.max(sourceAreaSize, 0.001);

            // 根据线段宽度计算预期的面积比
            // 如果线段宽度相同，面积比应该接近1.0
            double expectedRatio = 1.0;

            // 计算比例差异百分比
            double ratioDiffPercentage = Math.abs(areaRatio - expectedRatio) / expectedRatio * 100.0;

            // 如果面积比例接近预期值，则关系满足
            return ratioDiffPercentage <= SHAPE_PROPERTY_TOLERANCE;

        } catch (Exception e) {
            System.err.println("验证MR4时出错: " + e.getMessage());
            return false;
        }
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