package org.techishthoughts.bson.controller;

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
import org.techishthoughts.bson.service.BsonSerializationServiceV2;
import org.techishthoughts.payload.generator.HugePayloadGenerator.ComplexityLevel;
import org.techishthoughts.payload.service.BenchmarkConfig;
import org.techishthoughts.payload.service.SerializationException;
import org.techishthoughts.payload.service.result.BenchmarkResult;

@RestController
@RequestMapping("/api/bson/v2")
public class BsonBenchmarkControllerV2 {

    private static final Logger logger = LoggerFactory.getLogger(BsonBenchmarkControllerV2.class);

    private final BsonSerializationServiceV2 serializationService;

    public BsonBenchmarkControllerV2(BsonSerializationServiceV2 serializationService) {
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
            "benchmark", "/api/bson/v2/benchmark",
            "info", "/api/bson/v2/info",
            "health", "/actuator/health"
        ));
        return info;
    }

    @PostMapping("/benchmark")
    public Map<String, Object> runBenchmark(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();

        try {
            String complexityStr = (String) request.getOrDefault("complexity", "MEDIUM");
            Integer iterations = (Integer) request.getOrDefault("iterations", 100);
            Boolean enableWarmup = (Boolean) request.getOrDefault("enableWarmup", true);
            Boolean enableCompression = (Boolean) request.getOrDefault("enableCompression", true);
            Boolean enableRoundtrip = (Boolean) request.getOrDefault("enableRoundtrip", true);
            Boolean enableMemoryMonitoring = (Boolean) request.getOrDefault("enableMemoryMonitoring", true);

            ComplexityLevel complexity;
            try {
                complexity = ComplexityLevel.valueOf(complexityStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                response.put("success", false);
                response.put("error", "Invalid complexity level: " + complexityStr);
                response.put("validComplexityLevels", ComplexityLevel.values());
                return response;
            }

            BenchmarkConfig config = BenchmarkConfig.builder()
                    .withComplexity(complexity)
                    .withIterations(iterations)
                    .withWarmup(enableWarmup, 10)
                    .withCompression(enableCompression)
                    .withRoundtripTest(enableRoundtrip)
                    .withMemoryMonitoring(enableMemoryMonitoring)
                    .withTimeout(300000);

            logger.info("Starting BSON benchmark with config: {}", config);

            BenchmarkResult result = serializationService.runBenchmark(config);

            response.put("success", result.isSuccess());
            response.put("framework", result.getFramework());
            response.put("complexity", complexity.name());
            response.put("iterations", iterations);
            response.put("totalDurationMs", result.getTotalDurationMs());
            response.put("successfulSerializations", result.getSuccessfulSerializations());
            response.put("successfulCompressions", result.getSuccessfulCompressions());
            response.put("successRate", result.getSuccessRatePercent());
            response.put("roundtripSuccess", result.isRoundtripSuccess());

            result.getAverageSerializationTimeMs().ifPresent(avgTime ->
                    response.put("averageSerializationTimeMs", avgTime));
            result.getAverageCompressionRatio().ifPresent(avgRatio ->
                    response.put("averageCompressionRatio", avgRatio));
            result.getAverageSerializedSizeBytes().ifPresent(avgSize ->
                    response.put("averageSerializedSizeBytes", avgSize));

            if (result.getMemoryMetrics() != null) {
                Map<String, Object> memoryInfo = new HashMap<>();
                memoryInfo.put("peakMemoryMb", result.getMemoryMetrics().getPeakMemoryMb());
                memoryInfo.put("memoryDeltaMb", result.getMemoryMetrics().getMemoryDeltaMb());
                response.put("memoryMetrics", memoryInfo);
            }

            response.put("serializationTimeMs", result.getAverageSerializationTimeMs().orElse(0.0));
            response.put("deserializationTimeMs", 0.0);
            response.put("totalSizeBytes", result.getAverageSerializedSizeBytes().orElse(0.0));
            response.put("userCount", complexity.getUserCount());

            logger.info("BSON benchmark completed successfully");

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
        return runBenchmark(request);
    }

    @PostMapping("/benchmark/compression")
    public Map<String, Object> runCompressionBenchmark(@RequestBody Map<String, Object> request) {
        Map<String, Object> modifiedRequest = new HashMap<>(request);
        modifiedRequest.put("enableCompression", true);
        modifiedRequest.put("enableRoundtrip", false);
        return runBenchmark(modifiedRequest);
    }

    @PostMapping("/benchmark/performance")
    public Map<String, Object> runPerformanceBenchmark(@RequestBody Map<String, Object> request) {
        Map<String, Object> modifiedRequest = new HashMap<>(request);

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
