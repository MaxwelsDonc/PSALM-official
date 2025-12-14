package paper.pss.exp.lang_project.generation.phase1;

import java.util.*;
import java.util.stream.Collectors;
import java.io.IOException;
import paper.pss.exp.lang_project.model.TestCase;
import paper.pss.exp.lang_project.utils.langConfigExtractor_utils;
import paper.pss.exp.lang_project.utils.langConfigExtractor_utils.Partition;

/**
 * phase1_partition_generator.java
 *
 * This class implements a partition-based test generation strategy for the
 * isSameDay function. It uses the maximin algorithm to distribute
 * test cases across partitions based on their weights and sizes.
 *
 * Partition classification for isSameDay:
 * 1: 空输入 (null inputs)
 * 2: 相同日期 (identical dates)
 * 3: 年月相同但日不同 (same year and month, different day)
 * 4: 年日相同但月不同 (same year and day, different month)
 * 5: 月日相同但年不同 (same month and day, different year)
 * 6: 仅年相同 (same year only)
 * 7: 仅月相同 (same month only)
 * 8: 仅日相同 (same day only)
 * 9: 年月日都不同 (all different)
 */
public class phase1_partition_generator {
    private final langConfigExtractor_utils configExtractor;
    private final List<Partition> partitions;
    private final List<Integer> partitionWeights;
    private final Map<Integer, Integer> selectedCounts;
    private final Random random;

    public phase1_partition_generator() throws IOException {
        this("src/main/java/paper/pss/exp/lang_project/lang_config.json");
    }

    public phase1_partition_generator(String configPath) throws IOException {
        this.configExtractor = new langConfigExtractor_utils(configPath);
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
        
        return generateTestCaseForPartition(partitionId);
    }

    /**
     * 生成指定分区的测试用例
     */
    public TestCase generateTestCase(int partitionId) {
        return generateTestCaseForPartition(partitionId);
    }

    private TestCase generateTestCaseForPartition(int partitionId) {
        switch (partitionId) {
            case 1: return generateNullTestCase();
            case 2: return generateIdenticalDatesTestCase();
            case 3: return generateSameYearMonthTestCase();
            case 4: return generateSameYearDayTestCase();
            case 5: return generateSameMonthDayTestCase();
            case 6: return generateSameYearTestCase();
            case 7: return generateSameMonthTestCase();
            case 8: return generateSameDayTestCase();
            case 9: return generateAllDifferentTestCase();
            default: return generateRandomTestCase();
        }
    }
    
    private TestCase generateNullTestCase() {
        int nullCase = random.nextInt(3);
        if (nullCase == 0) {
            return new TestCase((Date) null, (Date) null, 1);
        } else if (nullCase == 1) {
            return new TestCase(null, generateRandomDate(), 1);
        } else {
            return new TestCase(generateRandomDate(), null, 1);
        }
    }
    
    private TestCase generateIdenticalDatesTestCase() {
        Date date = generateRandomDate();
        return new TestCase(date, new Date(date.getTime()), 2);
    }
    
    private TestCase generateSameYearMonthTestCase() {
        Calendar cal1 = Calendar.getInstance();
        cal1.set(2020 + random.nextInt(10), random.nextInt(12), 1 + random.nextInt(28));
        
        Calendar cal2 = Calendar.getInstance();
        cal2.set(cal1.get(Calendar.YEAR), cal1.get(Calendar.MONTH), 1 + random.nextInt(28));
        
        while (cal2.get(Calendar.DAY_OF_MONTH) == cal1.get(Calendar.DAY_OF_MONTH)) {
            cal2.set(Calendar.DAY_OF_MONTH, 1 + random.nextInt(28));
        }
        
        return new TestCase(cal1.getTime(), cal2.getTime(), 3);
    }
    
    private TestCase generateSameYearDayTestCase() {
        Calendar cal1 = Calendar.getInstance();
        cal1.set(2020 + random.nextInt(10), random.nextInt(12), 1 + random.nextInt(28));
        
        Calendar cal2 = Calendar.getInstance();
        cal2.set(cal1.get(Calendar.YEAR), random.nextInt(12), cal1.get(Calendar.DAY_OF_MONTH));
        
        while (cal2.get(Calendar.MONTH) == cal1.get(Calendar.MONTH)) {
            cal2.set(Calendar.MONTH, random.nextInt(12));
        }
        
        return new TestCase(cal1.getTime(), cal2.getTime(), 4);
    }
    
    private TestCase generateSameMonthDayTestCase() {
        Calendar cal1 = Calendar.getInstance();
        cal1.set(2020 + random.nextInt(10), random.nextInt(12), 1 + random.nextInt(28));
        
        Calendar cal2 = Calendar.getInstance();
        cal2.set(2020 + random.nextInt(10), cal1.get(Calendar.MONTH), cal1.get(Calendar.DAY_OF_MONTH));
        
        while (cal2.get(Calendar.YEAR) == cal1.get(Calendar.YEAR)) {
            cal2.set(Calendar.YEAR, 2020 + random.nextInt(10));
        }
        
        return new TestCase(cal1.getTime(), cal2.getTime(), 5);
    }
    
