package paper.pss.exp.math1_project.generation.phase1;

import paper.pss.exp.math1_project.model.TestCase;
import paper.pss.exp.math1_project.utils.Math1ConfigExtractor_utils;
import paper.pss.exp.math1_project.utils.Math1ConfigExtractor_utils.Partition;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * phase1_partition_generator.java
 *
 * This class implements a partition-based test generation strategy for the
 * convolve function. It generates test cases by allocating them to different
 * input domain partitions using a min-max allocation algorithm.
 *
 * Partitions for convolve function:
 * 1: Both arrays have same length
 * 2: First array longer than second
 * 3: Second array longer than first
 * 4: One or both arrays are empty
 */
public class phase1_partition_generator {
    private final Math1ConfigExtractor_utils configExtractor;
    private final List<Partition> partitions;
    private final List<Double> partitionWeights;
    private final Map<Integer, Integer> selectedCounts;
    private final Random random;

    // Constants for array length ranges
    private static final int LENGTH_MIN = 1;
    private static final int LENGTH_MAX = 10;

    // Constants for array value ranges
    private static final double[] ALLOWED_VALUES = {-1.0, 0.0, 1.0};

    // Tolerance for equality comparisons
    private static final double EPSILON = 0.0001;

    public phase1_partition_generator() throws IOException {
        this("src/main/java/paper/pss/exp/math1_project/math1_config.json");
    }

    public phase1_partition_generator(String configPath) throws IOException {
        this.configExtractor = new Math1ConfigExtractor_utils(configPath);
        this.partitions = configExtractor.getPartitions();
        this.partitionWeights = partitions.stream()
                .map(Partition::getWeight)
                .collect(Collectors.toList());
        this.selectedCounts = new HashMap<>();
        this.random = new Random();
        
        // 初始化选择计数
        for (Partition partition : partitions) {
            selectedCounts.put(partition.getId(), 0);
        }
        
        validateConfig();
    }

    /**
     * 验证配置是否有效
     */
    private void validateConfig() {
        if (partitions == null || partitions.isEmpty()) {
            throw new IllegalArgumentException("分区配置无效");
        }
        if (partitionWeights == null || partitionWeights.size() != partitions.size()) {
            throw new IllegalArgumentException("分区权重配置无效或与分区数量不匹配");
        }
    }

    /**
     * 直接返回分区结构体列表
     */
    public List<Partition> getPartitions() {
        return new ArrayList<>(partitions);
    }

    /**
     * 使用最大最小算法生成指定数量的测试用例
     */
    public List<TestCase> generate(int count) {
        return allocateTestCases(count);
    }

