package paper.pss.exp.jfreeChart_project.generation.phase2;

import paper.pss.exp.jfreeChart_project.model.MetamorphicGroup;
import paper.pss.exp.jfreeChart_project.utils.jfreeConfigExtractor_utils;
import paper.pss.exp.jfreeChart_project.utils.MGDomainGenerator_utils;
import paper.pss.exp.jfreeChart_project.utils.MRFactory_utils;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 基于分区的MG采样器
 * 每轮分配一个MG到当前采样率最低的分区-MR组合
 * 实现BMA (Balanced Multi-dimensional Allocation) 算法
 */
public class phase2_partition_generator {
    private final jfreeConfigExtractor_utils configExtractor;
    private List<MetamorphicGroup> mgDomain;
    private final List<String> mrTypes;
    private final List<Integer> sourcePartitionRatios;
    private final Random random;
    
    // 分区映射: (partition_id, mr_id) -> [MGs]
    private final Map<String, List<MetamorphicGroup>> partitionMgMap;
    
    // 分区大小: (partition_id, mr_id) -> weight
    private final Map<String, Double> partitionSize;
    
    // 分区选择计数和比率
    private final Map<String, Integer> partitionSelectedCounts;
    private final Map<String, Double> partitionSelectedRatio;
    
    /**
     * 使用默认配置路径初始化分区生成器
     * 
     * @param mgDomain MG域列表
     */
    public phase2_partition_generator(List<MetamorphicGroup> mgDomain) throws IOException {
        this("src/main/java/paper/pss/exp/jfreeChart_project/jfreeChart_config.json", mgDomain);
    }
    
    /**
     * 初始化分区生成器
     * 
     * @param mgDomain MG域列表
     * @param configPath 配置文件路径
     */
    public phase2_partition_generator(String configPath, List<MetamorphicGroup> mgDomain) throws IOException {
        this.configExtractor = new jfreeConfigExtractor_utils(configPath);
        this.random = new Random();
        this.mgDomain = mgDomain;
        
        // 从配置中获取分区比率和MR类型
        this.sourcePartitionRatios = configExtractor.getPartitions().stream()
                .map(jfreeConfigExtractor_utils.Partition::getWeight)
                .collect(Collectors.toList());
        
        // 从MRFactory获取所有MR类型
        this.mrTypes = MRFactory_utils.getAllRelationIds();
        
        // 构建分区映射
        this.partitionMgMap = new HashMap<>();        
        // 生成分区大小
        this.partitionSelectedCounts = new HashMap<>();
        this.partitionSelectedRatio = new HashMap<>();
        // 初始化分区映射
        for (MetamorphicGroup mg : mgDomain) {
            String key = createKey(mg.getSourceTest().getPartitionId(), mg.getMRId());
            this.partitionMgMap.computeIfAbsent(key, k -> new ArrayList<>()).add(mg);
        }
        // 初始化分区选择计数和比率
        for (String key : partitionMgMap.keySet()) {
            this.partitionSelectedCounts.put(key, 0);
            this.partitionSelectedRatio.put(key, 0.0);
        }
        // 生成分区大小
        this.partitionSize = generatePartitionSize();
    }
    
    /**
     * 创建分区键
     */
    private String createKey(int partitionId, String mrId) {
        return partitionId + "_" + mrId;
    }
    
