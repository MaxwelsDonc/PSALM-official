package paper.pss.exp.jackson_project.raw_results;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.*;
import java.lang.reflect.Method;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.*;
import java.util.Scanner;

// 导入现有的测试用例生成器
import paper.pss.exp.jackson_project.generation.phase1.phase1_random_generator;
import paper.pss.exp.jackson_project.generation.phase1.phase1_partition_generator;
import paper.pss.exp.jackson_project.generation.phase1.phase1_art_generator;
import paper.pss.exp.jackson_project.generation.phase2.phase2_random_generator;
import paper.pss.exp.jackson_project.generation.phase2.phase2_partition_generator;
import paper.pss.exp.jackson_project.generation.phase2.phase2_mtart_generator;

// Model 导入
import paper.pss.exp.jackson_project.model.TestCase;
import paper.pss.exp.jackson_project.model.MetamorphicGroup;
import paper.pss.exp.jackson_project.utils.MGDomainGenerator;

// 蜕变关系 导入
import paper.pss.exp.jackson_project.model.MetamorphicRelation;
import paper.pss.exp.jackson_project.utils.MRFactory_utils;

// 生成器接口定义
interface Phase1Generator {
    List<TestCase> generate(int count);
}

interface Phase2Generator {
    List<MetamorphicGroup> generate(int count);
}

/**
 * 测试用例生成方法有效性实验
 * 基于 get_raw_results_geometricSum.py 的 Java 实现
 * 用于对比不同方法在 parseInt 突变体上的效果
 * 使用现有的generation目录下的测试用例生成器
 */
public class get_raw_results {

    private static final int TIMEOUT_SECONDS = 3;
    private static final String MUTANTS_PACKAGE = "paper.pss.exp.jackson_project.mutants";
    private static final String LOG_DIR = "src/main/java/paper/pss/exp/jackson_project/raw_results/log";

    // 实验参数
    private final int internalIteration;
    private final int externalIteration;
    private final int maxTcsNum;
    private final int minTcsNum;
    private final String phase;
    private final String[] strategies;

    // 生成器实例映射 - 重构为具体类型以避免反射
    private Map<String, Phase1Generator> phase1Generators = new HashMap<>();
    private Map<String, Phase2Generator> phase2Generators = new HashMap<>();
    private List<MetamorphicGroup> mgDomain;

    // 日志和进度跟踪
    private Logger logger;
    private FileHandler fileHandler;
    private int totalExperiments;
    private int completedExperiments;

    // 指定的突变体列表
    private static final String[] TARGET_MUTANTS = {
            "caseStudy110", "caseStudy86", "mutant2", "mutant5", "mutant32", "mutant33", "mutant34", "mutant37",
            "mutant38", "mutant49", "mutant50", "mutant51", "mutant52", "mutant54", "mutant55", "mutant56",
            "mutant58", "mutant59", "mutant61", "mutant64", "mutant65", "mutant76", "mutant77", "mutant78",
            "mutant79", "mutant81", "mutant82", "mutant83", "mutant85", "mutant88", "mutant91", "mutant103",
            "mutant104", "mutant105", "mutant106", "mutant108", "mutant109", "mutant112", "mutant113",
            "mutant115", "mutant118", "mutant119", "mutant120", "mutant121", "mutant122"
    };

    // 实验结果封装类
    public static class ExperimentResult {
        public final Map<String, Map<String, List<Double>>> pMeasures;
        public final Map<String, Map<String, List<Double>>> timeRecords;

        public ExperimentResult() {
            this.pMeasures = new HashMap<>();
            this.timeRecords = new HashMap<>();
        }
    }

    public get_raw_results(String phase, String[] strategies, int internalIteration, int externalIteration,
            int maxTcsNum, int minTcsNum) {
        this.phase = phase;
        this.strategies = strategies;
        this.internalIteration = internalIteration;
        this.externalIteration = externalIteration;
        this.maxTcsNum = maxTcsNum;
        this.minTcsNum = minTcsNum;

        // 初始化日志
        setupLogger();

        // 如果是Phase2，生成MetamorphicGroup域
        if ("phase2".equals(phase)) {
            try {
                logger.info("正在生成MetamorphicGroup域...");
                MGDomainGenerator mgGenerator = new MGDomainGenerator();
                this.mgDomain = mgGenerator.generateDomain();
                logger.info("MG域生成完成，共" + mgDomain.size() + "个MetamorphicGroup");
            } catch (Exception e) {
                throw new RuntimeException("无法初始化MetamorphicGroup域: " + e.getMessage(), e);
            }
        }
        // 初始化生成器
        initializeGenerators(phase, strategies);
        // 计算总实验数量
        calculateTotalExperiments();
        // 重置
        this.completedExperiments = 0;
    }

