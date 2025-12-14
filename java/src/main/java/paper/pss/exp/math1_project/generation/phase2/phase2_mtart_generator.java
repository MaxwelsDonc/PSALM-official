package paper.pss.exp.math1_project.generation.phase2;

import paper.pss.exp.math1_project.model.MetamorphicGroup;
import paper.pss.exp.math1_project.model.TestCase;
import paper.pss.exp.math1_project.utils.Math1ConfigExtractor_utils;
import paper.pss.exp.math1_project.utils.MGDomainGenerator_utils;

import java.io.IOException;
import java.util.*;

/**
 * 候选结果类，用于封装候选MG和对应的分区索引
 */
class CandidateResult {
    public final List<MetamorphicGroup> candidates;
    public final List<Integer> partitionIndices;
    
    public CandidateResult(List<MetamorphicGroup> candidates, List<Integer> partitionIndices) {
        this.candidates = candidates;
        this.partitionIndices = partitionIndices;
    }
}

/**
 * MT-ART生成器，基于多维自适应随机测试策略实现
 * 针对convolve函数的数组测试用例进行优化
 */
public class phase2_mtart_generator {
    private final Math1ConfigExtractor_utils configExtractor;
    private final Random random;
    private final List<MetamorphicGroup> mgDomain;
    
    /**
     * 构造函数
     * 
     * @param configPath 配置文件路径
     * @param mgDomain   蜕变组域
     */
    public phase2_mtart_generator(String configPath, List<MetamorphicGroup> mgDomain) throws IOException {
        this.configExtractor = new Math1ConfigExtractor_utils(configPath);
        this.random = new Random();
        this.mgDomain = mgDomain;
    }
    
    /**
     * 默认构造函数，使用默认配置路径
     * 
     * @param mgDomain 蜕变组域
     */
    public phase2_mtart_generator(List<MetamorphicGroup> mgDomain) throws IOException {
        this("src/main/java/paper/pss/exp/math1_project/math1_config.json", mgDomain);
    }
    
    /**
     * 从TestCase中提取数组特征作为特征向量
     * 
     * @param testCase 测试用例
     * @return 特征向量 [x_length, h_length, x_avg, h_avg, x_var, h_var]
     */
    private double[] extractFeatures(TestCase testCase) {
        double[] x = testCase.getX();
        double[] h = testCase.getH();
        
        // 计算数组长度
        double xLength = x.length;
        double hLength = h.length;
        
        // 计算数组平均值
        double xAvg = calculateAverage(x);
        double hAvg = calculateAverage(h);
        
        // 计算数组方差
        double xVar = calculateVariance(x);
        double hVar = calculateVariance(h);
        
        return new double[]{xLength, hLength, xAvg, hAvg, xVar, hVar};
    }
    
    /**
     * 基于source_test的特征创建等距分区
     * 
     * @param mgDomain      蜕变组域
     * @param numPartitions 分区数量
     * @return 分区列表，每个分区包含对应的MetamorphicGroup
     */
    private List<List<MetamorphicGroup>> createPartitions(List<MetamorphicGroup> mgDomain, int numPartitions) {
        if (mgDomain.isEmpty() || numPartitions <= 0) {
            return new ArrayList<>();
        }

        // 获取所有source_test的数组长度和作为分区依据
        List<Double> values = new ArrayList<>();
        for (MetamorphicGroup mg : mgDomain) {
            double[] features = extractFeatures(mg.getSourceTest());
            values.add(features[0] + features[1]); // 使用x和h的长度和作为分区依据
        }

        double minVal = Collections.min(values);
        double maxVal = Collections.max(values);
        double step = numPartitions > 1 ? (maxVal - minVal) / numPartitions : 1.0;

        // 初始化分区
        List<List<MetamorphicGroup>> partitions = new ArrayList<>();
        for (int i = 0; i < numPartitions; i++) {
            partitions.add(new ArrayList<>());
        }

        // 将MG分配到对应分区
        for (MetamorphicGroup mg : mgDomain) {
            double[] features = extractFeatures(mg.getSourceTest());
            double value = features[0] + features[1]; // x和h的长度和
            int idx = (int) ((value - minVal) / step);
            // 处理边界情况
            idx = Math.min(idx, numPartitions - 1);
            partitions.get(idx).add(mg);
        }

        return partitions;
    }
    
    /**
     * 从未覆盖的分区中选择候选集
     * 
     * @param partitions        所有分区
     * @param coveredPartitions 已覆盖的分区索引
     * @param candidatesPerIter 每次迭代的候选数量
     * @return 候选MG列表和对应的分区索引
     */
    private CandidateResult selectCandidates(List<List<MetamorphicGroup>> partitions,
                                              Set<Integer> coveredPartitions,
                                              int candidatesPerIter) {
        List<MetamorphicGroup> candidates = new ArrayList<>();
        List<Integer> partitionIndices = new ArrayList<>();

        // 找到可用的分区（未覆盖且非空）
        List<Integer> availableIndices = new ArrayList<>();
        for (int i = 0; i < partitions.size(); i++) {
            if (!coveredPartitions.contains(i) && !partitions.get(i).isEmpty()) {
                availableIndices.add(i);
            }
        }

        if (availableIndices.isEmpty()) {
            return new CandidateResult(candidates, partitionIndices);
        }

        // 随机选择分区
        Collections.shuffle(availableIndices, random);
        int samplesToTake = Math.min(candidatesPerIter, availableIndices.size());

        for (int i = 0; i < samplesToTake; i++) {
            int partitionIdx = availableIndices.get(i);
            List<MetamorphicGroup> partition = partitions.get(partitionIdx);
            
            // 从该分区随机选择一个MG
            MetamorphicGroup selectedMG = partition.get(random.nextInt(partition.size()));
            candidates.add(selectedMG);
            partitionIndices.add(partitionIdx);
        }

        return new CandidateResult(candidates, partitionIndices);
    }
    
