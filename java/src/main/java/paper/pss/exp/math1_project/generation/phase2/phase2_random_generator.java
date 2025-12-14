package paper.pss.exp.math1_project.generation.phase2;

import paper.pss.exp.math1_project.model.MetamorphicGroup;
import paper.pss.exp.math1_project.utils.Math1ConfigExtractor_utils;
import paper.pss.exp.math1_project.utils.MGDomainGenerator_utils;

import java.io.IOException;
import java.util.*;

/**
 * 随机均匀抽取MetamorphicGroup (MG)的生成器
 * 从MG域中随机抽取指定数量的样本
 */
public class phase2_random_generator {
    private final Math1ConfigExtractor_utils configExtractor;
    private List<MetamorphicGroup> mgDomain;
    private Random random;
    
    /**
     * 使用默认配置路径初始化随机生成器
     * 
     * @param mgDomain MG域列表
     */
    public phase2_random_generator(List<MetamorphicGroup> mgDomain) throws IOException {
        this("src/main/java/paper/pss/exp/math1_project/math1_config.json", mgDomain);
    }
    
    /**
     * 初始化随机生成器
     * 
     * @param configPath 配置文件路径
     * @param mgDomain MG域列表
     */
    public phase2_random_generator(String configPath, List<MetamorphicGroup> mgDomain) throws IOException {
        this.configExtractor = new Math1ConfigExtractor_utils(configPath);
        this.mgDomain = mgDomain;
        this.random = new Random();
    }
    
    /**
     * 从MG域中随机抽取指定数量的样本
     * 
     * @param totalSamples 要抽取的样本数量
     * @return 随机抽取的MetamorphicGroup列表
     */
    public List<MetamorphicGroup> generate(int totalSamples) {

        List<MetamorphicGroup> sampled = new ArrayList<>();
        List<MetamorphicGroup> availableMgs = new ArrayList<>(mgDomain);
        
        // 随机抽取不重复的样本
        for (int i = 0; i < totalSamples; i++) {
            int randomIndex = random.nextInt(availableMgs.size());
            MetamorphicGroup selectedMg = availableMgs.remove(randomIndex);
            sampled.add(selectedMg);
        }
        
        return sampled;
    }
    
    /**
     * 获取分区统计信息
     * 
     * @param mgList MetamorphicGroup列表
     * @return 统计信息字符串
     */
    public String getPartitionStatistics(List<MetamorphicGroup> mgList) {
        Map<String, Integer> partitionCount = new HashMap<>();
        
        // 统计每个分区-MR组合的数量
        for (MetamorphicGroup mg : mgList) {
            String key = mg.getSourceTest().getPartitionId() + "_" + mg.getMRId();
            partitionCount.put(key, partitionCount.getOrDefault(key, 0) + 1);
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
                .toList();
        
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
        return mgDomain != null ? mgDomain.size() : 0;
    }
    
    /**
     * 获取MG域
     * 
     * @return MG域的副本
     */
    public List<MetamorphicGroup> getMGDomain() {
        return mgDomain != null ? new ArrayList<>(mgDomain) : new ArrayList<>();
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
            
            // 创建随机生成器
            phase2_random_generator generator = new phase2_random_generator(mgDomain);
            
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
            System.err.println("初始化失败: " + e.getMessage());
        }
    }
}