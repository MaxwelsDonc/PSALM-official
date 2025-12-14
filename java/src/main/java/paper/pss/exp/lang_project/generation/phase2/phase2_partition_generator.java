package paper.pss.exp.lang_project.generation.phase2;

import paper.pss.exp.lang_project.model.MetamorphicGroup;
import paper.pss.exp.lang_project.utils.langConfigExtractor_utils;
import paper.pss.exp.lang_project.utils.MGDomainGenerator_utils;
import paper.pss.exp.lang_project.utils.MRFactory_utils;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 基于平衡多维分配（BMA）算法的分区MG采样器
 * 通过将MG分配到当前采样率最低的分区-MR组合来生成测试用例
 */
public class phase2_partition_generator {
    private final langConfigExtractor_utils configExtractor;
    private final Random random;
    private final List<MetamorphicGroup> mgDomain;
    private final List<Integer> sourcePartitionRatios;
    private final List<String> mrTypes;
    private final Map<String, List<MetamorphicGroup>> partitionMgMap;
    private final Map<String, Integer> partitionSelectedCounts;
    private final Map<String, Double> partitionSelectedRatio;
    private final Map<String, Integer> partitionSize;
    
    /**
     * 使用默认配置路径初始化分区生成器
     * 
     * @param mgDomain MG域列表
     */
    public phase2_partition_generator(List<MetamorphicGroup> mgDomain) throws IOException {
        this("src/main/java/paper/pss/exp/lang_project/lang_config.json", mgDomain);
    }
    
    /**
     * 初始化分区生成器
     * 
     * @param mgDomain MG域列表
     * @param configPath 配置文件路径
     */
    public phase2_partition_generator(String configPath, List<MetamorphicGroup> mgDomain) throws IOException {
        this.configExtractor = new langConfigExtractor_utils(configPath);
        this.random = new Random();
        this.mgDomain = mgDomain;
        
        // 从配置中获取分区比率和MR类型
        this.sourcePartitionRatios = configExtractor.getPartitions().stream()
                .map(langConfigExtractor_utils.Partition::getWeight)
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
     * 生成分区大小映射
     * 
     * @return 分区大小映射
     */
    private Map<String, Integer> generatePartitionSize() {
        Map<String, Integer> sizeMap = new HashMap<>();
        
        for (String key : partitionMgMap.keySet()) {
            sizeMap.put(key, partitionMgMap.get(key).size());
        }
        
        return sizeMap;
    }
    
    /**
     * 创建分区-MR组合的键
     * 
     * @param partitionId 分区ID
     * @param mrId MR ID
     * @return 组合键
     */
    private String createKey(int partitionId, String mrId) {
        return partitionId + "-" + mrId;
    }
    
    /**
     * 使用BMA算法生成指定数量的MG
     * 
     * @param numMGs 要生成的MG数量
     * @return 生成的MG列表
     */
    public List<MetamorphicGroup> generate(int numMGs) {
        List<MetamorphicGroup> selectedMGs = new ArrayList<>();
        
        for (int i = 0; i < numMGs; i++) {
            // 找到当前采样率最低的分区-MR组合
            String lowestRatioKey = findLowestRatioPartition();
            
            // 从该分区随机选择一个MG
            List<MetamorphicGroup> candidateMGs = partitionMgMap.get(lowestRatioKey);
            if (candidateMGs != null && !candidateMGs.isEmpty()) {
                MetamorphicGroup selectedMG = candidateMGs.get(random.nextInt(candidateMGs.size()));
                selectedMGs.add(selectedMG);
                
                // 更新选择计数和比率
                updatePartitionStats(lowestRatioKey);
            }
        }
        
        return selectedMGs;
    }
    
    /**
     * 找到当前采样率最低的分区-MR组合
     * 
     * @return 采样率最低的分区-MR组合的键
     */
    private String findLowestRatioPartition() {
        String lowestKey = null;
        double lowestRatio = Double.MAX_VALUE;
        
        for (String key : partitionMgMap.keySet()) {
            double currentRatio = partitionSelectedRatio.get(key);
            if (currentRatio < lowestRatio) {
                lowestRatio = currentRatio;
                lowestKey = key;
            }
        }
        
        return lowestKey;
    }
    
    /**
     * 更新分区统计信息
     * 
     * @param key 分区-MR组合的键
     */
    private void updatePartitionStats(String key) {
        // 增加选择计数
        int currentCount = partitionSelectedCounts.get(key);
        partitionSelectedCounts.put(key, currentCount + 1);
        
        // 更新采样率
        int partitionSizeValue = partitionSize.get(key);
        double newRatio = (double) (currentCount + 1) / partitionSizeValue;
        partitionSelectedRatio.put(key, newRatio);
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
    public langConfigExtractor_utils getConfigExtractor() {
        return configExtractor;
    }
    
    /**
     * 获取MG域
     * 
     * @return MG域列表
     */
    public List<MetamorphicGroup> getMgDomain() {
        return mgDomain;
    }
    
    /**
     * 设置随机种子
     * 
     * @param seed 随机种子
     */
    public void setSeed(long seed) {
        random.setSeed(seed);
    }
    
    /**
     * 获取分区统计信息
     * 
     * @return 分区统计信息字符串
     */
    public String getStatistics() {
        StringBuilder sb = new StringBuilder();
        sb.append("Partition Statistics:\n");
        
        for (String key : partitionMgMap.keySet()) {
            int size = partitionSize.get(key);
            int selected = partitionSelectedCounts.get(key);
            double ratio = partitionSelectedRatio.get(key);
            
            sb.append(String.format("Partition %s: Size=%d, Selected=%d, Ratio=%.4f\n", 
                    key, size, selected, ratio));
        }
        
        return sb.toString();
    }
    
    public static void main(String[] args) {
        try {
            // 首先生成MG域
            MGDomainGenerator_utils domainGenerator = new MGDomainGenerator_utils(1000);
            List<MetamorphicGroup> mgDomain = domainGenerator.generateDomain();
            
            System.out.println("Generated MG domain with " + mgDomain.size() + " metamorphic groups");
            
            // 使用分区生成器采样
            phase2_partition_generator generator = new phase2_partition_generator(mgDomain);
            List<MetamorphicGroup> samples = generator.generate(50);
            
            System.out.println("\nSampled " + samples.size() + " metamorphic groups:");
            for (int i = 0; i < Math.min(10, samples.size()); i++) {
                MetamorphicGroup mg = samples.get(i);
                System.out.println("MG " + (i+1) + ": MR=" + mg.getMRId() + 
                                 ", Source Partition=" + mg.getSourceTest().getPartitionId());
            }
            
            System.out.println("\n" + generator.getStatistics());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}