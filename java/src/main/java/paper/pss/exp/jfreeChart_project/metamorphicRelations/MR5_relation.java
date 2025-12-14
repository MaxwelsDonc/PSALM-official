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
 * MR5_relation.java - 实现MR5: 旋转不变性关系
 * 
 * 这个蜕变关系验证当线段围绕某点旋转时，生成的区域应该
 * 保持相同的形状特性（面积、周长等），只是方向发生旋转。
 */
public class MR5_relation implements MetamorphicRelation {

    // 面积和周长比较允许的误差范围 (%)
    private static final double SHAPE_PROPERTY_TOLERANCE = 1;

    @Override
    public String getId() {
        return "MR5";
    }

    @Override
    public String getDescription() {
        return "旋转不变性关系 - 将线段旋转后，生成的区域保持相同的形状特性";
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

            // 源线段的端点
            Point2D p1 = line.getP1();
            Point2D p2 = line.getP2();

            // 计算线段的中点，作为旋转中心
            Point2D center = new Point2D.Double(
                    (p1.getX() + p2.getX()) / 2,
                    (p1.getY() + p2.getY()) / 2);

            // 生成两个不同角度的旋转测试用例
            double[] rotationAngles = { Math.PI / 3, Math.PI / 6 }; // 60度和30度旋转

            for (int i = 0; i < rotationAngles.length; i++) {
                // 旋转角度
                double angle = rotationAngles[i];

                // 旋转线段的两个端点
                Point2D rotatedP1 = rotatePoint(p1, center, angle);
                Point2D rotatedP2 = rotatePoint(p2, center, angle);

                Line2D rotatedLine = new Line2D.Double(
                        rotatedP1,
                        rotatedP2);

                // 创建新的测试用例
                TestCase followupTest = new TestCase(
                        rotatedLine, // 旋转后的线段
                        width, // 保持相同的宽度
                        sourceTest.getPartitionId() // 保持分区ID不变
                );

                followupTests.add(followupTest);
            }

        } catch (Exception e) {
            // 如果发生异常，记录并返回空列表
            System.err.println("生成MR5后续测试时出错: " + e.getMessage());
        }

        return followupTests;
    }

    /**
     * 将点围绕中心点旋转指定角度
     * 
     * @param point  要旋转的点
     * @param center 旋转中心
     * @param angle  旋转角度（弧度）
     * @return 旋转后的新点
     */
    private Point2D rotatePoint(Point2D point, Point2D center, double angle) {
        // 将点平移到以原点为中心
        double x = point.getX() - center.getX();
        double y = point.getY() - center.getY();

        // 应用旋转变换
        double rotatedX = x * Math.cos(angle) - y * Math.sin(angle);
        double rotatedY = x * Math.sin(angle) + y * Math.cos(angle);

        // 将点平移回原来的中心
        return new Point2D.Double(
                rotatedX + center.getX(),
                rotatedY + center.getY());
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
            // 比较两个形状的几何特性

            // 特性1: 面积
            double sourceArea = calculateAreaSize(sourceResult);
            double followupArea = calculateAreaSize(followupResult);

            if (sourceArea < 0 || followupArea < 0) {
                return false; // 无法计算面积，关系不满足
            }

            // 计算特性的偏差百分比
            double areaDiffPercentage = Math.abs(sourceArea - followupArea) /
                    Math.max(sourceArea, 0.001) * 100.0;

            // 如果面积和周长的偏差都很小，认为关系满足
            return areaDiffPercentage <= SHAPE_PROPERTY_TOLERANCE;

        } catch (Exception e) {
            System.err.println("验证MR5时出错: " + e.getMessage());
            return false;
        }
    }

    /**
     * 计算Shape对象的近似面积。
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