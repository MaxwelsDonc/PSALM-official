package paper.pss.exp.math2_project.raw_results;

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

// 导入math2项目相关类
import paper.pss.exp.math2_project.generation.phase1.phase1_random_generator;
import paper.pss.exp.math2_project.generation.phase1.phase1_partition_generator;
import paper.pss.exp.math2_project.generation.phase1.phase1_art_generator;
import paper.pss.exp.math2_project.generation.phase2.phase2_random_generator;
import paper.pss.exp.math2_project.generation.phase2.phase2_partition_generator;
import paper.pss.exp.math2_project.generation.phase2.phase2_mtart_generator;

// Model 导入
import paper.pss.exp.math2_project.model.TestCase;
import paper.pss.exp.math2_project.model.MetamorphicGroup;
import paper.pss.exp.math2_project.utils.MGDomainGenerator_utils;

// 蜕变关系 导入
import paper.pss.exp.math2_project.model.MetamorphicRelation;
import paper.pss.exp.math2_project.utils.MRFactory_utils;

// 生成器接口定义 - 多线程版本
interface Phase1GeneratorMT {
    List<TestCase> generate(int count);
}

interface Phase2GeneratorMT {
    List<MetamorphicGroup> generate(int count);
}

/**
 * 多线程版本的测试用例生成方法有效性实验
 * 每个mutant使用独立的线程进行处理，提高实验效率
 * 基于jackson_project的多线程实现架构
 */
public class get_raw_results_multithreaded {

    private static final int TIMEOUT_SECONDS = 3;
    private static final String MUTANTS_PACKAGE = "paper.pss.exp.math2_project.mutants";
    private static final String LOG_DIR = "src/main/java/paper/pss/exp/math2_project/raw_results/log";

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

