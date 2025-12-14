package paper.pss.exp.jackson_project.generation.phase2;

import paper.pss.exp.jackson_project.model.MetamorphicGroup;
import paper.pss.exp.jackson_project.utils.JacksonConfigExtractor_utils;
import paper.pss.exp.jackson_project.utils.MGDomainGenerator;

import java.io.IOException;
import java.util.*;

/**
 * 随机均匀抽取MetamorphicGroup（MG）的生成器
 * 支持加载config，便于后续扩展
 */
public class phase2_random_generator {
    private final JacksonConfigExtractor_utils configExtractor;
    private final Random random;

    private List<MetamorphicGroup> mgDomain;

    /**
     * 使用默认配置路径初始化随机生成器
     * 
     * 
     */
    public phase2_random_generator(List<MetamorphicGroup> mgDomain) throws IOException {
        this("src/main/java/paper/pss/exp/jackson_project/jackson_config.json", mgDomain);
    }

    /**
     * 初始化随机生成器
     * 
     *
     * @param configPath 配置文件路径
     */
    public phase2_random_generator(String configPath, List<MetamorphicGroup> mgDomain) throws IOException {
        this.configExtractor = new JacksonConfigExtractor_utils(configPath);
        this.random = new Random();
        this.mgDomain = mgDomain;
    }

    /**
     * 从MG域中随机均匀抽取指定数量的MetamorphicGroup
     * 
     * @param numSamples 需要抽取的样本数量
     * @return 随机抽取的MetamorphicGroup列表
     * @throws IllegalArgumentException 当样本数量大于MG域总量时抛出异常
     */
    public List<MetamorphicGroup> generate(int numSamples) {
        List<MetamorphicGroup> shuffled = new ArrayList<>(mgDomain);
        Collections.shuffle(shuffled, random);

        return shuffled.subList(0, numSamples);
    }

    /**
     * 获取MG域的大小
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
    public JacksonConfigExtractor_utils getConfigExtractor() {
        return configExtractor;
    }

    /**
     * 获取MG域的副本
     * 
     * @return MG域列表的副本
     */
    public List<MetamorphicGroup> getMgDomain() {
        return new ArrayList<>(mgDomain);
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
     * 测试代码
     */
    public static void main(String[] args) {
        try {
            System.out.println("开始生成分区");
            MGDomainGenerator generator = new MGDomainGenerator();
            List<MetamorphicGroup> mgDomain = generator.generateDomain();
            System.out.println("分区生成完成");

            phase2_random_generator sampler = new phase2_random_generator(mgDomain);
            List<MetamorphicGroup> cases = sampler.generate(100);

            System.out.println("随机抽取的测试用例:");
            for (int i = 0; i < Math.min(5, cases.size()); i++) {
                MetamorphicGroup mg = cases.get(i);
                System.out.println(String.format("RT测试用例 #%d: MR=%s, 源测试=%s, 后续测试=%s",
                        i + 1,
                        mg.getMRId(),
                        mg.getSourceTest().toString(),
                        mg.getFollowupTest().toString()));
            }

            System.out.println(String.format("\n总共从%d个MG中抽取了%d个样本",
                    sampler.getDomainSize(), cases.size()));

        } catch (IOException e) {
            System.err.println("配置文件读取失败: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.err.println("参数错误: " + e.getMessage());
        }
    }
}