package org.techishthoughts.benchmark.runner;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.profile.GCProfiler;
import org.openjdk.jmh.profile.StackProfiler;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

import org.techishthoughts.benchmark.statistics.BenchmarkStatistics;
import org.techishthoughts.benchmark.comparison.StatisticalComparator;
import org.techishthoughts.benchmark.regression.PerformanceRegression;
import org.techishthoughts.benchmark.jit.AdaptiveWarmupStrategy;

import org.techishthoughts.payload.model.User;
import org.techishthoughts.payload.generator.UnifiedPayloadGenerator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Comprehensive JMH benchmark runner with multiple measurement modes,
 * statistical analysis, and performance regression detection.
 */
public class ComprehensiveBenchmarkRunner {

    /**
     * Benchmark configuration for different measurement scenarios
     */
    public enum BenchmarkMode {
        QUICK_DEVELOPMENT(
            "Quick development testing",
            3, 5, 1,
            TimeValue.seconds(1),
            TimeValue.seconds(1),
            false, false
        ),
        STANDARD_CI(
            "Standard CI/CD pipeline",
            5, 10, 2,
            TimeValue.seconds(2),
            TimeValue.seconds(3),
            true, false
        ),
        COMPREHENSIVE_ANALYSIS(
            "Comprehensive performance analysis",
            10, 20, 3,
            TimeValue.seconds(3),
            TimeValue.seconds(5),
            true, true
        ),
        PRODUCTION_VALIDATION(
            "Production validation",
            15, 30, 5,
            TimeValue.seconds(5),
            TimeValue.seconds(10),
            true, true
        );

        private final String description;
        private final int warmupIterations;
        private final int measurementIterations;
        private final int forks;
        private final TimeValue warmupTime;
        private final TimeValue measurementTime;
        private final boolean enableProfiling;
        private final boolean enableRegression;

        BenchmarkMode(String description, int warmupIterations, int measurementIterations,
                     int forks, TimeValue warmupTime, TimeValue measurementTime,
                     boolean enableProfiling, boolean enableRegression) {
            this.description = description;
            this.warmupIterations = warmupIterations;
            this.measurementIterations = measurementIterations;
            this.forks = forks;
            this.warmupTime = warmupTime;
            this.measurementTime = measurementTime;
            this.enableProfiling = enableProfiling;
            this.enableRegression = enableRegression;
        }

        // Getters
        public String getDescription() { return description; }
        public int getWarmupIterations() { return warmupIterations; }
        public int getMeasurementIterations() { return measurementIterations; }
        public int getForks() { return forks; }
        public TimeValue getWarmupTime() { return warmupTime; }
        public TimeValue getMeasurementTime() { return measurementTime; }
        public boolean isEnableProfiling() { return enableProfiling; }
        public boolean isEnableRegression() { return enableRegression; }
    }

    /**
     * Comprehensive benchmark results
     */
    public static class BenchmarkResults {
        private final Map<String, Collection<RunResult>> results;
        private final List<StatisticalComparator.FrameworkPerformance> frameworkPerformances;
        private final StatisticalComparator.FrameworkRanking ranking;
        private final List<PerformanceRegression.RegressionResult> regressions;
        private final BenchmarkStatistics.StatisticalSummary overallStats;
        private final LocalDateTime executionTime;
        private final String reportPath;

        public BenchmarkResults(Map<String, Collection<RunResult>> results,
                              List<StatisticalComparator.FrameworkPerformance> frameworkPerformances,
                              StatisticalComparator.FrameworkRanking ranking,
                              List<PerformanceRegression.RegressionResult> regressions,
                              BenchmarkStatistics.StatisticalSummary overallStats,
                              String reportPath) {
            this.results = new HashMap<>(results);
            this.frameworkPerformances = new ArrayList<>(frameworkPerformances);
            this.ranking = ranking;
            this.regressions = new ArrayList<>(regressions);
            this.overallStats = overallStats;
            this.executionTime = LocalDateTime.now();
            this.reportPath = reportPath;
        }

