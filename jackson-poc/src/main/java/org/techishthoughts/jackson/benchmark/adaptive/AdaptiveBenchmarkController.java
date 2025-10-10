package org.techishthoughts.jackson.benchmark.adaptive;

import org.springframework.web.bind.annotation.*;
import org.techishthoughts.jackson.benchmark.statistical.OutlierDetectionStrategy;
import org.techishthoughts.jackson.benchmark.statistical.ConfidenceIntervalCalculator;
import org.techishthoughts.jackson.benchmark.statistical.StatisticalSignificanceDetector;
import org.techishthoughts.jackson.benchmark.statistical.MeasurementStabilityAnalyzer;
import org.techishthoughts.jackson.benchmark.warmup.DynamicWarmupStrategy;
import org.techishthoughts.jackson.service.JacksonSerializationService;
import org.techishthoughts.payload.generator.HugePayloadGenerator;
import org.techishthoughts.payload.model.User;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * Advanced adaptive benchmarking controller that dynamically adjusts measurement
 * parameters for optimal statistical accuracy. Integrates JIT warming, stability
 * analysis, outlier detection, and confidence interval-based stopping criteria.
 */
@RestController
@RequestMapping("/api/adaptive-benchmark")
public class AdaptiveBenchmarkController {

    private final JacksonSerializationService serializationService;

    public AdaptiveBenchmarkController(JacksonSerializationService serializationService) {
        this.serializationService = serializationService;
    }

    /**
     * Comprehensive adaptive benchmark result.
     */
    public static class AdaptiveBenchmarkResult {
        private final String benchmarkId;
        private final Instant startTime;
        private final Duration totalDuration;
        private final AdaptiveBenchmarkConfig config;

        // Warmup Results
        private final DynamicWarmupStrategy.WarmupResult warmupResult;

        // Measurement Results
        private final List<Double> rawMeasurements;
        private final List<Double> cleanedMeasurements;
        private final OutlierDetectionStrategy.OutlierAnalysis outlierAnalysis;

        // Statistical Analysis
        private final ConfidenceIntervalCalculator.ConfidenceInterval confidenceInterval;
        private final StatisticalSignificanceDetector.SignificanceResult significanceResult;
        private final MeasurementStabilityAnalyzer.StabilityAnalysis stabilityAnalysis;

        // Stopping Decision
        private final BenchmarkStoppingCriteria.StoppingDecision stoppingDecision;

        // Summary Statistics
        private final Map<String, Double> summaryStatistics;
        private final Map<String, Object> benchmarkMetadata;

        public AdaptiveBenchmarkResult(String benchmarkId, Instant startTime, Duration totalDuration,
                                     AdaptiveBenchmarkConfig config, DynamicWarmupStrategy.WarmupResult warmupResult,
                                     List<Double> rawMeasurements, List<Double> cleanedMeasurements,
                                     OutlierDetectionStrategy.OutlierAnalysis outlierAnalysis,
                                     ConfidenceIntervalCalculator.ConfidenceInterval confidenceInterval,
                                     StatisticalSignificanceDetector.SignificanceResult significanceResult,
                                     MeasurementStabilityAnalyzer.StabilityAnalysis stabilityAnalysis,
                                     BenchmarkStoppingCriteria.StoppingDecision stoppingDecision,
                                     Map<String, Double> summaryStatistics, Map<String, Object> benchmarkMetadata) {
            this.benchmarkId = benchmarkId;
            this.startTime = startTime;
            this.totalDuration = totalDuration;
            this.config = config;
            this.warmupResult = warmupResult;
            this.rawMeasurements = new ArrayList<>(rawMeasurements);
            this.cleanedMeasurements = new ArrayList<>(cleanedMeasurements);
            this.outlierAnalysis = outlierAnalysis;
            this.confidenceInterval = confidenceInterval;
            this.significanceResult = significanceResult;
            this.stabilityAnalysis = stabilityAnalysis;
            this.stoppingDecision = stoppingDecision;
            this.summaryStatistics = new HashMap<>(summaryStatistics);
            this.benchmarkMetadata = new HashMap<>(benchmarkMetadata);
        }