    private TestCase generateSameYearTestCase() {
        Calendar cal1 = Calendar.getInstance();
        cal1.set(2020 + random.nextInt(10), random.nextInt(12), 1 + random.nextInt(28));
        
        Calendar cal2 = Calendar.getInstance();
        cal2.set(cal1.get(Calendar.YEAR), random.nextInt(12), 1 + random.nextInt(28));
        
        while (cal2.get(Calendar.MONTH) == cal1.get(Calendar.MONTH) || 
               cal2.get(Calendar.DAY_OF_MONTH) == cal1.get(Calendar.DAY_OF_MONTH)) {
            cal2.set(Calendar.MONTH, random.nextInt(12));
            cal2.set(Calendar.DAY_OF_MONTH, 1 + random.nextInt(28));
        }
        
        return new TestCase(cal1.getTime(), cal2.getTime(), 6);
    }
    
    private TestCase generateSameMonthTestCase() {
        Calendar cal1 = Calendar.getInstance();
        cal1.set(2020 + random.nextInt(10), random.nextInt(12), 1 + random.nextInt(28));
        
        Calendar cal2 = Calendar.getInstance();
        cal2.set(2020 + random.nextInt(10), cal1.get(Calendar.MONTH), 1 + random.nextInt(28));
        
        while (cal2.get(Calendar.YEAR) == cal1.get(Calendar.YEAR) || 
               cal2.get(Calendar.DAY_OF_MONTH) == cal1.get(Calendar.DAY_OF_MONTH)) {
            cal2.set(Calendar.YEAR, 2020 + random.nextInt(10));
            cal2.set(Calendar.DAY_OF_MONTH, 1 + random.nextInt(28));
        }
        
        return new TestCase(cal1.getTime(), cal2.getTime(), 7);
    }
    
    private TestCase generateSameDayTestCase() {
        Calendar cal1 = Calendar.getInstance();
        cal1.set(2020 + random.nextInt(10), random.nextInt(12), 1 + random.nextInt(28));
        
        Calendar cal2 = Calendar.getInstance();
        cal2.set(2020 + random.nextInt(10), random.nextInt(12), cal1.get(Calendar.DAY_OF_MONTH));
        
        while (cal2.get(Calendar.YEAR) == cal1.get(Calendar.YEAR) || 
               cal2.get(Calendar.MONTH) == cal1.get(Calendar.MONTH)) {
            cal2.set(Calendar.YEAR, 2020 + random.nextInt(10));
            cal2.set(Calendar.MONTH, random.nextInt(12));
        }
        
        return new TestCase(cal1.getTime(), cal2.getTime(), 8);
    }
    
    private TestCase generateAllDifferentTestCase() {
        Calendar cal1 = Calendar.getInstance();
        cal1.set(2020 + random.nextInt(10), random.nextInt(12), 1 + random.nextInt(28));
        
        Calendar cal2 = Calendar.getInstance();
        do {
            cal2.set(2020 + random.nextInt(10), random.nextInt(12), 1 + random.nextInt(28));
        } while (cal2.get(Calendar.YEAR) == cal1.get(Calendar.YEAR) || 
                 cal2.get(Calendar.MONTH) == cal1.get(Calendar.MONTH) || 
                 cal2.get(Calendar.DAY_OF_MONTH) == cal1.get(Calendar.DAY_OF_MONTH));
        
        return new TestCase(cal1.getTime(), cal2.getTime(), 9);
    }
    
    private TestCase generateRandomTestCase() {
        Date date1 = generateRandomDate();
        Date date2 = generateRandomDate();
        return new TestCase(date1, date2, 0);
    }
    
    private Date generateRandomDate() {
        Calendar cal = Calendar.getInstance();
        cal.set(2020 + random.nextInt(10), random.nextInt(12), 1 + random.nextInt(28));
        return cal.getTime();
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
            case 1: return "分区 1: 空输入";
            case 2: return "分区 2: 相同日期";
            case 3: return "分区 3: 年月相同但日不同";
            case 4: return "分区 4: 年日相同但月不同";
            case 5: return "分区 5: 月日相同但年不同";
            case 6: return "分区 6: 仅年相同";
            case 7: return "分区 7: 仅月相同";
            case 8: return "分区 8: 仅日相同";
            case 9: return "分区 9: 年月日都不同";
            default: return "分区 " + partitionId;
        }
    }

    public static void main(String[] args) {
        try {
            phase1_partition_generator generator = new phase1_partition_generator();
            List<TestCase> testCases = generator.generate(9);
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