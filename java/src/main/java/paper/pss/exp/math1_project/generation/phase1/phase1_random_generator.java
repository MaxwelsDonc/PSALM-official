package paper.pss.exp.math1_project.generation.phase1;

import paper.pss.exp.math1_project.model.TestCase;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * phase1_random_generator.java
 *
 * This class implements a pure random test generation strategy for the
 * convolve function. It generates test cases by randomly creating
 * array pairs and determining their partition classification.
 *
 */
public class phase1_random_generator {
    private final Random random;

    // Constants for array length ranges
    private static final int LENGTH_MIN = 1;
    private static final int LENGTH_MAX = 10;

    // Constants for array value ranges
    private static final double[] ALLOWED_VALUES = {-1.0, 0.0, 1.0};

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
        double[] x = generateRandomArray();
        double[] h = generateRandomArray();
        int partitionId = -1;
        return new TestCase(x, h, partitionId);
    }

    /**
     * 生成随机数组
     */
    private double[] generateRandomArray() {
        // 随机决定数组长度（1到LENGTH_MAX）
        int length = LENGTH_MIN + random.nextInt(LENGTH_MAX - LENGTH_MIN + 1);
        
        double[] array = new double[length];
        for (int i = 0; i < length; i++) {
            // 从{-1, 0, 1}中随机选择一个值
            array[i] = ALLOWED_VALUES[random.nextInt(ALLOWED_VALUES.length)];
        }
        return array;
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
            "分区 1: 两个数组长度相同",
            "分区 2: 第一个数组比第二个长", 
            "分区 3: 第二个数组比第一个长",
            "分区 4: 一个或两个数组为空"
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