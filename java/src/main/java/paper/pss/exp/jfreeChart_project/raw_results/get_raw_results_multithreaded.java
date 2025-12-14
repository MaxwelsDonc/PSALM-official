package paper.pss.exp.jfreeChart_project.raw_results;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.*;
import java.lang.reflect.Method;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.*;
import java.util.Scanner;
import java.awt.Shape;
import java.awt.geom.Line2D;

// 导入现有的测试用例生成器
import paper.pss.exp.jfreeChart_project.generation.phase1.phase1_random_generator;
import paper.pss.exp.jfreeChart_project.generation.phase1.phase1_partition_generator;
import paper.pss.exp.jfreeChart_project.generation.phase1.phase1_art_generator;
import paper.pss.exp.jfreeChart_project.generation.phase2.phase2_random_generator;
import paper.pss.exp.jfreeChart_project.generation.phase2.phase2_partition_generator;
import paper.pss.exp.jfreeChart_project.generation.phase2.phase2_mtart_generator;

// Model 导入
import paper.pss.exp.jfreeChart_project.model.TestCase;
import paper.pss.exp.jfreeChart_project.model.MetamorphicGroup;
import paper.pss.exp.jfreeChart_project.utils.MGDomainGenerator_utils;

// 蜕变关系 导入
import paper.pss.exp.jfreeChart_project.model.MetamorphicRelation;
import paper.pss.exp.jfreeChart_project.utils.MRFactory_utils;

// 生成器接口定义 - 多线程版本
interface Phase1GeneratorMT {
    List<TestCase> generate(int count);
}

interface Phase2GeneratorMT {
    List<MetamorphicGroup> generate(int count);
}

/**
 * 多线程版本的测试用例生成方法有效性实验 - JFreeChart项目版本
 * 每个mutant使用独立的线程进行处理，提高实验效率
 * 基于jackson_project的get_raw_results_multithreaded.java改进版本
 * 适配createLineRegion函数的Line2D和float输入参数以及Shape输出
 */
public class get_raw_results_multithreaded {

    private static final int TIMEOUT_SECONDS = 3;
    private static final String MUTANTS_PACKAGE = "paper.pss.exp.jfreeChart_project.mutants";
    private static final String LOG_DIR = "src/main/java/paper/pss/exp/jfreeChart_project/raw_results/log";

    // 实验参数
    private final int internalIteration;
    private final int externalIteration;
    private final int maxTcsNum;
    private final int minTcsNum;
    private final String phase;
    private final String[] strategies;
    private final int threadPoolSize;
    private final boolean logMod; // 日志开关

    // 生成器实例映射
    private Map<String, Phase1GeneratorMT> phase1Generators = new HashMap<>();
    private Map<String, Phase2GeneratorMT> phase2Generators = new HashMap<>();
    private List<MetamorphicGroup> mgDomain;

    // 日志和进度跟踪
    private Logger logger;
    private FileHandler fileHandler;
    private int totalExperiments;
    private AtomicInteger completedExperiments = new AtomicInteger(0);

    // 线程安全的结果收集
    private final Object resultLock = new Object();

    // 指定的突变体列表 - 适配jfreeChart_project的突变体
private static final String[] TARGET_MUTANTS = {
        "mutant1", "mutant6", "mutant28", "mutant30", "mutant34", "mutant37", "mutant38", 
        "mutant39", "mutant42", "mutant43", "mutant44", "mutant45", "mutant46", "mutant47", 
        "mutant49", "mutant50", "mutant51", "mutant54", "mutant55", "mutant56", "mutant57", 
        "mutant58", "mutant59", "mutant60", "mutant62", "mutant63", "mutant65", "mutant67", 
        "mutant68", "mutant69", "mutant70", "mutant72", "mutant74", "mutant76", "mutant77", 
        "mutant78", "mutant81", "mutant83", "mutant84", "mutant85", "mutant86", "mutant91", 
        "mutant92", "mutant96", "mutant97", "mutant98", "mutant99", "mutant100", "mutant0", 
        "mutant31", "mutant40", "mutant41", "mutant53", "mutant61", "mutant64", "mutant66", 
        "mutant71", "mutant75", "mutant79", "mutant80", "mutant82", "mutant87", "mutant88", 
        "mutant89", "mutant90", "mutant93", "mutant94", "mutant95", "mutant101",
        "caseStudy8", "caseStudy9", "caseStudy10"
};

