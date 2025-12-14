package paper.pss.exp.jfreeChart_project.generation.phase1;

import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import paper.pss.exp.jfreeChart_project.model.TestCase;

/**
 * phase1_art_generator.java
 *
 * This class implements an Adaptive Random Testing (ART) strategy for the
 * createLineRegion function. ART generates test cases that are well-distributed
 * across the input domain by maintaining distance between generated test cases.
 *
 * The algorithm:
 * 1. Generate the first test case randomly
 * 2. For each subsequent test case, generate multiple candidates and select
 *    the one that maximizes the minimum distance to existing test cases
 */
public class phase1_art_generator {

    private static final Random random = new Random(System.currentTimeMillis());
    private final int candidateNum;

    // Constants for coordinate ranges
    private static final double COORD_MIN = -10.0;
    private static final double COORD_MAX = 10.0;

    // Constants for width range
    private static final float WIDTH_MIN = 1.0f;
    private static final float WIDTH_MAX = 2.0f;

    // Constants for line length range
    private static final double LENGTH_MIN = 1.0;
    private static final double LENGTH_MAX = 3.0;

    // Tolerance for equality comparisons
    private static final double EPSILON = 0.0001;

    public phase1_art_generator() {
        this.candidateNum = 10;
    }

    public List<TestCase> generate(int count) {
        List<TestCase> testCases = new ArrayList<>();

        // Step 1: generate the first random test case
        TestCase firstTestCase = generateRandomTestCase();
        testCases.add(firstTestCase);

        // Step 2: generate remaining test cases using ART strategy
        while (testCases.size() < count) {
            TestCase bestCandidate = null;
            double bestMinDist = -1;

            for (int i = 0; i < candidateNum; i++) {
                TestCase candidate = generateRandomTestCase();

                double minDist = Double.MAX_VALUE;
                for (TestCase existing : testCases) {
                    double dist = distance(candidate, existing);
                    if (dist < minDist) {
                        minDist = dist;
                    }
                }

                if (minDist > bestMinDist) {
                    bestMinDist = minDist;
                    bestCandidate = candidate;
                }
            }

            testCases.add(bestCandidate);
        }

        return testCases;
    }

    /**
     * 计算两个测试用例之间的距离
     * 使用线段中点坐标和宽度的欧几里得距离
     */
    private double distance(TestCase a, TestCase b) {
        Line2D lineA = a.getLine();
        Line2D lineB = b.getLine();
        
        // 计算线段中点
        double midXA = (lineA.getX1() + lineA.getX2()) / 2.0;
        double midYA = (lineA.getY1() + lineA.getY2()) / 2.0;
        double midXB = (lineB.getX1() + lineB.getX2()) / 2.0;
        double midYB = (lineB.getY1() + lineB.getY2()) / 2.0;
        
        // 计算坐标距离
        double coordDist = Math.sqrt(Math.pow(midXA - midXB, 2) + Math.pow(midYA - midYB, 2));
        
        // 计算宽度距离
        double widthDist = Math.abs(a.getWidth() - b.getWidth());
        
        // 组合距离（坐标距离权重更大）
        return coordDist + widthDist * 0.1;
    }

    /**
     * 生成一个随机测试用例
     */
    private TestCase generateRandomTestCase() {
        Line2D line = generateRandomLine();
        float width = WIDTH_MIN + random.nextFloat() * (WIDTH_MAX - WIDTH_MIN);
        width = Math.round(width * 100) / 100.0f; // 保留两位小数
        int partitionId = determinePartitionId(line);
        return new TestCase(line, width, partitionId);
    }

    /**
     * 生成一个随机线段
     */
    private Line2D generateRandomLine() {
        // 生成随机坐标并保留两位小数
        double x1 = Math.round((COORD_MIN + random.nextDouble() * (COORD_MAX - COORD_MIN)) * 100) / 100.0;
        double y1 = Math.round((COORD_MIN + random.nextDouble() * (COORD_MAX - COORD_MIN)) * 100) / 100.0;
        double x2 = Math.round((COORD_MIN + random.nextDouble() * (COORD_MAX - COORD_MIN)) * 100) / 100.0;
        double y2 = Math.round((COORD_MIN + random.nextDouble() * (COORD_MAX - COORD_MIN)) * 100) / 100.0;

        // 计算当前长度
        double length = Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));

        // 检查点是否太近或相同
        if (length < LENGTH_MIN) {
            // 方向向量
            double dx = x2 - x1;
            double dy = y2 - y1;

            // 如果点相同，创建随机方向
            if (Math.abs(dx) < EPSILON && Math.abs(dy) < EPSILON) {
                double angle = random.nextDouble() * 2 * Math.PI;
                dx = Math.cos(angle);
                dy = Math.sin(angle);
            } else {
                // 标准化方向向量
                double magnitude = Math.sqrt(dx * dx + dy * dy);
                dx = dx / magnitude;
                dy = dy / magnitude;
            }

            // 缩放到最小长度
            x2 = Math.round((x1 + dx * LENGTH_MIN) * 100) / 100.0;
            y2 = Math.round((y1 + dy * LENGTH_MIN) * 100) / 100.0;
        } else if (length > LENGTH_MAX) {
            // 缩放到最大长度
            double scale = LENGTH_MAX / length;
            x2 = Math.round((x1 + (x2 - x1) * scale) * 100) / 100.0;
            y2 = Math.round((y1 + (y2 - y1) * scale) * 100) / 100.0;
        }

        return new Line2D.Double(x1, y1, x2, y2);
    }

    /**
     * 根据线段特征确定分区ID
     */
    private int determinePartitionId(Line2D line) {
        double x1 = line.getX1();
        double y1 = line.getY1();
        double x2 = line.getX2();
        double y2 = line.getY2();

        // 检查水平线 (y1 == y2)
        if (Math.abs(y1 - y2) < EPSILON) {
            return 1;
        }

        // 检查垂直线 (x1 == x2)
        if (Math.abs(x1 - x2) < EPSILON) {
            return 2;
        }

        // 检查正斜率对角线
        if ((y2 > y1 && x2 > x1) || (y2 < y1 && x2 < x1)) {
            return 3;
        }

        // 必须是负斜率对角线
        return 4;
    }

    public static void main(String[] args) {
        phase1_art_generator generator = new phase1_art_generator();
        List<TestCase> testCases = generator.generate(5);

        for (int i = 0; i < testCases.size(); i++) {
            System.out.println("ART Test #" + (i + 1) + ": " + testCases.get(i));
        }
    }
}