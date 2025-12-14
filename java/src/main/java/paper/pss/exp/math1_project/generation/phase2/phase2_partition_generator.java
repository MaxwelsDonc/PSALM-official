package paper.pss.exp.math1_project.generation.phase2;

import paper.pss.exp.math1_project.model.MetamorphicGroup;
import paper.pss.exp.math1_project.utils.Math1ConfigExtractor_utils;
import paper.pss.exp.math1_project.utils.MGDomainGenerator_utils;
import paper.pss.exp.math1_project.utils.MRFactory_utils;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 基于分区的MG采样器
 * 每轮分配一个MG到当前采样率最低的分区-MR组合
 * 实现BMA (Balanced Multi-dimensional Allocation) 算法
 */
public class phase2_partition_generator {
    private final Math1ConfigExtractor_utils configExtractor;
    private List<MetamorphicGroup> mgDomain;
    private final List<String> mrTypes;
    private final List<Double> sourcePartitionRatios;
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
        this("src/main/java/paper/pss/exp/math1_project/math1_config.json", mgDomain);
    }
    
    /**
     * 初始化分区生成器
     * 
     * @param configPath 配置文件路径
     * @param mgDomain MG域列表
     */
    public phase2_partition_generator(String configPath, List<MetamorphicGroup> mgDomain) throws IOException {
        this.configExtractor = new Math1ConfigExtractor_utils(configPath);
        this.random = new Random();
        this.mgDomain = mgDomain;
        
        // 从配置中获取分区比率和MR类型
        this.sourcePartitionRatios = configExtractor.getPartitions().stream()
                .map(Math1ConfigExtractor_utils.Partition::getWeight)
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
        for (Math1ConfigExtractor_utils.Partition partition : configExtractor.getPartitions()) {
            partitionWeights.put(partition.getId(), partition.getWeight());
        }
        
        // 获取MR类型权重映射
        Map<String, Double> mrTypeWeights = new HashMap<>();
        for (String mrType : mrTypes) {
            mrTypeWeights.put(mrType, parseMRTypeWeight(mrType));
        }
        
        // 为每个有可用MG的(partition_id, mr_id)组合计算权重
        for (String key : partitionMgMap.keySet()) {
            String[] parts = key.split("_");
            int partitionId = Integer.parseInt(parts[0]);
            String mrId = parts[1];
            
            if (partitionWeights.containsKey(partitionId) && mrTypeWeights.containsKey(mrId)) {
                double weight = partitionWeights.get(partitionId) * mrTypeWeights.get(mrId);
                sizeDict.put(key, weight);
            }
        }
        
        return sizeDict;
    }
    
    /**
     * 解析MR类型权重
     * 这里简化处理，所有MR类型权重相等
     */
    private double parseMRTypeWeight(String type) {
        // 简化处理：所有MR类型权重为1.0
        return 1.0;
    }
    
    /**
     * 找到采样率最低的分区
     * 
     * @return 采样率最低的分区键
     */
    private String findLowestSamplingRatePartition() {
        String selectedPartition = null;
        double lowestRate = Double.POSITIVE_INFINITY;
        
        for (String key : partitionSize.keySet()) {
            double currentRate = partitionSelectedRatio.get(key);
            
            if (currentRate < lowestRate) {
                lowestRate = currentRate;
                selectedPartition = key;
            } else if (Math.abs(currentRate - lowestRate) < 1e-9) {
                // 如果采样率相等，选择权重更大的分区
                if (selectedPartition == null || partitionSize.get(key) > partitionSize.get(selectedPartition)) {
                    selectedPartition = key;
                }
            }
        }
        
        return selectedPartition;
    }
    
    /**
     * 从指定分区中随机选择一个MG
     * 
     * @param partitionKey 分区键
     * @return 选中的MetamorphicGroup
     */
    private MetamorphicGroup selectMGFromPartition(String partitionKey) {
        List<MetamorphicGroup> availableMGs = partitionMgMap.get(partitionKey);
        if (availableMGs == null || availableMGs.isEmpty()) {
            throw new IllegalStateException("分区 " + partitionKey + " 中没有可用的MG");
        }
        
        int randomIndex = random.nextInt(availableMGs.size());
        return availableMGs.get(randomIndex);
    }
    
    /**
     * 生成指定数量的MetamorphicGroup样本
     * 
     * @param totalSamples 要生成的样本总数
     * @return MetamorphicGroup列表
     */
    public List<MetamorphicGroup> generate(int totalSamples) {
        // 重置选择计数和比率
        for (String key : partitionSelectedCounts.keySet()) {
            partitionSelectedCounts.put(key, 0);
            partitionSelectedRatio.put(key, 0.0);
        }
        
        List<MetamorphicGroup> selectedMGs = new ArrayList<>();
        
        for (int i = 0; i < totalSamples; i++) {
            // 找到采样率最低的分区
            String selectedPartitionKey = findLowestSamplingRatePartition();
            
            // 从该分区中选择一个MG
            MetamorphicGroup selectedMG = selectMGFromPartition(selectedPartitionKey);
            selectedMGs.add(selectedMG);
            
            // 更新选择计数
            int currentCount = partitionSelectedCounts.get(selectedPartitionKey);
            partitionSelectedCounts.put(selectedPartitionKey, currentCount + 1);
            
            // 更新采样率
            double partitionWeight = partitionSize.get(selectedPartitionKey);
            double newRatio = (currentCount + 1) / partitionWeight;
            partitionSelectedRatio.put(selectedPartitionKey, newRatio);
        }
        
        return selectedMGs;
    }
    
    /**
     * 获取分区统计信息
     * 
     * @param mgList MetamorphicGroup列表
     * @return 统计信息字符串
     */
    public String getPartitionStatistics(List<MetamorphicGroup> mgList) {
        Map<String, Integer> partitionCount = new HashMap<>();
        
        // 初始化计数
        for (String key : partitionMgMap.keySet()) {
            partitionCount.put(key, 0);
        }
        
        // 统计每个分区的MG数量
        for (MetamorphicGroup mg : mgList) {
            String key = createKey(mg.getSourceTest().getPartitionId(), mg.getMRId());
            if (partitionCount.containsKey(key)) {
                partitionCount.put(key, partitionCount.get(key) + 1);
            }
        }
        
        StringBuilder report = new StringBuilder();
        report.append("MG总数: ").append(mgList.size()).append("\n");
        
        // 按分区ID和MR ID排序
        List<String> sortedKeys = partitionCount.keySet().stream()
                .sorted((a, b) -> {
                    String[] partsA = a.split("_");
                    String[] partsB = b.split("_");
                    int partitionCompare = Integer.compare(Integer.parseInt(partsA[0]), Integer.parseInt(partsB[0]));
                    if (partitionCompare != 0) {
                        return partitionCompare;
                    }
                    return partsA[1].compareTo(partsB[1]);
                })
                .collect(Collectors.toList());
        
        for (String key : sortedKeys) {
            String[] parts = key.split("_");
            int partitionId = Integer.parseInt(parts[0]);
            String mrId = parts[1];
            int count = partitionCount.get(key);
            double percent = mgList.size() > 0 ? 100.0 * count / mgList.size() : 0;
            
            report.append(String.format(" 分区 %d - MR %s : %d (%.2f%%)\n", 
                    partitionId, mrId, count, percent));
        }
        
        return report.toString();
    }
    
    /**
     * 获取域大小
     * 
     * @return MG域的大小
     */
    public int getDomainSize() {
        return mgDomain.size();
    }
    
    /**
     * 获取配置提取器
     * 
     * @return 配置提取器实例
     */
    public Math1ConfigExtractor_utils getConfigExtractor() {
        return configExtractor;
    }
    
    public static void main(String[] args) {
        try {
            // 生成MG域
            MGDomainGenerator_utils mgGenerator = new MGDomainGenerator_utils();
            List<MetamorphicGroup> mgDomain = mgGenerator.generateDomain();
            
            // 创建分区生成器
            phase2_partition_generator generator = new phase2_partition_generator(mgDomain);
            
            // 生成样本
            List<MetamorphicGroup> samples = generator.generate(100);
            
            // 输出统计信息
            System.out.println(generator.getPartitionStatistics(samples));
            
            System.out.println("\n示例MG:");
            for (int i = 0; i < Math.min(3, samples.size()); i++) {
                MetamorphicGroup mg = samples.get(i);
                System.out.println("MG #" + (i + 1) + ": 分区 " + mg.getSourceTest().getPartitionId() + 
                        ", MR " + mg.getMRId());
            }
        } catch (IOException e) {
            System.err.println("配置文件读取失败: " + e.getMessage());
        }
    }
}