    // 实验结果封装类 - 线程安全版本
    public static class ExperimentResult {
        public final ConcurrentHashMap<String, Map<String, List<Double>>> pMeasures;

        public ExperimentResult() {
            this.pMeasures = new ConcurrentHashMap<>();
        }
    }

    // 单个mutant的实验任务
    private static class MutantTask implements Callable<MutantResult> {
        private final String mutantName;
        private final String strategy;
        private final get_raw_results_multithreaded experiment;

        public MutantTask(String mutantName, String strategy, get_raw_results_multithreaded experiment) {
            this.mutantName = mutantName;
            this.strategy = strategy;
            this.experiment = experiment;
        }

        @Override
        public MutantResult call() throws Exception {
            String threadId = Thread.currentThread().getName();
            experiment.logThreadSafe(String.format("[%s] 开始处理突变体: %s", threadId, mutantName));

            MutantResult result = new MutantResult(mutantName);
            Method createLineRegionMethod = null;

            // 加载突变体类
            String className = MUTANTS_PACKAGE + "." + mutantName + ".createLineRegion";
            try {
                Class<?> mutantClass = Class.forName(className);
                createLineRegionMethod = mutantClass.getMethod("createLineRegion", Line2D.class, float.class);
            } catch (ClassNotFoundException e) {
                experiment.logThreadSafe(String.format("[%s] 错误: 无法加载突变体 %s", threadId, mutantName));
                return result;
            } catch (Exception e) {
                experiment.logThreadSafe(
                        String.format("[%s] 错误: 突变体 %s 执行失败 - %s", threadId, mutantName, e.getMessage()));
                return result;
            }

            // 遍历不同的测试用例数量
            for (int testCasesNum = experiment.minTcsNum; testCasesNum <= experiment.maxTcsNum; testCasesNum++) {

                List<Double> pMeasures = new ArrayList<>();

                // 更新进度
                experiment.updateProgress(strategy, mutantName, testCasesNum);

                // 重复 externalIteration 次
                for (int iter = 0; iter < experiment.externalIteration; iter++) {
                    double pMeasure = experiment.calculatePMeasure(createLineRegionMethod, testCasesNum, strategy);
                    pMeasures.add(pMeasure);
                }

                result.pMeasures.put(String.valueOf(testCasesNum), pMeasures);
            }

            experiment.logThreadSafe(String.format("[%s] 完成突变体 %s", threadId, mutantName));
            return result;
        }
    }

    // 单个mutant的结果
    private static class MutantResult {
        public final String mutantName;
        public final Map<String, List<Double>> pMeasures;

        public MutantResult(String mutantName) {
            this.mutantName = mutantName;
            this.pMeasures = new HashMap<>();
        }
    }

