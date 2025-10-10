package org.techishthoughts.benchmark.integration;

import org.techishthoughts.payload.service.AbstractSerializationService;
import org.techishthoughts.payload.service.SerializationService;
import org.techishthoughts.payload.service.BenchmarkConfig;
import org.techishthoughts.payload.service.SerializationException;
import org.techishthoughts.payload.service.result.BenchmarkResult;
import org.techishthoughts.payload.service.result.SerializationResult;
import org.techishthoughts.payload.model.User;

import org.techishthoughts.benchmark.statistics.BenchmarkStatistics;
import org.techishthoughts.benchmark.comparison.StatisticalComparator;
import org.techishthoughts.benchmark.regression.PerformanceRegression;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * Adapter that integrates existing AbstractSerializationService implementations
 * with the new JMH-based benchmarking framework. Provides compatibility layer
 * and enhanced statistical analysis capabilities.
 */
public class SerializationServiceBenchmarkAdapter {

    /**
     * Enhanced benchmark configuration for JMH integration
     */
    public static class EnhancedBenchmarkConfig extends BenchmarkConfig {
        private final boolean enableJMHProfiling;
        private final boolean enableStatisticalAnalysis;
        private final boolean enableRegressionDetection;
        private final double confidenceLevel;
        private final int minSampleSize;
        private final long maxBenchmarkDurationMs;

        public EnhancedBenchmarkConfig(BenchmarkConfig baseConfig,
                                     boolean enableJMHProfiling,
                                     boolean enableStatisticalAnalysis,
                                     boolean enableRegressionDetection,
                                     double confidenceLevel,
                                     int minSampleSize,
                                     long maxBenchmarkDurationMs) {
            super(baseConfig.getIterations(),
                  baseConfig.getWarmupIterations(),
                  baseConfig.getTimeoutMs(),
                  baseConfig.getComplexity(),
                  baseConfig.isEnableWarmup(),
                  baseConfig.isEnableCompression(),
                  baseConfig.isEnableMemoryMonitoring(),
                  baseConfig.isEnableRoundtripTest());

            this.enableJMHProfiling = enableJMHProfiling;
            this.enableStatisticalAnalysis = enableStatisticalAnalysis;
            this.enableRegressionDetection = enableRegressionDetection;
            this.confidenceLevel = confidenceLevel;
            this.minSampleSize = minSampleSize;
            this.maxBenchmarkDurationMs = maxBenchmarkDurationMs;
        }

        public boolean isEnableJMHProfiling() { return enableJMHProfiling; }
        public boolean isEnableStatisticalAnalysis() { return enableStatisticalAnalysis; }
        public boolean isEnableRegressionDetection() { return enableRegressionDetection; }
        public double getConfidenceLevel() { return confidenceLevel; }
        public int getMinSampleSize() { return minSampleSize; }
        public long getMaxBenchmarkDurationMs() { return maxBenchmarkDurationMs; }

        public static EnhancedBenchmarkConfig from(BenchmarkConfig baseConfig) {
            return new EnhancedBenchmarkConfig(
                baseConfig,
                true,  // Enable JMH profiling
                true,  // Enable statistical analysis
                true,  // Enable regression detection
                0.95,  // 95% confidence level
                30,    // Minimum 30 samples
                300_000 // 5 minutes max duration
            );
        }
    }

    /**
     * Enhanced benchmark result with statistical analysis
     */
    public static class EnhancedBenchmarkResult extends BenchmarkResult {
        private final BenchmarkStatistics.StatisticalSummary serializationStats;
        private final BenchmarkStatistics.StatisticalSummary deserializationStats;
        private final List<PerformanceRegression.PerformanceMeasurement> performanceHistory;
        private final StatisticalComparator.FrameworkPerformance frameworkPerformance;
        private final Map<String, Object> advancedMetrics;

