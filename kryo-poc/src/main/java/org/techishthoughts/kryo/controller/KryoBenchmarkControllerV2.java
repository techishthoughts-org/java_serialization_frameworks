package org.techishthoughts.kryo.controller;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.techishthoughts.kryo.service.KryoSerializationService;
import org.techishthoughts.payload.generator.HugePayloadGenerator.ComplexityLevel;
import org.techishthoughts.payload.service.BenchmarkConfig;
import org.techishthoughts.payload.service.SerializationException;
import org.techishthoughts.payload.service.result.BenchmarkResult;

/**
 * Modernized Kryo benchmark controller using the new service architecture.
 * Provides clean API endpoints with proper error handling and standardized responses.
 */
@RestController
@RequestMapping("/api/kryo/v2")
public class KryoBenchmarkControllerV2 {

    private static final Logger logger = LoggerFactory.getLogger(KryoBenchmarkControllerV2.class);

    private final KryoSerializationService serializationService;

    public KryoBenchmarkControllerV2(KryoSerializationService serializationService) {
        this.serializationService = serializationService;
    }

    @GetMapping("/info")
    public Map<String, Object> getFrameworkInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("framework", serializationService.getFrameworkName());
        info.put("version", "2.0");
        info.put("supportedCompressionAlgorithms", serializationService.getSupportedCompressionAlgorithms());
        info.put("supportsSchemaEvolution", serializationService.supportsSchemaEvolution());
        info.put("typicalUseCase", serializationService.getTypicalUseCase());
        info.put("endpoints", Map.of(
            "benchmark", "/api/kryo/v2/benchmark",
            "info", "/api/kryo/v2/info",
            "health", "/actuator/health"
        ));
        return info;
    }

    @PostMapping("/benchmark")
    public Map<String, Object> runBenchmark(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();

        try {
            // Parse request parameters
            String complexityStr = (String) request.getOrDefault("complexity", "MEDIUM");
            Integer iterations = (Integer) request.getOrDefault("iterations", 100);
            Boolean enableWarmup = (Boolean) request.getOrDefault("enableWarmup", true);
            Boolean enableCompression = (Boolean) request.getOrDefault("enableCompression", true);
            Boolean enableRoundtrip = (Boolean) request.getOrDefault("enableRoundtrip", true);
            Boolean enableMemoryMonitoring = (Boolean) request.getOrDefault("enableMemoryMonitoring", true);

            // Validate complexity level
            ComplexityLevel complexity;
            try {
                complexity = ComplexityLevel.valueOf(complexityStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                response.put("success", false);
                response.put("error", "Invalid complexity level: " + complexityStr);
                response.put("validComplexityLevels", ComplexityLevel.values());
                return response;
            }

            // Create benchmark configuration
            BenchmarkConfig config = BenchmarkConfig.builder()
                    .withComplexity(complexity)
                    .withIterations(iterations)
                    .withWarmup(enableWarmup, 10)
                    .withCompression(enableCompression)
                    .withRoundtripTest(enableRoundtrip)
                    .withMemoryMonitoring(enableMemoryMonitoring)
                    .withTimeout(300000); // 5 minutes

            logger.info("Starting Kryo benchmark with config: {}", config);

            // Run benchmark
            BenchmarkResult result = serializationService.runBenchmark(config);

            // Build response
            response.put("success", result.isSuccess());
            response.put("framework", result.getFramework());
            response.put("complexity", complexity.name());
            response.put("iterations", iterations);
            response.put("totalDurationMs", result.getTotalDurationMs());
            response.put("successfulSerializations", result.getSuccessfulSerializations());
            response.put("successfulCompressions", result.getSuccessfulCompressions());
            response.put("successRate", result.getSuccessRatePercent());
            response.put("roundtripSuccess", result.isRoundtripSuccess());

            // Add performance metrics
            result.getAverageSerializationTimeMs().ifPresent(avgTime ->
                    response.put("averageSerializationTimeMs", avgTime));
            result.getAverageCompressionRatio().ifPresent(avgRatio ->
                    response.put("averageCompressionRatio", avgRatio));
            result.getAverageSerializedSizeBytes().ifPresent(avgSize ->
                    response.put("averageSerializedSizeBytes", avgSize));

            // Add memory metrics if available
            if (result.getMemoryMetrics() != null) {
                Map<String, Object> memoryInfo = new HashMap<>();
                memoryInfo.put("peakMemoryMb", result.getMemoryMetrics().getPeakMemoryMb());
                memoryInfo.put("memoryDeltaMb", result.getMemoryMetrics().getMemoryDeltaMb());
                response.put("memoryMetrics", memoryInfo);
            }

            // Standard format for compatibility with existing benchmark scripts
            response.put("serializationTimeMs", result.getAverageSerializationTimeMs().orElse(0.0));
            response.put("deserializationTimeMs", 0.0); // Not measured in current implementation
            response.put("totalSizeBytes", result.getAverageSerializedSizeBytes().orElse(0.0));
            response.put("userCount", complexity.getUserCount());

            logger.info("Kryo benchmark completed successfully");

        } catch (SerializationException e) {
            logger.error("Serialization error during benchmark", e);
            response.put("success", false);
            response.put("error", e.getMessage());
            response.put("framework", e.getFrameworkName());
            response.put("operation", e.getOperation());
        } catch (Exception e) {
            logger.error("Unexpected error during benchmark", e);
            response.put("success", false);
            response.put("error", "Unexpected error: " + e.getMessage());
        }

        return response;
    }

    @PostMapping("/benchmark/serialization")
    public Map<String, Object> runSerializationBenchmark(@RequestBody Map<String, Object> request) {
        // Legacy endpoint for compatibility with existing scripts
        return runBenchmark(request);
    }

    @PostMapping("/benchmark/compression")
    public Map<String, Object> runCompressionBenchmark(@RequestBody Map<String, Object> request) {
        // Legacy endpoint for compatibility with existing scripts
        Map<String, Object> modifiedRequest = new HashMap<>(request);
        modifiedRequest.put("enableCompression", true);
        modifiedRequest.put("enableRoundtrip", false); // Focus on compression
        return runBenchmark(modifiedRequest);
    }

    @PostMapping("/benchmark/performance")
    public Map<String, Object> runPerformanceBenchmark(@RequestBody Map<String, Object> request) {
        // Legacy endpoint for compatibility with existing scripts
        Map<String, Object> modifiedRequest = new HashMap<>(request);

        // Map legacy payload_size parameter to complexity
        String payloadSize = (String) request.get("payload_size");
        if (payloadSize != null) {
            String complexity = switch (payloadSize.toLowerCase()) {
                case "small" -> "SMALL";
                case "large" -> "LARGE";
                case "huge" -> "HUGE";
                default -> "MEDIUM";
            };
            modifiedRequest.put("complexity", complexity);
        }

        return runBenchmark(modifiedRequest);
    }

    @GetMapping("/test-roundtrip")
    public Map<String, Object> testRoundtrip(@RequestParam(value = "complexity", defaultValue = "SMALL") String complexity) {
        Map<String, Object> response = new HashMap<>();

        try {
            ComplexityLevel level = ComplexityLevel.valueOf(complexity.toUpperCase());

            BenchmarkConfig config = BenchmarkConfig.builder()
                    .withComplexity(level)
                    .withIterations(1)
                    .withWarmup(false, 0)
                    .withCompression(false)
                    .withRoundtripTest(true)
                    .withMemoryMonitoring(false);

            BenchmarkResult result = serializationService.runBenchmark(config);

            response.put("success", result.isSuccess());
            response.put("roundtripSuccessful", result.isRoundtripSuccess());
            response.put("complexity", complexity);
            response.put("userCount", level.getUserCount());

            if (result.isSuccess() && !result.getSerializationResults().isEmpty()) {
                var serResult = result.getSerializationResults().get(0);
                response.put("serializationTime", serResult.getSerializationTimeMs());
                response.put("size", serResult.getSizeBytes());
            }

        } catch (Exception e) {
            logger.error("Error during roundtrip test", e);
            response.put("success", false);
            response.put("error", e.getMessage());
        }

        return response;
    }

    @GetMapping("/health")
    public Map<String, Object> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("framework", serializationService.getFrameworkName());
        health.put("timestamp", System.currentTimeMillis());

        try {
            // Quick serialization test
            BenchmarkConfig testConfig = BenchmarkConfig.builder()
                    .withComplexity(ComplexityLevel.SMALL)
                    .withIterations(1)
                    .withWarmup(false, 0)
                    .withCompression(false)
                    .withRoundtripTest(true)
                    .withMemoryMonitoring(false);

            BenchmarkResult result = serializationService.runBenchmark(testConfig);
            health.put("functionalTest", result.isSuccess() && result.isRoundtripSuccess() ? "PASS" : "FAIL");

        } catch (Exception e) {
            health.put("status", "DOWN");
            health.put("functionalTest", "FAIL");
            health.put("error", e.getMessage());
        }

        return health;
    }
}
