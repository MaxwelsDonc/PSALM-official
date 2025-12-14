package paper.pss.exp.math1_project.generation.phase1;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import paper.pss.exp.math1_project.model.TestCase;

/**
 * phase1_art_generator.java
 *
 * This class implements an Adaptive Random Testing (ART) strategy for the
 * convolve function. ART generates test cases that are well-distributed
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

    // Constants for array length ranges
    private static final int LENGTH_MIN = 1;
    private static final int LENGTH_MAX = 10;

    // Constants for array value ranges
    private static final double VALUE_MIN = -10.0;
    private static final double VALUE_MAX = 10.0;

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
     * 使用数组长度和数组元素值的欧几里得距离
     */
    private double distance(TestCase a, TestCase b) {
        double[] xA = a.getX();
        double[] hA = a.getH();
        double[] xB = b.getX();
        double[] hB = b.getH();
        
        // 计算长度距离
        double lengthDist = Math.sqrt(Math.pow(xA.length - xB.length, 2) + Math.pow(hA.length - hB.length, 2));
        
        // 计算数组元素的平均值距离
        double avgXA = calculateAverage(xA);
        double avgHA = calculateAverage(hA);
        double avgXB = calculateAverage(xB);
        double avgHB = calculateAverage(hB);
        
        double avgDist = Math.sqrt(Math.pow(avgXA - avgXB, 2) + Math.pow(avgHA - avgHB, 2));
        
        // 组合距离（长度距离权重更大）
        return lengthDist + avgDist * 0.1;
    }

    /**
     * 计算数组的平均值
     */
    private double calculateAverage(double[] array) {
        if (array.length == 0) {
            return 0.0;
        }
        double sum = 0.0;
        for (double value : array) {
            sum += value;
        }
        return sum / array.length;
    }

    /**
     * 生成一个随机测试用例
     */
    private TestCase generateRandomTestCase() {
        double[] x = generateRandomArray();
        double[] h = generateRandomArray();
        int partitionId = determinePartitionId(x, h);
        return new TestCase(x, h, partitionId);
    }

    /**
     * 生成一个随机数组
     */
    private double[] generateRandomArray() {
        // 随机决定数组长度（包括0长度的可能性）
        int length = random.nextInt(LENGTH_MAX + 1); // 0 到 LENGTH_MAX
        
        double[] array = new double[length];
        for (int i = 0; i < length; i++) {
            array[i] = Math.round((VALUE_MIN + random.nextDouble() * (VALUE_MAX - VALUE_MIN)) * 100) / 100.0;
        }
        return array;
    }

    /**
     * 根据数组特征确定分区ID
     */
    private int determinePartitionId(double[] x, double[] h) {
        int xLength = x.length;
        int hLength = h.length;

        // 检查是否有空数组
        if (xLength == 0 || hLength == 0) {
            return 4; // 一个或两个数组为空
        }

        // 检查长度关系
        if (xLength == hLength) {
            return 1; // 两个数组长度相同
        } else if (xLength > hLength) {
            return 2; // 第一个数组比第二个长
        } else {
            return 3; // 第二个数组比第一个长
        }
    }

    public static void main(String[] args) {
        phase1_art_generator generator = new phase1_art_generator();
        List<TestCase> testCases = generator.generate(5);

        for (int i = 0; i < testCases.size(); i++) {
            System.out.println("ART Test #" + (i + 1) + ": " + testCases.get(i));
        }
    }
}