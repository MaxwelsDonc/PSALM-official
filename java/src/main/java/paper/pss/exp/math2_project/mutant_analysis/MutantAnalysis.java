package paper.pss.exp.math2_project.mutant_analysis;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

// 导入项目特定的类
import paper.pss.exp.math2_project.generation.phase1.phase1_random_generator;
import paper.pss.exp.math2_project.model.TestCase;
import paper.pss.exp.math2_project.model.MetamorphicGroup;
// 导入蜕变关系相关类
import paper.pss.exp.math2_project.model.MetamorphicRelation;
import paper.pss.exp.math2_project.utils.MRFactory_utils;

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
    private static final String MUTANTS_PACKAGE = "paper.pss.exp.math2_project.mutants";

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
        try {
            // 使用random_generator生成随机测试用例
            phase1_random_generator generator = new phase1_random_generator();
            testCases = generator.generate(count);
        } catch (IOException e) {
            System.err.println("生成测试用例失败: " + e.getMessage());
            testCases = new ArrayList<>();
        }
    }

    /**
     * 发现并加载突变体（直接通过类路径）
     */
    public void loadMutants() throws IOException {
        mutantNames.clear();
        // 扫描mutant目录
        Path mutantsPath = Paths.get("src/main/java/paper/pss/exp/math2_project/mutants");

        try {
            if (!Files.exists(mutantsPath)) {
                System.err.println("突变体目录不存在: " + mutantsPath);
                return;
            }
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
            try {
                loadMutants();
            } catch (IOException e) {
                System.err.println("加载突变体失败: " + e.getMessage());
                return;
            }
        }
        // 2. 批量执行突变体
        System.out.println("批量执行突变体测试...");
        for (String mutantName : mutantNames) {
            MutantResult result = new MutantResult();
            mutantResults.put(mutantName, result);

            try {
                // 直接通过类名加载
                String className = MUTANTS_PACKAGE + "." + mutantName + ".copySign";
                Class<?> mutantClass = Class.forName(className);
                Method mutantMethod = mutantClass.getMethod("copySign", long.class, long.class);

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
                String testCaseKey = getTestCaseKey(testCase);
                    if ((sourceResult instanceof TimeoutException) || (followupResult instanceof TimeoutException)) {
                        result.statuses.put(testCaseKey, "timeout");
                        // 不立即设置为TIMEOUT，等所有测试用例执行完再统计
                    } else if ((sourceResult instanceof Exception) || (followupResult instanceof Exception)) {
                        result.statuses.put(testCaseKey, "error");
                        result.results.put(testCaseKey, " exception error");
                        // 不立即设置为ERROR，等所有测试用例执行完再统计
                    } else {
                        result.statuses.put(testCaseKey, "success");
                        result.results.put(testCaseKey, sourceResult);
                        // 验证 group是否满足蜕变关系
                        if (sourceResult instanceof Long && followupResult instanceof Long) {
                            long sourceResultLong = (Long) sourceResult;
                            long followupResultLong = (Long) followupResult;
                            if (!relation.verifyRelation(group.getSourceTest(), group.getFollowupTest(),
                                    sourceResultLong, followupResultLong, "", "")) {
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
     * 生成测试用例的唯一键
     */
    private String getTestCaseKey(TestCase testCase) {
        return String.format("magnitude=%d,sign=%d", 
                (long)testCase.getMagnitude(), (long)testCase.getSign());
    }

    /**
     * 带超时的执行方法
     */
    private Object executeWithTimeout(Method method, TestCase input) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<Object> future = executor.submit(() -> method.invoke(null, (long)input.getMagnitude(), (long)input.getSign()));
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
        try {
            // 直接调用反射方法
            return method.invoke(null, (long)input.getMagnitude(), (long)input.getSign());
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

            // 首先根据执行状态确定突变体类型
            long errorCount = result.statuses.values().stream().filter(s -> "error".equals(s)).count();
            long timeoutCount = result.statuses.values().stream().filter(s -> "timeout".equals(s)).count();
            long successCount = result.statuses.values().stream().filter(s -> "success".equals(s)).count();
            
            if (errorCount == result.statuses.size()) {
                // 所有测试用例都出错
                result.type = MutantResult.MutantType.ERROR;
            } else if (timeoutCount > 0 && successCount == 0) {
                // 有超时且没有成功的测试用例
                result.type = MutantResult.MutantType.TIMEOUT;
            } else {
                // 有成功的测试用例，保持NORMAL状态进行进一步分析
                if (result.type == MutantResult.MutantType.NORMAL) {
                    // 检查是否为等价突变体
                    if (result.killedBy.isEmpty()) {
                        result.type = MutantResult.MutantType.EQUIVALENT;
                    }
                    // 检查是否被所有成功的测试用例杀死
                    else if (result.killedBy.size() == successCount) {
                        result.type = MutantResult.MutantType.ALLKILLED;
                    }
                }
            }
        }

        // 检查包含关系 - 双向记录
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
                    resultB.subsumedBy.add(mutantA);
                    resultA.subsumes.add(mutantB);
                }
            }
        }
        
        // 根据包含关系设置突变体类型
        for (String mutantName : mutantNames) {
            MutantResult result = mutantResults.get(mutantName);
            if (result.type == MutantResult.MutantType.NORMAL && !result.subsumedBy.isEmpty()) {
                result.type = MutantResult.MutantType.SUBSUMED;
            }
        }
    }

    /**
     * 计算最大独立集（不包含包含关系的突变体集合）
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
     * 生成JSON报告
     */
    public void generateReport(String filename) {
        analyzeMutants();

        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode report = objectMapper.createObjectNode();
        
        // 统计各类突变体数量
        Map<MutantResult.MutantType, List<String>> typeGroups = new HashMap<>();
        for (MutantResult.MutantType type : MutantResult.MutantType.values()) {
            typeGroups.put(type, new ArrayList<>());
        }

        for (String mutantName : mutantNames) {
            MutantResult result = mutantResults.get(mutantName);
            typeGroups.get(result.type).add(mutantName);
        }

        int totalMutants = mutantNames.size();
        int normalMutants = typeGroups.get(MutantResult.MutantType.NORMAL).size();
        int equivalentMutants = typeGroups.get(MutantResult.MutantType.EQUIVALENT).size();
        int allKilledMutants = typeGroups.get(MutantResult.MutantType.ALLKILLED).size();
        int subsumedMutants = typeGroups.get(MutantResult.MutantType.SUBSUMED).size();
        int timeoutMutants = typeGroups.get(MutantResult.MutantType.TIMEOUT).size();
        int errorMutants = typeGroups.get(MutantResult.MutantType.ERROR).size();

        // 创建统计信息对象
         ObjectNode statistics = objectMapper.createObjectNode();
         statistics.put("total_mutants", totalMutants);
         statistics.put("normal_mutants", normalMutants);
         statistics.put("equivalent_mutants", equivalentMutants);
         statistics.put("trivial_mutants", allKilledMutants);
         statistics.put("subsumed_mutants", subsumedMutants);
         statistics.put("timeout_mutants", timeoutMutants);
         statistics.put("error_mutants", errorMutants);
        
        report.set("statistics", statistics);

        // 添加包含关系信息
         ObjectNode subsumptionRelations = objectMapper.createObjectNode();
         for (String mutantName : mutantNames) {
             MutantResult result = mutantResults.get(mutantName);
             if (!result.subsumes.isEmpty()) {
                 ArrayNode subsumesArray = objectMapper.createArrayNode();
                 // 对被包含的突变体按数字顺序排序
                 List<String> sortedSubsumes = result.subsumes.stream()
                         .sorted((a, b) -> {
                             try {
                                 int numA = Integer.parseInt(a.replaceAll("\\D+", ""));
                                 int numB = Integer.parseInt(b.replaceAll("\\D+", ""));
                                 return Integer.compare(numA, numB);
                             } catch (NumberFormatException e) {
                                 return a.compareTo(b);
                             }
                         })
                         .collect(Collectors.toList());
                 for (String subsumed : sortedSubsumes) {
                     subsumesArray.add(subsumed);
                 }
                 subsumptionRelations.set(mutantName, subsumesArray);
             }
         }
         report.set("subsumption_relations", subsumptionRelations);

         // 计算最大独立集
         List<String> maxIndependentSet = calculateMaximumIndependentSet();
         ArrayNode independentSetArray = objectMapper.createArrayNode();
         for (String mutant : maxIndependentSet) {
             independentSetArray.add(mutant);
         }
         report.set("maximum_independent_set", independentSetArray);
         report.put("independent_set_size", maxIndependentSet.size());

         // 按类型分组的突变体 - 直接添加到报告根级别
         for (MutantResult.MutantType type : MutantResult.MutantType.values()) {
             List<String> mutants = typeGroups.get(type);
             // 对每个类型的突变体也按数字顺序排序
             List<String> sortedMutants = mutants.stream()
                     .sorted((a, b) -> {
                         try {
                             int numA = Integer.parseInt(a.replaceAll("\\D+", ""));
                             int numB = Integer.parseInt(b.replaceAll("\\D+", ""));
                             return Integer.compare(numA, numB);
                         } catch (NumberFormatException e) {
                             return a.compareTo(b);
                         }
                     })
                     .collect(Collectors.toList());
             ArrayNode typeArray = objectMapper.createArrayNode();
             for (String mutant : sortedMutants) {
                 typeArray.add(mutant);
             }
             
             // 使用用户要求的键名格式
             String keyName;
             switch (type) {
                 case EQUIVALENT:
                     keyName = "equivalent_mutants";
                     break;
                 case NORMAL:
                     keyName = "normal_mutants";
                     break;
                 case SUBSUMED:
                     keyName = "subsumed_mutants";
                     break;
                 case ALLKILLED:
                     keyName = "trivial_mutants";
                     break;
                 case TIMEOUT:
                     keyName = "timeout_mutants";
                     break;
                 case ERROR:
                     keyName = "error_mutants";
                     break;
                 default:
                     keyName = type.toString().toLowerCase() + "_mutants";
             }
             report.set(keyName, typeArray);
         }

        // 保存JSON报告
        if (filename != null && !filename.isEmpty()) {
            try (FileWriter writer = new FileWriter(filename)) {
                objectMapper.writerWithDefaultPrettyPrinter().writeValue(writer, report);
                System.out.println("JSON报告已保存到: " + filename);
            } catch (IOException e) {
                System.err.println("保存JSON报告失败: " + e.getMessage());
            }
        } else {
            try {
                System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(report));
            } catch (IOException e) {
                System.err.println("生成JSON报告失败: " + e.getMessage());
            }
        }
    }

    /**
     * 保存结果到CSV文件
     */
    public void saveResults(String filename) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            // CSV头部
            writer.println("Mutant,Type,KillCount,KillRate,Status");

            // 数据行
            for (String mutantName : mutantNames) {
                MutantResult result = mutantResults.get(mutantName);
                double killRate = testCases.isEmpty() ? 0.0 : (double) result.killedBy.size() / testCases.size();
                String status = result.statuses.values().stream()
                        .collect(Collectors.groupingBy(s -> s, Collectors.counting()))
                        .entrySet().stream()
                        .max(Map.Entry.comparingByValue())
                        .map(Map.Entry::getKey)
                        .orElse("unknown");

                writer.printf("%s,%s,%d,%.4f,%s%n",
                        mutantName,
                        result.type,
                        result.killedBy.size(),
                        killRate,
                        status);
            }

            System.out.println("结果已保存: " + filename);

        } catch (IOException e) {
            System.err.println("保存结果失败: " + e.getMessage());
        }
    }

    /**
     * 主函数
     * 可以通过命令行参数指定测试用例数量，例如：java MutantAnalysis 100
     */
    public static void main(String[] args) {
        try {
            System.out.println("🚀 开始简化版突变体分析 - Math2项目...");

            MutantAnalysis analyzer = new MutantAnalysis();
            int testCaseCount =1000; // 默认值
            System.out.println("将生成 " + testCaseCount + " 个随机测试用例");

            // 生成指定数量的测试用例并执行测试
            analyzer.loadMutants();
            analyzer.generateTestCases(testCaseCount);
            analyzer.executeAllTests();

            // 分析突变体
            analyzer.analyzeMutants();
            
            // 生成报告
            analyzer.generateReport(
                    "src/main/java/paper/pss/exp/math2_project/mutant_analysis/mutant_analysis_report.json");

            // 保存结果
            analyzer.saveResults(
                    "src/main/java/paper/pss/exp/math2_project/mutant_analysis/simplified_mutant_analysis.csv");

            System.out.println("\n🎉 分析完成！");

        } catch (Exception e) {
            System.err.println("执行失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}