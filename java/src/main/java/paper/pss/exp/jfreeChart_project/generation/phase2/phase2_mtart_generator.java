package paper.pss.exp.jfreeChart_project.generation.phase2;

import paper.pss.exp.jfreeChart_project.model.MetamorphicGroup;
import paper.pss.exp.jfreeChart_project.model.TestCase;
import paper.pss.exp.jfreeChart_project.utils.jfreeConfigExtractor_utils;
import paper.pss.exp.jfreeChart_project.utils.MGDomainGenerator_utils;

import java.awt.geom.Line2D;
import java.io.IOException;
import java.util.*;

/**
 * MT-ART生成器，基于论文策略1（优先平均距离+内部距离）实现，包含分区逻辑和全点对距离计算
 * 针对createLineRegion函数的线段测试用例进行优化
 */
public class phase2_mtart_generator {
    private final jfreeConfigExtractor_utils configExtractor;
    private final Random random;
    private final List<MetamorphicGroup> mgDomain;

    /**
     * 构造函数
     * 
     * @param configPath 配置文件路径
     * @param mgDomain   蜕变组域
     */
    public phase2_mtart_generator(String configPath, List<MetamorphicGroup> mgDomain) throws IOException {
        this.configExtractor = new jfreeConfigExtractor_utils(configPath);
        this.random = new Random();
        this.mgDomain = mgDomain;
    }

    /**
     * 默认构造函数，使用默认配置路径
     * 
     * @param mgDomain 蜕变组域
     */
    public phase2_mtart_generator(List<MetamorphicGroup> mgDomain) throws IOException {
        this("src/main/java/paper/pss/exp/jfreeChart_project/jfreeChart_config.json", mgDomain);
    }

    /**
     * 从TestCase中提取线段的中点坐标和宽度作为特征向量
     * 
     * @param testCase 测试用例
     * @return 特征向量 [midX, midY, width]
     */
    private double[] extractFeatures(TestCase testCase) {
        Line2D line = testCase.getLine();
        float width = testCase.getWidth();
        
        // 计算线段中点坐标
        double midX = (line.getX1() + line.getX2()) / 2.0;
        double midY = (line.getY1() + line.getY2()) / 2.0;
        
        return new double[]{midX, midY, width};
    }

    /**
     * 基于source_test的特征创建等距分区（参考Python版本的统一分区策略）
     * 
     * @param mgDomain      蜕变组域
     * @param numPartitions 分区数量
     * @return 分区列表，每个分区包含对应的MetamorphicGroup
     */
    private List<List<MetamorphicGroup>> createPartitions(List<MetamorphicGroup> mgDomain, int numPartitions) {
        if (mgDomain.isEmpty() || numPartitions <= 0) {
            return new ArrayList<>();
        }

        // 获取所有source_test的中点X坐标作为分区依据
        List<Double> values = new ArrayList<>();
        for (MetamorphicGroup mg : mgDomain) {
            double[] features = extractFeatures(mg.getSourceTest());
            values.add(features[0]); // 使用midX作为分区依据
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
            double value = features[0]; // midX
            int idx = (int) ((value - minVal) / step);
            // 处理边界情况
            idx = Math.min(idx, numPartitions - 1);
            partitions.get(idx).add(mg);
        }

        return partitions;
    }

    /**
     * 从未覆盖的分区中选择候选集（参考Python版本的两段随机选择）
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
     * 计算MG的综合分数（参考Python版本，只计算与最近5个测试用例的距离）
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
     * 主生成流程，采用统一分区划分和选择策略（参考Python版本）
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

        // 统一创建分区（参考Python版本：num_partitions = max(1, int(num_samples * 1.5))）
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
     * 辅助类：候选结果，包含候选MG列表和对应的分区索引
     */
    private static class CandidateResult {
        List<MetamorphicGroup> candidates;
        List<Integer> partitionIndices;

        CandidateResult(List<MetamorphicGroup> candidates, List<Integer> partitionIndices) {
            this.candidates = candidates;
            this.partitionIndices = partitionIndices;
        }
    }

    /**
     * 测试代码
     */
    public static void main(String[] args) {
        try {
            System.out.println("开始生成分区");
            MGDomainGenerator_utils generator = new MGDomainGenerator_utils();
            List<MetamorphicGroup> mgDomain = generator.generateDomain();
            System.out.println("分区生成完成，总候选数: " + mgDomain.size());

            phase2_mtart_generator sampler = new phase2_mtart_generator(mgDomain);

            // 生成50个样本
            List<MetamorphicGroup> cases = sampler.generate(50);
            
            System.out.println(sampler.getPartitionStatistics(cases));
            
            System.out.println("\nMT-ART测试用例:");
            for (int i = 0; i < Math.min(5, cases.size()); i++) {
                MetamorphicGroup mg = cases.get(i);
                System.out.println(String.format("MT-ART测试用例 #%d: MR=%s, 源测试=%s, 后续测试=%s", 
                    i + 1, 
                    mg.getMRId(), 
                    mg.getSourceTest().toString(), 
                    mg.getFollowupTest().toString()));
            }
            
            System.out.println(String.format("\n总共从%d个MG中采样了%d个样本", 
                sampler.getDomainSize(), cases.size()));
                
        } catch (IOException e) {
            System.err.println("初始化失败: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("生成过程中出错: " + e.getMessage());
            e.printStackTrace();
        }
    }
}