    // 动态加载的突变体列表
    private static final String[] TARGET_MUTANTS = {
            "mutant1", "mutant2", "mutant3", "mutant4", "mutant5", "mutant6", "mutant7", "mutant8",
            "mutant9", "mutant10", "mutant11", "mutant12", "mutant13", "mutant14", "mutant15", "mutant16",
            "mutant17", "mutant18", "mutant19", "mutant20", "mutant21", "mutant22", "mutant23", "mutant24",
            "mutant25", "mutant26", "mutant27", "mutant28", "mutant29", "mutant30", "mutant31", "mutant32",
            "mutant33", "mutant34", "mutant35"
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
            Method copySignMethod = null;

            // 加载突变体类
            String className = MUTANTS_PACKAGE + "." + mutantName + ".copySign";
            try {
                Class<?> mutantClass = Class.forName(className);
                copySignMethod = mutantClass.getMethod("copySign", long.class, long.class);
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
                    double pMeasure = experiment.calculatePMeasure(copySignMethod, testCasesNum, strategy);
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
                String configPath = "src/main/java/paper/pss/exp/math2_project/math2_config.json";
                MGDomainGenerator_utils mgGenerator = new MGDomainGenerator_utils(configPath, 1000);
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

        // 加载突变体列表
        // loadMutants(); // 不再需要动态加载，使用固定的 TARGET_MUTANTS 数组

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
                } else if (phase.equals("phase2")) {
                    Phase2GeneratorMT generator = null;
                    String configPath = "src/main/java/paper/pss/exp/math2_project/math2_config.json";
                    
                    if (strategy.equals("phase2.random")) {
                        generator = new Phase2GeneratorMT() {
                            private phase2_random_generator impl = new phase2_random_generator(configPath, mgDomain);

                            @Override
                            public List<MetamorphicGroup> generate(int count) {
                                return impl.generate(count);
                            }
                        };
                    } else if (strategy.equals("phase2.partition")) {
                        generator = new Phase2GeneratorMT() {
                            private phase2_partition_generator impl = new phase2_partition_generator(configPath, mgDomain);

                            @Override
                            public List<MetamorphicGroup> generate(int count) {
                                return impl.generate(count);
                            }
                        };
                    } else if (strategy.equals("phase2.mtart")) {
                        generator = new Phase2GeneratorMT() {
                            private phase2_mtart_generator impl = new phase2_mtart_generator(configPath, mgDomain);

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
            if (logMod && logger != null) {
                logger.severe("Failed to initialize generators: " + e.getMessage());
            }
            throw new RuntimeException(e);
        }
    }

    // 不再需要 loadMutants 方法，使用固定的 TARGET_MUTANTS 数组

    /**
     * 生成测试用例
     */
    public synchronized Object generateTestCases(int count, String strategy) {
        try {
            if (phase.equals("phase1")) {
                Phase1GeneratorMT generator = phase1Generators.get(strategy);
                if (generator != null) {
                    return generator.generate(count);
                }
            } else if (phase.equals("phase2")) {
                Phase2GeneratorMT generator = phase2Generators.get(strategy);
                if (generator != null) {
                    return generator.generate(count);
                }
            }
        } catch (Exception e) {
            if (logMod && logger != null) {
                logger.severe("Failed to generate test cases: " + e.getMessage());
            }
            throw new RuntimeException(e);
        }
        return new ArrayList<>();
    }

    /**
     * 计算P-measure
     */
    public double calculatePMeasure(Method mutantMethod, int testCasesNum, String strategy) {
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
     */
    public int executeMutant(Method mutantMethod, TestCase input) {
        try {
            long magnitude = input.getMagnitude();
            long sign = input.getSign();
            // 直接调用突变体方法
            Object result = mutantMethod.invoke(null, magnitude, sign);

            // 检查结果是否为预期类型 (Long)
            if (result instanceof Long) {
                return ((Long) result).intValue();
            } else {
                return 0; // 如果结果不是 long 类型，返回默认值
            }
        } catch (Exception e) {
            // 通过返回 -1 处理异常（错误值）
            return -1;
        }
    }

    // executeWithTimeout 方法已删除，不再需要

    /**
     * 运行多线程实验
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
            Path resultsDir = Paths.get("src/main/java/paper/pss/exp/math2_project/raw_results/" + phase);
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
        try {
            // 创建日志目录
            Files.createDirectories(Paths.get(LOG_DIR));

            // 配置日志记录器
            logger = Logger.getLogger(get_raw_results_multithreaded.class.getName());
            logger.setLevel(Level.ALL);

            // 创建文件处理器
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String logFileName = String.format("%s/experiment_%s.log", LOG_DIR, timestamp);
            fileHandler = new FileHandler(logFileName, true);
            fileHandler.setFormatter(new SimpleFormatter());
            logger.addHandler(fileHandler);

            // 移除默认的控制台处理器以避免重复输出
            Logger rootLogger = Logger.getLogger("");
            Handler[] handlers = rootLogger.getHandlers();
            for (Handler handler : handlers) {
                if (handler instanceof ConsoleHandler) {
                    rootLogger.removeHandler(handler);
                }
            }

            logger.info("日志系统初始化完成: " + logFileName);
        } catch (IOException e) {
            System.err.println("无法初始化日志系统: " + e.getMessage());
        }
    }

    /**
     * 计算总实验数量
     */
    private void calculateTotalExperiments() {
        int testCaseConfigs = maxTcsNum - minTcsNum + 1;
        totalExperiments = TARGET_MUTANTS.length * testCaseConfigs;
    }

    /**
     * 线程安全的日志记录
     */
    private void logThreadSafe(String message) {
        synchronized (this) {
            System.out.println(message);
            if (logMod && logger != null) {
                logger.info(message);
            }
        }
    }

    /**
     * 更新进度
     */
    private void updateProgress(String method, String mutant, int testCaseNum) {
        int completed = completedExperiments.incrementAndGet();
        if (completed % 100 == 0 || completed == totalExperiments) {
            double progress = (double) completed / totalExperiments * 100;
            logThreadSafe(String.format("进度: %.1f%% (%d/%d) - %s:%s:%d",
                    progress, completed, totalExperiments, method, mutant, testCaseNum));
        }
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
        int maxTcsNum = 12;
        int minTcsNum = 4;
        int threadPoolSize = Math.min(8, Runtime.getRuntime().availableProcessors()); // 根据CPU核心数决定线程数
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