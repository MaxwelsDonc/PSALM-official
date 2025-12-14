package paper.pss.exp.lang_project.generation.phase2;

import paper.pss.exp.lang_project.model.MetamorphicGroup;
import paper.pss.exp.lang_project.utils.langConfigExtractor_utils;
import paper.pss.exp.lang_project.utils.MGDomainGenerator_utils;

import java.io.IOException;
import java.util.*;

/**
 * 随机均匀抽取蜕变组（MG）的生成器
 * 支持从MG域中随机抽取指定数量的MG
 */
public class phase2_random_generator {
    private final langConfigExtractor_utils configExtractor;
    private final Random random;
    private final List<MetamorphicGroup> mgDomain;
    
    /**
     * 使用默认配置路径初始化随机生成器
     * 
     * @param mgDomain MG域列表
     */
    public phase2_random_generator(List<MetamorphicGroup> mgDomain) throws IOException {
        this("src/main/java/paper/pss/exp/lang_project/lang_config.json", mgDomain);
    }
    
    /**
     * 初始化随机生成器
     * 
     * @param configPath 配置文件路径
     * @param mgDomain MG域列表
     */
    public phase2_random_generator(String configPath, List<MetamorphicGroup> mgDomain) throws IOException {
        this.configExtractor = new langConfigExtractor_utils(configPath);
        this.random = new Random();
        this.mgDomain = mgDomain;
    }
    
    /**
     * 随机生成指定数量的蜕变组
     * 
     * @param count 要生成的MG数量
     * @return 随机选择的MG列表
     */
    public List<MetamorphicGroup> generate(int count) {
        if (count <= 0) {
            return new ArrayList<>();
        }
        
        if (count >= mgDomain.size()) {
            return new ArrayList<>(mgDomain);
        }
        
        List<MetamorphicGroup> result = new ArrayList<>();
        Set<Integer> selectedIndices = new HashSet<>();
        
        while (selectedIndices.size() < count) {
            int index = random.nextInt(mgDomain.size());
            if (selectedIndices.add(index)) {
                result.add(mgDomain.get(index));
            }
        }
        
        return result;
    }
    
    /**
     * 获取MG域的大小
     * 
     * @return MG域中的MG数量
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
     * 获取生成的MG统计信息
     * 
     * @param mgs 生成的MG列表
     * @return 统计信息映射
     */
    public Map<String, Integer> getStatistics(List<MetamorphicGroup> mgs) {
        Map<String, Integer> stats = new HashMap<>();
        
        for (MetamorphicGroup mg : mgs) {
            String mgType = mg.getClass().getSimpleName();
            stats.put(mgType, stats.getOrDefault(mgType, 0) + 1);
        }
        
        return stats;
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
     * 获取MG域
     * 
     * @return MG域列表
     */
    public List<MetamorphicGroup> getMgDomain() {
        return mgDomain;
    }
    
    public static void main(String[] args) {
        try {
            // 首先生成MG域
            MGDomainGenerator_utils domainGenerator = new MGDomainGenerator_utils();
            List<MetamorphicGroup> mgDomain = domainGenerator.generateDomain();
            
            System.out.println("Generated MG domain with " + mgDomain.size() + " metamorphic groups");
            
            // 创建随机生成器
            phase2_random_generator generator = new phase2_random_generator(mgDomain);
            
            // 随机生成50个MG
            List<MetamorphicGroup> selectedMGs = generator.generate(50);
            
            System.out.println("\nRandomly selected " + selectedMGs.size() + " metamorphic groups:");
            for (int i = 0; i < Math.min(10, selectedMGs.size()); i++) {
                MetamorphicGroup mg = selectedMGs.get(i);
                System.out.println("MG " + (i+1) + ": MR=" + mg.getMRId() + 
                                 ", Source Partition=" + mg.getSourceTest().getPartitionId());
            }
            
            // 输出统计信息
            Map<String, Integer> stats = generator.getStatistics(selectedMGs);
            System.out.println("\nMG Type Statistics:");
            for (Map.Entry<String, Integer> entry : stats.entrySet()) {
                System.out.println(entry.getKey() + ": " + entry.getValue());
            }
            
        } catch (IOException e) {
            System.err.println("Initialization failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}