        public EnhancedBenchmarkResult(BenchmarkResult baseResult,
                                     BenchmarkStatistics.StatisticalSummary serializationStats,
                                     BenchmarkStatistics.StatisticalSummary deserializationStats,
                                     List<PerformanceRegression.PerformanceMeasurement> performanceHistory,
                                     StatisticalComparator.FrameworkPerformance frameworkPerformance,
                                     Map<String, Object> advancedMetrics) {
            super(baseResult.getFrameworkName(),
                  baseResult.getConfig(),
                  baseResult.getStartTime(),
                  baseResult.getEndTime(),
                  baseResult.getSerializationResults(),
                  baseResult.getCompressionResults(),
                  baseResult.getMemoryMetrics(),
                  baseResult.isRoundtripSuccess(),
                  baseResult.getError());

            this.serializationStats = serializationStats;
            this.deserializationStats = deserializationStats;
            this.performanceHistory = new ArrayList<>(performanceHistory);
            this.frameworkPerformance = frameworkPerformance;
            this.advancedMetrics = new HashMap<>(advancedMetrics);
        }

        public BenchmarkStatistics.StatisticalSummary getSerializationStats() { return serializationStats; }
        public BenchmarkStatistics.StatisticalSummary getDeserializationStats() { return deserializationStats; }
        public List<PerformanceRegression.PerformanceMeasurement> getPerformanceHistory() {
            return new ArrayList<>(performanceHistory);
        }
        public StatisticalComparator.FrameworkPerformance getFrameworkPerformance() { return frameworkPerformance; }
        public Map<String, Object> getAdvancedMetrics() { return new HashMap<>(advancedMetrics); }

        /**
         * Get overall performance score based on multiple metrics
         */
        public double getOverallPerformanceScore() {
            double serializationScore = serializationStats != null ?
                1.0 / serializationStats.getMean() : 0.0;
            double deserializationScore = deserializationStats != null ?
                1.0 / deserializationStats.getMean() : 0.0;

            return (serializationScore + deserializationScore) / 2.0;
        }

        /**
         * Get performance grade based on statistical analysis
         */
        public String getPerformanceGrade() {
            if (serializationStats == null) return "N/A";

            BenchmarkStatistics.DataQuality quality = serializationStats.getDataQuality();
            switch (quality) {
                case EXCELLENT: return "A+";
                case GOOD: return "A";
                case FAIR: return "B";
                case POOR: return "C";
                default: return "F";
            }
        }
    }

    private final SerializationService serializationService;
    private final PerformanceRegression.RegressionDetector regressionDetector;

    public SerializationServiceBenchmarkAdapter(SerializationService serializationService) {
        this.serializationService = serializationService;
        this.regressionDetector = new PerformanceRegression.RegressionDetector();
    }