        // Getters
        public String getBenchmarkId() { return benchmarkId; }
        public Instant getStartTime() { return startTime; }
        public Duration getTotalDuration() { return totalDuration; }
        public AdaptiveBenchmarkConfig getConfig() { return config; }
        public DynamicWarmupStrategy.WarmupResult getWarmupResult() { return warmupResult; }
        public List<Double> getRawMeasurements() { return rawMeasurements; }
        public List<Double> getCleanedMeasurements() { return cleanedMeasurements; }
        public OutlierDetectionStrategy.OutlierAnalysis getOutlierAnalysis() { return outlierAnalysis; }
        public ConfidenceIntervalCalculator.ConfidenceInterval getConfidenceInterval() { return confidenceInterval; }
        public StatisticalSignificanceDetector.SignificanceResult getSignificanceResult() { return significanceResult; }
        public MeasurementStabilityAnalyzer.StabilityAnalysis getStabilityAnalysis() { return stabilityAnalysis; }
        public BenchmarkStoppingCriteria.StoppingDecision getStoppingDecision() { return stoppingDecision; }
        public Map<String, Double> getSummaryStatistics() { return summaryStatistics; }
        public Map<String, Object> getBenchmarkMetadata() { return benchmarkMetadata; }

        public boolean isSuccessful() {
            return stoppingDecision.shouldStop() &&
                   !stoppingDecision.getPrimaryReason().equals(BenchmarkStoppingCriteria.StoppingReason.TIME_LIMIT_REACHED) &&
                   !stoppingDecision.getPrimaryReason().equals(BenchmarkStoppingCriteria.StoppingReason.MAXIMUM_SAMPLES_REACHED);
        }

        public double getMeanPerformance() {
            return summaryStatistics.getOrDefault("mean", 0.0);
        }

        public double getPerformanceStdDev() {
            return summaryStatistics.getOrDefault("stdDev", 0.0);
        }
    }

    /**
     * Runs comprehensive adaptive benchmark for Jackson serialization.
     */
    @PostMapping("/serialization")
    public Map<String, Object> runAdaptiveSerializationBenchmark(@RequestBody Map<String, Object> request) {
        String benchmarkId = UUID.randomUUID().toString();
        Instant startTime = Instant.now();

        try {
            // Parse configuration
            AdaptiveBenchmarkConfig config = parseConfiguration(request);
            String complexity = (String) request.getOrDefault("complexity", "MEDIUM");
            String format = (String) request.getOrDefault("format", "json");

            // Generate test data
            HugePayloadGenerator.ComplexityLevel level = HugePayloadGenerator.ComplexityLevel.valueOf(complexity.toUpperCase());
            List<User> testData = HugePayloadGenerator.generateHugeDataset(level);

            // Create benchmark function
            Supplier<Double> benchmarkFunction = createSerializationBenchmarkFunction(testData, format);

            // Execute adaptive benchmark
            AdaptiveBenchmarkResult result = executeAdaptiveBenchmark(benchmarkId, config, benchmarkFunction);

            // Return formatted response
            return formatBenchmarkResponse(result, testData.size(), complexity, format);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("benchmarkId", benchmarkId);
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            errorResponse.put("duration", Duration.between(startTime, Instant.now()).toString());
            return errorResponse;
        }
    }

    /**
     * Runs adaptive benchmark for compression performance.
     */
    @PostMapping("/compression")
    public Map<String, Object> runAdaptiveCompressionBenchmark(@RequestBody Map<String, Object> request) {
        String benchmarkId = UUID.randomUUID().toString();
        Instant startTime = Instant.now();

        try {
            AdaptiveBenchmarkConfig config = parseConfiguration(request);
            String complexity = (String) request.getOrDefault("complexity", "MEDIUM");
            String algorithm = (String) request.getOrDefault("algorithm", "gzip");

            HugePayloadGenerator.ComplexityLevel level = HugePayloadGenerator.ComplexityLevel.valueOf(complexity.toUpperCase());
            List<User> testData = HugePayloadGenerator.generateHugeDataset(level);

            // Pre-serialize data for compression
            JacksonSerializationService.SerializationResult jsonResult =
                serializationService.serializeUsersToJson(testData);
            byte[] dataToCompress = jsonResult.getData();

            Supplier<Double> benchmarkFunction = createCompressionBenchmarkFunction(dataToCompress, algorithm);
            AdaptiveBenchmarkResult result = executeAdaptiveBenchmark(benchmarkId, config, benchmarkFunction);

            return formatBenchmarkResponse(result, testData.size(), complexity, algorithm);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("benchmarkId", benchmarkId);
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            errorResponse.put("duration", Duration.between(startTime, Instant.now()).toString());
            return errorResponse;
        }
    }

