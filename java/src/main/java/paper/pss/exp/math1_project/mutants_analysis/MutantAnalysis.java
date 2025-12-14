package paper.pss.exp.math1_project.mutants_analysis;

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

// 导入测试用例生成相关类
import paper.pss.exp.math1_project.generation.phase1.phase1_random_generator;
import paper.pss.exp.math1_project.model.TestCase;
import paper.pss.exp.math1_project.model.MetamorphicGroup;

// 蜕变关系 导入
import paper.pss.exp.math1_project.model.MetamorphicRelation;
import paper.pss.exp.math1_project.utils.MRFactory_utils;

/**
 * 简化版突变体分析器 - Math1项目版本
 * 主要优化：
 * 1. 直接通过类路径加载突变体，无需临时编译
 * 2. 批量执行和缓存结果
 * 3. 统一的数据结构
 * 4. 简化的报告生成
 */
public class MutantAnalysis {

    private static final int TIMEOUT_SECONDS = 3;
    private static final String MUTANTS_PACKAGE = "paper.pss.exp.math1_project.mutants";

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
     * 生成测试用例
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
        Path mutantsPath = Paths.get("src/main/java/paper/pss/exp/math1_project/mutants");

        try {
            List<String> mutants = Files.list(mutantsPath)
                    .filter(Files::isDirectory)
                    .map(path -> path.getFileName().toString())
                    .filter(name -> (name.startsWith("caseStudy") || name.startsWith("mutant") || name.startsWith("undetected")) && !name.contains("_"))
                    .sorted((a, b) -> {
                        try {
                            // 处理caseStudy、mutant和undetected的排序
                            if (a.equals("caseStudy")) return -1;
                            if (b.equals("caseStudy")) return 1;
                            
                            // 提取数字部分进行排序
                            String numA = a.replaceAll("[^0-9]", "");
                            String numB = b.replaceAll("[^0-9]", "");
                            
                            if (!numA.isEmpty() && !numB.isEmpty()) {
                                int intA = Integer.parseInt(numA);
                                int intB = Integer.parseInt(numB);
                                return Integer.compare(intA, intB);
                            }
                            
                            return a.compareTo(b);
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
                String className = MUTANTS_PACKAGE + "." + mutantName + ".convolve";
                Class<?> mutantClass = Class.forName(className);
                Method mutantMethod = mutantClass.getMethod("convolve", double[].class, double[].class);

                // 执行所有测试用例
                for (TestCase testCase : testCases) {
                    // 执行突变体
                    Object sourceResult = executeWithTimeout(mutantMethod, testCase);
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
                        result.type = MutantResult.MutantType.TIMEOUT;
                    } else if ((sourceResult instanceof Exception) || (followupResult instanceof Exception)) {
                        result.statuses.put(testCaseKey, "error");
                        result.results.put(testCaseKey, " exception error");
                        result.type = MutantResult.MutantType.ERROR;
                    } else {
                        result.statuses.put(testCaseKey, "success");
                        result.results.put(testCaseKey, sourceResult);
                        // 验证 group是否满足蜕变关系
                        if (sourceResult instanceof double[] && followupResult instanceof double[]) {
                            if (!relation.verifyRelation(group.getSourceTest(), group.getFollowupTest(),
                                    (double[]) sourceResult, (double[]) followupResult, "", "")) {
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
            Future<Object> future = executor.submit(() -> method.invoke(null, testCase.getX(), testCase.getH()));
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
    private Object executeWithoutTimeout(Method method, TestCase testCase) {
        try {
            // 直接调用反射方法
            return method.invoke(null, testCase.getX(), testCase.getH());
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

        // 多轮迭代检查包含关系，直到没有新的包含关系被发现
        boolean foundNewSubsumption;
        int iteration = 0;
        do {
            foundNewSubsumption = false;
            iteration++;
            System.out.println("包含关系检查第 " + iteration + " 轮...");
            
            // 获取所有可以参与包含关系检查的突变体（NORMAL, ALLKILLED, 以及已经被标记为SUBSUMED的）
            List<String> candidateMutants = mutantNames.stream()
                .filter(name -> {
                    MutantResult.MutantType type = mutantResults.get(name).type;
                    return type == MutantResult.MutantType.NORMAL || 
                           type == MutantResult.MutantType.ALLKILLED ||
                           type == MutantResult.MutantType.SUBSUMED;
                })
                .collect(Collectors.toList());
            
            for (int i = 0; i < candidateMutants.size(); i++) {
                 String mutantA = candidateMutants.get(i);
                 MutantResult resultA = mutantResults.get(mutantA);
                 
                 // 跳过已经是ALLKILLED类型的突变体（它们不能被其他突变体包含）
                 if (resultA.type == MutantResult.MutantType.ALLKILLED)
                     continue;
                     
                 for (int j = 0; j < candidateMutants.size(); j++) {
                     if (i == j)
                         continue;
                     String mutantB = candidateMutants.get(j);
                     MutantResult resultB = mutantResults.get(mutantB);
                     

                     
                     // 检查A是否包含B（B的所有kill都在A中，且A的kill数量更少）
                      if (resultA.killedBy.containsAll(resultB.killedBy) && 
                          resultA.killedBy.size() < resultB.killedBy.size() && 
                          !resultB.subsumedBy.contains(mutantA)) { // 避免重复记录
                          
                          // 如果B之前是NORMAL类型，现在标记为SUBSUMED
                          if (resultB.type == MutantResult.MutantType.NORMAL) {
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
     * 计算两两不包含突变体的最大独立集
     * 只考虑NORMAL和SUBSUMED类型的突变体，使用贪心算法
     */
    private List<String> calculateMaximumIndependentSet() {
        // 获取NORMAL和SUBSUMED类型的突变体（排除EQUIVALENT、ERROR、ALLKILLED）
        List<String> candidateMutants = mutantResults.entrySet().stream()
                .filter(entry -> entry.getValue().type == MutantResult.MutantType.NORMAL ||
                               entry.getValue().type == MutantResult.MutantType.SUBSUMED)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        if (candidateMutants.isEmpty()) {
            return new ArrayList<>();
        }

        // 构建包含关系图（无向图）
        Map<String, Set<String>> inclusionGraph = new HashMap<>();
        for (String mutant : candidateMutants) {
            inclusionGraph.put(mutant, new HashSet<>());
        }

        // 添加包含关系边：如果两个突变体之间存在包含关系，则它们不能同时在独立集中
        for (String mutantA : candidateMutants) {
            MutantResult resultA = mutantResults.get(mutantA);
            for (String mutantB : candidateMutants) {
                if (!mutantA.equals(mutantB)) {
                    MutantResult resultB = mutantResults.get(mutantB);
                    // 检查是否存在包含关系
                    boolean aIncludesB = resultB.killedBy.containsAll(resultA.killedBy) && !resultA.killedBy.isEmpty();
                    boolean bIncludesA = resultA.killedBy.containsAll(resultB.killedBy) && !resultB.killedBy.isEmpty();
                    
                    if (aIncludesB || bIncludesA) {
                        inclusionGraph.get(mutantA).add(mutantB);
                        inclusionGraph.get(mutantB).add(mutantA);
                    }
                }
            }
        }

        // 使用贪心算法找到最大独立集
        return findMaximumIndependentSetGreedy(candidateMutants, inclusionGraph);
    }

    /**
     * 使用贪心算法找到最大独立集
     * 按照节点的度数（连接数）从小到大排序，优先选择度数小的节点
     * 这样可以选择更多的节点来获得更大的独立集
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
            System.out.println("🚀 开始简化版突变体分析 - Math1项目...");

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
                    "src/main/java/paper/pss/exp/math1_project/mutants_analysis/mutant_analysis_report.json");

            // 保存结果
            analyzer.saveResults(
                    "src/main/java/paper/pss/exp/math1_project/mutants_analysis/simplified_mutant_analysis.csv");

            System.out.println("\n🎉 分析完成！");

        } catch (Exception e) {
            System.err.println("执行失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}