    /**
     * Run enhanced benchmark with statistical analysis
     */
    public EnhancedBenchmarkResult runEnhancedBenchmark(EnhancedBenchmarkConfig config)
            throws SerializationException {

        // Run base benchmark
        BenchmarkResult baseResult = serializationService.runBenchmark(config);

        if (baseResult.hasError()) {
            // Return enhanced result with error
            return new EnhancedBenchmarkResult(baseResult, null, null,
                new ArrayList<>(), null, new HashMap<>());
        }

        // Extract performance data for statistical analysis
        List<SerializationResult> serializationResults = baseResult.getSerializationResults();

        // Calculate statistical summaries
        BenchmarkStatistics.StatisticalSummary serializationStats = null;
        BenchmarkStatistics.StatisticalSummary deserializationStats = null;

        if (!serializationResults.isEmpty() && config.isEnableStatisticalAnalysis()) {
            double[] serializationTimes = serializationResults.stream()
                .mapToDouble(r -> r.getSerializationTime() / 1_000_000.0) // Convert to ms
                .toArray();

            serializationStats = BenchmarkStatistics.calculateSummary(serializationTimes, config.getConfidenceLevel());

            // If deserialization data is available (would need to be collected separately)
            // deserializationStats = calculateDeserializationStats(config);
        }

        // Create performance measurements for regression detection
        List<PerformanceRegression.PerformanceMeasurement> performanceHistory = new ArrayList<>();
        if (config.isEnableRegressionDetection() && serializationStats != null) {
            PerformanceRegression.PerformanceMeasurement measurement =
                new PerformanceRegression.PerformanceMeasurement(
                    LocalDateTime.now(),
                    serializationService.getFrameworkName(),
                    "serialization_latency",
                    serializationStats.getMean(),
                    1.0 - serializationStats.getCoefficientOfVariation(),
                    createMetadata(baseResult, config)
                );

            performanceHistory.add(measurement);
            regressionDetector.addMeasurement(measurement);
        }

        // Create framework performance object
        StatisticalComparator.FrameworkPerformance frameworkPerformance = null;
        if (serializationStats != null) {
            frameworkPerformance = new StatisticalComparator.FrameworkPerformance(
                serializationService.getFrameworkName());

            frameworkPerformance.addMetric(
                StatisticalComparator.ComparisonMetric.LATENCY,
                new double[]{serializationStats.getMean()}
            );

            // Add throughput metric if available
            if (serializationResults.size() > 0) {
                double avgThroughput = calculateThroughput(serializationResults, baseResult);
                frameworkPerformance.addMetric(
                    StatisticalComparator.ComparisonMetric.THROUGHPUT,
                    new double[]{avgThroughput}
                );
            }
        }

        // Calculate advanced metrics
        Map<String, Object> advancedMetrics = calculateAdvancedMetrics(
            baseResult, serializationStats, config);

        return new EnhancedBenchmarkResult(
            baseResult,
            serializationStats,
            deserializationStats,
            performanceHistory,
            frameworkPerformance,
            advancedMetrics
        );
    }

    /**
     * Compare multiple serialization services
     */
    public static StatisticalComparator.FrameworkRanking compareServices(
            List<SerializationService> services,
            EnhancedBenchmarkConfig config) throws SerializationException {

        List<StatisticalComparator.FrameworkPerformance> performances = new ArrayList<>();

        for (SerializationService service : services) {
            SerializationServiceBenchmarkAdapter adapter =
                new SerializationServiceBenchmarkAdapter(service);

            EnhancedBenchmarkResult result = adapter.runEnhancedBenchmark(config);

            if (result.getFrameworkPerformance() != null) {
                performances.add(result.getFrameworkPerformance());
            }
        }

        // Create default weights for comparison
        Map<StatisticalComparator.ComparisonMetric, Double> weights = new HashMap<>();
        weights.put(StatisticalComparator.ComparisonMetric.THROUGHPUT, 2.0);
        weights.put(StatisticalComparator.ComparisonMetric.LATENCY, 2.0);
        weights.put(StatisticalComparator.ComparisonMetric.MEMORY_USAGE, 1.0);

        return StatisticalComparator.rankFrameworks(performances, weights);
    }