    /**
     * Runs comprehensive roundtrip benchmark with adaptive measurement.
     */
    @PostMapping("/roundtrip")
    public Map<String, Object> runAdaptiveRoundtripBenchmark(@RequestBody Map<String, Object> request) {
        String benchmarkId = UUID.randomUUID().toString();
        Instant startTime = Instant.now();

        try {
            AdaptiveBenchmarkConfig config = parseConfiguration(request);
            String complexity = (String) request.getOrDefault("complexity", "MEDIUM");
            String format = (String) request.getOrDefault("format", "json");

            HugePayloadGenerator.ComplexityLevel level = HugePayloadGenerator.ComplexityLevel.valueOf(complexity.toUpperCase());
            List<User> testData = HugePayloadGenerator.generateHugeDataset(level);

            Supplier<Double> benchmarkFunction = createRoundtripBenchmarkFunction(testData, format);
            AdaptiveBenchmarkResult result = executeAdaptiveBenchmark(benchmarkId, config, benchmarkFunction);

            return formatBenchmarkResponse(result, testData.size(), complexity, format);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("benchmarkId", benchmarkId);
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            errorResponse.put("duration", Duration.between(startTime, Instant.now()).toString());
            return errorResponse;
        }
    }

    /**
     * Provides benchmark configuration recommendations based on requirements.
     */
    @PostMapping("/recommend-config")
    public Map<String, Object> recommendConfiguration(@RequestBody Map<String, Object> requirements) {
        Map<String, Object> response = new HashMap<>();

        try {
            String accuracy = (String) requirements.getOrDefault("accuracy", "medium");
            String timeConstraint = (String) requirements.getOrDefault("timeConstraint", "moderate");
            boolean isProduction = (Boolean) requirements.getOrDefault("isProduction", false);

            AdaptiveBenchmarkConfig recommendedConfig = recommendConfigurationBasedOnRequirements(
                accuracy, timeConstraint, isProduction);

            response.put("success", true);
            response.put("recommendedConfig", configToMap(recommendedConfig));
            response.put("reasoning", buildConfigurationReasoning(accuracy, timeConstraint, isProduction));

        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
        }

        return response;
    }

    /**
     * Gets the status of available configurations and their use cases.
     */
    @GetMapping("/config-options")
    public Map<String, Object> getConfigurationOptions() {
        Map<String, Object> response = new HashMap<>();

        Map<String, Object> defaultConfig = configToMap(AdaptiveBenchmarkConfig.defaultConfig());
        Map<String, Object> highPrecisionConfig = configToMap(AdaptiveBenchmarkConfig.highPrecisionConfig());
        Map<String, Object> quickConfig = configToMap(AdaptiveBenchmarkConfig.quickConfig());

        Map<String, Object> configs = new HashMap<>();
        configs.put("default", Map.of(
            "config", defaultConfig,
            "useCase", "General purpose benchmarking with balanced accuracy and speed",
            "estimatedDuration", "5-30 minutes",
            "accuracy", "Good"
        ));
        configs.put("highPrecision", Map.of(
            "config", highPrecisionConfig,
            "useCase", "High-precision measurements for research and detailed analysis",
            "estimatedDuration", "30 minutes - 6 hours",
            "accuracy", "Excellent"
        ));
        configs.put("quick", Map.of(
            "config", quickConfig,
            "useCase", "Quick performance checks and development testing",
            "estimatedDuration", "30 seconds - 15 minutes",
            "accuracy", "Basic"
        ));

        response.put("availableConfigurations", configs);
        response.put("configurationGuide", buildConfigurationGuide());

        return response;
    }

    // Private implementation methods

