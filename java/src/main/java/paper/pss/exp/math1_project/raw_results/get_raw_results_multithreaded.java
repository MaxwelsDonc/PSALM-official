package paper.pss.exp.math1_project.raw_results;

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

// 导入math1_project相关的生成器
import paper.pss.exp.math1_project.generation.phase1.phase1_random_generator;
import paper.pss.exp.math1_project.generation.phase1.phase1_partition_generator;
import paper.pss.exp.math1_project.generation.phase1.phase1_art_generator;
import paper.pss.exp.math1_project.generation.phase2.phase2_random_generator;
import paper.pss.exp.math1_project.generation.phase2.phase2_partition_generator;
import paper.pss.exp.math1_project.generation.phase2.phase2_mtart_generator;

// 导入math1_project相关的模型
import paper.pss.exp.math1_project.model.TestCase;
import paper.pss.exp.math1_project.model.MetamorphicGroup;
import paper.pss.exp.math1_project.utils.MGDomainGenerator_utils;

// 导入math1_project相关的蜕变关系
import paper.pss.exp.math1_project.model.MetamorphicRelation;
import paper.pss.exp.math1_project.utils.MRFactory_utils;

// 定义生成器接口
interface Phase1GeneratorMT {
    List<TestCase> generate(int count);
}

interface Phase2GeneratorMT {
    List<MetamorphicGroup> generate(int count);
}

/**
 * 多线程版本的测试用例生成方法有效性实验
 * 用于处理变异体以提高实验效率
 */
public class get_raw_results_multithreaded {

    private static final int TIMEOUT_SECONDS = 3;
    private static final String MUTANTS_PACKAGE = "paper.pss.exp.math1_project.mutants";
    private static final String LOG_DIR = "src/main/java/paper/pss/exp/math1_project/raw_results/log";

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

    // math1_project的目标变异体列表
    private static final String[] TARGET_MUTANTS = {
            "mutant17", "mutant14", "mutant18", "mutant19", "mutant20",
            "caseStudy29", "caseStudy30", "mutant38", "mutant39", "mutant40",
            "mutant46", "mutant47", "mutant48", "mutant49", "mutant51"
    };

    // 实验结果封装类
    public static class ExperimentResult {
        public final ConcurrentHashMap<String, Map<String, List<Double>>> pMeasures;

        public ExperimentResult() {
            this.pMeasures = new ConcurrentHashMap<>();
        }
    }

    // 针对特定变异体的实验任务
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
            Method convolveMethod = null;

