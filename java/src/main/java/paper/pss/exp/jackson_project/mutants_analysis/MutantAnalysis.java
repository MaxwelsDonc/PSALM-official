package paper.pss.exp.jackson_project.mutants_analysis;

import java.io.*;
import java.lang.reflect.Method;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

// 导入测试用例生成相关类
import paper.pss.exp.jackson_project.generation.phase1.phase1_random_generator;
import paper.pss.exp.jackson_project.model.TestCase;
import paper.pss.exp.jackson_project.model.MetamorphicGroup;

// 蜕变关系 导入
import paper.pss.exp.jackson_project.model.MetamorphicRelation;
import paper.pss.exp.jackson_project.utils.MRFactory_utils;

/**
 * 简化版突变体分析器
 * 主要优化：
 * 1. 直接通过类路径加载突变体，无需临时编译
 * 2. 批量执行和缓存结果
 * 3. 统一的数据结构
 * 4. 简化的报告生成
 */
public class MutantAnalysis {

    private static final int TIMEOUT_SECONDS = 3;
    private static final String MUTANTS_PACKAGE = "paper.pss.exp.jackson_project.mutants";

    // 统一的数据结构
    private List<String> mutantNames;
    private Map<String, MutantResult> mutantResults;
    private List<TestCase> testCases;

    public MutantAnalysis() {
        this.mutantNames = new ArrayList<>();
        this.mutantResults = new HashMap<>();
        this.testCases = new ArrayList<>();
    }

    /**
     * 突变体结果封装类
     */
    public static class MutantResult {
        public final Map<String, Object> results = new HashMap<>(); // 测试输入 -> 结果
        public final Map<String, String> statuses = new HashMap<>(); // 测试输入 -> 状态
        public final Set<String> killedBy = new HashSet<>(); // 被哪些测试用例kill
        public final Set<String> subsumedBy = new HashSet<>(); // 被哪些突变体包含
        public final Set<String> subsumes = new HashSet<>(); // 包含哪些突变体
        public MutantType type = MutantType.NORMAL;

        public enum MutantType {
            NORMAL, EQUIVALENT, SUBSUMED, ALLKILLED, ERROR, TIMEOUT
        }
    }

    /**
     * 生成测试用例（使用随机生成器）
     * 
     * @param count 要生成的测试用例数量
     */
    public void generateTestCases(int count) {
        // 使用random_generator生成随机测试用例
        phase1_random_generator generator = new phase1_random_generator();
        testCases = generator.generate(count);
    }