    private AdaptiveBenchmarkConfig parseConfiguration(Map<String, Object> request) {
        String configType = (String) request.getOrDefault("configType", "default");

        switch (configType.toLowerCase()) {
            case "high-precision":
            case "precision":
                return AdaptiveBenchmarkConfig.highPrecisionConfig();
            case "quick":
            case "fast":
                return AdaptiveBenchmarkConfig.quickConfig();
            case "default":
            default:
                return AdaptiveBenchmarkConfig.defaultConfig();
        }
    }

    private Supplier<Double> createSerializationBenchmarkFunction(List<User> testData, String format) {
        return () -> {
            try {
                long startTime = System.nanoTime();

                switch (format.toLowerCase()) {
                    case "cbor":
                        serializationService.serializeUsersToCbor(testData);
                        break;
                    case "messagepack":
                        serializationService.serializeUsersToMessagePack(testData);
                        break;
                    case "smile":
                        serializationService.serializeUsersToSmile(testData);
                        break;
                    case "compact":
                        serializationService.serializeUsersToCompactJson(testData);
                        break;
                    case "json":
                    default:
                        serializationService.serializeUsersToJson(testData);
                        break;
                }

                long endTime = System.nanoTime();
                return (endTime - startTime) / 1_000_000.0; // Convert to milliseconds

            } catch (Exception e) {
                throw new RuntimeException("Benchmark execution failed: " + e.getMessage(), e);
            }
        };
    }

    private Supplier<Double> createCompressionBenchmarkFunction(byte[] data, String algorithm) {
        return () -> {
            try {
                long startTime = System.nanoTime();

                switch (algorithm.toLowerCase()) {
                    case "zstd":
                        serializationService.compressWithZstd(data);
                        break;
                    case "brotli":
                        serializationService.compressWithBrotli(data);
                        break;
                    case "gzip":
                    default:
                        serializationService.compressWithGzip(data);
                        break;
                }

                long endTime = System.nanoTime();
                return (endTime - startTime) / 1_000_000.0; // Convert to milliseconds

            } catch (Exception e) {
                throw new RuntimeException("Compression benchmark failed: " + e.getMessage(), e);
            }
        };
    }

    private Supplier<Double> createRoundtripBenchmarkFunction(List<User> testData, String format) {
        return () -> {
            try {
                long startTime = System.nanoTime();

                // Serialize
                JacksonSerializationService.SerializationResult serResult;
                switch (format.toLowerCase()) {
                    case "cbor":
                        serResult = serializationService.serializeUsersToCbor(testData);
                        serializationService.deserializeUsersFromCbor(serResult.getData());
                        break;
                    case "messagepack":
                        serResult = serializationService.serializeUsersToMessagePack(testData);
                        serializationService.deserializeUsersFromMessagePack(serResult.getData());
                        break;
                    case "smile":
                        serResult = serializationService.serializeUsersToSmile(testData);
                        serializationService.deserializeUsersFromSmile(serResult.getData());
                        break;
                    case "compact":
                        serResult = serializationService.serializeUsersToCompactJson(testData);
                        serializationService.deserializeUsersFromCompactJson(serResult.getData());
                        break;
                    case "json":
                    default:
                        serResult = serializationService.serializeUsersToJson(testData);
                        serializationService.deserializeUsersFromJson(serResult.getData());
                        break;
                }

                long endTime = System.nanoTime();
                return (endTime - startTime) / 1_000_000.0; // Convert to milliseconds

            } catch (Exception e) {
                throw new RuntimeException("Roundtrip benchmark failed: " + e.getMessage(), e);
            }
        };
    }

