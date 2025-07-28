package org.techishthoughts.protobuf.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.techishthoughts.payload.generator.HugePayloadGenerator;
import org.techishthoughts.payload.model.User;
import org.techishthoughts.protobuf.service.ProtobufSerializationService;

/**
 * Protocol Buffers Benchmark Controller
 *
 * 2025 SOLUTION: Provides REST endpoints for testing protobuf serialization
 * using a hybrid JSON approach that avoids complex conversion issues.
 */
@RestController
@RequestMapping("/api/protobuf")
public class ProtobufBenchmarkController {

    private final ProtobufSerializationService protobufService;

    public ProtobufBenchmarkController(ProtobufSerializationService protobufService) {
        this.protobufService = protobufService;
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        return ResponseEntity.ok(protobufService.getSerializationStats());
    }

    @PostMapping("/serialize")
    public ResponseEntity<Map<String, Object>> serializeUsers(@RequestParam(defaultValue = "100") int count) {
        List<User> users = HugePayloadGenerator.generateHugeDataset(HugePayloadGenerator.ComplexityLevel.SMALL);
        byte[] serialized = protobufService.serializeUsers(users);

        return ResponseEntity.ok(Map.of(
            "framework", "Protocol Buffers (Hybrid)",
            "user_count", count,
            "serialized_size_bytes", serialized.length,
            "serialized_size_kb", serialized.length / 1024.0,
            "status", "success"
        ));
    }

    @PostMapping("/deserialize")
    public ResponseEntity<Map<String, Object>> deserializeUsers(@RequestParam(defaultValue = "100") int count) {
        List<User> users = HugePayloadGenerator.generateHugeDataset(HugePayloadGenerator.ComplexityLevel.SMALL);
        byte[] serialized = protobufService.serializeUsers(users);
        List<User> deserialized = protobufService.deserializeUsers(serialized);

        return ResponseEntity.ok(Map.of(
            "framework", "Protocol Buffers (Hybrid)",
            "user_count", count,
            "original_count", users.size(),
            "deserialized_count", deserialized.size(),
            "validation_success", users.size() == deserialized.size(),
            "status", "success"
        ));
    }

    @PostMapping("/benchmark")
    public ResponseEntity<Map<String, Object>> benchmark(@RequestParam(defaultValue = "100") int count) {
        List<User> users = HugePayloadGenerator.generateHugeDataset(HugePayloadGenerator.ComplexityLevel.SMALL);

        // Serialization test
        long startTime = System.nanoTime();
        byte[] serialized = protobufService.serializeUsers(users);
        long serializationTime = System.nanoTime() - startTime;

        // Deserialization test
        startTime = System.nanoTime();
        List<User> deserialized = protobufService.deserializeUsers(serialized);
        long deserializationTime = System.nanoTime() - startTime;

        // Calculate throughput
        double serializationThroughput = (1_000_000_000.0 / serializationTime) * count;
        double deserializationThroughput = (1_000_000_000.0 / deserializationTime) * count;

        return ResponseEntity.ok(Map.of(
            "framework", "Protocol Buffers (Hybrid)",
            "user_count", count,
            "serialization_time_ms", serializationTime / 1_000_000.0,
            "deserialization_time_ms", deserializationTime / 1_000_000.0,
            "serialized_size_bytes", serialized.length,
            "serialized_size_kb", serialized.length / 1024.0,
            "serialization_throughput_ops_per_sec", serializationThroughput,
            "deserialization_throughput_ops_per_sec", deserializationThroughput,
            "validation_success", users.size() == deserialized.size(),
            "status", "success"
        ));
    }

    @PostMapping("/test")
    public ResponseEntity<Map<String, Object>> testSerialization(@RequestParam(defaultValue = "10") int count) {
        List<User> users = HugePayloadGenerator.generateHugeDataset(HugePayloadGenerator.ComplexityLevel.SMALL);
        boolean testResult = protobufService.testSerialization(users);

        return ResponseEntity.ok(Map.of(
            "framework", "Protocol Buffers (Hybrid)",
            "test_result", testResult,
            "user_count", count,
            "status", testResult ? "passed" : "failed"
        ));
    }

    // ===== HUGE PAYLOAD ENDPOINTS =====