    /**
     * 生成每个(partition_id, mr_id)组合的期望大小/权重
     * 对于每对，值 = partition_weight * mr_type_weight
     * 只有当分区有可用MG时才添加到权重映射中
     * 
     * @return 从(partition_id, mr_id)到权重的映射
     */
    private Map<String, Double> generatePartitionSize() {
        Map<String, Double> sizeDict = new HashMap<>();
        
        // 获取分区权重映射
        Map<Integer, Double> partitionWeights = new HashMap<>();
        for (jfreeConfigExtractor_utils.Partition partition : configExtractor.getPartitions()) {
            partitionWeights.put(partition.getId(), (double) partition.getWeight());
        }
        
        // 获取MR类型权重映射（根据type字段解析权重）
        Map<String, Double> mrTypeWeights = new HashMap<>();
        for (Map.Entry<String, jfreeConfigExtractor_utils.MR> entry : configExtractor.getMrs().entrySet()) {
            String mrId = entry.getKey();
            String type = entry.getValue().getType();
            // 解析type字段，例如"1-1"表示权重为1，"1-2"表示权重为2
            double weight = parseMRTypeWeight(type);
            mrTypeWeights.put(mrId, weight);
        }
        
        for (String mrId : mrTypes) {
            for (Map.Entry<Integer, Double> partitionEntry : partitionWeights.entrySet()) {
                int partitionId = partitionEntry.getKey();
                double partitionWeight = partitionEntry.getValue();
                
                String key = createKey(partitionId, mrId);
                // 只有当分区有MG时才添加到权重映射中
                double mrWeight = mrTypeWeights.getOrDefault(mrId, 1.0);
                if (partitionMgMap.containsKey(key)) {
                    sizeDict.put(key, partitionWeight * mrWeight);
                }

            }
        }
        
        return sizeDict;
    }
    
    /**
     * 解析MR类型权重
     * 例如："1-1" -> 1.0, "1-2" -> 2.0
     * 
     * @param type MR类型字符串
     * @return 权重值
     */
    private double parseMRTypeWeight(String type) {
        if (type == null || type.isEmpty()) {
            return 1.0;
        }
        
        // 解析"x-y"格式，取y作为权重
        String[] parts = type.split("-");
        if (parts.length >= 2) {
            try {
                return Double.parseDouble(parts[1]);
            } catch (NumberFormatException e) {
                System.err.println("警告: 无法解析MR类型权重: " + type + "，使用默认权重1.0");
                return 1.0;
            }
        }
        
        return 1.0;
    }
    
    /**
     * 找到采样率最低的(partition_id, mr_id)键
     * 如果多个具有相同的最小比率，选择partition_size最大的
     * 
     * @return (partition_id, mr_id)键
     */
    private String findLowestSamplingRatePartition() {
        Set<String> keys = partitionSize.keySet();
        
        // 1. 找所有采样率最小的key
        double minRatio = keys.stream()
                .mapToDouble(k -> partitionSelectedRatio.getOrDefault(k, 0.0))
                .min()
                .orElse(0.0);
        
        List<String> minKeys = keys.stream()
                .filter(k -> Math.abs(partitionSelectedRatio.getOrDefault(k, 0.0) - minRatio) < 1e-9)
                .collect(Collectors.toList());
        
        // 2. 如果多个采样率相同，选size最大的
        if (minKeys.size() == 1) {
            return minKeys.get(0);
        }
        
        // 有多个，选size最大
        double maxSize = minKeys.stream()
                .mapToDouble(k -> partitionSize.get(k))
                .max()
                .orElse(0.0);
        
        List<String> candidates = minKeys.stream()
                .filter(k -> Math.abs(partitionSize.get(k) - maxSize) < 1e-9)
                .collect(Collectors.toList());
        
        // 如还有平局，随便取一个
        return candidates.get(0);
    }
    
    /**
     * 动态地一次分配一个MG到当前采样率最低的(分区, MR)
     * 
     * @param totalSamples 总样本数
     * @return 采样的MetamorphicGroup列表
     */
    public List<MetamorphicGroup> generate(int totalSamples) {
        
        // 准备采样
        List<MetamorphicGroup> sampled = new ArrayList<>();
        
        // 初始化
        for (String key : partitionMgMap.keySet()) {
            partitionSelectedCounts.put(key, 0);
            partitionSelectedRatio.put(key, 0.0);
        }
        
        for (int i = 0; i < totalSamples; i++) {            
            String targetPartitionKey = findLowestSamplingRatePartition();
            
            // 检查该分区是否有可用的MG（理论上不应该发生，因为已经预过滤）
            List<MetamorphicGroup> availableMgs = partitionMgMap.get(targetPartitionKey);
            MetamorphicGroup targetMg = availableMgs.get(random.nextInt(availableMgs.size()));
            sampled.add(targetMg);
            
            // 更新计数和比率
            int newCount = partitionSelectedCounts.get(targetPartitionKey) + 1;
            partitionSelectedCounts.put(targetPartitionKey, newCount);
            
            double newRatio = newCount / partitionSize.get(targetPartitionKey);
            partitionSelectedRatio.put(targetPartitionKey, newRatio);
        }
        
        return sampled;
    }
    