    private AdaptiveBenchmarkResult executeAdaptiveBenchmark(String benchmarkId, AdaptiveBenchmarkConfig config,
                                                           Supplier<Double> benchmarkFunction) {
        Instant overallStart = Instant.now();
        Map<String, Object> metadata = new HashMap<>();

        // Phase 1: Dynamic Warmup
        DynamicWarmupStrategy warmupStrategy = new DynamicWarmupStrategy(config);
        DynamicWarmupStrategy.WarmupResult warmupResult = warmupStrategy.executeWarmup(benchmarkFunction);

        metadata.put("warmupPhase", warmupResult);

        // Phase 2: Adaptive Measurement Collection
        List<Double> measurements = new ArrayList<>();
        BenchmarkStoppingCriteria stoppingCriteria = new BenchmarkStoppingCriteria(config);

        AtomicInteger iterationCount = new AtomicInteger(0);
        while (measurements.size() < config.getMaximumSampleSize()) {
            double measurement = benchmarkFunction.get();
            measurements.add(measurement);
            iterationCount.incrementAndGet();

            // Check stopping criteria every few measurements
            if (measurements.size() >= config.getMinimumSampleSize() && measurements.size() % 5 == 0) {
                BenchmarkStoppingCriteria.StoppingDecision decision = stoppingCriteria.evaluateStoppingCriteria(measurements);
                if (decision.shouldStop()) {
                    metadata.put("stoppingDecision", decision);
                    break;
                }
            }

            // Timeout check
            if (Duration.between(overallStart, Instant.now()).compareTo(config.getMaxBenchmarkDuration()) > 0) {
                break;
            }
        }

        // Final stopping decision
        BenchmarkStoppingCriteria.StoppingDecision finalStoppingDecision =
            stoppingCriteria.evaluateStoppingCriteria(measurements);

        // Phase 3: Statistical Analysis
        OutlierDetectionStrategy outlierDetector = new OutlierDetectionStrategy(config);
        OutlierDetectionStrategy.OutlierAnalysis outlierAnalysis = outlierDetector.detectOutliers(measurements);

        List<Double> cleanedMeasurements = outlierDetector.handleOutliers(measurements, outlierAnalysis);

        ConfidenceIntervalCalculator confidenceCalculator = new ConfidenceIntervalCalculator(config.getConfidenceLevel());
        ConfidenceIntervalCalculator.ConfidenceInterval confidenceInterval =
            confidenceCalculator.calculateTConfidenceInterval(cleanedMeasurements);

        StatisticalSignificanceDetector significanceDetector = new StatisticalSignificanceDetector(config);
        StatisticalSignificanceDetector.SignificanceResult significanceResult =
            significanceDetector.performOneSampleTTest(cleanedMeasurements);

        MeasurementStabilityAnalyzer stabilityAnalyzer = new MeasurementStabilityAnalyzer(config);
        MeasurementStabilityAnalyzer.StabilityAnalysis stabilityAnalysis =
            stabilityAnalyzer.analyzeStability(cleanedMeasurements);

        // Calculate summary statistics
        Map<String, Double> summaryStats = calculateSummaryStatistics(cleanedMeasurements, outlierAnalysis);

        Duration totalDuration = Duration.between(overallStart, Instant.now());
        metadata.put("totalIterations", iterationCount.get());
        metadata.put("measurementIterations", measurements.size());
        metadata.put("cleanedMeasurements", cleanedMeasurements.size());
        metadata.put("outlierCount", outlierAnalysis.getOutlierCount());
        metadata.put("configUsed", config.toString());

        return new AdaptiveBenchmarkResult(benchmarkId, overallStart, totalDuration, config, warmupResult,
                                         measurements, cleanedMeasurements, outlierAnalysis, confidenceInterval,
                                         significanceResult, stabilityAnalysis, finalStoppingDecision,
                                         summaryStats, metadata);
    }

    private Map<String, Double> calculateSummaryStatistics(List<Double> measurements,
                                                          OutlierDetectionStrategy.OutlierAnalysis outlierAnalysis) {
        Map<String, Double> stats = new HashMap<>();

        if (measurements.isEmpty()) {
            return stats;
        }

        double mean = measurements.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double variance = measurements.stream()
                .mapToDouble(value -> Math.pow(value - mean, 2))
                .sum() / (measurements.size() - 1);
        double stdDev = Math.sqrt(variance);

        stats.put("mean", mean);
        stats.put("stdDev", stdDev);
        stats.put("cv", mean > 0 ? stdDev / mean : Double.MAX_VALUE);
        stats.put("min", measurements.stream().mapToDouble(Double::doubleValue).min().orElse(0.0));
        stats.put("max", measurements.stream().mapToDouble(Double::doubleValue).max().orElse(0.0));

        // Percentiles
        double[] sortedValues = measurements.stream().mapToDouble(Double::doubleValue).sorted().toArray();
        stats.put("median", calculatePercentile(sortedValues, 0.5));
        stats.put("p95", calculatePercentile(sortedValues, 0.95));
        stats.put("p99", calculatePercentile(sortedValues, 0.99));

        // Add robust statistics
        Map<String, Double> robustStats = outlierAnalysis.getStatistics().entrySet().stream()
                .filter(entry -> entry.getValue() instanceof Double)
                .collect(HashMap::new, (map, entry) -> map.put(entry.getKey(), (Double) entry.getValue()), HashMap::putAll);
        stats.putAll(robustStats);

        return stats;
    }

