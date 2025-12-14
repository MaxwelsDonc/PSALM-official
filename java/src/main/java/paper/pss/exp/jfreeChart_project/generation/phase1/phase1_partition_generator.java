package paper.pss.exp.jfreeChart_project.generation.phase1;

import paper.pss.exp.jfreeChart_project.model.TestCase;
import paper.pss.exp.jfreeChart_project.utils.jfreeConfigExtractor_utils;
import paper.pss.exp.jfreeChart_project.utils.jfreeConfigExtractor_utils.Partition;

import java.awt.geom.Line2D;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * phase1_partition_generator.java
 *
 * This class implements a partition-based test generation strategy for the
 * createLineRegion function. It uses the maximin algorithm to distribute
 * test cases across partitions based on their weights and sizes.
 *
 * Partition classification for createLineRegion:
 * 1: Horizontal line segment (y1 == y2)
 * 2: Vertical line segment (x1 == x2)
 * 3: Diagonal line with positive slope
 * 4: Diagonal line with negative slope
 */
public class phase1_partition_generator {
    private final jfreeConfigExtractor_utils configExtractor;
    private final List<Partition> partitions;
    private final List<Integer> partitionWeights;
    private final Map<Integer, Integer> selectedCounts;
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

    public phase1_partition_generator() throws IOException {
        this("src/main/java/paper/pss/exp/jfreeChart_project/jfreeChart_config.json");
    }

    public phase1_partition_generator(String configPath) throws IOException {
        this.configExtractor = new jfreeConfigExtractor_utils(configPath);
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
        
        Line2D line = generateLineForPartition(partitionId);
        float width = WIDTH_MIN + random.nextFloat() * (WIDTH_MAX - WIDTH_MIN);
        width = Math.round(width * 100) / 100.0f; // 保留两位小数
        
        return new TestCase(line, width, partitionId);
    }

    /**
     * 为指定分区生成线段
     */
    private Line2D generateLineForPartition(int partitionId) {
        switch (partitionId) {
            case 1: // 水平线段
                return generateHorizontalLine();
            case 2: // 垂直线段
                return generateVerticalLine();
            case 3: // 正斜率对角线
                return generateDiagonalLine(true);
            case 4: // 负斜率对角线
                return generateDiagonalLine(false);
            default:
                throw new IllegalArgumentException("无效的分区ID: " + partitionId);
        }
    }

    /**
     * 生成水平线段 (y1 == y2)
     */
    private Line2D generateHorizontalLine() {
        // 随机起始点（保留两位小数）
        double x1 = Math.round((COORD_MIN + random.nextDouble() * (COORD_MAX - COORD_MIN)) * 100) / 100.0;
        double y = Math.round((COORD_MIN + random.nextDouble() * (COORD_MAX - COORD_MIN)) * 100) / 100.0;

        // 在约束范围内的随机长度
        double length = LENGTH_MIN + random.nextDouble() * (LENGTH_MAX - LENGTH_MIN);

        // 随机方向（左或右）
        boolean goRight = random.nextBoolean();
        double x2 = goRight ? x1 + length : x1 - length;

        // 保留两位小数
        x2 = Math.round(x2 * 100) / 100.0;

        // 确保x2在边界内，同时保持最小长度
        if (x2 > COORD_MAX) {
            x2 = COORD_MAX;
            if (Math.abs(x2 - x1) < LENGTH_MIN) {
                x2 = Math.round((x1 - LENGTH_MIN) * 100) / 100.0;
            }
        } else if (x2 < COORD_MIN) {
            x2 = COORD_MIN;
            if (Math.abs(x2 - x1) < LENGTH_MIN) {
                x2 = Math.round((x1 + LENGTH_MIN) * 100) / 100.0;
            }
        }

        return new Line2D.Double(x1, y, x2, y);
    }

    /**
     * 生成垂直线段 (x1 == x2)
     */
    private Line2D generateVerticalLine() {
        // 随机起始点（保留两位小数）
        double x = Math.round((COORD_MIN + random.nextDouble() * (COORD_MAX - COORD_MIN)) * 100) / 100.0;
        double y1 = Math.round((COORD_MIN + random.nextDouble() * (COORD_MAX - COORD_MIN)) * 100) / 100.0;

        // 在约束范围内的随机长度
        double length = LENGTH_MIN + random.nextDouble() * (LENGTH_MAX - LENGTH_MIN);

        // 随机方向（上或下）
        boolean goUp = random.nextBoolean();
        double y2 = goUp ? y1 + length : y1 - length;

        // 保留两位小数
        y2 = Math.round(y2 * 100) / 100.0;

        // 确保y2在边界内，同时保持最小长度
        if (y2 > COORD_MAX) {
            y2 = COORD_MAX;
            if (Math.abs(y2 - y1) < LENGTH_MIN) {
                y2 = Math.round((y1 - LENGTH_MIN) * 100) / 100.0;
            }
        } else if (y2 < COORD_MIN) {
            y2 = COORD_MIN;
            if (Math.abs(y2 - y1) < LENGTH_MIN) {
                y2 = Math.round((y1 + LENGTH_MIN) * 100) / 100.0;
            }
        }

        return new Line2D.Double(x, y1, x, y2);
    }

