package paper.pss.exp.math2_project.generation.phase1;

import paper.pss.exp.math2_project.model.TestCase;
import paper.pss.exp.math2_project.utils.math2ConfigExtractor_utils;
import paper.pss.exp.math2_project.utils.math2ConfigExtractor_utils.Partition;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * phase1_partition_generator.java
 *
 * 此类实现了对 copySign 函数的分区测试生成策略。
 * 使用最大最小算法确保测试用例在不同分区间的合理分布。
 * 
 * 分区定义（基于 magnitude 和 sign 的正负性质）：
 * 1. magnitude > 0, sign > 0
 * 2. magnitude > 0, sign <= 0
 * 3. magnitude <= 0, sign > 0
 * 4. magnitude <= 0, sign <= 0
 */
public class phase1_partition_generator {
    private final math2ConfigExtractor_utils configExtractor;
    private final List<Partition> partitions;
    private final List<Double> partitionWeights;
    private final Map<Integer, Integer> selectedCounts;
    private final Random random;

    /**
     * 使用默认配置文件路径创建生成器
     */
    public phase1_partition_generator() throws IOException {
        this("src/main/java/paper/pss/exp/math2_project/math2_config.json");
    }

    /**
     * 使用指定配置文件路径创建生成器
     *
     * @param configPath 配置文件路径
     * @throws IOException 如果配置文件读取失败
     */
    public phase1_partition_generator(String configPath) throws IOException {
        this.configExtractor = new math2ConfigExtractor_utils(configPath);
        this.partitions = configExtractor.getPartitions();
        this.partitionWeights = partitions.stream()
                .map(Partition::getWeight)
                .collect(Collectors.toList());
        this.selectedCounts = new HashMap<>();
        this.random = new Random();
        
        // 初始化分区选择计数
        for (Partition partition : partitions) {
            selectedCounts.put(partition.getId(), 0);
        }
        
        validateConfig();
    }

    /**
     * 验证配置的有效性
     */
    private void validateConfig() {
        if (partitions.isEmpty()) {
            throw new IllegalStateException("配置文件中未找到分区定义");
        }
        if (partitions.size() != 4) {
            throw new IllegalStateException("copySign 函数应该有 4 个分区，但配置文件中定义了 " + partitions.size() + " 个分区");
        }
    }

    /**
     * 获取分区列表
     *
     * @return 分区列表
     */
    public List<Partition> getPartitions() {
        return new ArrayList<>(partitions);
    }

    /**
     * 生成指定数量的测试用例
     *
     * @param count 要生成的测试用例数量
     * @return 生成的测试用例列表
     */
    public List<TestCase> generate(int count) {
        return allocateTestCases(count);
    }

    /**
     * 使用最大最小算法分配测试用例到各分区
     *
     * @param count 总测试用例数量
     * @return 分配的测试用例列表
     */
    private List<TestCase> allocateTestCases(int count) {
        List<TestCase> testCases = new ArrayList<>();
        
        // 重置选择计数
        for (Integer partitionId : selectedCounts.keySet()) {
            selectedCounts.put(partitionId, 0);
        }

        // 使用最大最小算法分配测试用例
        for (int i = 0; i < count; i++) {
            // 计算每个分区的当前采样率
            Map<Integer, Double> samplingRates = new HashMap<>();
            for (Partition partition : partitions) {
                int partitionId = partition.getId();
                double weight = partition.getWeight();
                int selected = selectedCounts.get(partitionId);
                double samplingRate = weight > 0 ? (double) selected / weight : Double.MAX_VALUE;
                samplingRates.put(partitionId, samplingRate);
            }

            // 选择采样率最低的分区
            int selectedPartitionId = findLowestSamplingRatePartition(samplingRates);
            
            // 在选定分区中生成测试用例
            TestCase testCase = generateTestCaseInPartition(selectedPartitionId);
            testCases.add(testCase);
            
            // 更新选择计数
            selectedCounts.put(selectedPartitionId, selectedCounts.get(selectedPartitionId) + 1);
        }
        
        return testCases;
    }