    /**
     * 返回每个(partition_id, mr_id)中采样MG数量的字符串摘要
     * 显示绝对计数和总数的百分比
     * 
     * @param mgList MG列表
     * @return 统计信息字符串
     */
    public String getPartitionStatistics(List<MetamorphicGroup> mgList) {
        // Collect counts
        Map<String, Integer> countMap = new HashMap<>();
        Set<String> mrSet = new TreeSet<>();
        Set<Integer> partitionSet = new TreeSet<>();
    
        for (MetamorphicGroup mg : mgList) {
            int partitionId = mg.getSourceTest().getPartitionId();
            String mrId = mg.getMRId();
            String key = partitionId + "_" + mrId;
    
            countMap.put(key, countMap.getOrDefault(key, 0) + 1);
            mrSet.add(mrId);
            partitionSet.add(partitionId);
        }
    
        // Build table header
        StringBuilder sb = new StringBuilder();
        sb.append("Sampling Summary Table (unit: count)\n");
        sb.append(String.format("%-10s", "MR \\ P"));
    
        for (int pid : partitionSet) {
            sb.append(String.format("%8s", "P" + pid));
        }
        sb.append(String.format("%10s\n", "Total"));
    
        // Build table rows
        for (String mrId : mrSet) {
            sb.append(String.format("%-10s", mrId));
            int rowSum = 0;
            for (int pid : partitionSet) {
                String key = pid + "_" + mrId;
                int count = countMap.getOrDefault(key, 0);
                sb.append(String.format("%8d", count));
                rowSum += count;
            }
            sb.append(String.format("%10d\n", rowSum));
        }
    
        // Build column totals
        sb.append(String.format("%-10s", "Total"));
        for (int pid : partitionSet) {
            int colSum = 0;
            for (String mrId : mrSet) {
                String key = pid + "_" + mrId;
                colSum += countMap.getOrDefault(key, 0);
            }
            sb.append(String.format("%8d", colSum));
        }
    
        int total = mgList.size();
        sb.append(String.format("%10d\n", total));
    
        return sb.toString();
    }
    
    /**
     * 获取MG域大小
     * 
     * @return MG域中MetamorphicGroup的总数
     */
    public int getDomainSize() {
        return mgDomain.size();
    }
    
    /**
     * 获取配置提取器
     * 
     * @return 配置提取器实例
     */
    public jfreeConfigExtractor_utils getConfigExtractor() {
        return configExtractor;
    }
    
    /**
     * 测试代码
     */
    public static void main(String[] args) {
        try {
            System.out.println("开始生成分区");
            MGDomainGenerator_utils generator = new MGDomainGenerator_utils();
            List<MetamorphicGroup> mgDomain = generator.generateDomain();
            System.out.println("分区生成完成");
            
            phase2_partition_generator sampler = new phase2_partition_generator(mgDomain);
            List<MetamorphicGroup> cases = sampler.generate(50);
            
            System.out.println(sampler.getPartitionStatistics(cases));
            
            System.out.println("\nBMA测试用例:");
            for (int i = 0; i < Math.min(5, cases.size()); i++) {
                MetamorphicGroup mg = cases.get(i);
                System.out.println(String.format("BMA测试用例 #%d: MR=%s, 源测试=%s, 后续测试=%s", 
                    i + 1, 
                    mg.getMRId(), 
                    mg.getSourceTest().toString(), 
                    mg.getFollowupTest().toString()));
            }
            
            System.out.println(String.format("\n总共从%d个MG中采样了%d个样本", 
                sampler.getDomainSize(), cases.size()));
                
        } catch (IOException e) {
            System.err.println("配置文件读取失败: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("执行错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
}