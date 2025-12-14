package paper.pss.exp.jfreeChart_project.generation.phase1;

import paper.pss.exp.jfreeChart_project.model.TestCase;

import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * phase1_random_generator.java
 *
 * This class implements a pure random test generation strategy for the
 * createLineRegion function. It generates test cases by randomly creating
 * line segments and determining their partition classification.
 *
 * Partition classification for createLineRegion:
 * 1: Horizontal line segment (y1 == y2)
 * 2: Vertical line segment (x1 == x2)
 * 3: Diagonal line with positive slope
 * 4: Diagonal line with negative slope
 */
public class phase1_random_generator {
    private final Random random;

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

    public phase1_random_generator() {
        this.random = new Random();
    }

    public phase1_random_generator(long seed) {
        this.random = new Random(seed);
    }

    /**
     * 生成指定数量的随机测试用例
     */
    public List<TestCase> generate(int count) {
        List<TestCase> testCases = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            testCases.add(generateRandomTestCase());
        }
        return testCases;
    }

    /**
     * 生成单个随机测试用例
     */
    private TestCase generateRandomTestCase() {
        Line2D line = generateRandomLine();
        float width = generateRandomWidth();
        // int partitionId = determinePartitionId(line);
        return new TestCase(line, width, -1);
    }

    /**
     * 生成随机线段
     */
    private Line2D generateRandomLine() {
        // 生成随机起始点（保留两位小数）
        double x1 = Math.round((COORD_MIN + random.nextDouble() * (COORD_MAX - COORD_MIN)) * 100) / 100.0;
        double y1 = Math.round((COORD_MIN + random.nextDouble() * (COORD_MAX - COORD_MIN)) * 100) / 100.0;

        // 生成随机长度
        double length = LENGTH_MIN + random.nextDouble() * (LENGTH_MAX - LENGTH_MIN);

        // 生成随机角度（0到2π）
        double angle = random.nextDouble() * 2 * Math.PI;

        // 计算终点并保留两位小数
        double x2 = Math.round((x1 + length * Math.cos(angle)) * 100) / 100.0;
        double y2 = Math.round((y1 + length * Math.sin(angle)) * 100) / 100.0;

        // 确保终点在边界内
        x2 = Math.max(COORD_MIN, Math.min(x2, COORD_MAX));
        y2 = Math.max(COORD_MIN, Math.min(y2, COORD_MAX));

        // 如果调整后的线段太短，重新生成
        double actualLength = Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
        if (actualLength < LENGTH_MIN) {
            // 简单策略：在当前点附近生成一个最小长度的线段
            double newAngle = random.nextDouble() * 2 * Math.PI;
            x2 = Math.round((x1 + LENGTH_MIN * Math.cos(newAngle)) * 100) / 100.0;
            y2 = Math.round((y1 + LENGTH_MIN * Math.sin(newAngle)) * 100) / 100.0;
            
            // 再次确保在边界内
            x2 = Math.max(COORD_MIN, Math.min(x2, COORD_MAX));
            y2 = Math.max(COORD_MIN, Math.min(y2, COORD_MAX));
        }

        return new Line2D.Double(x1, y1, x2, y2);
    }

    /**
     * 生成随机宽度
     */
    private float generateRandomWidth() {
        float width = WIDTH_MIN + random.nextFloat() * (WIDTH_MAX - WIDTH_MIN);
        return Math.round(width * 100) / 100.0f; // 保留两位小数
    }

    /**
     * 根据线段特征确定分区ID
     */
    private int determinePartitionId(Line2D line) {
        double x1 = line.getX1();
        double y1 = line.getY1();
        double x2 = line.getX2();
        double y2 = line.getY2();

        // 检查是否为水平线段
        if (Math.abs(y1 - y2) < EPSILON) {
            return 1; // 水平线段
        }

        // 检查是否为垂直线段
        if (Math.abs(x1 - x2) < EPSILON) {
            return 2; // 垂直线段
        }

        // 计算斜率
        double slope = (y2 - y1) / (x2 - x1);

        // 根据斜率判断对角线类型
        if (slope > 0) {
            return 3; // 正斜率对角线
        } else {
            return 4; // 负斜率对角线
        }
    }

    /**
     * 统计每个分区的测试用例数量和占比
     */
    public String getStatistics(List<TestCase> testCases) {
        int[] partitionCount = new int[5]; // 索引0不使用，1-4对应分区1-4
        int total = testCases.size();
        
        for (TestCase tc : testCases) {
            int pid = tc.getPartitionId();
            if (pid >= 1 && pid <= 4) {
                partitionCount[pid]++;
            }
        }
        
        StringBuilder report = new StringBuilder();
        report.append("测试用例总数: ").append(total).append("\n");
        
        String[] descriptions = {
            "", // 索引0不使用
            "分区 1: 水平线段",
            "分区 2: 垂直线段", 
            "分区 3: 正斜率对角线",
            "分区 4: 负斜率对角线"
        };
        
        for (int i = 1; i <= 4; i++) {
            int count = partitionCount[i];
            double percent = total > 0 ? 100.0 * count / total : 0;
            report.append(String.format(" %s : %d (%.2f%%)\n", descriptions[i], count, percent));
        }
        
        return report.toString();
    }

    public static void main(String[] args) {
        phase1_random_generator generator = new phase1_random_generator();
        List<TestCase> testCases = generator.generate(1000);
        
        System.out.println(generator.getStatistics(testCases));
        System.out.println("\n示例测试用例:");
        for (int i = 0; i < Math.min(5, testCases.size()); i++) {
            System.out.println("测试用例 #" + (i + 1) + ": " + testCases.get(i));
        }
    }
}