    @PostMapping("/benchmark/serialization")
    public ResponseEntity<Map<String, Object>> runSerializationBenchmark(
            @org.springframework.web.bind.annotation.RequestBody Map<String, Object> request) {

        String complexity = (String) request.getOrDefault("complexity", "SMALL");
        int iterations = (int) request.getOrDefault("iterations", 1);

        System.out.println("Running Protobuf serialization benchmark with complexity: " + complexity);

        try {
            HugePayloadGenerator.ComplexityLevel level = HugePayloadGenerator.ComplexityLevel.valueOf(complexity);
            List<User> users = HugePayloadGenerator.generateHugeDataset(level);

            Map<String, Object> results = new java.util.HashMap<>();
            results.put("complexity", complexity);
            results.put("userCount", users.size());
            results.put("iterations", iterations);

            // Run serialization benchmark
            Map<String, Object> serializationResults = benchmarkFormatsWithIterations(users, iterations);

            // Extract protobuf results and format for test script
            Map<String, Object> protobufResults = (Map<String, Object>) serializationResults.get("protobuf");
            results.put("serializationTimeMs", protobufResults.get("serializationTime"));
            results.put("deserializationTimeMs", 0.0); // Not measured in current implementation
            results.put("totalSizeBytes", protobufResults.get("size"));
            results.put("userCount", users.size());
            results.put("success", true);

            return ResponseEntity.ok(results);

        } catch (Exception e) {
            Map<String, Object> errorResult = new java.util.HashMap<>();
            errorResult.put("complexity", complexity);
            errorResult.put("error", e.getMessage());
            errorResult.put("iterations", iterations);
            e.printStackTrace();
            return ResponseEntity.ok(errorResult);
        }
    }

    @PostMapping("/benchmark/compression")
    public ResponseEntity<Map<String, Object>> runCompressionBenchmark(
            @org.springframework.web.bind.annotation.RequestBody Map<String, Object> request) {

        String complexity = (String) request.getOrDefault("complexity", "SMALL");
        int iterations = (int) request.getOrDefault("iterations", 1);

        System.out.println("Running Protobuf compression benchmark with complexity: " + complexity);

        try {
            HugePayloadGenerator.ComplexityLevel level = HugePayloadGenerator.ComplexityLevel.valueOf(complexity);
            List<User> users = HugePayloadGenerator.generateHugeDataset(level);

            Map<String, Object> results = new java.util.HashMap<>();
            results.put("complexity", complexity);
            results.put("userCount", users.size());
            results.put("iterations", iterations);

            // Run compression benchmark
            Map<String, Object> compressionResults = benchmarkCompressionWithIterations(users, iterations);
            results.put("compression", compressionResults);

            return ResponseEntity.ok(results);

        } catch (Exception e) {
            Map<String, Object> errorResult = new java.util.HashMap<>();
            errorResult.put("complexity", complexity);
            errorResult.put("error", e.getMessage());
            errorResult.put("iterations", iterations);
            e.printStackTrace();
            return ResponseEntity.ok(errorResult);
        }
    }

    @PostMapping("/benchmark/performance")
    public ResponseEntity<Map<String, Object>> runPerformanceBenchmark(
            @org.springframework.web.bind.annotation.RequestBody Map<String, Object> request) {

        String payloadSize = (String) request.getOrDefault("payload_size", "SMALL");
        int iterations = (int) request.getOrDefault("iterations", 1);

        System.out.println("Running Protobuf performance benchmark with payload size: " + payloadSize);

        try {
            HugePayloadGenerator.ComplexityLevel level = HugePayloadGenerator.ComplexityLevel.valueOf(payloadSize);
            List<User> users = HugePayloadGenerator.generateHugeDataset(level);

            Map<String, Object> results = new java.util.HashMap<>();
            results.put("payloadSize", payloadSize);
            results.put("userCount", users.size());
            results.put("iterations", iterations);

            // Run performance benchmark
            Map<String, Object> performanceResults = benchmarkPerformance(users, iterations);
            results.put("performance", performanceResults);

            return ResponseEntity.ok(results);

        } catch (Exception e) {
            Map<String, Object> errorResult = new java.util.HashMap<>();
            errorResult.put("payloadSize", payloadSize);
            errorResult.put("error", e.getMessage());
            errorResult.put("iterations", iterations);
            e.printStackTrace();
            return ResponseEntity.ok(errorResult);
        }
    }

    @GetMapping("/test-huge-payload")
    public ResponseEntity<Map<String, Object>> testHugePayload(
            @RequestParam(value = "complexity", defaultValue = "SMALL") String complexity) {

        Map<String, Object> results = new java.util.HashMap<>();
        try {
            System.out.println("Testing HugePayloadGenerator with complexity: " + complexity);
            HugePayloadGenerator.ComplexityLevel level = HugePayloadGenerator.ComplexityLevel.valueOf(complexity);
            List<User> users = HugePayloadGenerator.generateHugeDataset(level);
            results.put("success", true);
            results.put("complexity", complexity);
            results.put("userCount", users.size());
            results.put("message", "HugePayloadGenerator working correctly");
        } catch (Exception e) {
            results.put("success", false);
            results.put("error", e.getMessage());
            results.put("stackTrace", e.getStackTrace());
            e.printStackTrace();
        }
        return ResponseEntity.ok(results);
    }

