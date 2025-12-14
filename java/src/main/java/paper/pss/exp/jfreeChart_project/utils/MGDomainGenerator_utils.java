package paper.pss.exp.jfreeChart_project.utils;

import paper.pss.exp.jfreeChart_project.model.MetamorphicGroup;
import paper.pss.exp.jfreeChart_project.model.TestCase;
import paper.pss.exp.jfreeChart_project.generation.phase1.phase1_partition_generator;
import paper.pss.exp.jfreeChart_project.model.MetamorphicRelation;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 蜕变组域生成器
 * 基于jackson_project的MGDomainGenerator实现
 */
public class MGDomainGenerator_utils {
    private String configPath;
    private jfreeConfigExtractor_utils configExtractor;
    private int domainSize;
    private phase1_partition_generator partitionGenerator;
    private List<String> allMRTypes;

    /**
     * 默认构造函数
     */
    public MGDomainGenerator_utils() throws IOException {
        this(1000);
    }

    /**
     * 初始化蜕变组域生成器
     * 
     * @param domainSize 域的大小(源测试用例数量)
     */
    public MGDomainGenerator_utils(int domainSize) throws IOException {
        this.configPath = "src/main/java/paper/pss/exp/jfreeChart_project/jfreeChart_config.json";
        this.configExtractor = new jfreeConfigExtractor_utils(configPath);
        this.domainSize = calculateDomainSize();
        this.partitionGenerator = new phase1_partition_generator(configPath);
        this.allMRTypes = loadAllMRTypes();
    }

    /**
     * 加载所有蜕变关系类型
     */
    private List<String> loadAllMRTypes() {
        return MRFactory_utils.getAllRelationIds();
    }

    /**
     * 生成蜕变组域
     * 首先生成一组源测试用例，然后为每个测试用例应用所有适用的蜕变关系，
     * 生成蜕变组
     * 
     * @return 生成的蜕变组域
     */
    public List<MetamorphicGroup> generateDomain() {
        // 1. 生成源测试用例
        List<TestCase> sourceTests = partitionGenerator.generate(domainSize);

        // 2. 为每个源测试用例应用所有适用的蜕变关系
        List<MetamorphicGroup> mgDomain = new ArrayList<>();

        for (TestCase sourceTest : sourceTests) {
            List<MetamorphicGroup> mgs = MRFactory_utils.generateMetamorphicGroups(sourceTest);
            mgDomain.addAll(mgs);
        }

        return mgDomain;
    }

    /**
     * 计算合适的域大小，确保每个类别(MR×分区组合)至少有指定数量的样本
     * 
     * @param minSamplesPerCategory 每个类别最少的样本数
     * @return 建议的域大小
     */
    public int calculateDomainSize(int minSamplesPerCategory) {
        // 获取分区数量
        int partitionCount = configExtractor.getPartitions().size();
        
        // 获取MR类型数量
        List<String> mrTypes = MRFactory_utils.getAllRelationIds();
        int mrCount = mrTypes.size();
        
        // 计算类别数
        int totalCategories = partitionCount * mrCount;
        
        // 推荐域大小（每类最少minSamplesPerCategory，考虑安全系数1.5）
        int suggestedSize = (int) (minSamplesPerCategory * totalCategories * 1.5);
        
        // 最小1000，最大3000
        return Math.min(Math.max(suggestedSize, 5000), 5000);
    }

    /**
     * 使用默认参数计算域大小
     */
    public int calculateDomainSize() {
        return calculateDomainSize(5);
    }

    /**
     * 输出MR × 分区交叉表，缺失处自动填0，保证每行每列长度一致
     * 
     * @param mgDomain 蜕变组域
     * @return 统计信息的字符串表示
     */
    public String getDomainStatistics(List<MetamorphicGroup> mgDomain) {
        if (mgDomain == null || mgDomain.isEmpty()) {
            return "蜕变组域为空，无统计信息可用";
        }

        // 从配置获取所有MR类型
        List<String> mrList = allMRTypes;
        
        // 从域中获取分区
        Set<Integer> partitionSet = new HashSet<>();
        for (MetamorphicGroup mg : mgDomain) {
            partitionSet.add(mg.getSourceTest().getPartitionId());
        }
        List<Integer> partitionList = partitionSet.stream().sorted().collect(Collectors.toList());

        // 构造交叉表 {(mr_id, partition_id): count}
        Map<String, Integer> crossTable = new HashMap<>();
        for (MetamorphicGroup mg : mgDomain) {
            String mrId = mg.getMRId();
            int partitionId = mg.getSourceTest().getPartitionId();
            String key = mrId + "_" + partitionId;
            crossTable.put(key, crossTable.getOrDefault(key, 0) + 1);
        }

        // 构建统计报告
        List<String> stats = new ArrayList<>();
        
        // 表头
        List<String> header = new ArrayList<>();
        header.add("MR \\ 分区");
        for (Integer pid : partitionList) {
            header.add("P" + pid);
        }
        header.add("合计");
        stats.add("| " + String.join(" | ", header) + " |");
        
        // 分隔线
        List<String> separator = new ArrayList<>();
        for (int i = 0; i < header.size(); i++) {
            separator.add("---");
        }
        stats.add("|" + String.join("|", separator) + "|");

        // 行：每个MR
        Map<String, Integer> totalPerRow = new HashMap<>();
        Map<Integer, Integer> totalPerCol = new HashMap<>();
        for (Integer pid : partitionList) {
            totalPerCol.put(pid, 0);
        }
        int grandTotal = 0;
        
        for (String mrId : mrList) {
            List<String> row = new ArrayList<>();
            row.add(mrId);
            int rowTotal = 0;
            
            for (Integer pid : partitionList) {
                String key = mrId + "_" + pid;
                int count = crossTable.getOrDefault(key, 0);
                row.add(String.valueOf(count));
                rowTotal += count;
                totalPerCol.put(pid, totalPerCol.get(pid) + count);
                grandTotal += count;
            }
            
            row.add(String.valueOf(rowTotal));
            totalPerRow.put(mrId, rowTotal);
            stats.add("| " + String.join(" | ", row) + " |");
        }

        // 最后一行：各分区合计
        List<String> totalRow = new ArrayList<>();
        totalRow.add("合计");
        for (Integer pid : partitionList) {
            totalRow.add(String.valueOf(totalPerCol.get(pid)));
        }
        totalRow.add(String.valueOf(grandTotal));
        stats.add("| " + String.join(" | ", totalRow) + " |");

        return String.join("\n", stats);
    }

    /**
     * 获取域大小
     */
    public int getDomainSize() {
        return domainSize;
    }

    /**
     * 获取配置路径
     */
    public String getConfigPath() {
        return configPath;
    }

    /**
     * 获取所有MR类型
     */
    public List<String> getAllMRTypes() {
        return new ArrayList<>(allMRTypes);
    }

    /**
     * 测试代码
     */
    public static void main(String[] args) {
        try {
            MGDomainGenerator_utils generator = new MGDomainGenerator_utils();
            System.out.println("正在生成蜕变组域...");
            List<MetamorphicGroup> mgDomain = generator.generateDomain();
            System.out.println("蜕变组域生成完成");
            System.out.println(generator.getDomainStatistics(mgDomain));
        } catch (IOException e) {
            System.err.println("配置文件读取失败: " + e.getMessage());
        }
    }
}