    /**
     * 发现并加载突变体（直接通过类路径）
     */
    public void loadMutants() {
        mutantNames.clear();
        // 扫描mutant目录
        Path mutantsPath = Paths.get("src/main/java/paper/pss/exp/jackson_project/mutants");

        try {
            List<String> mutants = Files.list(mutantsPath)
                    .filter(Files::isDirectory)
                    .map(path -> path.getFileName().toString())
                    .filter(name -> (name.startsWith("caseStudy") || name.startsWith("mutant")) && !name.contains("_"))
                    .sorted((a, b) -> {
                        try {
                            int numA = Integer.parseInt(a.substring(6));
                            int numB = Integer.parseInt(b.substring(6));
                            return Integer.compare(numA, numB);
                        } catch (NumberFormatException e) {
                            return a.compareTo(b);
                        }
                    })
                    .collect(Collectors.toList());
            mutantNames.addAll(mutants);
            System.out.println("发现 " + mutantNames.size() + " 个突变体: " + mutants);

        } catch (Exception e) {
            System.err.println("加载突变体失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 批量执行所有测试（核心优化）
     */
    public void executeAllTests() {
        // 确保测试用例和突变体已加载
        if (testCases.isEmpty()) {
            System.out.println("警告：没有测试用例，使用默认生成方法");
        }
        if (mutantNames.isEmpty()) {
            System.out.println("警告：没有突变体，尝试加载突变体");
            loadMutants();
        }
        // 2. 批量执行突变体
        System.out.println("批量执行突变体测试...");
        for (String mutantName : mutantNames) {
            MutantResult result = new MutantResult();
            mutantResults.put(mutantName, result);

            try {
                // 直接通过类名加载
                String className = MUTANTS_PACKAGE + "." + mutantName + ".parseInt";
                Class<?> mutantClass = Class.forName(className);
                Method mutantMethod = mutantClass.getMethod("parseInt", String.class);

                // 执行所有测试用例
                for (TestCase testCase : testCases) {
                    // 执行突变体
                    Object sourceResult = executeWithTimeout(mutantMethod, testCase);
                    // 得到所有的蜕变关系
                    List<MetamorphicRelation> relations = MRFactory_utils.getApplicableRelations(testCase);
                    // 随机选择一个蜕变关系
                    MetamorphicRelation relation = relations.get(new Random().nextInt(relations.size()));
                    // 从选择的蜕变关系中生成所有的MG
                    List<MetamorphicGroup> groups = relation.createGroups(testCase);
                    // 随机选择一个MG
                    MetamorphicGroup group = groups.get(new Random().nextInt(groups.size()));
                    // 得到后续测试用例的输出
                    Object followupResult = executeWithTimeout(mutantMethod, group.getFollowupTest());
                    if ((sourceResult instanceof TimeoutException) || (followupResult instanceof TimeoutException)) {
                        result.statuses.put(testCase.getInput(), "timeout");
                        result.type = MutantResult.MutantType.TIMEOUT;
                    } else if ((sourceResult instanceof Exception) || (followupResult instanceof Exception)) {
                        result.statuses.put(testCase.getInput(), "error");
                        result.results.put(testCase.getInput(), " exception error");
                        result.type = MutantResult.MutantType.ERROR;

                        // followupResult = executeWithoutTimeout(mutantMethod, group.getFollowupTest());

                    } else {
                        result.statuses.put(testCase.getInput(), "success");
                        result.results.put(testCase.getInput(), sourceResult);
                        // 验证 group是否满足蜕变关系
                        if (sourceResult instanceof Integer && followupResult instanceof Integer) {
                            if (!relation.verifyRelation(group.getSourceTest(), group.getFollowupTest(),
                                    (Integer) sourceResult, (Integer) followupResult, "", "")) {
                                result.killedBy.add(testCase.getInput());
                            }
                        }
                    }
                }

                System.out.println(mutantName + ": " + result.killedBy.size() + " kills");

            } catch (ClassNotFoundException e) {
                System.err.println("无法加载突变体: " + mutantName);
                result.type = MutantResult.MutantType.ERROR;
            } catch (Exception e) {
                System.err.println("突变体执行失败: " + mutantName + " -> " + e.getMessage());
                result.type = MutantResult.MutantType.ERROR;
            }
        }
    }

    /**
     * 带超时的执行方法
     */
    private Object executeWithTimeout(Method method, TestCase input) {
        String input_str = input.getInput();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<Object> future = executor.submit(() -> method.invoke(null, input_str));
            return future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            return e;
        } catch (Exception e) {
            return e;
        } finally {
            executor.shutdownNow();
        }
    }

    /**
     * 不带超时的执行方法，方便调试
     */
    private Object executeWithoutTimeout(Method method, TestCase input) {
        String input_str = input.getInput();
        try {
            // 直接调用反射方法
            return method.invoke(null, input_str);
        } catch (Exception e) {
            // 捕获并打印异常，方便调试
            System.err.println("执行反射方法时出错：" + e.getMessage());
            e.printStackTrace(); // 打印堆栈跟踪，帮助定位问题
            return e;
        }
    }

    /**
     * 分析突变体类型和包含关系
     */
    public void analyzeMutants() {
        System.out.println("分析突变体类型...");

        // 第一步：基于killedBy集合判断突变体类型
        for (String mutantName : mutantNames) {
            MutantResult result = mutantResults.get(mutantName);

            if (result.type == MutantResult.MutantType.NORMAL) {
                // 检查是否为等价突变体
                if (result.killedBy.isEmpty()) {
                    result.type = MutantResult.MutantType.EQUIVALENT;
                }
                // 检查是否被所有测试用例杀死
                else if (result.killedBy.size() == testCases.size()) {
                    result.type = MutantResult.MutantType.ALLKILLED;
                }
            }
        }

        // 第二步：多轮迭代检查包含关系
        System.out.println("检查包含关系...");
        boolean foundNewSubsumption;
        int iteration = 0;
        
        do {
            iteration++;
            System.out.println("包含关系检查第 " + iteration + " 轮...");
            foundNewSubsumption = false;
            
            for (int i = 0; i < mutantNames.size(); i++) {
                String mutantA = mutantNames.get(i);
                MutantResult resultA = mutantResults.get(mutantA);
                
                // 跳过已经确定为等价、错误或超时的突变体
                if (resultA.type == MutantResult.MutantType.EQUIVALENT ||
                    resultA.type == MutantResult.MutantType.ERROR ||
                    resultA.type == MutantResult.MutantType.TIMEOUT) {
                    continue;
                }
                
                for (int j = 0; j < mutantNames.size(); j++) {
                    if (i == j) continue;
                    
                    String mutantB = mutantNames.get(j);
                    MutantResult resultB = mutantResults.get(mutantB);
                    
                    // 跳过已经确定为等价、错误或超时的突变体
                    if (resultB.type == MutantResult.MutantType.EQUIVALENT ||
                        resultB.type == MutantResult.MutantType.ERROR ||
                        resultB.type == MutantResult.MutantType.TIMEOUT) {
                        continue;
                    }
                    
                    // 检查包含关系：如果A的所有kill都被B包含，且A不为空，则A包含B
                    if (!resultA.killedBy.isEmpty() && 
                        resultB.killedBy.containsAll(resultA.killedBy) &&
                        resultA.killedBy.size() < resultB.killedBy.size()) {
                        
                        // 如果B之前不是SUBSUMED类型，现在变成SUBSUMED
                        if (resultB.type != MutantResult.MutantType.SUBSUMED) {
                            resultB.type = MutantResult.MutantType.SUBSUMED;
                            foundNewSubsumption = true;
                        }
                        // 如果B之前已经是SUBSUMED类型，但发现了新的包含关系
                        else if (resultB.type == MutantResult.MutantType.SUBSUMED && 
                                !resultB.subsumedBy.contains(mutantA)) {
                            foundNewSubsumption = true;
                        }
                        
                        resultB.subsumedBy.add(mutantA);
                        resultA.subsumes.add(mutantB);
                        
                        System.out.println("发现包含关系: " + mutantA + " 包含 " + mutantB + 
                                         " (" + mutantA + " kill_count: " + resultA.killedBy.size() + 
                                         ", " + mutantB + " kill_count: " + resultB.killedBy.size() + ")");
                    }
               }
           }
      } while (foundNewSubsumption); // 直到没有新的包含关系被发现
      
      System.out.println("包含关系检查完成，共进行了 " + iteration + " 轮");
  }

    /**
     * 计算最大独立集
     */
    public List<String> calculateMaximumIndependentSet() {
        // 获取所有NORMAL类型的突变体
        List<String> normalMutants = new ArrayList<>();
        for (String mutantName : mutantNames) {
            MutantResult result = mutantResults.get(mutantName);
            if (result.type == MutantResult.MutantType.NORMAL) {
                normalMutants.add(mutantName);
            }
        }
        
        if (normalMutants.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 使用贪心算法计算最大独立集
        return findMaximumIndependentSetGreedy(normalMutants);
    }
    
    /**
     * 使用贪心算法寻找最大独立集
     */
    private List<String> findMaximumIndependentSetGreedy(List<String> candidates) {
        List<String> independentSet = new ArrayList<>();
        Set<String> remaining = new HashSet<>(candidates);
        
        while (!remaining.isEmpty()) {
            // 找到度数最小的节点（被包含关系最少的突变体）
            String minDegreeNode = null;
            int minDegree = Integer.MAX_VALUE;
            
            for (String mutant : remaining) {
                MutantResult result = mutantResults.get(mutant);
                int degree = 0;
                
                // 计算与其他剩余节点的连接数（包含关系）
                for (String other : remaining) {
                    if (!mutant.equals(other)) {
                        MutantResult otherResult = mutantResults.get(other);
                        // 如果存在包含关系，则它们之间有边
                        if (result.subsumedBy.contains(other) || result.subsumes.contains(other) ||
                            otherResult.subsumedBy.contains(mutant) || otherResult.subsumes.contains(mutant)) {
                            degree++;
                        }
                    }
                }
                
                if (degree < minDegree) {
                    minDegree = degree;
                    minDegreeNode = mutant;
                }
            }
            
            if (minDegreeNode != null) {
                // 将度数最小的节点加入独立集
                independentSet.add(minDegreeNode);
                remaining.remove(minDegreeNode);
                
                // 移除所有与该节点相邻的节点
                MutantResult selectedResult = mutantResults.get(minDegreeNode);
                Set<String> toRemove = new HashSet<>();
                
                for (String other : remaining) {
                    MutantResult otherResult = mutantResults.get(other);
                    // 如果存在包含关系，则移除相邻节点
                    if (selectedResult.subsumedBy.contains(other) || selectedResult.subsumes.contains(other) ||
                        otherResult.subsumedBy.contains(minDegreeNode) || otherResult.subsumes.contains(minDegreeNode)) {
                        toRemove.add(other);
                    }
                }
                
                remaining.removeAll(toRemove);
            } else {
                break;
            }
        }
        
        return independentSet;
    }

    /**
     * 生成JSON格式的分析报告
     */
    public void generateReport(String filename) {
        analyzeMutants();

        // 统计各类型突变体数量
        Map<MutantResult.MutantType, List<String>> typeGroups = new HashMap<>();
        for (MutantResult.MutantType type : MutantResult.MutantType.values()) {
            typeGroups.put(type, new ArrayList<>());
        }

        for (String mutantName : mutantNames) {
            MutantResult result = mutantResults.get(mutantName);
            typeGroups.get(result.type).add(mutantName);
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode report = mapper.createObjectNode();

            // 添加统计信息
            ObjectNode statistics = mapper.createObjectNode();
            statistics.put("total_mutants", mutantNames.size());
            statistics.put("normal_mutants", typeGroups.get(MutantResult.MutantType.NORMAL).size());
            statistics.put("equivalent_mutants", typeGroups.get(MutantResult.MutantType.EQUIVALENT).size());
            statistics.put("subsumed_mutants", typeGroups.get(MutantResult.MutantType.SUBSUMED).size());
            statistics.put("allkilled_mutants", typeGroups.get(MutantResult.MutantType.ALLKILLED).size());
            statistics.put("timeout_mutants", typeGroups.get(MutantResult.MutantType.TIMEOUT).size());
            statistics.put("error_mutants", typeGroups.get(MutantResult.MutantType.ERROR).size());
            
            int effective = typeGroups.get(MutantResult.MutantType.NORMAL).size();
            double score = mutantNames.size() > 0 ? (double) effective / mutantNames.size() * 100 : 0;
            statistics.put("mutation_score", Math.round(score * 100.0) / 100.0);
            
            report.set("statistics", statistics);

            // 添加包含关系信息
            ArrayNode subsumptionRelations = mapper.createArrayNode();
            for (String mutantName : mutantNames) {
                MutantResult result = mutantResults.get(mutantName);
                if (!result.subsumes.isEmpty()) {
                    for (String subsumed : result.subsumes) {
                        ObjectNode relation = mapper.createObjectNode();
                        relation.put("subsumer", mutantName);
                        relation.put("subsumed", subsumed);
                        subsumptionRelations.add(relation);
                    }
                }
            }
            report.set("subsumption_relations", subsumptionRelations);

            // 计算并添加最大独立集信息
            List<String> maxIndependentSet = calculateMaximumIndependentSet();
            ObjectNode independentSetInfo = mapper.createObjectNode();
            independentSetInfo.put("size", maxIndependentSet.size());
            ArrayNode independentSetArray = mapper.createArrayNode();
            for (String mutant : maxIndependentSet) {
                independentSetArray.add(mutant);
            }
            independentSetInfo.set("mutants", independentSetArray);
            report.set("maximum_independent_set", independentSetInfo);

            // 按类型分组突变体
            ObjectNode mutantsByType = mapper.createObjectNode();
            for (MutantResult.MutantType type : MutantResult.MutantType.values()) {
                List<String> mutants = typeGroups.get(type);
                ArrayNode mutantArray = mapper.createArrayNode();
                for (String mutant : mutants) {
                    mutantArray.add(mutant);
                }
                mutantsByType.set(type.toString().toLowerCase(), mutantArray);
            }
            report.set("mutants_by_type", mutantsByType);

            // 保存或输出报告
            if (filename != null && !filename.isEmpty()) {
                try (FileWriter writer = new FileWriter(filename)) {
                    mapper.writerWithDefaultPrettyPrinter().writeValue(writer, report);
                    System.out.println("JSON报告已保存到: " + filename);
                } catch (IOException e) {
                    System.err.println("保存JSON报告失败: " + e.getMessage());
                }
            } else {
                String jsonString = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(report);
                System.out.println(jsonString);
            }

        } catch (Exception e) {
            System.err.println("生成JSON报告时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 保存结果到CSV（修改版 - 纵轴为测试用例，横轴为突变体）
     */
    public void saveResults(String filename) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            // 表头 - 第一列是TestCase，后面每两列是一个突变体的result和status
            writer.print("TestCase");
            for (String mutantName : mutantNames) {
                writer.print("," + mutantName + "_result," + mutantName + "_status");
            }
            writer.println();

            // 数据 - 每行是一个测试用例，每列是对应突变体的结果
            for (TestCase testCase : testCases) {
                String input = testCase.getInput();

                writer.print("\"" + input + "\"");

                for (String mutantName : mutantNames) {
                    MutantResult result = mutantResults.get(mutantName);
                    Object res = result.results.get(input);
                    String status = result.statuses.get(input);
                    writer.print(",\"" + (res != null ? res : "") + "\"," + (status != null ? status : "unknown"));
                }
                writer.println();
            }

            // 额外添加一行突变体信息（类型和被杀死次数）
            writer.print("MutantType");
            for (String mutantName : mutantNames) {
                MutantResult result = mutantResults.get(mutantName);
                writer.print("," + result.type + ",");
            }
            writer.println();

            writer.print("KillCount");
            for (String mutantName : mutantNames) {
                MutantResult result = mutantResults.get(mutantName);
                writer.print("," + result.killedBy.size() + ",");
            }
            writer.println();

            System.out.println("结果已保存到: " + filename);
        } catch (IOException e) {
            System.err.println("保存失败: " + e.getMessage());
        }
    }

    /**
     * 主函数
     * 可以通过命令行参数指定测试用例数量，例如：java MutantAnalysis 100
     */
    public static void main(String[] args) {
        try {
            System.out.println("🚀 开始简化版突变体分析...");

            MutantAnalysis analyzer = new MutantAnalysis();
            int testCaseCount = 1000; // 默认值
            System.out.println("将生成 " + testCaseCount + " 个随机测试用例");

            // 生成指定数量的测试用例并执行测试
            analyzer.loadMutants();
            analyzer.generateTestCases(testCaseCount);
            analyzer.executeAllTests();

            // 生成报告
            analyzer.generateReport(
                    "src/main/java/paper/pss/exp/jackson_project/mutants_analysis/mutant_analysis_report.json");

            // 保存结果
            analyzer.saveResults(
                    "src/main/java/paper/pss/exp/jackson_project/mutants_analysis/simplified_mutant_analysis.csv");

            System.out.println("\n🎉 分析完成！");

        } catch (Exception e) {
            System.err.println("执行失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}