    public get_raw_results_multithreaded(String phase, String[] strategies, int internalIteration,
            int externalIteration,
            int maxTcsNum, int minTcsNum, int threadPoolSize, boolean logMod) {
        this.phase = phase;
        this.strategies = strategies;
        this.internalIteration = internalIteration;
        this.externalIteration = externalIteration;
        this.maxTcsNum = maxTcsNum;
        this.minTcsNum = minTcsNum;
        this.threadPoolSize = threadPoolSize;
        this.logMod = logMod;

        // 初始化日志（根据logMod参数决定是否启用）
        if (logMod) {
            setupLogger();
        }

        // 如果是Phase2，生成MetamorphicGroup域
        if ("phase2".equals(phase)) {
            try {
                System.out.println("[DEBUG] 开始生成MetamorphicGroup域...");
                if (logMod && logger != null) {
                    logger.info("正在生成MetamorphicGroup域...");
                }
                MGDomainGenerator_utils mgGenerator = new MGDomainGenerator_utils();
                this.mgDomain = mgGenerator.generateDomain();
                if (logMod && logger != null) {
                    logger.info("MG域生成完成，共" + mgDomain.size() + "个MetamorphicGroup");
                }
                System.out.println("[DEBUG] MG域生成完成，共" + mgDomain.size() + "个MetamorphicGroup");
            } catch (Exception e) {
                System.err.println("[ERROR] 无法初始化MetamorphicGroup域: " + e.getMessage());
                e.printStackTrace();
                throw new RuntimeException("无法初始化MetamorphicGroup域: " + e.getMessage(), e);
            }
        }

        System.out.println("[DEBUG] 开始初始化生成器...");
        // 初始化生成器
        try {
            initializeGenerators(phase, strategies);
            System.out.println("[DEBUG] 生成器初始化完成");
        } catch (Exception e) {
            System.err.println("[ERROR] 生成器初始化失败: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }

        // 计算总实验数量
        calculateTotalExperiments();
        System.out.println("[DEBUG] 总实验数量计算完成: " + totalExperiments);

        // 重置
        this.completedExperiments.set(0);
        System.out.println("[DEBUG] 构造函数完成");
    }

    /**
     * 初始化生成器 - 重构为直接调用避免反射
     */
    private void initializeGenerators(String phase, String[] strategies) {
        try {
            for (String strategy : strategies) {
                if (phase.equals("phase1")) {
                    Phase1GeneratorMT generator = null;
                    if (strategy.equals("phase1.random")) {
                        generator = new Phase1GeneratorMT() {
                            private phase1_random_generator impl = new phase1_random_generator();

                            @Override
                            public List<TestCase> generate(int count) {
                                return impl.generate(count);
                            }
                        };
                    } else if (strategy.equals("phase1.partition")) {
                        generator = new Phase1GeneratorMT() {
                            private phase1_partition_generator impl;
                            {
                                try {
                                    impl = new phase1_partition_generator();
                                } catch (Exception e) {
                                    throw new RuntimeException("Failed to initialize phase1_partition_generator", e);
                                }
                            }

                            @Override
                            public List<TestCase> generate(int count) {
                                return impl.generate(count);
                            }
                        };
                    } else if (strategy.equals("phase1.art")) {
                        generator = new Phase1GeneratorMT() {
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
                    Phase2GeneratorMT generator = null;
                    if (strategy.equals("phase2.random")) {
                        generator = new Phase2GeneratorMT() {
                            private phase2_random_generator impl;
                            {
                                try {
                                    impl = new phase2_random_generator(mgDomain);
                                } catch (Exception e) {
                                    throw new RuntimeException("Failed to initialize phase2_random_generator", e);
                                }
                            }

                            @Override
                            public List<MetamorphicGroup> generate(int count) {
                                return impl.generate(count);
                            }
                        };
                    } else if (strategy.equals("phase2.partition")) {
                        generator = new Phase2GeneratorMT() {
                            private phase2_partition_generator impl;
                            {
                                try {
                                    impl = new phase2_partition_generator(mgDomain);
                                } catch (Exception e) {
                                    throw new RuntimeException("Failed to initialize phase2_partition_generator", e);
                                }
                            }

                            @Override
                            public List<MetamorphicGroup> generate(int count) {
                                return impl.generate(count);
                            }
                        };
                    } else if (strategy.equals("phase2.mtart")) {
                        generator = new Phase2GeneratorMT() {
                            private phase2_mtart_generator impl;
                            {
                                try {
                                    impl = new phase2_mtart_generator(mgDomain);
                                } catch (Exception e) {
                                    throw new RuntimeException("Failed to initialize phase2_mtart_generator", e);
                                }
                            }

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
     * 生成测试用例 - 线程安全版本
     */
    public synchronized Object generateTestCases(int count, String strategy) {
        try {
            if (strategy.startsWith("phase1")) {
                Phase1GeneratorMT generator = phase1Generators.get(strategy);
                if (generator == null) {
                    throw new IllegalArgumentException("No phase1 generator found for strategy: " + strategy);
                }
                return generator.generate(count);
            } else {
                Phase2GeneratorMT generator = phase2Generators.get(strategy);
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
    public double calculatePMeasure(Method mutantMethod, int testCasesNum, String strategy) {
        int defectDetectedCount = 0;

        for (int i = 0; i < internalIteration; i++) {
            boolean defectDetected = false;
            if (strategy.startsWith("phase1")) {
                List<TestCase> testCases = (List<TestCase>) generateTestCases(testCasesNum, strategy);
                for (TestCase testCase : testCases) {
                    // 执行突变体
                    Shape sourceResult = executeMutant(mutantMethod, testCase);
                    if (sourceResult == null) {
                        continue; // 跳过执行失败的测试用例
                    }
                    
                    // 得到所有的蜕变关系
                    List<MetamorphicRelation> relations = MRFactory_utils.getApplicableRelations(testCase);
                    if (relations.isEmpty()) {
                        continue; // 跳过没有适用蜕变关系的测试用例
                    }
                    
                    // 随机选择一个蜕变关系
                    MetamorphicRelation relation = relations.get(new Random().nextInt(relations.size()));
                    // 从选择的蜕变关系中生成所有的MG
                    List<MetamorphicGroup> groups = relation.createGroups(testCase);
                    if (groups.isEmpty()) {
                        continue; // 跳过没有生成蜕变组的测试用例
                    }
                    
                    // 随机选择一个MG
                    MetamorphicGroup group = groups.get(new Random().nextInt(groups.size()));
                    // 得到后续测试用例的输出
                    Shape followupResult = executeMutant(mutantMethod, group.getFollowupTest());
                    if (followupResult == null) {
                        continue; // 跳过执行失败的后续测试用例
                    }
                    
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
                    Shape sourceResult = executeMutant(mutantMethod, mg.getSourceTest());
                    if (sourceResult == null) {
                        continue; // 跳过执行失败的测试用例
                    }
                    
                    // 得到后续测试用例的输出
                    Shape followupResult = executeMutant(mutantMethod, mg.getFollowupTest());
                    if (followupResult == null) {
                        continue; // 跳过执行失败的后续测试用例
                    }
                    
                    // 得到 mg 对应的蜕变关系 ID
                    String relation_id = mg.getMRId();
                    // get the metamorphic relation from relation_id
                    MetamorphicRelation relation = MRFactory_utils.getRelationById(relation_id);
                    if (relation == null) {
                        continue; // 跳过无法找到蜕变关系的MG
                    }
                    
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
     * 执行突变体测试 - 适配createLineRegion函数
     */
    public Shape executeMutant(Method mutantMethod, TestCase input) {
        try {
            Line2D line = input.getLine();
            float width = input.getWidth();
            // Directly invoke the mutant method
            Object result = mutantMethod.invoke(null, line, width);

            // Check if the result is of the expected type (Shape)
            if (result instanceof Shape) {
                return (Shape) result;
            } else {
                return null; // Return null if the result is not a Shape
            }
        } catch (Exception e) {
            // Handle exceptions by returning null
            return null;
        }
    }

    /**
     * 多线程运行完整实验
     */
    public ExperimentResult runExperimentMultithreaded(String strategy) {
        ExperimentResult result = new ExperimentResult();

        // 创建线程池
        ExecutorService executor = Executors.newFixedThreadPool(threadPoolSize);
        List<Future<MutantResult>> futures = new ArrayList<>();

        logThreadSafe(String.format("=== 开始多线程处理策略: %s ===", strategy));
        logThreadSafe(String.format("线程池配置: %d个线程并行处理 %d个突变体", threadPoolSize, TARGET_MUTANTS.length));
        logThreadSafe(String.format("预计处理时间: 根据CPU性能和突变体复杂度而定"));

        // 为每个mutant创建任务
        for (String mutantName : TARGET_MUTANTS) {
            MutantTask task = new MutantTask(mutantName, strategy, this);
            Future<MutantResult> future = executor.submit(task);
            futures.add(future);
        }

        logThreadSafe(String.format("所有任务已提交到线程池，开始并行执行..."));

        // 收集结果
        for (Future<MutantResult> future : futures) {
            try {
                MutantResult mutantResult = future.get();
                synchronized (resultLock) {
                    result.pMeasures.put(mutantResult.mutantName, mutantResult.pMeasures);
                }

            } catch (InterruptedException | ExecutionException e) {
                logThreadSafe("错误: 处理mutant时发生异常 - " + e.getMessage());
                e.printStackTrace();
            }
        }

        // 关闭线程池
        executor.shutdown();
        try {
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                logThreadSafe("警告: 线程池未能在60秒内正常关闭，强制关闭");
                executor.shutdownNow();
            } else {
                logThreadSafe("线程池已正常关闭");
            }
        } catch (InterruptedException e) {
            logThreadSafe("线程池关闭时被中断，强制关闭");
            executor.shutdownNow();
        }

        logThreadSafe(String.format("=== 策略 %s 完成 ===", strategy));

        return result;
    }

    /**
     * 保存实验结果
     */
    public void saveResults(ExperimentResult result, String methodName) {
        try {
            // 创建结果目录
            Path resultsDir = Paths.get("src/main/java/paper/pss/exp/jfreeChart_project/raw_results/" + phase);
            Files.createDirectories(resultsDir);

            // 保存 P-measure 结果
            ObjectMapper mapper = new ObjectMapper();
            Path pMeasureFile = resultsDir.resolve("P-measure_" + methodName + "_multithreaded.json");
            mapper.writerWithDefaultPrettyPrinter().writeValue(pMeasureFile.toFile(), result.pMeasures);

            // 统计结果信息
            int totalMutants = result.pMeasures.size();
            int totalTestCaseConfigs = result.pMeasures.values().iterator().hasNext()
                    ? result.pMeasures.values().iterator().next().size()
                    : 0;

            logThreadSafe(String.format("结果保存完成:"));
            logThreadSafe(String.format("  P-measure文件: %s", pMeasureFile.getFileName()));
            logThreadSafe(String.format("  包含数据: %d个突变体 × %d个测试用例配置", totalMutants, totalTestCaseConfigs));

        } catch (IOException e) {
            logThreadSafe("错误: 保存结果失败 - " + e.getMessage());
            System.err.println("保存结果失败: " + e.getMessage());
        }
    }

    /**
     * 设置日志记录
     */
    private void setupLogger() {
        if (!logMod) {
            return; // 如果日志模式关闭，直接返回
        }

        try {
            // 创建日志目录
            Path logDir = Paths.get(LOG_DIR);
            if (!Files.exists(logDir)) {
                Files.createDirectories(logDir);
            }

            // 设置日志文件名（包含时间戳）
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String logFileName = LOG_DIR + "/experiment_multithreaded_" + timestamp + ".log";

            // 创建logger
            this.logger = Logger.getLogger(get_raw_results_multithreaded.class.getName());
            this.fileHandler = new FileHandler(logFileName, true);

            // 设置日志格式
            SimpleFormatter formatter = new SimpleFormatter();
            fileHandler.setFormatter(formatter);

            logger.addHandler(fileHandler);
            logger.setLevel(Level.INFO);

            logger.info("多线程实验开始 - 日志文件: " + logFileName);

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

        if (logMod && logger != null) {
            logger.info(String.format("总实验配置: %d个突变体 × %d个测试用例数量= %d次实验",
                    mutantsCount, testCaseRangeCount, totalExperiments));
        }
    }

    /**
     * 线程安全的日志记录方法
     */
    private void logThreadSafe(String message) {
        if (!logMod || logger == null) {
            return; // 如果日志模式关闭或logger未初始化，直接返回
        }
        synchronized (logger) {
            logger.info(message);
        }
    }

    /**
     * 更新进度 - 线程安全版本，减少频繁输出
     */
    private void updateProgress(String method, String mutant, int testCaseNum) {
        int completed = completedExperiments.incrementAndGet();
        double progress = (double) completed / totalExperiments * 100;

        // 只在特定进度点输出，避免日志过多
        String progressMsg = String.format("总体进度: %.1f%% (%d/%d) - 当前: %s",
                progress, completed, totalExperiments, mutant);
        logThreadSafe(progressMsg);

    }

    /**
     * 主函数
     */
    public static void main(String[] args) {
        System.out.println("[DEBUG] 多线程实验程序启动");

        // 变量定义
        String phase = "phase2";
        String[] strategies;
        int internalIteration = 1000;
        int externalIteration = 50;
        int maxTcsNum = 18;
        int minTcsNum = 6;
        int threadPoolSize = Math.min(TARGET_MUTANTS.length, Runtime.getRuntime().availableProcessors()); // 根据CPU核心数和mutant数量决定线程数
        boolean logMod = true; // 日志开关，true启用日志，false关闭日志

        System.out.println("[DEBUG] 参数配置完成: phase=" + phase + ", threadPoolSize=" + threadPoolSize);

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

        System.out.println("[DEBUG] 策略配置完成: " + String.join(", ", strategies));

        // 创建实验实例
        System.out.println("[DEBUG] 开始创建实验实例...");
        get_raw_results_multithreaded experiment;
        try {
            experiment = new get_raw_results_multithreaded(
                    phase, strategies, internalIteration, externalIteration, maxTcsNum, minTcsNum, threadPoolSize,
                    logMod);
            System.out.println("[DEBUG] 实验实例创建成功");
        } catch (Exception e) {
            System.err.println("[ERROR] 创建实验实例失败: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        // 日志记录相关的实验信息
        experiment.logThreadSafe("=== 多线程测试用例生成有效性实验开始 ===");
        experiment.logThreadSafe(String.format("实验配置:"));
        experiment.logThreadSafe(String.format("  Phase: %s", phase));
        experiment.logThreadSafe(
                String.format("  线程池大小: %d (CPU核心数: %d)", threadPoolSize, Runtime.getRuntime().availableProcessors()));
        experiment.logThreadSafe(String.format("  内部迭代次数: %d", internalIteration));
        experiment.logThreadSafe(String.format("  外层重复次数: %d", externalIteration));
        experiment.logThreadSafe(String.format("  测试用例数量范围: %d - %d", minTcsNum, maxTcsNum));
        experiment.logThreadSafe(String.format("  目标突变体数量: %d", TARGET_MUTANTS.length));
        experiment.logThreadSafe(String.format("  策略列表: %s", String.join(", ", experiment.strategies)));

        // 运行实验
        for (int i = 0; i < experiment.strategies.length; i++) {
            String strategy = experiment.strategies[i];
            experiment.logThreadSafe(
                    String.format("\n=== 开始策略 %d/%d: %s ===", i + 1, experiment.strategies.length, strategy));

            // 运行多线程实验
            experiment.completedExperiments.set(0);
            ExperimentResult result = experiment.runExperimentMultithreaded(strategy);

            // 保存结果
            experiment.saveResults(result, strategy);

            experiment.logThreadSafe(String.format("策略 %s 完成", strategy));
        }

        experiment.logThreadSafe("\n=== 所有多线程实验完成! ===");

        // 关闭日志处理器（仅在启用日志时）
        if (logMod && experiment.fileHandler != null) {
            experiment.fileHandler.close();
        }
    }
}