    /**
     * 找到采样率最低的分区
     *
     * @param samplingRates 各分区的采样率
     * @return 采样率最低的分区ID
     */
    private int findLowestSamplingRatePartition(Map<Integer, Double> samplingRates) {
        double minSamplingRate = Double.MAX_VALUE;
        List<Integer> candidatePartitions = new ArrayList<>();
        
        for (Map.Entry<Integer, Double> entry : samplingRates.entrySet()) {
            double rate = entry.getValue();
            if (rate < minSamplingRate) {
                minSamplingRate = rate;
                candidatePartitions.clear();
                candidatePartitions.add(entry.getKey());
            } else if (rate == minSamplingRate) {
                candidatePartitions.add(entry.getKey());
            }
        }
        
        // 如果有多个分区具有相同的最低采样率，随机选择一个
        return candidatePartitions.get(random.nextInt(candidatePartitions.size()));
    }

    /**
     * 在指定分区中生成测试用例
     *
     * @param partitionId 分区ID
     * @return 生成的测试用例
     */
    private TestCase generateTestCaseInPartition(int partitionId) {
        long magnitude = generateMagnitudeForPartition(partitionId);
        long sign = generateSignForPartition(partitionId);
        return new TestCase(magnitude, sign, partitionId);
    }

    /**
     * 根据分区ID生成magnitude值
     *
     * @param partitionId 分区ID (1-4)
     * @return 生成的magnitude值
     */
    private long generateMagnitudeForPartition(int partitionId) {
        switch (partitionId) {
            case 1: // magnitude > 0, sign > 0
            case 2: // magnitude > 0, sign <= 0
                return generatePositiveLong();
            case 3: // magnitude <= 0, sign > 0
            case 4: // magnitude <= 0, sign <= 0
                return generateNonPositiveLong();
            default:
                throw new IllegalArgumentException("无效的分区ID: " + partitionId);
        }
    }

    /**
     * 根据分区ID生成sign值
     *
     * @param partitionId 分区ID (1-4)
     * @return 生成的sign值
     */
    private long generateSignForPartition(int partitionId) {
        switch (partitionId) {
            case 1: // magnitude > 0, sign > 0
            case 3: // magnitude <= 0, sign > 0
                return generatePositiveLong();
            case 2: // magnitude > 0, sign <= 0
            case 4: // magnitude <= 0, sign <= 0
                return generateNonPositiveLong();
            default:
                throw new IllegalArgumentException("无效的分区ID: " + partitionId);
        }
    }

    /**
     * 生成正数
     *
     * @return 正数long值
     */
    private long generatePositiveLong() {
        // 使用默认正数范围
        long min = 1L;
        long max = 1000L;
        
        if (max <= 0) {
            max = 1000L; // 默认正数范围
        }
        
        return min + Math.round(random.nextDouble() * (max - min));
    }

    /**
     * 生成非正数（包括0和负数）
     *
     * @return 非正数long值
     */
    private long generateNonPositiveLong() {
        // 使用默认非正数范围
        long min = -1000L;
        long max = 0L;
        
        if (min > 0) {
            min = -1000L; // 默认负数范围
        }
        
        return min + Math.round(random.nextDouble() * (max - min));
    }

    /**
     * 生成指定分区的测试用例
     *
     * @param partitionId 分区ID
     * @return 生成的测试用例
     */
    public TestCase generateTestCase(int partitionId) {
        return generateTestCaseInPartition(partitionId);
    }

    /**
     * 统计每个分区的测试用例数量和占比
     *
     * @param testCases 测试用例列表
     * @return 统计报告字符串
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
     *
     * @param partitionId 分区ID
     * @return 分区描述字符串
     */
    private String getPartitionDescription(int partitionId) {
        switch (partitionId) {
            case 1: return "分区 1: magnitude > 0, sign > 0";
            case 2: return "分区 2: magnitude > 0, sign <= 0";
            case 3: return "分区 3: magnitude <= 0, sign > 0";
            case 4: return "分区 4: magnitude <= 0, sign <= 0";
            default: return "分区 " + partitionId;
        }
    }

    /**
     * 主函数，用于测试生成器
     */
    public static void main(String[] args) {
        try {
            phase1_partition_generator generator = new phase1_partition_generator();
            List<TestCase> testCases = generator.generate(10000);
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