package paper.pss.exp.jackson_project.generation.phase1;

import paper.pss.exp.jackson_project.model.TestCase;
import paper.pss.exp.jackson_project.utils.JacksonConfigExtractor_utils;
import paper.pss.exp.jackson_project.utils.JacksonConfigExtractor_utils.Partition;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 几何和测试的分区测试生成器
 * 使用最大最小(maximin)算法从各分区中选择测试用例，
 * 以确保测试用例在不同分区间的合理分布
 */
public class phase1_partition_generator {
    private final JacksonConfigExtractor_utils configExtractor;
    private final List<Partition> partitions;
    private final List<Integer> partitionWeights;
    private final Map<Integer, Integer> selectedCounts;
    private final Random random;

    public phase1_partition_generator() throws IOException {
        this("src/main/java/paper/pss/exp/jackson_project/jackson_config.json");
    }

    public phase1_partition_generator(String configPath) throws IOException {
        this.configExtractor = new JacksonConfigExtractor_utils(configPath);
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
            int partitionWeight = partitionWeights.get(selectedPartition - 1);
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
                int currentWeight = partitionWeights.get(partitionId - 1);
                int selectedWeight = partitionWeights.get(selectedPartition - 1);
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
        if (partitionId < 1 || partitionId > partitions.size()) {
            throw new IllegalArgumentException("无效的分区ID: " + partitionId);
        }
        
        String inputString = generateInputStringForPartition(partitionId);
        return new TestCase(inputString, partitionId);
    }

    /**
     * 为指定分区生成输入字符串
     */
    private String generateInputStringForPartition(int partitionId) {
        switch (partitionId) {
            case 1: // Positive, 1–2 digits
                return String.valueOf(random.nextInt(99) + 1);
            case 2: // Positive, 3–8 digits
                int min2 = (int) Math.pow(10, 2); // 100 (3 digits)
                int max2 = (int) Math.pow(10, 8) - 1; // 99999999 (8 digits)
                return String.valueOf(random.nextInt(max2 - min2 + 1) + min2);
            case 3: // Positive, exactly 9 digits
                return String.valueOf(random.nextInt(900000000) + 100000000);
            case 4: // Negative, 2–3 characters (including minus sign)
                return "-" + (random.nextInt(99) + 1);
            case 5: // Negative, 4–10 characters (including minus sign)
                int min5 = (int) Math.pow(10, 2);
                int max5 = (int) Math.pow(10, 8) - 1;
                return "-" + (random.nextInt(max5 - min5 + 1) + min5);
            case 6: // Negative, exactly 10 characters (including minus sign)
                return "-" + (random.nextInt(900000000) + 100000000);
            default:
                throw new IllegalArgumentException("无效的分区ID: " + partitionId);
        }
    }

    /**
     * 生成指定分区的测试用例
     */
    public TestCase generateTestCase(int partitionId) {
        return generateTestCaseInPartition(partitionId);
    }

    /**
     * 统计每个分区的测试用例数量和占比（简洁版）
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
            case 1: return "分区 1: 正数, 1-2位数字";
            case 2: return "分区 2: 正数, 3-8位数字";
            case 3: return "分区 3: 正数, 9位数字";
            case 4: return "分区 4: 负数, 2-3个字符";
            case 5: return "分区 5: 负数, 4-10个字符";
            case 6: return "分区 6: 负数, 10个字符";
            default: return "分区 " + partitionId;
        }
    }

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