    /**
     * 计算两个特征向量之间的欧几里得距离
     * 
     * @param features1 特征向量1
     * @param features2 特征向量2
     * @return 欧几里得距离
     */
    private double euclideanDistance(double[] features1, double[] features2) {
        double sum = 0.0;
        for (int i = 0; i < Math.min(features1.length, features2.length); i++) {
            double diff = features1[i] - features2[i];
            sum += diff * diff;
        }
        return Math.sqrt(sum);
    }
    
    /**
     * 计算MG的综合分数（只计算与最近5个测试用例的距离）
     * 
     * @param mg          候选MG
     * @param recentFeatures 最近选择的测试用例特征（最多5个）
     * @return 综合分数
     */
    private double computeScore(MetamorphicGroup mg, List<double[][]> recentFeatures) {
        if (recentFeatures.isEmpty()) {
            return Double.POSITIVE_INFINITY;
        }

        double[] stcFeatures = extractFeatures(mg.getSourceTest());
        double[] ftcFeatures = extractFeatures(mg.getFollowupTest());
        
        // 计算与最近选择的MG的平均距离
        double totalDist = 0.0;
        for (double[][] mgFeatures : recentFeatures) {
            double[] recentStc = mgFeatures[0];
            double[] recentFtc = mgFeatures[1];
            
            // 计算source test和followup test的距离
            double stcDist = euclideanDistance(stcFeatures, recentStc);
            double ftcDist = euclideanDistance(ftcFeatures, recentFtc);
            
            // 使用平均距离
            totalDist += (stcDist + ftcDist) / 2.0;
        }
        double avgDist = totalDist / recentFeatures.size();
        
        // 内部距离（source test和followup test之间的距离，权重较小）
        double internalDist = euclideanDistance(stcFeatures, ftcFeatures);
        
        return avgDist + internalDist * 0.001;
    }
    
    /**
     * 主生成流程，采用统一分区划分和选择策略
     * 
     * @param numSamples 需要生成的样本数
     * @return 生成的蜕变组列表
     */
    public List<MetamorphicGroup> generate(int numSamples) {
        int candidatesPerIter = 5;
        if (numSamples > mgDomain.size()) {
            throw new IllegalArgumentException("样本数超过MG域大小");
        }

        if (mgDomain.isEmpty()) {
            return new ArrayList<>();
        }

        // 统一创建分区
        int numPartitions = Math.max(1, (int) (numSamples * 1.5));
        List<List<MetamorphicGroup>> partitions = createPartitions(mgDomain, numPartitions);

        List<MetamorphicGroup> selected = new ArrayList<>();
        List<double[][]> recentFeatures = new ArrayList<>(); // 维护最近5个MG的特征
        Set<Integer> coveredPartitions = new HashSet<>();

        for (int iteration = 0; iteration < numSamples; iteration++) {
            // 选择候选集
            CandidateResult candidateResult = selectCandidates(partitions, coveredPartitions, candidatesPerIter);
            List<MetamorphicGroup> candidates = candidateResult.candidates;
            List<Integer> partitionIndices = candidateResult.partitionIndices;

            if (candidates.isEmpty()) {
                break;
            }

            // 计算每个候选的分数并选择最佳
            MetamorphicGroup bestMG = null;
            double bestScore = Double.NEGATIVE_INFINITY;
            int bestPartitionIdx = -1;

            for (int i = 0; i < candidates.size(); i++) {
                MetamorphicGroup candidate = candidates.get(i);
                double score = computeScore(candidate, recentFeatures);
                
                if (score > bestScore) {
                    bestScore = score;
                    bestMG = candidate;
                    bestPartitionIdx = partitionIndices.get(i);
                }
            }

            // 添加最佳候选到结果
            if (bestMG != null) {
                selected.add(bestMG);
                
                // 添加到最近特征列表，保持最多5个
                double[][] mgFeatures = {
                    extractFeatures(bestMG.getSourceTest()),
                    extractFeatures(bestMG.getFollowupTest())
                };
                recentFeatures.add(mgFeatures);
                if (recentFeatures.size() > 5) {
                    recentFeatures.remove(0); // 移除最旧的特征
                }
                
                coveredPartitions.add(bestPartitionIdx);
            }
        }

        return selected;
     }
     
    /**
     * 计算数组的平均值
     */
    private double calculateAverage(double[] array) {
        if (array.length == 0) {
            return 0.0;
        }
        double sum = 0.0;
        for (double value : array) {
            sum += value;
        }
        return sum / array.length;
    }
    
    /**
     * 计算数组的方差
     */
    private double calculateVariance(double[] array) {
        if (array.length == 0) {
            return 0.0;
        }
        
        double mean = calculateAverage(array);
        double sum = 0.0;
        
        for (double value : array) {
            double diff = value - mean;
            sum += diff * diff;
        }
        
        return sum / array.length;
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
            
            // 创建MT-ART生成器
            phase2_mtart_generator generator = new phase2_mtart_generator(mgDomain);
            
            // 生成样本
            List<MetamorphicGroup> samples = generator.generate(50);
            
            // 输出统计信息
            System.out.println(generator.getPartitionStatistics(samples));
            
            System.out.println("\n示例MG:");
            for (int i = 0; i < Math.min(3, samples.size()); i++) {
                MetamorphicGroup mg = samples.get(i);
                System.out.println("MT-ART MG #" + (i + 1) + ": 分区 " + mg.getSourceTest().getPartitionId() + 
                        ", MR " + mg.getMRId());
            }
        } catch (IOException e) {
            System.err.println("初始化失败: " + e.getMessage());
        }
    }
}