    // ===== PRIVATE HELPER METHODS =====

    private Map<String, Object> benchmarkFormatsWithIterations(List<User> users, int iterations) {
        Map<String, Object> results = new java.util.HashMap<>();

        // Protobuf serialization
        long totalTime = 0;
        byte[] serializedData = null;

        for (int i = 0; i < iterations; i++) {
            System.out.println("  Protobuf serialization iteration " + (i + 1) + "/" + iterations);
            long startTime = System.nanoTime();
            serializedData = protobufService.serializeUsers(users);
            long endTime = System.nanoTime();
            totalTime += (endTime - startTime);
        }

        Map<String, Object> protobufResult = new java.util.HashMap<>();
        protobufResult.put("size", serializedData.length);
        protobufResult.put("serializationTime", totalTime / 1_000_000.0 / iterations);
        results.put("protobuf", protobufResult);

        return results;
    }

    private Map<String, Object> benchmarkCompressionWithIterations(List<User> users, int iterations) {
        Map<String, Object> results = new java.util.HashMap<>();

        // Serialize first
        byte[] serializedData = protobufService.serializeUsers(users);

        // GZIP compression
        long totalGzipTime = 0;
        byte[] gzipCompressed = null;

        for (int i = 0; i < iterations; i++) {
            long startTime = System.nanoTime();
            try {
                gzipCompressed = protobufService.compressWithGzip(serializedData);
            } catch (Exception e) {
                System.err.println("GZIP compression failed: " + e.getMessage());
                continue;
            }
            long endTime = System.nanoTime();
            totalGzipTime += (endTime - startTime);
        }

        if (gzipCompressed != null) {
            Map<String, Object> gzipResult = new java.util.HashMap<>();
            gzipResult.put("originalSize", serializedData.length);
            gzipResult.put("compressedSize", gzipCompressed.length);
            gzipResult.put("compressionRatio", (double) gzipCompressed.length / serializedData.length);
            gzipResult.put("compressionTime", totalGzipTime / 1_000_000.0 / iterations);
            results.put("gzip", gzipResult);
        }

        // Zstandard compression
        long totalZstdTime = 0;
        byte[] zstdCompressed = null;

        for (int i = 0; i < iterations; i++) {
            long startTime = System.nanoTime();
            try {
                zstdCompressed = protobufService.compressWithZstd(serializedData);
            } catch (Exception e) {
                System.err.println("Zstandard compression failed: " + e.getMessage());
                continue;
            }
            long endTime = System.nanoTime();
            totalZstdTime += (endTime - startTime);
        }

        if (zstdCompressed != null) {
            Map<String, Object> zstdResult = new java.util.HashMap<>();
            zstdResult.put("originalSize", serializedData.length);
            zstdResult.put("compressedSize", zstdCompressed.length);
            zstdResult.put("compressionRatio", (double) zstdCompressed.length / serializedData.length);
            zstdResult.put("compressionTime", totalZstdTime / 1_000_000.0 / iterations);
            results.put("zstandard", zstdResult);
        }

        return results;
    }

    private Map<String, Object> benchmarkPerformance(List<User> users, int iterations) {
        Map<String, Object> results = new java.util.HashMap<>();

        // Serialization performance
        long totalSerializationTime = 0;
        byte[] serializedData = null;

        for (int i = 0; i < iterations; i++) {
            long startTime = System.nanoTime();
            serializedData = protobufService.serializeUsers(users);
            long endTime = System.nanoTime();
            totalSerializationTime += (endTime - startTime);
        }

        // Deserialization performance
        long totalDeserializationTime = 0;

        for (int i = 0; i < iterations; i++) {
            long startTime = System.nanoTime();
            protobufService.deserializeUsers(serializedData);
            long endTime = System.nanoTime();
            totalDeserializationTime += (endTime - startTime);
        }

        // Compression performance
        long totalCompressionTime = 0;

        for (int i = 0; i < iterations; i++) {
            long startTime = System.nanoTime();
            try {
                protobufService.compressWithGzip(serializedData);
            } catch (Exception e) {
                System.err.println("Compression benchmark failed: " + e.getMessage());
            }
            long endTime = System.nanoTime();
            totalCompressionTime += (endTime - startTime);
        }

        results.put("avgSerializationTime", totalSerializationTime / 1_000_000.0 / iterations);
        results.put("avgDeserializationTime", totalDeserializationTime / 1_000_000.0 / iterations);
        results.put("avgCompressionTime", totalCompressionTime / 1_000_000.0 / iterations);
        results.put("totalIterations", iterations);

        return results;
    }
}