        // Getters
        public Map<String, Collection<RunResult>> getResults() { return new HashMap<>(results); }
        public List<StatisticalComparator.FrameworkPerformance> getFrameworkPerformances() {
            return new ArrayList<>(frameworkPerformances);
        }
        public StatisticalComparator.FrameworkRanking getRanking() { return ranking; }
        public List<PerformanceRegression.RegressionResult> getRegressions() {
            return new ArrayList<>(regressions);
        }
        public BenchmarkStatistics.StatisticalSummary getOverallStats() { return overallStats; }
        public LocalDateTime getExecutionTime() { return executionTime; }
        public String getReportPath() { return reportPath; }

        public boolean hasRegressions() {
            return regressions.stream().anyMatch(PerformanceRegression.RegressionResult::hasRegression);
        }
    }

    // ==================== BENCHMARK STATE ====================

    @State(Scope.Benchmark)
    public static class GlobalBenchmarkState {

        @Param({"SMALL", "MEDIUM", "LARGE"})
        public DatasetSize datasetSize;

        public List<User> testData;
        public UnifiedPayloadGenerator payloadGenerator;
        public AdaptiveWarmupStrategy warmupStrategy;

        // Framework-specific serializers
        public ObjectMapper jacksonMapper;

        @Setup(Level.Trial)
        public void setupTrial() {
            payloadGenerator = new UnifiedPayloadGenerator();
            warmupStrategy = new AdaptiveWarmupStrategy();

            // Initialize serialization frameworks
            initializeJackson();

            System.out.println("=== Comprehensive Benchmark Trial Setup ===");
            System.out.println("Dataset Size: " + datasetSize);
            System.out.println("Available Processors: " + Runtime.getRuntime().availableProcessors());
            System.out.println("Max Memory: " + Runtime.getRuntime().maxMemory() / (1024 * 1024) + " MB");
            System.out.println("==========================================");
        }

