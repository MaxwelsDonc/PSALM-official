package paper.pss.exp.jfreeChart_project.generation.phase2;

import paper.pss.exp.jfreeChart_project.model.MetamorphicGroup;
import paper.pss.exp.jfreeChart_project.utils.jfreeConfigExtractor_utils;
import paper.pss.exp.jfreeChart_project.utils.MGDomainGenerator_utils;

import java.io.IOException;
import java.util.*;

/**
 * 随机均匀抽取MetamorphicGroup (MG)的生成器
 * 从MG域中随机抽取指定数量的样本
 */
public class phase2_random_generator {
    private final jfreeConfigExtractor_utils configExtractor;
    private List<MetamorphicGroup> mgDomain;
    private Random random;
    
    /**
     * 使用默认配置路径初始化随机生成器
     * 
     * @param mgDomain MG域列表
     */
    public phase2_random_generator(List<MetamorphicGroup> mgDomain) throws IOException {
        this("src/main/java/paper/pss/exp/jfreeChart_project/jfreeChart_config.json", mgDomain);
    }
    
    /**
     * 初始化随机生成器
     * 
     * @param configPath 配置文件路径
     * @param mgDomain MG域列表
     */
    public phase2_random_generator(String configPath, List<MetamorphicGroup> mgDomain) throws IOException {
        this.configExtractor = new jfreeConfigExtractor_utils(configPath);
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
     * 获取MG域
     * 
     * @return MG域列表
     */
    public List<MetamorphicGroup> getMgDomain() {
        return new ArrayList<>(mgDomain);
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
     * 测试代码
     */
    public static void main(String[] args) {
        try {
            System.out.println("开始生成随机MG域");
            MGDomainGenerator_utils generator = new MGDomainGenerator_utils();
            List<MetamorphicGroup> mgDomain = generator.generateDomain();
            //输出mgDomain的相关统计特性
            System.out.println("mgDomain的相关统计特性:");
            System.out.println(generator.getDomainStatistics(mgDomain));
            System.out.println("随机MG域生成完成");
            
            phase2_random_generator sampler = new phase2_random_generator(mgDomain);
            
            List<MetamorphicGroup> cases = sampler.generate(50);
            
            System.out.println(sampler.getPartitionStatistics(cases));
            
            System.out.println("\n随机测试用例:");
            for (int i = 0; i < Math.min(5, cases.size()); i++) {
                MetamorphicGroup mg = cases.get(i);
                System.out.println(String.format("随机测试用例 #%d: MR=%s, 源测试=%s, 后续测试=%s", 
                    i + 1, 
                    mg.getMRId(), 
                    mg.getSourceTest().toString(), 
                    mg.getFollowupTest().toString()));
            }
            
            System.out.println(String.format("\n总共从%d个MG中随机采样了%d个样本", 
                sampler.getDomainSize(), cases.size()));
                
        } catch (IOException e) {
            System.err.println("配置文件读取失败: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("执行错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
}