    /**
     * Generate comprehensive comparison report
     */
    public static String generateComparisonReport(
            List<EnhancedBenchmarkResult> results) {

        StringBuilder report = new StringBuilder();
        report.append("=== Serialization Service Comparison Report ===\n");
        report.append("Generated: ").append(LocalDateTime.now()).append("\n\n");

        // Summary statistics
        report.append("Summary:\n");
        report.append("========\n");
        report.append("Total Services Tested: ").append(results.size()).append("\n");

        // Individual service analysis
        report.append("\nDetailed Analysis:\n");
        report.append("==================\n");

        for (EnhancedBenchmarkResult result : results) {
            report.append("\nFramework: ").append(result.getFrameworkName()).append("\n");
            report.append("Performance Grade: ").append(result.getPerformanceGrade()).append("\n");
            report.append("Overall Score: ").append(String.format("%.6f", result.getOverallPerformanceScore())).append("\n");

            if (result.getSerializationStats() != null) {
                BenchmarkStatistics.StatisticalSummary stats = result.getSerializationStats();
                report.append("Serialization Performance:\n");
                report.append("  Mean: ").append(String.format("%.3f ms", stats.getMean())).append("\n");
                report.append("  Std Dev: ").append(String.format("%.3f ms", stats.getStandardDeviation())).append("\n");
                report.append("  CV: ").append(String.format("%.2f%%", stats.getCoefficientOfVariation() * 100)).append("\n");
                report.append("  Confidence Interval: ").append(stats.getConfidenceInterval().toString()).append("\n");
                report.append("  Data Quality: ").append(stats.getDataQuality()).append("\n");

                if (stats.hasOutliers()) {
                    report.append("  Outliers: ").append(stats.getOutliers().size()).append(" detected\n");
                }
            }

            // Advanced metrics
            Map<String, Object> advancedMetrics = result.getAdvancedMetrics();
            if (!advancedMetrics.isEmpty()) {
                report.append("Advanced Metrics:\n");
                for (Map.Entry<String, Object> entry : advancedMetrics.entrySet()) {
                    report.append("  ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
                }
            }

            if (result.hasError()) {
                report.append("Error: ").append(result.getError()).append("\n");
            }

            report.append("\n");
        }

        return report.toString();
    }

    private Map<String, Object> createMetadata(BenchmarkResult result, EnhancedBenchmarkConfig config) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("iterations", config.getIterations());
        metadata.put("warmupIterations", config.getWarmupIterations());
        metadata.put("dataComplexity", config.getComplexity().toString());
        metadata.put("compressionEnabled", config.isEnableCompression());
        metadata.put("memoryMonitoringEnabled", config.isEnableMemoryMonitoring());
        metadata.put("executionTime", LocalDateTime.now());

        if (result.getMemoryMetrics() != null) {
            metadata.put("peakMemoryMB", result.getMemoryMetrics().getPeakMemoryMb());
            metadata.put("memoryDeltaMB", result.getMemoryMetrics().getFinalMemoryMb() -
                result.getMemoryMetrics().getInitialMemoryMb());
        }

        return metadata;
    }

    private double calculateThroughput(List<SerializationResult> results, BenchmarkResult benchmarkResult) {
        if (results.isEmpty()) return 0.0;

        long totalTime = benchmarkResult.getEndTime().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli() -
                        benchmarkResult.getStartTime().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();

        if (totalTime == 0) return 0.0;

        // Calculate operations per second
        return (double) results.size() * 1000.0 / totalTime;
    }

    private Map<String, Object> calculateAdvancedMetrics(BenchmarkResult baseResult,
                                                        BenchmarkStatistics.StatisticalSummary stats,
                                                        EnhancedBenchmarkConfig config) {
        Map<String, Object> metrics = new HashMap<>();

        if (stats != null) {
            metrics.put("coefficient_of_variation", stats.getCoefficientOfVariation());
            metrics.put("data_quality", stats.getDataQuality().toString());
            metrics.put("outlier_count", stats.getOutliers().size());
            metrics.put("sample_size", stats.getSampleSize());
            metrics.put("confidence_interval_width", stats.getConfidenceInterval().getWidth());
        }

        if (baseResult.getMemoryMetrics() != null) {
            BenchmarkResult.MemoryMetrics memory = baseResult.getMemoryMetrics();
            metrics.put("memory_efficiency",
                (double) memory.getInitialMemoryMb() / memory.getPeakMemoryMb());
            metrics.put("memory_stability",
                1.0 - Math.abs(memory.getFinalMemoryMb() - memory.getInitialMemoryMb()) / memory.getPeakMemoryMb());
        }

        metrics.put("benchmark_efficiency",
            (double) config.getIterations() / Math.max(1, baseResult.getDurationMs() / 1000.0));

        return metrics;
    }

    /**
     * Get regression detector for this adapter
     */
    public PerformanceRegression.RegressionDetector getRegressionDetector() {
        return regressionDetector;
    }

    /**
     * Check for performance regressions
     */
    public List<PerformanceRegression.RegressionResult> checkForRegressions() {
        return regressionDetector.detectRegressions();
    }
}