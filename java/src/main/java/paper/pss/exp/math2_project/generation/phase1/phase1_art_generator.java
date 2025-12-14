package paper.pss.exp.math2_project.generation.phase1;

import paper.pss.exp.math2_project.model.TestCase;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * ART (Adaptive Random Testing) 生成器
 * 为 copySign 函数实现自适应随机测试策略
 * 通过最大化测试用例间的距离来提高测试效果
 */
public class phase1_art_generator {
    
    private static final Random random = new Random(System.currentTimeMillis());
    private static final long MAX_MAGNITUDE = 1000L;
    private static final long MIN_MAGNITUDE = -1000L;
    private static final long MAX_SIGN = 1000L;
    private static final long MIN_SIGN = -1000L;
    private final int candidateNum;
    
    public phase1_art_generator() {
        this.candidateNum = 10;
    }
    
    public phase1_art_generator(int candidateNum) {
        this.candidateNum = candidateNum;
    }
    
    /**
     * 生成指定数量的测试用例
     * 
     * @param count 要生成的测试用例数量
     * @return 生成的测试用例列表
     */
    public List<TestCase> generate(int count) {
        List<TestCase> testCases = new ArrayList<>();
        
        // 第一步：生成第一个随机测试用例
        long firstMagnitude = generateRandomLong(MIN_MAGNITUDE, MAX_MAGNITUDE);
        long firstSign = generateRandomLong(MIN_SIGN, MAX_SIGN);
        testCases.add(new TestCase(firstMagnitude, firstSign, -1));
        
        // 第二步：使用ART策略生成剩余的测试用例
        while (testCases.size() < count) {
            TestCase bestCandidate = null;
            double bestMinDist = -1;
            
            // 生成候选测试用例并选择距离最远的
            for (int i = 0; i < candidateNum; i++) {
                long magnitude = generateRandomLong(MIN_MAGNITUDE, MAX_MAGNITUDE);
                long sign = generateRandomLong(MIN_SIGN, MAX_SIGN);
                TestCase candidate = new TestCase(magnitude, sign, -1);
                
                // 计算候选用例与已有用例的最小距离
                double minDist = Double.MAX_VALUE;
                for (TestCase existing : testCases) {
                    double dist = distance(candidate, existing);
                    if (dist < minDist) {
                        minDist = dist;
                    }
                }
                
                // 选择最小距离最大的候选用例
                if (minDist > bestMinDist) {
                    bestMinDist = minDist;
                    bestCandidate = candidate;
                }
            }
            
            if (bestCandidate != null) {
                testCases.add(bestCandidate);
            }
        }
        
        return testCases;
    }
    
    /**
     * 计算两个测试用例之间的欧几里得距离
     * 
     * @param a 测试用例A
     * @param b 测试用例B
     * @return 两个测试用例之间的距离
     */
    private double distance(TestCase a, TestCase b) {
        double magnitudeDiff = a.getMagnitude() - b.getMagnitude();
        double signDiff = a.getSign() - b.getSign();
        return Math.sqrt(magnitudeDiff * magnitudeDiff + signDiff * signDiff);
    }
    
    /**
     * 生成指定范围内的随机长整数
     * 
     * @param min 最小值
     * @param max 最大值
     * @return 随机长整数
     */
    private long generateRandomLong(long min, long max) {
        return min + (long) (random.nextDouble() * (max - min + 1));
    }
    
    /**
     * 获取测试用例的统计信息
     * 
     * @param testCases 测试用例列表
     * @return 统计信息字符串
     */
    public String getStatistics(List<TestCase> testCases) {
        if (testCases == null || testCases.isEmpty()) {
            return "没有测试用例";
        }
        
        // 统计各分区的测试用例数量
        int[] partitionCounts = new int[5]; // 索引0不使用，1-4对应分区1-4
        for (TestCase testCase : testCases) {
            partitionCounts[testCase.getPartitionId()]++;
        }
        
        StringBuilder stats = new StringBuilder();
        stats.append("ART生成器统计信息:\n");
        stats.append("总测试用例数: ").append(testCases.size()).append("\n");
        stats.append("候选数量: ").append(candidateNum).append("\n");
        stats.append("分区分布:\n");
        
        for (int i = 1; i <= 4; i++) {
            double percentage = (double) partitionCounts[i] / testCases.size() * 100;
            stats.append(String.format("  分区%d: %d个 (%.1f%%)\n", i, partitionCounts[i], percentage));
        }
        
        return stats.toString();
    }
    
    /**
     * 主函数，用于测试ART生成器
     */
    public static void main(String[] args) {
        try {
            phase1_art_generator generator = new phase1_art_generator();
            
            System.out.println("=== Math2 ART生成器测试 ===");
            
            // 生成1000个测试用例
            List<TestCase> testCases = generator.generate(1000);
            
            // 输出统计信息
            System.out.println(generator.getStatistics(testCases));
            
            // 输出前10个测试用例作为示例
            System.out.println("前10个测试用例示例:");
            for (int i = 0; i < Math.min(10, testCases.size()); i++) {
                TestCase tc = testCases.get(i);
                System.out.printf("  TestCase %d: magnitude=%d, sign=%d, partition=%d\n", 
                    i + 1, tc.getMagnitude(), tc.getSign(), tc.getPartitionId());
            }
            
        } catch (Exception e) {
            System.err.println("ART生成器测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}