    /**
     * 初始化生成器 - 重构为直接调用避免反射
     */
    private void initializeGenerators(String phase, String[] strategies) {
        try {
            for (String strategy : strategies) {
                if (phase.equals("phase1")) {
                    Phase1Generator generator = null;
                    if (strategy.equals("phase1.random")) {
                        generator = new Phase1Generator() {
                            private phase1_random_generator impl = new phase1_random_generator();

                            @Override
                            public List<TestCase> generate(int count) {
                                return impl.generate(count);
                            }
                        };
                    } else if (strategy.equals("phase1.partition")) {
                        generator = new Phase1Generator() {
                            private phase1_partition_generator impl = new phase1_partition_generator();

                            @Override
                            public List<TestCase> generate(int count) {
                                return impl.generate(count);
                            }
                        };
                    } else if (strategy.equals("phase1.art")) {
                        generator = new Phase1Generator() {
                            private phase1_art_generator impl = new phase1_art_generator();

                            @Override
                            public List<TestCase> generate(int count) {
                                return impl.generate(count);
                            }
                        };
                    }
                    if (generator != null) {
                        phase1Generators.put(strategy, generator);
                    }
                } else {
                    Phase2Generator generator = null;
                    if (strategy.equals("phase2.random")) {
                        generator = new Phase2Generator() {
                            private phase2_random_generator impl = new phase2_random_generator(mgDomain);

                            @Override
                            public List<MetamorphicGroup> generate(int count) {
                                return impl.generate(count);
                            }
                        };
                    } else if (strategy.equals("phase2.partition")) {
                        generator = new Phase2Generator() {
                            private phase2_partition_generator impl = new phase2_partition_generator(mgDomain);

                            @Override
                            public List<MetamorphicGroup> generate(int count) {
                                return impl.generate(count);
                            }
                        };
                    } else if (strategy.equals("phase2.mtart")) {
                        generator = new Phase2Generator() {
                            private phase2_mtart_generator impl = new phase2_mtart_generator(mgDomain);

                            @Override
                            public List<MetamorphicGroup> generate(int count) {
                                return impl.generate(count);
                            }
                        };
                    }
                    if (generator != null) {
                        phase2Generators.put(strategy, generator);
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize generators: " + e.getMessage(), e);
        }
    }

    /**
     * 生成测试用例 - 重构为直接调用避免反射
     */
    public Object generateTestCases(int count, String strategy) {
        try {
            if (strategy.startsWith("phase1")) {
                Phase1Generator generator = phase1Generators.get(strategy);
                if (generator == null) {
                    throw new IllegalArgumentException("No phase1 generator found for strategy: " + strategy);
                }
                return generator.generate(count);
            } else {
                Phase2Generator generator = phase2Generators.get(strategy);
                if (generator == null) {
                    throw new IllegalArgumentException("No phase2 generator found for strategy: " + strategy);
                }
                return generator.generate(count);
            }
        } catch (Exception e) {
            System.err.println("生成测试用例时出错: " + e.getMessage());
            return null;
        }
    }

    /**
     * 计算 P-measure 值
     */
    public double calculatePMeasure(Method mutantMethod, int testCasesNum,
            String strategy) {
        int defectDetectedCount = 0;

        for (int i = 0; i < internalIteration; i++) {
            boolean defectDetected = false;
            if (strategy.startsWith("phase1")) {
                List<TestCase> testCases = (List<TestCase>) generateTestCases(testCasesNum, strategy);
                for (TestCase testCase : testCases) {
                    // 执行突变体
                    int sourceResult = executeMutant(mutantMethod, testCase);
                    // 得到所有的蜕变关系
                    List<MetamorphicRelation> relations = MRFactory_utils.getApplicableRelations(testCase);
                    // 随机选择一个蜕变关系
                    MetamorphicRelation relation = relations.get(new Random().nextInt(relations.size()));
                    // 从选择的蜕变关系中生成所有的MG
                    List<MetamorphicGroup> groups = relation.createGroups(testCase);
                    // 随机选择一个MG
                    MetamorphicGroup group = groups.get(new Random().nextInt(groups.size()));
                    // 得到后续测试用例的输出
                    int followupResult = executeMutant(mutantMethod, group.getFollowupTest());
                    // 验证 group是否满足蜕变关系
                    if (!relation.verifyRelation(group.getSourceTest(), group.getFollowupTest(), sourceResult,
                            followupResult, "", "")) {
                        defectDetected = true;
                        break;
                    }
                }

            } else {
                // Phase2
                List<MetamorphicGroup> mgList = (List<MetamorphicGroup>) generateTestCases(testCasesNum, strategy);
                for (MetamorphicGroup mg : mgList) {
                    // 得到原测试用例的输出
                    int sourceResult = executeMutant(mutantMethod, mg.getSourceTest());
                    // 得到后续测试用例的输出
                    int followupResult = executeMutant(mutantMethod, mg.getFollowupTest());
                    // 得到 mg 对应的蜕变关系 ID
                    String relation_id = mg.getMRId();
                    // get the metamorphic relation from relation_id
                    MetamorphicRelation relation = MRFactory_utils.getRelationById(relation_id);
                    // 验证 group是否满足蜕变关系
                    if (!relation.verifyRelation(mg.getSourceTest(), mg.getFollowupTest(), sourceResult,
                            followupResult, "", "")) {
                        defectDetected = true;
                        break;
                    }
                }
            }
            if (defectDetected) {
                defectDetectedCount++;
            }
        }
        return (double) defectDetectedCount / internalIteration;
    }

    /**
     * 执行突变体测试
     * FIXME
     */
    public int executeMutant(Method mutantMethod, TestCase input) {
        try {
            String inputStr = input.getInput();
            // Directly invoke the mutant method
            Object result = mutantMethod.invoke(null, inputStr);

            // Check if the result is of the expected type (Integer)
            if (result instanceof Integer) {
                return (Integer) result;
            } else {
                return -1; // Return error code if the result is not an integer
            }
        } catch (Exception e) {
            // Handle exceptions by returning -1 (error value)
            return -1;
        }
    }

    /**
     * 运行完整实验
     */
    public ExperimentResult runExperiment(String strategy) {
        ExperimentResult result = new ExperimentResult();
        Method parseIntMethod = null;

        // 遍历指定的突变体
        for (String mutantName : TARGET_MUTANTS) {
            logger.info("\n处理突变体: " + mutantName);

            // try {
            // 加载突变体类
            String className = MUTANTS_PACKAGE + "." + mutantName + ".parseInt";
            try {
                Class<?> mutantClass = Class.forName(className);
                parseIntMethod = mutantClass.getMethod("parseInt", String.class);
            } catch (ClassNotFoundException e) {
                System.err.println("无法加载突变体: " + mutantName);
            } catch (Exception e) {
                System.err.println("突变体执行失败: " + mutantName + " -> " + e.getMessage());
            }

            result.pMeasures.put(mutantName, new HashMap<>());
            result.timeRecords.put(mutantName, new HashMap<>());

            // 遍历不同的测试用例数量
            for (int testCasesNum = minTcsNum; testCasesNum <= maxTcsNum; testCasesNum++) {
                logger.info("  测试用例数量: " + testCasesNum);

                List<Double> pMeasures = new ArrayList<>();
                List<Double> iterationTimes = new ArrayList<>();

                // 更新进度
                updateProgress(strategy, mutantName, testCasesNum);

                // 重复 externalIteration 次
                for (int iter = 0; iter < externalIteration; iter++) {
                    long startTime = System.currentTimeMillis();

                    double pMeasure = calculatePMeasure(parseIntMethod, testCasesNum, strategy);

                    long endTime = System.currentTimeMillis();
                    double iterationTime = (endTime - startTime) / 1000.0;

                    pMeasures.add(pMeasure);
                    iterationTimes.add(iterationTime);
                }

                result.pMeasures.get(mutantName).put(String.valueOf(testCasesNum), pMeasures);
                result.timeRecords.get(mutantName).put(String.valueOf(testCasesNum), iterationTimes);
            }
        }

        return result;
    }

    /**
     * 保存实验结果
     */
    public void saveResults(ExperimentResult result, String methodName) {
        try {
            // 创建结果目录
            Path resultsDir = Paths.get("src/main/java/paper/pss/exp/jackson_project/raw_results/" + phase);
            Files.createDirectories(resultsDir);

            // 保存 P-measure 结果
            ObjectMapper mapper = new ObjectMapper();
            Path pMeasureFile = resultsDir.resolve("P-measure_" + methodName + ".json");
            mapper.writerWithDefaultPrettyPrinter().writeValue(pMeasureFile.toFile(), result.pMeasures);
            logger.info("\nP-measure结果已保存到: " + pMeasureFile);

            // 保存时间记录
            Path timeFile = resultsDir.resolve("time_consumption_" + methodName + ".json");
            mapper.writerWithDefaultPrettyPrinter().writeValue(timeFile.toFile(), result.timeRecords);
            logger.info("时间记录已保存到: " + timeFile);

        } catch (IOException e) {
            System.err.println("保存结果失败: " + e.getMessage());
        }
    }

    /**
     * 设置日志记录
     */
    private void setupLogger() {
        try {
            // 创建日志目录
            Path logDir = Paths.get(LOG_DIR);
            if (!Files.exists(logDir)) {
                Files.createDirectories(logDir);
            }

            // 设置日志文件名（包含时间戳）
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String logFileName = LOG_DIR + "/experiment_" + timestamp + ".log";

            // 创建logger
            this.logger = Logger.getLogger(get_raw_results.class.getName());
            this.fileHandler = new FileHandler(logFileName, true);

            // 设置日志格式
            SimpleFormatter formatter = new SimpleFormatter();
            fileHandler.setFormatter(formatter);

            logger.addHandler(fileHandler);
            logger.setLevel(Level.INFO);

            logger.info("实验开始 - 日志文件: " + logFileName);

        } catch (IOException e) {
            System.err.println("Error setting up logger: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 计算总实验数量
     */
    private void calculateTotalExperiments() {
        int mutantsCount = TARGET_MUTANTS.length;
        int testCaseRangeCount = maxTcsNum - minTcsNum + 1;
        this.totalExperiments = mutantsCount * testCaseRangeCount;

        if (logger != null) {
            logger.info(String.format("总实验配置: %d个突变体 × %d个测试用例数量= %d次实验",
                    mutantsCount, testCaseRangeCount, totalExperiments));
        }
    }

    /**
     * 更新进度
     */
    private void updateProgress(String method, String mutant, int testCaseNum) {
        completedExperiments++;
        double progress = (double) completedExperiments / totalExperiments * 100;

        String progressMsg = String.format("进度: %.2f%% (%d/%d) - %s, %s, 测试用例数: %d",
                progress, completedExperiments, totalExperiments, method, mutant, testCaseNum);

        if (logger != null) {
            logger.info(progressMsg);
        }
    }

    /**
     * 主函数
     */
    public static void main(String[] args) {
        // 变量定义
        String phase = "phase2";
        String[] strategies;
        int internalIteration = 1000;
        int externalIteration = 50;
        int maxTcsNum = 18;
        int minTcsNum = 6;

        // 显式定义strategies
        if ("phase1".equals(phase)) {
            strategies = new String[] {
                    "phase1.random",
                    "phase1.partition",
                    "phase1.art"
            };
        } else {
            strategies = new String[] {
                    "phase2.random",
                    "phase2.partition",
                    "phase2.mtart"
            };
        }

        // 创建实验实例
        get_raw_results experiment = new get_raw_results(
                phase, strategies, internalIteration, externalIteration, maxTcsNum, minTcsNum);

        // 日志记录相关的实验信息
        experiment.logger.info("=== 测试用例生成有效性实验开始 ===");
        experiment.logger.info(String.format("Phase: %s", phase));
        experiment.logger.info(String.format("内部迭代次数: %d", internalIteration));
        experiment.logger.info(String.format("外层重复次数: %d", externalIteration));
        experiment.logger.info(String.format("测试用例数量范围: %d - %d", minTcsNum, maxTcsNum));

        // 运行实验
        for (String strategy : experiment.strategies) {
            experiment.logger.info("\n=== 开始实验: " + strategy + " ===");
            long methodStartTime = System.currentTimeMillis();

            // 运行实验
            experiment.completedExperiments = 0;
            ExperimentResult result = experiment.runExperiment(strategy);

            // 保存结果
            experiment.saveResults(result, strategy);

            long methodEndTime = System.currentTimeMillis();
            double methodDuration = (methodEndTime - methodStartTime) / 1000.0;

            experiment.logger.info(String.format("方法 %s 完成，耗时: %.2f秒", strategy, methodDuration));
        }

        experiment.logger.info("\n=== 所有实验完成! ===");

        // 关闭日志处理器
        if (experiment.fileHandler != null) {
            experiment.fileHandler.close();
        }
    }
}