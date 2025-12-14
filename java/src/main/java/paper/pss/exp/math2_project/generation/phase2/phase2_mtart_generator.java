package paper.pss.exp.math2_project.generation.phase2;

import paper.pss.exp.math2_project.model.MetamorphicGroup;
import paper.pss.exp.math2_project.model.TestCase;
import paper.pss.exp.math2_project.utils.math2ConfigExtractor_utils;
import paper.pss.exp.math2_project.utils.MGDomainGenerator_utils;

import java.io.IOException;
import java.util.*;

/**
 * MT-ART生成器，基于论文策略1（优先平均距离+内部距离）实现，包含分区逻辑和全点对距离计算
 */
public class phase2_mtart_generator {
    private final math2ConfigExtractor_utils configExtractor;
    private final Random random;
    private final List<MetamorphicGroup> mgDomain;

    /**
     * 构造函数
     * 
     * @param mgDomain   蜕变组域
     * @param configPath 配置文件路径
     */
    public phase2_mtart_generator(String configPath, List<MetamorphicGroup> mgDomain) throws IOException {
        this.configExtractor = new math2ConfigExtractor_utils(configPath);
        this.random = new Random();
        this.mgDomain = mgDomain;
    }

    /**
     * 默认构造函数，使用默认配置路径
     * 
     * @param mgDomain 蜕变组域
     */
    public phase2_mtart_generator(List<MetamorphicGroup> mgDomain) throws IOException {
        this("java/src/main/java/paper/pss/exp/math2_project/math2_config.json", mgDomain);
    }

    /**
     * 从TestCase中提取数值（适配math2项目的双参数结构）
     * 
     * @param testCase 测试用例
     * @return 数值数组 [magnitude, sign]
     */
    private double[] extractValues(TestCase testCase) {
        return new double[]{(double) testCase.getMagnitude(), (double) testCase.getSign()};
    }

    /**
     * 计算两个数值数组之间的欧几里得距离
     * 
     * @param values1 第一个数值数组
     * @param values2 第二个数值数组
     * @return 欧几里得距离
     */
    private double calculateDistance(double[] values1, double[] values2) {
        double sum = 0.0;
        for (int i = 0; i < values1.length; i++) {
            double diff = values1[i] - values2[i];
            sum += diff * diff;
        }
        return Math.sqrt(sum);
    }

    /**
     * 基于source_test值创建等距分区（参考Python版本的统一分区策略）
     * 
     * @param mgDomain      蜕变组域
     * @param numPartitions 分区数量
     * @return 分区列表，每个分区包含对应的MetamorphicGroup
     */
    private List<List<MetamorphicGroup>> createPartitions(List<MetamorphicGroup> mgDomain, int numPartitions) {
        if (mgDomain.isEmpty() || numPartitions <= 0) {
            return new ArrayList<>();
        }

        // 获取所有source_test的magnitude值用于分区
        List<Double> magnitudes = new ArrayList<>();
        for (MetamorphicGroup mg : mgDomain) {
            magnitudes.add((double) mg.getSourceTest().getMagnitude());
        }

        double minVal = Collections.min(magnitudes);
        double maxVal = Collections.max(magnitudes);
        double step = numPartitions > 1 ? (maxVal - minVal) / numPartitions : 1.0;

        // 初始化分区
        List<List<MetamorphicGroup>> partitions = new ArrayList<>();
        for (int i = 0; i < numPartitions; i++) {
            partitions.add(new ArrayList<>());
        }

        // 将MG分配到对应分区
        for (MetamorphicGroup mg : mgDomain) {
            double magnitude = mg.getSourceTest().getMagnitude();
            int idx = (int) ((magnitude - minVal) / step);
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
     * 计算MG的综合分数（参考Python版本，只计算与最近5个测试用例的距离）
     * 
     * @param mg          候选MG
     * @param recentCoords 最近选择的测试用例坐标（最多5个）
     * @return 综合分数
     */
    private double computeScore(MetamorphicGroup mg, List<double[]> recentCoords) {
        if (recentCoords.isEmpty()) {
            return Double.POSITIVE_INFINITY;
        }

        double[] stcValues = extractValues(mg.getSourceTest());
        double[] ftcValues = extractValues(mg.getFollowupTest());
        
        // 计算与最近选择的MG的平均距离
        double totalDist = 0.0;
        for (double[] coord : recentCoords) {
            // 使用曼哈顿距离（参考Python版本）
            double dist = Math.abs(stcValues[0] - coord[0]) + Math.abs(stcValues[1] - coord[1]) +
                         Math.abs(ftcValues[0] - coord[2]) + Math.abs(ftcValues[1] - coord[3]);
            totalDist += dist;
        }
        double avgDist = totalDist / recentCoords.size();
        
        // 内部距离（权重较小）
        double internalDist = calculateDistance(stcValues, ftcValues);
        
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
        List<double[]> recentCoords = new ArrayList<>(); // 维护最近5个坐标
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
                double score = computeScore(candidate, recentCoords);
                
                if (score > bestScore) {
                    bestScore = score;
                    bestMG = candidate;
                    bestPartitionIdx = partitionIndices.get(i);
                }
            }

            // 添加最佳候选到结果
            if (bestMG != null) {
                selected.add(bestMG);
                
                // 添加到最近坐标列表，保持最多5个
                double[] stcValues = extractValues(bestMG.getSourceTest());
                double[] ftcValues = extractValues(bestMG.getFollowupTest());
                double[] coord = {stcValues[0], stcValues[1], ftcValues[0], ftcValues[1]};
                recentCoords.add(coord);
                if (recentCoords.size() > 5) {
                    recentCoords.remove(0); // 移除最旧的坐标
                }
                
                coveredPartitions.add(bestPartitionIdx);
            }
        }

        return selected;
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
     * 主函数，用于测试
     */
    public static void main(String[] args) {
        try {
            System.out.println("开始生成分区");
            MGDomainGenerator_utils generator = new MGDomainGenerator_utils();
            List<MetamorphicGroup> mgDomain = generator.generateDomain();
            System.out.println("分区生成完成，总候选数: " + mgDomain.size());

            phase2_mtart_generator sampler = new phase2_mtart_generator(mgDomain);

            // 生成50个样本，每次迭代采样50个候选
            List<MetamorphicGroup> cases = sampler.generate(50);
            for (int i = 0; i < Math.min(5, cases.size()); i++) {
                MetamorphicGroup mg = cases.get(i);
                System.out.printf("MT-ART样本 #%d: STC=(%d,%d), FTC=(%d,%d)%n",
                        i + 1,
                        mg.getSourceTest().getMagnitude(),
                        mg.getSourceTest().getSign(),
                        mg.getFollowupTest().getMagnitude(),
                        mg.getFollowupTest().getSign());
            }
        } catch (IOException e) {
            System.err.println("初始化失败: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("生成过程中出错: " + e.getMessage());
            e.printStackTrace();
        }
    }
}