package paper.pss.exp.lang_project.mutants_analysis;
import paper.pss.exp.lang_project.mutants.mutant1.isSameDayset;

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
import paper.pss.exp.lang_project.generation.phase1.phase1_random_generator;
import paper.pss.exp.lang_project.model.TestCase;
import paper.pss.exp.lang_project.model.MetamorphicGroup;

// 蜕变关系 导入
import paper.pss.exp.lang_project.model.MetamorphicRelation;
import paper.pss.exp.lang_project.utils.MRFactory_utils;

/**
 * 简化版突变体分析器 - Lang项目版本
 * 主要优化：
 * 1. 直接通过类路径加载突变体，无需临时编译
 * 2. 批量执行和缓存结果
 * 3. 统一的数据结构
 * 4. 简化的报告生成
 */
public class MutantAnalysis {

    private static final int TIMEOUT_SECONDS = 3;
    private static final String MUTANTS_PACKAGE = "paper.pss.exp.lang_project.mutants";

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
            NORMAL, EQUIVALENT, SUBSUMED, TRIVIAL, ERROR, TIMEOUT
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
        Path mutantsPath = Paths.get("src/main/java/paper/pss/exp/lang_project/mutants");

        try {
            List<String> mutants = Files.list(mutantsPath)
                    .filter(Files::isDirectory)
                    .map(path -> path.getFileName().toString())
                    .filter(name -> (name.startsWith("caseStudy") || name.startsWith("mutant")) && !name.contains("_"))
                    .sorted((a, b) -> {
                        try {
                            // 处理caseStudy和mutant的排序
                            if (a.equals("caseStudy")) return -1;
                            if (b.equals("caseStudy")) return 1;
                            
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
                String className = MUTANTS_PACKAGE + "." + mutantName + ".isSameDayset";
                Class<?> mutantClass = Class.forName(className);
                Method mutantMethod = mutantClass.getMethod("isSameDay", java.util.Date.class, java.util.Date.class);

                // 执行所有测试用例
                for (TestCase testCase : testCases) {
                    // 执行突变体
                    Object sourceResult = executeWithTimeout(mutantMethod, testCase);
                    // // 执行原始方法作为对比 - 也需要通过executeWithTimeout处理异常
                    // Method originalMethod = isSameDayset.class.getMethod("isSameDay", java.util.Date.class, java.util.Date.class);
                    // Object originalResult = executeWithTimeout(originalMethod, testCase);
                    // 得到所有的蜕变关系
                    List<MetamorphicRelation> relations = MRFactory_utils.getApplicableRelations(testCase);
                    if (relations.isEmpty()) {
                        // 如果没有适用的蜕变关系，跳过这个测试用例
                        continue;
                    }
                    // 随机选择一个蜕变关系
                    MetamorphicRelation relation = relations.get(new Random().nextInt(relations.size()));
                    // 从选择的蜕变关系中生成所有的MG
                    List<MetamorphicGroup> groups = relation.createGroups(testCase);
                    if (groups.isEmpty()) {
                        // 如果没有生成的蜕变组，跳过这个测试用例
                        continue;
                    }
                    // 随机选择一个MG
                    MetamorphicGroup group = groups.get(new Random().nextInt(groups.size()));
                    // 得到后续测试用例的输出
                    Object followupResult = executeWithTimeout(mutantMethod, group.getFollowupTest());
                    
                    String testCaseKey = getTestCaseKey(testCase);
                    
                    if ((sourceResult instanceof TimeoutException) || (followupResult instanceof TimeoutException)) {
                        result.statuses.put(testCaseKey, "timeout");
                        // 不在这里直接设置TIMEOUT类型，在analyzeMutants中统一处理
                    } else if ((sourceResult instanceof IllegalArgumentException) || (followupResult instanceof IllegalArgumentException)) {
                        // IllegalArgumentException（如null参数）视为正常的测试结果，不是错误
                        result.statuses.put(testCaseKey, "success");
                        result.results.put(testCaseKey, sourceResult instanceof IllegalArgumentException ? sourceResult : followupResult);
                        // 对于IllegalArgumentException，不进行蜕变关系验证
                    } else if ((sourceResult instanceof Exception) || (followupResult instanceof Exception)) {
                        result.statuses.put(testCaseKey, "error");
                        result.results.put(testCaseKey, " exception error");
                        // 不在这里直接设置ERROR类型，在analyzeMutants中统一处理
                    } else {
                        result.statuses.put(testCaseKey, "success");
                        result.results.put(testCaseKey, sourceResult);
                        // 验证 group是否满足蜕变关系
                        if (sourceResult instanceof Boolean && followupResult instanceof Boolean) {
                            if (!relation.verifyRelation(group.getSourceTest(), group.getFollowupTest(),
                                    (Boolean) sourceResult, (Boolean) followupResult, "", "")) {
                                result.killedBy.add(testCaseKey);
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
     * 获取测试用例的唯一标识符
     */
    private String getTestCaseKey(TestCase testCase) {
        return testCase.toString();
    }



    /**
     * 带超时的执行方法
     */
    private Object executeWithTimeout(Method method, TestCase testCase) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<Object> future = executor.submit(() -> method.invoke(null, testCase.getDate1(), testCase.getDate2()));
            return future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            return e;
        } catch (Exception e) {
            // 解包嵌套异常，获取真正的根本原因
            Throwable cause = e;
            while (cause.getCause() != null) {
                cause = cause.getCause();
            }
            return cause;
        } finally {
            executor.shutdownNow();
        }
    }

    /**
     * 不带超时的执行方法，方便调试
     */
    private Object executeWithoutTimeout(Method method, TestCase testCase) {
        try {
            // 直接调用反射方法
            return method.invoke(null, testCase.getDate1(), testCase.getDate2());
        } catch (Exception e) {
            // 捕获并打印异常，方便调试
            System.err.println("执行反射方法时出错：" + e.getMessage());
            e.printStackTrace(); // 打印堆栈跟踪，帮助定位问题
            return e;
        }
    }

    /**
     * 分析突变体类型
     */
    public void analyzeMutants() {
        System.out.println("分析突变体类型...");

        for (String mutantName : mutantNames) {
            MutantResult result = mutantResults.get(mutantName);

            if (result.type == MutantResult.MutantType.NORMAL) {
                // 首先检查ERROR和TIMEOUT状态
                int totalTests = result.statuses.size();
                int errorCount = 0;
                int timeoutCount = 0;
                int successCount = 0;
                
                for (String status : result.statuses.values()) {
                    if ("error".equals(status)) {
                        errorCount++;
                    } else if ("timeout".equals(status)) {
                        timeoutCount++;
                    } else if ("success".equals(status)) {
                        successCount++;
                    }
                }
                
                // 调试信息：打印第一个突变体的状态分布
                if (mutantName.equals("mutant1")) {
                    System.out.println("mutant1状态分布: total=" + totalTests + ", error=" + errorCount + ", timeout=" + timeoutCount + ", success=" + successCount);
                }
                
                // 只有当所有测试用例都出错时才标记为ERROR
                if (errorCount == totalTests && totalTests > 0) {
                    result.type = MutantResult.MutantType.ERROR;
                    continue;
                }
                
                // 只有当所有测试用例都超时时才标记为TIMEOUT
                if (timeoutCount == totalTests && totalTests > 0) {
                    result.type = MutantResult.MutantType.TIMEOUT;
                    continue;
                }
                
                // 检查是否为等价突变体
                if (result.killedBy.isEmpty()) {
                    result.type = MutantResult.MutantType.EQUIVALENT;
                }
                // 检查是否被所有测试用例杀死（平凡突变体）
                else if (result.killedBy.size() == testCases.size()) {
                    result.type = MutantResult.MutantType.TRIVIAL;
                }
            }
        }

        // 检查包含关系 - 两轮检查确保完整性
        System.out.println("检查包含关系...");
        for (int round = 1; round <= 2; round++) {
            System.out.println("第" + round + "轮包含关系检查");
            for (int i = 0; i < mutantNames.size(); i++) {
                String mutantA = mutantNames.get(i);
                MutantResult resultA = mutantResults.get(mutantA);
                // 只对NORMAL类型的突变体进行包含关系检查
                if (resultA.type != MutantResult.MutantType.NORMAL)
                    continue;
                for (int j = 0; j < mutantNames.size(); j++) {
                    if (i == j)
                        continue;
                    String mutantB = mutantNames.get(j);
                    MutantResult resultB = mutantResults.get(mutantB);
                    // 只与NORMAL类型的突变体比较
                    if (resultB.type != MutantResult.MutantType.NORMAL)
                        continue;
                    // 如果A包含B（B的所有kill都被A包含，且A的kill数量更少），则A包含B
                    if (!resultB.killedBy.isEmpty() && resultA.killedBy.containsAll(resultB.killedBy) && resultA.killedBy.size() < resultB.killedBy.size()) {
                        resultB.type = MutantResult.MutantType.SUBSUMED;
                        resultB.subsumedBy.add(mutantA);
                        resultA.subsumes.add(mutantB);
                        System.out.println(mutantA + " 包含 " + mutantB);
                        break;
                    }
                }
            }
        }
    }

    /**
     * 计算两两不包含突变体的最大独立集
     */
    private List<String> calculateMaximumIndependentSet() {
        // 构建包含关系图
        Map<String, Set<String>> graph = new HashMap<>();
        for (String mutantName : mutantNames) {
            graph.put(mutantName, new HashSet<>());
        }
        
        // 添加包含关系边
        for (String mutantName : mutantNames) {
            MutantResult result = mutantResults.get(mutantName);
            for (String subsumed : result.subsumes) {
                graph.get(mutantName).add(subsumed);
                graph.get(subsumed).add(mutantName);
            }
        }
        
        // 只考虑NORMAL和SUBSUMED类型的突变体
        List<String> candidateNodes = mutantNames.stream()
                .filter(name -> {
                    MutantResult.MutantType type = mutantResults.get(name).type;
                    return type == MutantResult.MutantType.NORMAL || type == MutantResult.MutantType.SUBSUMED;
                })
                .collect(Collectors.toList());
        
        return findMaximumIndependentSetGreedy(candidateNodes, graph);
    }
    
    /**
     * 使用贪心算法找到最大独立集
     */
    private List<String> findMaximumIndependentSetGreedy(List<String> nodes, Map<String, Set<String>> graph) {
        // 按照节点的度数（连接数）从小到大排序，优先选择度数小的节点
        List<String> sortedNodes = new ArrayList<>(nodes);
        sortedNodes.sort((a, b) -> {
            int degreeA = graph.getOrDefault(a, Collections.emptySet()).size();
            int degreeB = graph.getOrDefault(b, Collections.emptySet()).size();
            return Integer.compare(degreeA, degreeB); // 从小到大排序
        });
        
        List<String> independentSet = new ArrayList<>();
        Set<String> excludedNodes = new HashSet<>();
        
        for (String node : sortedNodes) {
            if (!excludedNodes.contains(node)) {
                // 添加当前节点到独立集
                independentSet.add(node);
                // 排除所有与当前节点相邻的节点
                excludedNodes.addAll(graph.getOrDefault(node, Collections.emptySet()));
                excludedNodes.add(node); // 也排除当前节点本身
            }
        }
        
        return independentSet;
    }

    /**
     * 生成JSON格式报告
     */
    public void generateReport(String filename) {
        analyzeMutants();
        
        try {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode root = mapper.createObjectNode();
            
            // 统计信息
            ObjectNode statistics = mapper.createObjectNode();
            Map<MutantResult.MutantType, List<String>> typeGroups = new HashMap<>();
            for (MutantResult.MutantType type : MutantResult.MutantType.values()) {
                typeGroups.put(type, new ArrayList<>());
            }
            
            for (String mutantName : mutantNames) {
                MutantResult result = mutantResults.get(mutantName);
                typeGroups.get(result.type).add(mutantName);
            }
            
            statistics.put("total_mutants", mutantNames.size());
            statistics.put("normal_mutants", typeGroups.get(MutantResult.MutantType.NORMAL).size());
            statistics.put("equivalent_mutants", typeGroups.get(MutantResult.MutantType.EQUIVALENT).size());
            statistics.put("trivial_mutants", typeGroups.get(MutantResult.MutantType.TRIVIAL).size());
            statistics.put("subsumed_mutants", typeGroups.get(MutantResult.MutantType.SUBSUMED).size());
            statistics.put("timeout_mutants", typeGroups.get(MutantResult.MutantType.TIMEOUT).size());
            statistics.put("error_mutants", typeGroups.get(MutantResult.MutantType.ERROR).size());
            root.set("statistics", statistics);
            
            // 包含关系
            ObjectNode subsumptionRelations = mapper.createObjectNode();
            for (String mutantName : mutantNames) {
                MutantResult result = mutantResults.get(mutantName);
                if (!result.subsumes.isEmpty()) {
                    ArrayNode subsumedArray = mapper.createArrayNode();
                    for (String subsumed : result.subsumes) {
                        subsumedArray.add(subsumed);
                    }
                    subsumptionRelations.set(mutantName, subsumedArray);
                }
            }
            root.set("subsumption_relations", subsumptionRelations);
            
            // 最大独立集
            List<String> independentSet = calculateMaximumIndependentSet();
            ArrayNode independentSetArray = mapper.createArrayNode();
            for (String mutant : independentSet) {
                independentSetArray.add(mutant);
            }
            root.set("maximum_independent_set", independentSetArray);
            root.put("independent_set_size", independentSet.size());
            
            // 按类型列出突变体
            ArrayNode normalMutants = mapper.createArrayNode();
            for (String mutant : typeGroups.get(MutantResult.MutantType.NORMAL)) {
                normalMutants.add(mutant);
            }
            root.set("normal_mutants", normalMutants);
            
            ArrayNode equivalentMutants = mapper.createArrayNode();
            for (String mutant : typeGroups.get(MutantResult.MutantType.EQUIVALENT)) {
                equivalentMutants.add(mutant);
            }
            root.set("equivalent_mutants", equivalentMutants);
            
            ArrayNode subsumedMutants = mapper.createArrayNode();
            for (String mutant : typeGroups.get(MutantResult.MutantType.SUBSUMED)) {
                subsumedMutants.add(mutant);
            }
            root.set("subsumed_mutants", subsumedMutants);
            
            ArrayNode trivialMutants = mapper.createArrayNode();
            for (String mutant : typeGroups.get(MutantResult.MutantType.TRIVIAL)) {
                trivialMutants.add(mutant);
            }
            root.set("trivial_mutants", trivialMutants);
            
            ArrayNode errorMutants = mapper.createArrayNode();
            for (String mutant : typeGroups.get(MutantResult.MutantType.ERROR)) {
                errorMutants.add(mutant);
            }
            root.set("error_mutants", errorMutants);
            
            ArrayNode timeoutMutants = mapper.createArrayNode();
            for (String mutant : typeGroups.get(MutantResult.MutantType.TIMEOUT)) {
                timeoutMutants.add(mutant);
            }
            root.set("timeout_mutants", timeoutMutants);
            
            // 保存JSON文件
            mapper.writerWithDefaultPrettyPrinter().writeValue(new File(filename), root);
            System.out.println("JSON报告已保存到: " + filename);
            
        } catch (IOException e) {
            System.err.println("生成JSON报告失败: " + e.getMessage());
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
                String testId = getTestCaseKey(testCase);

                writer.print("\"" + testId + "\"");

                for (String mutantName : mutantNames) {
                    MutantResult result = mutantResults.get(mutantName);
                    Object res = result.results.get(testId);
                    String status = result.statuses.get(testId);
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
                    "src/main/java/paper/pss/exp/lang_project/mutants_analysis/mutant_analysis_report.json");

            // 保存结果
            analyzer.saveResults(
                    "src/main/java/paper/pss/exp/lang_project/mutants_analysis/simplified_mutant_analysis.csv");

            System.out.println("\n🎉 分析完成！");

        } catch (Exception e) {
            System.err.println("执行失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}