    private double calculatePercentile(double[] sortedValues, double percentile) {
        if (sortedValues.length == 0) return 0.0;
        if (sortedValues.length == 1) return sortedValues[0];

        double index = percentile * (sortedValues.length - 1);
        int lowerIndex = (int) Math.floor(index);
        int upperIndex = (int) Math.ceil(index);

        if (lowerIndex == upperIndex) {
            return sortedValues[lowerIndex];
        } else {
            double weight = index - lowerIndex;
            return sortedValues[lowerIndex] * (1 - weight) + sortedValues[upperIndex] * weight;
        }
    }

    private Map<String, Object> formatBenchmarkResponse(AdaptiveBenchmarkResult result, int dataSize,
                                                       String complexity, String operation) {
        Map<String, Object> response = new HashMap<>();

        // Basic Information
        response.put("benchmarkId", result.getBenchmarkId());
        response.put("success", result.isSuccessful());
        response.put("startTime", result.getStartTime().toString());
        response.put("totalDuration", result.getTotalDuration().toString());

        // Test Configuration
        Map<String, Object> testInfo = new HashMap<>();
        testInfo.put("dataSize", dataSize);
        testInfo.put("complexity", complexity);
        testInfo.put("operation", operation);
        response.put("testConfiguration", testInfo);

        // Warmup Results
        Map<String, Object> warmup = new HashMap<>();
        warmup.put("completed", result.getWarmupResult().isComplete());
        warmup.put("iterations", result.getWarmupResult().getTotalIterations());
        warmup.put("duration", result.getWarmupResult().getTotalDuration().toString());
        warmup.put("finalCV", result.getWarmupResult().getCoefficientOfVariation());
        response.put("warmup", warmup);

        // Measurement Results
        Map<String, Object> measurements = new HashMap<>();
        measurements.put("totalSamples", result.getRawMeasurements().size());
        measurements.put("cleanedSamples", result.getCleanedMeasurements().size());
        measurements.put("outlierCount", result.getOutlierAnalysis().getOutlierCount());
        measurements.put("outlierRate", result.getOutlierAnalysis().getOutlierRate());
        response.put("measurements", measurements);

        // Performance Statistics
        Map<String, Object> performance = new HashMap<>();
        performance.put("mean", result.getMeanPerformance());
        performance.put("stdDev", result.getPerformanceStdDev());
        performance.put("coefficientOfVariation", result.getSummaryStatistics().get("cv"));
        performance.put("median", result.getSummaryStatistics().get("median"));
        performance.put("p95", result.getSummaryStatistics().get("p95"));
        performance.put("p99", result.getSummaryStatistics().get("p99"));
        response.put("performance", performance);

        // Statistical Analysis
        Map<String, Object> statistics = new HashMap<>();
        statistics.put("confidenceInterval", Map.of(
            "lowerBound", result.getConfidenceInterval().getLowerBound(),
            "upperBound", result.getConfidenceInterval().getUpperBound(),
            "width", result.getConfidenceInterval().getWidth(),
            "relativeWidth", result.getConfidenceInterval().getRelativeWidth()
        ));
        statistics.put("significance", Map.of(
            "isSignificant", result.getSignificanceResult().isSignificant(),
            "pValue", result.getSignificanceResult().getPValue(),
            "effectSize", result.getSignificanceResult().getEffectSize(),
            "power", result.getSignificanceResult().getPower()
        ));
        statistics.put("stability", Map.of(
            "isStable", result.getStabilityAnalysis().isStable(),
            "stabilityScore", result.getStabilityAnalysis().getStabilityScore(),
            "hasChangePoint", result.getStabilityAnalysis().hasChangePoint()
        ));
        response.put("statisticalAnalysis", statistics);

        // Stopping Decision
        Map<String, Object> stopping = new HashMap<>();
        stopping.put("reason", result.getStoppingDecision().getPrimaryReason().toString());
        stopping.put("explanation", result.getStoppingDecision().getDetailedExplanation());
        stopping.put("confidenceScore", result.getStoppingDecision().getConfidenceScore());
        stopping.put("stabilityScore", result.getStoppingDecision().getStabilityScore());
        response.put("stoppingCriteria", stopping);

        // Metadata
        response.put("metadata", result.getBenchmarkMetadata());

        return response;
    }