    /**
     * 生成对角线段，具有正斜率或负斜率
     */
    private Line2D generateDiagonalLine(boolean positiveSlope) {
        // 随机起始点（保留两位小数）
        double x1 = Math.round((COORD_MIN + random.nextDouble() * (COORD_MAX - COORD_MIN)) * 100) / 100.0;
        double y1 = Math.round((COORD_MIN + random.nextDouble() * (COORD_MAX - COORD_MIN)) * 100) / 100.0;

        // 在约束范围内的随机长度
        double length = LENGTH_MIN + random.nextDouble() * (LENGTH_MAX - LENGTH_MIN);

        // 根据斜率类型选择角度
        double angle;
        if (positiveSlope) {
            // 正斜率：第一或第三象限
            angle = random.nextBoolean() ? random.nextDouble() * Math.PI / 2 : // 0到90度
                    Math.PI + random.nextDouble() * Math.PI / 2; // 180到270度
        } else {
            // 负斜率：第二或第四象限
            angle = random.nextBoolean() ? Math.PI / 2 + random.nextDouble() * Math.PI / 2 : // 90到180度
                    3 * Math.PI / 2 + random.nextDouble() * Math.PI / 2; // 270到360度
        }

        // 计算终点并保留两位小数
        double x2 = Math.round((x1 + length * Math.cos(angle)) * 100) / 100.0;
        double y2 = Math.round((y1 + length * Math.sin(angle)) * 100) / 100.0;

        // 检查终点是否超出边界
        boolean outOfBounds = x2 < COORD_MIN || x2 > COORD_MAX || y2 < COORD_MIN || y2 > COORD_MAX;

        if (outOfBounds) {
            // 尝试找到边界交点，同时保持方向
            double dx = Math.cos(angle);
            double dy = Math.sin(angle);

            // 计算各边界的缩放因子
            double scaleX1 = dx != 0 ? (COORD_MIN - x1) / dx : Double.MAX_VALUE;
            double scaleX2 = dx != 0 ? (COORD_MAX - x1) / dx : Double.MAX_VALUE;
            double scaleY1 = dy != 0 ? (COORD_MIN - y1) / dy : Double.MAX_VALUE;
            double scaleY2 = dy != 0 ? (COORD_MAX - y1) / dy : Double.MAX_VALUE;

            // 过滤有效的缩放因子（正数且有限）
            List<Double> validScales = new ArrayList<>();
            if (Double.isFinite(scaleX1) && scaleX1 > 0) validScales.add(scaleX1);
            if (Double.isFinite(scaleX2) && scaleX2 > 0) validScales.add(scaleX2);
            if (Double.isFinite(scaleY1) && scaleY1 > 0) validScales.add(scaleY1);
            if (Double.isFinite(scaleY2) && scaleY2 > 0) validScales.add(scaleY2);

            // 找到最小的正缩放因子（与边界的第一个交点）
            if (!validScales.isEmpty()) {
                double minScale = Collections.min(validScales);
                minScale = Math.min(minScale, length);

                // 应用缩放得到新的终点并保留两位小数
                x2 = Math.round((x1 + dx * minScale) * 100) / 100.0;
                y2 = Math.round((y1 + dy * minScale) * 100) / 100.0;
            }
        }

        // 确保最小长度
        double actualLength = Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
        if (actualLength < LENGTH_MIN) {
            // 放大到最小长度
            double scale = LENGTH_MIN / actualLength;
            x2 = Math.round((x1 + (x2 - x1) * scale) * 100) / 100.0;
            y2 = Math.round((y1 + (y2 - y1) * scale) * 100) / 100.0;

            // 再次检查边界
            x2 = Math.max(COORD_MIN, Math.min(x2, COORD_MAX));
            y2 = Math.max(COORD_MIN, Math.min(y2, COORD_MAX));
        }

        return new Line2D.Double(x1, y1, x2, y2);
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
            case 1: return "分区 1: 水平线段";
            case 2: return "分区 2: 垂直线段";
            case 3: return "分区 3: 正斜率对角线";
            case 4: return "分区 4: 负斜率对角线";
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