            // 加载突变体类
            String className = MUTANTS_PACKAGE + "." + mutantName + ".convolve";
            try {
                Class<?> mutantClass = Class.forName(className);
                // math1_project使用convolve方法，接受两个double[]参数
                convolveMethod = mutantClass.getMethod("convolve", double[].class, double[].class);
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
                    double pMeasure = experiment.calculatePMeasure(convolveMethod, testCasesNum, strategy);
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
                            private phase1_partition_generator impl = new phase1_partition_generator();

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
                            private phase2_random_generator impl = new phase2_random_generator(mgDomain);

                            @Override
                            public List<MetamorphicGroup> generate(int count) {
                                return impl.generate(count);
                            }
                        };
                    } else if (strategy.equals("phase2.partition")) {
                        generator = new Phase2GeneratorMT() {
                            private phase2_partition_generator impl = new phase2_partition_generator(mgDomain);

                            @Override
                            public List<MetamorphicGroup> generate(int count) {
                                return impl.generate(count);
                            }
                        };
                    } else if (strategy.equals("phase2.mtart")) {
                        generator = new Phase2GeneratorMT() {
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
     * 生成测试用例 - 线程安全版本
     */
    public synchronized Object generateTestCases(int count, String strategy) {
        try {
            if (strategy.startsWith("phase1")) {
                Phase1GeneratorMT generator = phase1Generators.get(strategy);
                if (generator != null) {
                    return generator.generate(count);
                } else {
                    throw new RuntimeException("未找到策略对应的生成器: " + strategy);
                }
            } else {
                Phase2GeneratorMT generator = phase2Generators.get(strategy);
                if (generator != null) {
                    return generator.generate(count);
                } else {
                    throw new RuntimeException("未找到策略对应的生成器: " + strategy);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("生成测试用例失败: " + e.getMessage(), e);
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
                    double[] sourceResult = executeMutant(mutantMethod, testCase);
                    // 得到所有的蜕变关系
                    List<MetamorphicRelation> relations = MRFactory_utils.getApplicableRelations(testCase);
                    if (relations.isEmpty()) {
                        break;
                    }
                    // 随机选择一个蜕变关系
                    MetamorphicRelation relation = relations.get(new Random().nextInt(relations.size()));
                    // 从选择的蜕变关系中生成所有的MG
                    List<MetamorphicGroup> groups = relation.createGroups(testCase);
                    if (groups.isEmpty()) {
                        break;
                    }
                    // 随机选择一个MG
                    MetamorphicGroup group = groups.get(new Random().nextInt(groups.size()));
                    // 得到后续测试用例的输出
                    double[] followupResult = executeMutant(mutantMethod, group.getFollowupTest());
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
                    double[] sourceResult = executeMutant(mutantMethod, mg.getSourceTest());
                    // 得到后续测试用例的输出
                    double[] followupResult = executeMutant(mutantMethod, mg.getFollowupTest());
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
     */
    public double[] executeMutant(Method mutantMethod, TestCase input) {
        try {
            // math1_project的convolve方法接受两个double[]参数
            double[] x = input.getX();
            double[] h = input.getH();

            // Directly invoke the mutant method
            Object result = mutantMethod.invoke(null, x, h);

            // Check if the result is of the expected type (double[])
            if (result instanceof double[]) {
                return (double[]) result;
            } else {
                return new double[0]; // Return empty array if the result is not a double array
            }
        } catch (Exception e) {
            // Handle exceptions by returning empty array (error value)
            return new double[0];
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
            }
        } catch (InterruptedException e) {
            logThreadSafe("线程池关闭被中断");
            executor.shutdownNow();
        }

        logThreadSafe(String.format("=== 策略 %s 多线程处理完成 ===", strategy));
        return result;
    }

    /**
     * 保存实验结果到JSON文件
     */
    public void saveResults(ExperimentResult result, String methodName) {
        try {
            // 创建结果目录
            Path resultsDir = Paths.get("src/main/java/paper/pss/exp/math1_project/raw_results/" + phase);
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
     * 设置日志记录器
     */
    private void setupLogger() {
        try {
            // 创建日志目录
            Path logDir = Paths.get(LOG_DIR);
            Files.createDirectories(logDir);

            // 创建日志文件
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String logFileName = String.format("experiment_%s.log", timestamp);
            Path logFile = logDir.resolve(logFileName);

            // 配置日志记录器
            logger = Logger.getLogger(get_raw_results_multithreaded.class.getName());
            fileHandler = new FileHandler(logFile.toString(), true);
            fileHandler.setFormatter(new SimpleFormatter());
            logger.addHandler(fileHandler);
            logger.setLevel(Level.INFO);

            // 同时输出到控制台
            ConsoleHandler consoleHandler = new ConsoleHandler();
            consoleHandler.setFormatter(new SimpleFormatter());
            logger.addHandler(consoleHandler);

            logger.info("日志系统初始化完成: " + logFile.toAbsolutePath());

        } catch (IOException e) {
            System.err.println("无法设置日志记录器: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 计算总实验数量
     */
    private void calculateTotalExperiments() {
        int mutantCount = TARGET_MUTANTS.length;
        int testCaseRangeCount = maxTcsNum - minTcsNum + 1;
        int strategyCount = strategies.length;
        this.totalExperiments = mutantCount * testCaseRangeCount * externalIteration * strategyCount;
    }

    /**
     * 线程安全的日志记录
     */
    private void logThreadSafe(String message) {
        synchronized (this) {
            if (logMod && logger != null) {
                logger.info(message);
            }
            System.out.println("[" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")) + "] "
                    + message);
        }
    }

    /**
     * 更新进度
     */
    private void updateProgress(String method, String mutant, int testCaseNum) {
        int completed = completedExperiments.incrementAndGet();
        double progress = (double) completed / totalExperiments * 100;

        if (completed % 100 == 0 || progress >= 100) {
            logThreadSafe(String.format("进度: %.1f%% (%d/%d) - %s, %s, TCs=%d",
                    progress, completed, totalExperiments, method, mutant, testCaseNum));
        }
    }

    public static void main(String[] args) {
        System.out.println("[DEBUG] 多线程实验程序启动");

        // 变量定义
        String phase = "phase1";
        String[] strategies;
        int internalIteration = 1000;
        int externalIteration = 50;
        int maxTcsNum = 36;
        int minTcsNum = 12;
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