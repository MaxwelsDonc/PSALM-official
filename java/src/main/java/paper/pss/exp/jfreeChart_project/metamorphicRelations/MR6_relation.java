package paper.pss.exp.jfreeChart_project.metamorphicRelations;

import java.awt.Shape;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import paper.pss.exp.jfreeChart_project.model.MetamorphicRelation;
import paper.pss.exp.jfreeChart_project.model.MetamorphicGroup;
import paper.pss.exp.jfreeChart_project.model.TestCase;

/**
 * MR6_relation.java - 实现MR6: 平移不变性关系
 * 
 * 这个蜕变关系验证当线段在平面上平移时，生成的区域应该
 * 保持相同的形状特性（面积、周长等），只是位置发生相应的平移。
 */
public class MR6_relation implements MetamorphicRelation {

    private static final Random random = new Random();

    // 面积和周长比较允许的误差范围 (%)
    private static final double SHAPE_PROPERTY_TOLERANCE = 1;

    @Override
    public String getId() {
        return "MR6";
    }

    @Override
    public String getDescription() {
        return "平移不变性关系 - 将线段平移后，生成的区域保持相同的形状特性";
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

            // 生成两个不同方向的平移测试用例
            for (int i = 0; i < 2; i++) {
                // 生成随机平移向量
                double translateX, translateY;

                if (i == 0) {
                    // 第一个测试用例：沿X轴平移
                    translateX = 10 + random.nextDouble() * 20; // 10到30之间的随机值
                    translateY = 0;
                } else {
                    // 第二个测试用例：随机方向平移
                    translateX = -15 + random.nextDouble() * 30; // -15到15之间的随机值
                    translateY = -15 + random.nextDouble() * 30; // -15到15之间的随机值
                }

                // 平移线段的两个端点
                Line2D translatedLine = new Line2D.Double(
                        line.getX1() + translateX, line.getY1() + translateY,
                        line.getX2() + translateX, line.getY2() + translateY);

                // 创建新的测试用例
                TestCase followupTest = new TestCase(
                        translatedLine, // 平移后的线段
                        width, // 保持相同的宽度
                        sourceTest.getPartitionId() // 保持分区ID不变
                );

                followupTests.add(followupTest);
            }

        } catch (Exception e) {
            // 如果发生异常，记录并返回空列表
            System.err.println("生成MR6后续测试时出错: " + e.getMessage());
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
            // 比较两个形状的几何特性

            // 特性1: 面积
            double sourceArea = calculateAreaSize(sourceResult);
            double followupArea = calculateAreaSize(followupResult);
            if (sourceArea < 0 || followupArea < 0) {
                return false; // 无法计算面积，关系不满足
            }

            // 特性2: 边界矩形的宽高比
            Rectangle2D sourceBounds = sourceResult.getBounds2D();
            Rectangle2D followupBounds = followupResult.getBounds2D();

            double sourceAspectRatio = sourceBounds.getWidth() / sourceBounds.getHeight();
            double followupAspectRatio = followupBounds.getWidth() / followupBounds.getHeight();

            // 如果宽度或高度接近零，则将宽高比设置为特殊值以避免除零错误
            if (sourceBounds.getHeight() < 0.001 || followupBounds.getHeight() < 0.001) {
                sourceAspectRatio = followupAspectRatio = 1000; // 特殊值表示非常细长的矩形
            }

            // 计算面积和宽高比的偏差
            double areaDiffPercentage = Math.abs(sourceArea - followupArea) / Math.max(sourceArea, 0.001) * 100.0;
            double aspectRatioDiffPercentage = Math.abs(sourceAspectRatio - followupAspectRatio) /
                    Math.max(sourceAspectRatio, 0.001) * 100.0;

            // 如果面积和宽高比的偏差都很小，认为关系满足
            return areaDiffPercentage <= SHAPE_PROPERTY_TOLERANCE &&
                    aspectRatioDiffPercentage <= SHAPE_PROPERTY_TOLERANCE;

        } catch (Exception e) {
            System.err.println("验证MR6时出错: " + e.getMessage());
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