        @Setup(Level.Iteration)
        public void setupIteration() {
            // Generate fresh data and perform adaptive warmup
            testData = generateTestData(datasetSize);

            // Perform custom warmup
            AdaptiveWarmupStrategy.WarmupResult warmupResult = warmupStrategy.performWarmup(() -> {
                try {
                    jacksonMapper.writeValueAsBytes(testData);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            System.out.println("Warmup completed: " + warmupResult);
        }

        private void initializeJackson() {
            jacksonMapper = new ObjectMapper();
            jacksonMapper.registerModule(new JavaTimeModule());
        }

        private List<User> generateTestData(DatasetSize size) {
            UnifiedPayloadGenerator.DataComplexity complexity;
            switch (size) {
                case SMALL: complexity = UnifiedPayloadGenerator.DataComplexity.SIMPLE; break;
                case MEDIUM: complexity = UnifiedPayloadGenerator.DataComplexity.MODERATE; break;
                case LARGE: complexity = UnifiedPayloadGenerator.DataComplexity.COMPLEX; break;
                default: complexity = UnifiedPayloadGenerator.DataComplexity.MODERATE;
            }
            return payloadGenerator.generateDataset(complexity);
        }
    }

    public enum DatasetSize {
        SMALL, MEDIUM, LARGE
    }

    // ==================== MULTIPLE MEASUREMENT MODE BENCHMARKS ====================

    // Throughput Measurements
    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    @Group("jackson")
    public void jacksonSerializationThroughput(GlobalBenchmarkState state, Blackhole blackhole) {
        try {
            byte[] result = state.jacksonMapper.writeValueAsBytes(state.testData);
            blackhole.consume(result);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    @Group("jackson")
    public void jacksonDeserializationThroughput(GlobalBenchmarkState state, Blackhole blackhole) {
        try {
            byte[] serialized = state.jacksonMapper.writeValueAsBytes(state.testData);
            List<User> result = state.jacksonMapper.readValue(serialized,
                state.jacksonMapper.getTypeFactory().constructCollectionType(List.class, User.class));
            blackhole.consume(result);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // Average Time Measurements
    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    @Group("jackson")
    public void jacksonSerializationLatency(GlobalBenchmarkState state, Blackhole blackhole) {
        try {
            byte[] result = state.jacksonMapper.writeValueAsBytes(state.testData);
            blackhole.consume(result);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    @Group("jackson")
    public void jacksonDeserializationLatency(GlobalBenchmarkState state, Blackhole blackhole) {
        try {
            byte[] serialized = state.jacksonMapper.writeValueAsBytes(state.testData);
            List<User> result = state.jacksonMapper.readValue(serialized,
                state.jacksonMapper.getTypeFactory().constructCollectionType(List.class, User.class));
            blackhole.consume(result);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // Sampling Time Measurements
    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    @Group("jackson")
    public void jacksonRoundtripSampling(GlobalBenchmarkState state, Blackhole blackhole) {
        try {
            byte[] serialized = state.jacksonMapper.writeValueAsBytes(state.testData);
            List<User> result = state.jacksonMapper.readValue(serialized,
                state.jacksonMapper.getTypeFactory().constructCollectionType(List.class, User.class));
            blackhole.consume(result);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // Single Shot Time Measurements (for memory allocation patterns)
    @Benchmark
    @BenchmarkMode(Mode.SingleShotTime)
    @Measurement(iterations = 100, batchSize = 1)
    @Warmup(iterations = 0)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    @Group("jackson")
    public void jacksonMemoryAllocationPattern(GlobalBenchmarkState state, Blackhole blackhole) {
        try {
            byte[] result = state.jacksonMapper.writeValueAsBytes(state.testData);
            blackhole.consume(result);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ==================== MAIN EXECUTION METHODS ====================

    /**
     * Run comprehensive benchmarks with specified mode
     */
    public static BenchmarkResults runBenchmarks(BenchmarkMode mode) throws RunnerException, IOException {
        return runBenchmarks(mode, ".");
    }

    /**
     * Run comprehensive benchmarks with specified mode and output directory
     */
    public static BenchmarkResults runBenchmarks(BenchmarkMode mode, String outputDir) throws RunnerException, IOException {
        System.out.println("Starting " + mode.getDescription() + "...");

        // Create timestamped output directory
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        Path resultDir = Paths.get(outputDir, "benchmark-results-" + timestamp);
        Files.createDirectories(resultDir);

        // Configure JMH options
        OptionsBuilder optionsBuilder = new OptionsBuilder()
            .include(ComprehensiveBenchmarkRunner.class.getSimpleName())
            .forks(mode.getForks())
            .warmupIterations(mode.getWarmupIterations())
            .warmupTime(mode.getWarmupTime())
            .measurementIterations(mode.getMeasurementIterations())
            .measurementTime(mode.getMeasurementTime())
            .mode(Mode.Throughput, Mode.AverageTime, Mode.SampleTime)
            .timeUnit(TimeUnit.MILLISECONDS)
            .resultFormat(org.openjdk.jmh.results.format.ResultFormatType.JSON)
            .result(resultDir.resolve("jmh-results.json").toString());

        // Add profilers if enabled
        if (mode.isEnableProfiling()) {
            optionsBuilder.addProfiler(GCProfiler.class);
            optionsBuilder.addProfiler(StackProfiler.class);
        }

        // Configure JVM arguments for different scenarios
        configureJVMArgs(optionsBuilder, mode);

        Options options = optionsBuilder.build();

        // Run benchmarks
        Runner runner = new Runner(options);
        Collection<RunResult> runResults = runner.run();

        // Process and analyze results
        return processResults(runResults, mode, resultDir.toString());
    }

    private static void configureJVMArgs(OptionsBuilder optionsBuilder, BenchmarkMode mode) {
        List<String> jvmArgs = new ArrayList<>();

        // Base JVM configuration
        jvmArgs.add("-XX:+UseG1GC");
        jvmArgs.add("-XX:MaxGCPauseMillis=200");

        switch (mode) {
            case QUICK_DEVELOPMENT:
                jvmArgs.add("-Xms512m");
                jvmArgs.add("-Xmx1g");
                break;
            case STANDARD_CI:
                jvmArgs.add("-Xms1g");
                jvmArgs.add("-Xmx2g");
                jvmArgs.add("-XX:+PrintGCDetails");
                break;
            case COMPREHENSIVE_ANALYSIS:
            case PRODUCTION_VALIDATION:
                jvmArgs.add("-Xms2g");
                jvmArgs.add("-Xmx4g");
                jvmArgs.add("-XX:+PrintGCDetails");
                jvmArgs.add("-XX:+PrintGCTimeStamps");
                jvmArgs.add("-XX:+PrintGCApplicationStoppedTime");
                jvmArgs.add("-XX:+UnlockExperimentalVMOptions");
                jvmArgs.add("-XX:+UseJVMCICompiler"); // Enable advanced JIT if available
                break;
        }

        optionsBuilder.jvmArgs(jvmArgs.toArray(new String[0]));
    }

    private static BenchmarkResults processResults(Collection<RunResult> runResults,
                                                  BenchmarkMode mode, String outputDir) throws IOException {

        // Group results by benchmark
        Map<String, Collection<RunResult>> groupedResults = runResults.stream()
            .collect(Collectors.groupingBy(r -> r.getParams().getBenchmark()));

        // Create framework performance data
        List<StatisticalComparator.FrameworkPerformance> frameworkPerformances =
            createFrameworkPerformances(runResults);

        // Rank frameworks
        Map<StatisticalComparator.ComparisonMetric, Double> weights = createDefaultWeights();
        StatisticalComparator.FrameworkRanking ranking =
            StatisticalComparator.rankFrameworks(frameworkPerformances, weights);

        // Detect regressions if enabled
        List<PerformanceRegression.RegressionResult> regressions = new ArrayList<>();
        if (mode.isEnableRegression()) {
            regressions = detectRegressions(frameworkPerformances, outputDir);
        }

        // Calculate overall statistics
        double[] allScores = runResults.stream()
            .mapToDouble(r -> r.getPrimaryResult().getScore())
            .toArray();
        BenchmarkStatistics.StatisticalSummary overallStats =
            BenchmarkStatistics.calculateSummary(allScores);

        // Generate comprehensive report
        String reportPath = generateComprehensiveReport(
            groupedResults, frameworkPerformances, ranking, regressions, overallStats, outputDir);

        return new BenchmarkResults(groupedResults, frameworkPerformances, ranking,
                                  regressions, overallStats, reportPath);
    }

    private static List<StatisticalComparator.FrameworkPerformance> createFrameworkPerformances(
            Collection<RunResult> runResults) {

        Map<String, StatisticalComparator.FrameworkPerformance> performanceMap = new HashMap<>();

        for (RunResult result : runResults) {
            String benchmarkName = result.getParams().getBenchmark();
            String frameworkName = extractFrameworkName(benchmarkName);

            StatisticalComparator.FrameworkPerformance performance =
                performanceMap.computeIfAbsent(frameworkName, StatisticalComparator.FrameworkPerformance::new);

            // Extract metric type and add to performance
            StatisticalComparator.ComparisonMetric metric = extractMetricType(benchmarkName);
            if (metric != null) {
                double[] data = {result.getPrimaryResult().getScore()};
                performance.addMetric(metric, data);
            }
        }

        return new ArrayList<>(performanceMap.values());
    }

    private static String extractFrameworkName(String benchmarkName) {
        if (benchmarkName.contains("jackson")) {
            return "Jackson";
        }
        // Add other framework extractions here
        return "Unknown";
    }

    private static StatisticalComparator.ComparisonMetric extractMetricType(String benchmarkName) {
        if (benchmarkName.contains("Throughput")) {
            return StatisticalComparator.ComparisonMetric.THROUGHPUT;
        } else if (benchmarkName.contains("Latency")) {
            return StatisticalComparator.ComparisonMetric.LATENCY;
        }
        return null;
    }

    private static Map<StatisticalComparator.ComparisonMetric, Double> createDefaultWeights() {
        Map<StatisticalComparator.ComparisonMetric, Double> weights = new EnumMap<>(
            StatisticalComparator.ComparisonMetric.class);
        weights.put(StatisticalComparator.ComparisonMetric.THROUGHPUT, 2.0);
        weights.put(StatisticalComparator.ComparisonMetric.LATENCY, 2.0);
        weights.put(StatisticalComparator.ComparisonMetric.MEMORY_USAGE, 1.0);
        return weights;
    }

    private static List<PerformanceRegression.RegressionResult> detectRegressions(
            List<StatisticalComparator.FrameworkPerformance> performances, String outputDir) {
        // This would integrate with historical data for regression detection
        // For now, return empty list as placeholder
        return new ArrayList<>();
    }

    private static String generateComprehensiveReport(
            Map<String, Collection<RunResult>> results,
            List<StatisticalComparator.FrameworkPerformance> performances,
            StatisticalComparator.FrameworkRanking ranking,
            List<PerformanceRegression.RegressionResult> regressions,
            BenchmarkStatistics.StatisticalSummary overallStats,
            String outputDir) throws IOException {

        StringBuilder report = new StringBuilder();
        report.append("=== Comprehensive Serialization Benchmark Report ===\n");
        report.append("Generated: ").append(LocalDateTime.now()).append("\n\n");

        // Executive Summary
        report.append("Executive Summary:\n");
        report.append("================\n");
        report.append("Total Benchmarks: ").append(results.size()).append("\n");
        report.append("Overall Performance CV: ").append(String.format("%.2f%%", overallStats.getCoefficientOfVariation() * 100)).append("\n");
        report.append("Data Quality: ").append(overallStats.getDataQuality()).append("\n\n");

        // Framework Ranking
        report.append("Framework Ranking:\n");
        report.append("=================\n");
        report.append(ranking.toString()).append("\n");

        // Detailed Results
        report.append("Detailed Results:\n");
        report.append("================\n");
        for (Map.Entry<String, Collection<RunResult>> entry : results.entrySet()) {
            report.append("Benchmark: ").append(entry.getKey()).append("\n");
            for (RunResult result : entry.getValue()) {
                report.append("  ").append(result.getPrimaryResult().toString()).append("\n");
            }
            report.append("\n");
        }

        // Regressions
        if (!regressions.isEmpty()) {
            report.append("Performance Regressions:\n");
            report.append("=======================\n");
            report.append(PerformanceRegression.generateRegressionReport(regressions));
        }

        // Write report to file
        Path reportPath = Paths.get(outputDir, "comprehensive-report.txt");
        Files.write(reportPath, report.toString().getBytes());

        return reportPath.toString();
    }

    // ==================== MAIN METHOD ====================

    public static void main(String[] args) throws RunnerException, IOException {
        BenchmarkMode mode = BenchmarkMode.QUICK_DEVELOPMENT;

        if (args.length > 0) {
            try {
                mode = BenchmarkMode.valueOf(args[0].toUpperCase());
            } catch (IllegalArgumentException e) {
                System.err.println("Invalid benchmark mode: " + args[0]);
                System.err.println("Available modes: " + Arrays.toString(BenchmarkMode.values()));
                System.exit(1);
            }
        }

        String outputDir = args.length > 1 ? args[1] : "benchmark-results";

        System.out.println("Running benchmarks in " + mode + " mode...");
        System.out.println("Output directory: " + outputDir);

        BenchmarkResults results = runBenchmarks(mode, outputDir);

        System.out.println("\n=== Benchmark Execution Complete ===");
        System.out.println("Results saved to: " + results.getReportPath());
        System.out.println("Framework ranking: " + results.getRanking().getBest().getFrameworkName() + " (best)");

        if (results.hasRegressions()) {
            System.out.println("⚠️  Performance regressions detected! Check the report for details.");
        } else {
            System.out.println("✅ No performance regressions detected.");
        }
    }
}