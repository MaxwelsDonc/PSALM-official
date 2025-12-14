package paper.pss.exp.jfreeChart_project.metamorphicRelations;

import java.awt.Shape;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.List;
import java.awt.geom.Point2D;
import java.awt.geom.PathIterator;

import paper.pss.exp.jfreeChart_project.model.MetamorphicRelation;
import paper.pss.exp.jfreeChart_project.model.MetamorphicGroup;
import paper.pss.exp.jfreeChart_project.model.TestCase;

/**
 * MR3_relation.java - 实现MR3: 线段对称性关系
 * 
 * 这个蜕变关系验证当交换线段的起点和终点坐标时，
 * 生成的区域形状应该保持几何等价。
 * 
 * 由于createLineRegion函数的实现特性，交换线段端点
 * 不应该改变生成区域的几何特性，但可能会有微小的数值差异。
 */
public class MR3_relation implements MetamorphicRelation {

    // 面积比例验证允许的误差范围 (%)
    private static final double AREA_DIFF_TOLERANCE = 0.01;

    @Override
    public String getId() {
        return "MR3";
    }

    @Override
    public String getDescription() {
        return "线段对称性关系 - 交换线段的起点和终点后，生成的区域形状应保持不变";
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

            // 交换线段的起点和终点
            Line2D reversedLine = new Line2D.Double(
                    line.getP2(), // 原终点作为新起点
                    line.getP1() // 原起点作为新终点
            );

            // 创建新的测试用例
            TestCase followupTest = new TestCase(
                    reversedLine, // 反向的线段
                    width, // 保持相同的宽度
                    sourceTest.getPartitionId() // 保持分区ID不变
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

            // 计算源形状的面积作为参考
            double sourceAreaSize = calculateAreaSize(sourceResult);
            double followupAreaSize = calculateAreaSize(followupResult);
            if (sourceAreaSize < 0 || followupAreaSize < 0) {
                return false; // 无法计算面积，关系不满足
            }
            double differenceSizePercent = Math.abs(followupAreaSize - sourceAreaSize) / sourceAreaSize * 100.0;

            // 如果差异很小，认为关系满足
            return differenceSizePercent <= AREA_DIFF_TOLERANCE;

        } catch (Exception e) {
            System.err.println("验证MR3时出错: " + e.getMessage());
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