    /**
     * 使用最大最小算法分配测试用例到各分区
     */
    private List<TestCase> allocateTestCases(int count) {
        // 重置选择计数
        for (Integer partitionId : selectedCounts.keySet()) {
            selectedCounts.put(partitionId, 0);
        }

        Map<Integer, Double> samplingRates = new HashMap<>();
        for (Partition partition : partitions) {
            samplingRates.put(partition.getId(), 0.0);
        }

        List<TestCase> testCases = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            int selectedPartition = findLowestSamplingRatePartition(samplingRates);
            TestCase testCase = generateTestCaseInPartition(selectedPartition);
            testCases.add(testCase);
            
            selectedCounts.put(selectedPartition, selectedCounts.get(selectedPartition) + 1);
            
            // 更新采样率
            double partitionWeight = partitionWeights.get(selectedPartition - 1);
            samplingRates.put(selectedPartition, 
                (double) selectedCounts.get(selectedPartition) / partitionWeight);
        }
        return testCases;
    }

    /**
     * 找到采样率最低的分区
     */
    private int findLowestSamplingRatePartition(Map<Integer, Double> samplingRates) {
        int selectedPartition = 1;
        double lowestRate = Double.POSITIVE_INFINITY;
        
        for (Map.Entry<Integer, Double> entry : samplingRates.entrySet()) {
            int partitionId = entry.getKey();
            double rate = entry.getValue();
            
            if (rate < lowestRate) {
                lowestRate = rate;
                selectedPartition = partitionId;
            } else if (rate == lowestRate && partitionId <= partitionWeights.size()) {
                double currentWeight = partitionWeights.get(partitionId - 1);
                double selectedWeight = partitionWeights.get(selectedPartition - 1);
                if (currentWeight > selectedWeight) {
                    selectedPartition = partitionId;
                }
            }
        }
        return selectedPartition;
    }

    /**
     * 在指定分区中生成测试用例
     */
    private TestCase generateTestCaseInPartition(int partitionId) {
        if (partitionId < 1 || partitionId > 12) {
            throw new IllegalArgumentException("无效的分区ID: " + partitionId);
        }
        
        double[] x, h;
        switch (partitionId) {
            case 1: // P1_P4: len(x) > len(h), 两数组均全正
                double[][] arrays1 = generateArraysWithLengthRelation("x_longer");
                x = makeAllPositive(arrays1[0]);
                h = makeAllPositive(arrays1[1]);
                break;
            case 2: // P1_P5: len(x) > len(h), 两数组均不全正
                double[][] arrays2 = generateArraysWithLengthRelation("x_longer");
                x = makeNotAllPositive(arrays2[0]);
                h = makeNotAllPositive(arrays2[1]);
                break;
            case 3: // P1_P6: len(x) > len(h), x全正, h不全正
                double[][] arrays3 = generateArraysWithLengthRelation("x_longer");
                x = makeAllPositive(arrays3[0]);
                h = makeNotAllPositive(arrays3[1]);
                break;
            case 4: // P1_P7: len(x) > len(h), x不全正, h全正
                double[][] arrays4 = generateArraysWithLengthRelation("x_longer");
                x = makeNotAllPositive(arrays4[0]);
                h = makeAllPositive(arrays4[1]);
                break;
            case 5: // P2_P4: len(x) < len(h), 两数组均全正
                double[][] arrays5 = generateArraysWithLengthRelation("h_longer");
                x = makeAllPositive(arrays5[0]);
                h = makeAllPositive(arrays5[1]);
                break;
            case 6: // P2_P5: len(x) < len(h), 两数组均不全正
                double[][] arrays6 = generateArraysWithLengthRelation("h_longer");
                x = makeNotAllPositive(arrays6[0]);
                h = makeNotAllPositive(arrays6[1]);
                break;
            case 7: // P2_P6: len(x) < len(h), x全正, h不全正
                double[][] arrays7 = generateArraysWithLengthRelation("h_longer");
                x = makeAllPositive(arrays7[0]);
                h = makeNotAllPositive(arrays7[1]);
                break;
            case 8: // P2_P7: len(x) < len(h), x不全正, h全正
                double[][] arrays8 = generateArraysWithLengthRelation("h_longer");
                x = makeNotAllPositive(arrays8[0]);
                h = makeAllPositive(arrays8[1]);
                break;
            case 9: // P3_P4: len(x) = len(h), 两数组均全正
                double[][] arrays9 = generateArraysWithLengthRelation("equal");
                x = makeAllPositive(arrays9[0]);
                h = makeAllPositive(arrays9[1]);
                break;
            case 10: // P3_P5: len(x) = len(h), 两数组均不全正
                double[][] arrays10 = generateArraysWithLengthRelation("equal");
                x = makeNotAllPositive(arrays10[0]);
                h = makeNotAllPositive(arrays10[1]);
                break;
            case 11: // P3_P6: len(x) = len(h), x全正, h不全正
                double[][] arrays11 = generateArraysWithLengthRelation("equal");
                x = makeAllPositive(arrays11[0]);
                h = makeNotAllPositive(arrays11[1]);
                break;
            case 12: // P3_P7: len(x) = len(h), x不全正, h全正
                double[][] arrays12 = generateArraysWithLengthRelation("equal");
                x = makeNotAllPositive(arrays12[0]);
                h = makeAllPositive(arrays12[1]);
                break;
            default:
                throw new IllegalArgumentException("无效的分区ID: " + partitionId);
        }
        
        return new TestCase(x, h, partitionId);
    }

    /**
     * 根据长度关系生成数组对
     */
    private double[][] generateArraysWithLengthRelation(String relation) {
        double[] x, h;
        
        switch (relation) {
            case "equal": // 长度相等
                int length = LENGTH_MIN + random.nextInt(LENGTH_MAX - LENGTH_MIN + 1);
                x = generateRandomArray(length);
                h = generateRandomArray(length);
                break;
            case "x_longer": // x比h长
                int hLength = LENGTH_MIN + random.nextInt(LENGTH_MAX - LENGTH_MIN);
                int xLength = hLength + 1 + random.nextInt(LENGTH_MAX - hLength);
                x = generateRandomArray(xLength);
                h = generateRandomArray(hLength);
                break;
            case "h_longer": // h比x长
                int xLen = LENGTH_MIN + random.nextInt(LENGTH_MAX - LENGTH_MIN);
                int hLen = xLen + 1 + random.nextInt(LENGTH_MAX - xLen);
                x = generateRandomArray(xLen);
                h = generateRandomArray(hLen);
                break;
            default:
                throw new IllegalArgumentException("无效的长度关系: " + relation);
        }
        
        return new double[][]{x, h};
    }
    
    /**
     * 确保数组全为正数（使用1）
     */
    private double[] makeAllPositive(double[] array) {
        double[] result = new double[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = 1.0; // 全正
        }
        return result;
    }
    
    /**
     * 确保数组不全为正数（至少包含一个非正数）
     */
    private double[] makeNotAllPositive(double[] array) {
        if (array.length == 0) {
            return array;
        }
        
        double[] result = new double[array.length];
        // 先填充随机值
        for (int i = 0; i < array.length; i++) {
            result[i] = ALLOWED_VALUES[random.nextInt(ALLOWED_VALUES.length)];
        }
        
        // 确保至少有一个非正数
        boolean hasNonPositive = false;
        for (double value : result) {
            if (value <= 0) {
                hasNonPositive = true;
                break;
            }
        }
        
        if (!hasNonPositive) {
            // 随机选择一个位置设为非正数
            int pos = random.nextInt(result.length);
            result[pos] = random.nextBoolean() ? -1.0 : 0.0;
        }
        
        return result;
    }

    /**
     * 生成指定长度的随机数组
     */
    private double[] generateRandomArray(int length) {
        double[] array = new double[length];
        for (int i = 0; i < length; i++) {
            // 从{-1, 0, 1}中随机选择一个值
            array[i] = ALLOWED_VALUES[random.nextInt(ALLOWED_VALUES.length)];
        }
        return array;
    }

    /**
     * 生成指定分区的测试用例
     */
    public TestCase generateTestCase(int partitionId) {
        return generateTestCaseInPartition(partitionId);
    }

    /**
     * 统计每个分区的测试用例数量和占比
     */
    public String getStatistics(List<TestCase> testCases) {
        Map<Integer, Integer> partitionCount = new HashMap<>();
        for (Partition partition : partitions) {
            partitionCount.put(partition.getId(), 0);
        }
        
        int total = testCases.size();
        for (TestCase tc : testCases) {
            int pid = tc.getPartitionId();
            if (partitionCount.containsKey(pid)) {
                partitionCount.put(pid, partitionCount.get(pid) + 1);
            }
        }
        
        StringBuilder report = new StringBuilder();
        report.append("测试用例总数: ").append(total).append("\n");
        
        for (Integer pid : partitionCount.keySet().stream().sorted().collect(Collectors.toList())) {
            String desc = getPartitionDescription(pid);
            int count = partitionCount.get(pid);
            double percent = total > 0 ? 100.0 * count / total : 0;
            report.append(String.format(" %s : %d (%.2f%%)\n", desc, count, percent));
        }
        
        return report.toString();
    }

    /**
     * 获取分区描述
     */
    private String getPartitionDescription(int partitionId) {
        switch (partitionId) {
            case 1: return "分区 P1_P4: len(x) > len(h), 两数组均全正";
            case 2: return "分区 P1_P5: len(x) > len(h), 两数组均不全正";
            case 3: return "分区 P1_P6: len(x) > len(h), x全正, h不全正";
            case 4: return "分区 P1_P7: len(x) > len(h), x不全正, h全正";
            case 5: return "分区 P2_P4: len(x) < len(h), 两数组均全正";
            case 6: return "分区 P2_P5: len(x) < len(h), 两数组均不全正";
            case 7: return "分区 P2_P6: len(x) < len(h), x全正, h不全正";
            case 8: return "分区 P2_P7: len(x) < len(h), x不全正, h全正";
            case 9: return "分区 P3_P4: len(x) = len(h), 两数组均全正";
            case 10: return "分区 P3_P5: len(x) = len(h), 两数组均不全正";
            case 11: return "分区 P3_P6: len(x) = len(h), x全正, h不全正";
            case 12: return "分区 P3_P7: len(x) = len(h), x不全正, h全正";
            default: return "分区 " + partitionId;
        }
    }

    public static void main(String[] args) {
        try {
            phase1_partition_generator generator = new phase1_partition_generator();
            List<TestCase> testCases = generator.generate(1000);
            System.out.println(generator.getStatistics(testCases));
            System.out.println("\n示例测试用例:");
            for (int i = 0; i < Math.min(5, testCases.size()); i++) {
                System.out.println("测试用例 #" + (i + 1) + ": " + testCases.get(i));
            }
        } catch (IOException e) {
            System.err.println("配置文件读取失败: " + e.getMessage());
        }
    }
}