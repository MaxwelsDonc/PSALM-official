package paper.pss.exp.math2_project.generation.phase1;

import paper.pss.exp.math2_project.model.TestCase;
import paper.pss.exp.math2_project.utils.math2ConfigExtractor_utils;
import paper.pss.exp.math2_project.utils.math2ConfigExtractor_utils.Partition;

import java.io.IOException;
import java.util.*;

/**
 * phase1_random_generator.java
 *
 * 此类实现了对 copySign 函数的纯随机测试生成策略。
 * 它生成具有随机 magnitude 和 sign 值的测试用例，不考虑分区策略。
 * 分区ID将根据生成的值自动确定。
 */
public class phase1_random_generator {
    private final math2ConfigExtractor_utils configExtractor;
    private final List<Partition> partitions;
    private final Random random;
    
    // 默认范围常量
    private static final long DEFAULT_MIN_VALUE = -1000L;
    private static final long DEFAULT_MAX_VALUE = 1000L;

    /**
     * 使用默认配置文件路径创建生成器
     */
    public phase1_random_generator() throws IOException {
        this("src/main/java/paper/pss/exp/math2_project/math2_config.json");
    }

    /**
     * 使用指定配置文件路径创建生成器
     *
     * @param configPath 配置文件路径
     * @throws IOException 如果配置文件读取失败
     */
    public phase1_random_generator(String configPath) throws IOException {
        this.configExtractor = new math2ConfigExtractor_utils(configPath);
        this.partitions = configExtractor.getPartitions();
        this.random = new Random();
        
        validateConfig();
    }

    /**
     * 使用指定随机种子创建生成器
     *
     * @param configPath 配置文件路径
     * @param seed 随机种子，用于可重现的结果
     * @throws IOException 如果配置文件读取失败
     */
    public phase1_random_generator(String configPath, long seed) throws IOException {
        this.configExtractor = new math2ConfigExtractor_utils(configPath);
        this.partitions = configExtractor.getPartitions();
        this.random = new Random(seed);
        
        validateConfig();
    }

    /**
     * 验证配置的有效性
     */
    private void validateConfig() {
        if (partitions.isEmpty()) {
            throw new IllegalStateException("配置文件中未找到分区定义");
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
     * 生成指定数量的随机测试用例
     *
     * @param count 要生成的测试用例数量
     * @return 生成的测试用例列表
     */
    public List<TestCase> generate(int count) {
        List<TestCase> testCases = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            TestCase testCase = generateRandomTestCase();
            testCases.add(testCase);
        }
        
        return testCases;
    }

    /**
     * 生成单个随机测试用例
     *
     * @return 生成的测试用例
     */
    public TestCase generateRandomTestCase() {
        long magnitude = generateRandomLong();
        long sign = generateRandomLong();
        int partitionId = -1;
        
        return new TestCase(magnitude, sign, partitionId);
    }

    /**
     * 生成随机long值
     *
     * @return 随机long值
     */
    private long generateRandomLong() {
        // 使用默认范围生成随机值
        long range = DEFAULT_MAX_VALUE - DEFAULT_MIN_VALUE + 1;
        return DEFAULT_MIN_VALUE + (long) (random.nextDouble() * range);
    }

    /**
     * 统计每个分区的测试用例数量和占比
     *
     * @param testCases 测试用例列表
     * @return 统计报告字符串
     */
    public String getStatistics(List<TestCase> testCases) {
        Map<Integer, Integer> partitionCount = new HashMap<>();
        
        // 初始化计数器
        for (int i = 1; i <= 4; i++) {
            partitionCount.put(i, 0);
        }
        
        int total = testCases.size();
        for (TestCase tc : testCases) {
            int pid = tc.getPartitionId();
            if (partitionCount.containsKey(pid)) {
                partitionCount.put(pid, partitionCount.get(pid) + 1);
            }
        }
        
        StringBuilder report = new StringBuilder();
        report.append("随机测试用例总数: ").append(total).append("\n");
        
        for (int pid = 1; pid <= 4; pid++) {
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
     * 计算测试用例的基本统计信息
     *
     * @param testCases 测试用例列表
     * @return 统计信息字符串
     */
    public String getDetailedStatistics(List<TestCase> testCases) {
        if (testCases.isEmpty()) {
            return "无测试用例";
        }
        
        // 计算magnitude统计
        long minMagnitude = testCases.stream().mapToLong(TestCase::getMagnitude).min().orElse(0);
        long maxMagnitude = testCases.stream().mapToLong(TestCase::getMagnitude).max().orElse(0);
        double avgMagnitude = testCases.stream().mapToLong(TestCase::getMagnitude).average().orElse(0);
        
        // 计算sign统计
        long minSign = testCases.stream().mapToLong(TestCase::getSign).min().orElse(0);
        long maxSign = testCases.stream().mapToLong(TestCase::getSign).max().orElse(0);
        double avgSign = testCases.stream().mapToLong(TestCase::getSign).average().orElse(0);
        
        StringBuilder report = new StringBuilder();
        report.append(getStatistics(testCases));
        report.append("\n详细统计信息:\n");
        report.append(String.format(" Magnitude - 最小值: %d, 最大值: %d, 平均值: %.2f\n", 
                minMagnitude, maxMagnitude, avgMagnitude));
        report.append(String.format(" Sign - 最小值: %d, 最大值: %d, 平均值: %.2f\n", 
                minSign, maxSign, avgSign));
        
        return report.toString();
    }

    /**
     * 主函数，用于测试生成器
     */
    public static void main(String[] args) {
        try {
            phase1_random_generator generator = new phase1_random_generator();
            List<TestCase> testCases = generator.generate(10000);
            System.out.println(generator.getDetailedStatistics(testCases));
            System.out.println("\n示例测试用例:");
            for (int i = 0; i < Math.min(5, testCases.size()); i++) {
                System.out.println("测试用例 #" + (i + 1) + ": " + testCases.get(i));
            }
        } catch (IOException e) {
            System.err.println("配置文件读取失败: " + e.getMessage());
        }
    }
}