    private AdaptiveBenchmarkConfig recommendConfigurationBasedOnRequirements(String accuracy,
                                                                            String timeConstraint, boolean isProduction) {
        if ("high".equals(accuracy) || isProduction) {
            return AdaptiveBenchmarkConfig.highPrecisionConfig();
        } else if ("low".equals(accuracy) || "tight".equals(timeConstraint)) {
            return AdaptiveBenchmarkConfig.quickConfig();
        } else {
            return AdaptiveBenchmarkConfig.defaultConfig();
        }
    }

    private Map<String, Object> configToMap(AdaptiveBenchmarkConfig config) {
        Map<String, Object> configMap = new HashMap<>();
        configMap.put("confidenceLevel", config.getConfidenceLevel());
        configMap.put("marginOfError", config.getMarginOfError());
        configMap.put("minimumSampleSize", config.getMinimumSampleSize());
        configMap.put("maximumSampleSize", config.getMaximumSampleSize());
        configMap.put("minimumWarmupIterations", config.getMinimumWarmupIterations());
        configMap.put("maximumWarmupIterations", config.getMaximumWarmupIterations());
        configMap.put("coefficientOfVariationThreshold", config.getCoefficientOfVariationThreshold());
        configMap.put("outlierThreshold", config.getOutlierThreshold());
        configMap.put("maxBenchmarkDuration", config.getMaxBenchmarkDuration().toString());
        return configMap;
    }

    private String buildConfigurationReasoning(String accuracy, String timeConstraint, boolean isProduction) {
        StringBuilder reasoning = new StringBuilder();
        reasoning.append("Configuration selected based on: ");

        if (isProduction) {
            reasoning.append("Production environment requires high precision. ");
        }

        switch (accuracy.toLowerCase()) {
            case "high":
                reasoning.append("High accuracy requirements necessitate extensive sampling and statistical rigor. ");
                break;
            case "low":
                reasoning.append("Low accuracy requirements allow for quick measurements. ");
                break;
            default:
                reasoning.append("Medium accuracy provides balanced approach. ");
        }

        switch (timeConstraint.toLowerCase()) {
            case "tight":
                reasoning.append("Time constraints require minimal sampling with basic statistical checks.");
                break;
            case "relaxed":
                reasoning.append("Relaxed time constraints allow for comprehensive analysis.");
                break;
            default:
                reasoning.append("Moderate time constraints balanced with statistical requirements.");
        }

        return reasoning.toString();
    }

    private Map<String, Object> buildConfigurationGuide() {
        Map<String, Object> guide = new HashMap<>();

        guide.put("configurationParameters", Map.of(
            "confidenceLevel", "Statistical confidence level (0.90-0.99)",
            "marginOfError", "Acceptable relative error in results (0.01-0.10)",
            "sampleSize", "Number of measurements (min-max range)",
            "warmupIterations", "JIT warmup iterations (min-max range)",
            "coefficientOfVariationThreshold", "Acceptable variability in measurements",
            "outlierThreshold", "Threshold for outlier detection (typically 2.0-3.5)"
        ));

        guide.put("usageGuidelines", List.of(
            "Use quick config for development and preliminary testing",
            "Use default config for regular benchmarking and comparisons",
            "Use high-precision config for research and final validation",
            "Consider time constraints when selecting configuration",
            "Production environments should use higher precision settings"
        ));

        return guide;
    }
}