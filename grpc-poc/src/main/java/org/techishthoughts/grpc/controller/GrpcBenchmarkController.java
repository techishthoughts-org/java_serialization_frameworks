package org.techishthoughts.grpc.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.techishthoughts.grpc.service.GrpcSerializationService;
import org.techishthoughts.payload.generator.PayloadGenerator;
import org.techishthoughts.payload.model.User;

@RestController
@RequestMapping("/api/grpc")
public class GrpcBenchmarkController {

    private final GrpcSerializationService serializationService;

    public GrpcBenchmarkController(GrpcSerializationService serializationService) {
        this.serializationService = serializationService;
    }

    @GetMapping("/benchmark")
    public Map<String, Object> runBenchmark(@RequestParam(defaultValue = "1000") int userCount) {
        Map<String, Object> results = new HashMap<>();

        try {
            System.out.println("Running gRPC benchmark with " + userCount + " users...");

            // Generate test data
            List<User> users = PayloadGenerator.generateUsers(userCount);

            // Run serialization benchmark
            long startTime = System.nanoTime();
            byte[] serialized = serializationService.serializeUsers(users);
            long serializationTime = System.nanoTime() - startTime;

            // Run deserialization benchmark
            startTime = System.nanoTime();
            List<User> deserialized = serializationService.deserializeUsers(serialized);
            long deserializationTime = System.nanoTime() - startTime;

            // Compile results
            results.put("userCount", userCount);
            results.put("serializationTimeMs", serializationTime / 1_000_000.0);
            results.put("deserializationTimeMs", deserializationTime / 1_000_000.0);
            results.put("totalSizeBytes", serialized.length);
            results.put("success", true);

        } catch (Exception e) {
            results.put("error", e.getMessage());
            e.printStackTrace();
        }

        return results;
    }

    @PostMapping("/benchmark/serialization")
    public Map<String, Object> runSerializationBenchmark(@RequestBody Map<String, Object> request) {
        String complexity = (String) request.getOrDefault("complexity", "SMALL");
        int iterations = (int) request.getOrDefault("iterations", 1);

        System.out.println("Running gRPC serialization benchmark with complexity: " + complexity);

        Map<String, Object> results = new HashMap<>();
        try {
            // Generate test data based on complexity
            int userCount = getComplexityCount(complexity);
            List<User> users = PayloadGenerator.generateUsers(userCount);

            // Run serialization benchmark
            long totalSerializationTime = 0;
            long totalDeserializationTime = 0;
            long totalSize = 0;

            for (int i = 0; i < iterations; i++) {
                long startTime = System.nanoTime();
                byte[] serialized = serializationService.serializeUsers(users);
                totalSerializationTime += (System.nanoTime() - startTime);
                totalSize += serialized.length;

                startTime = System.nanoTime();
                List<User> deserialized = serializationService.deserializeUsers(serialized);
                totalDeserializationTime += (System.nanoTime() - startTime);
            }

            results.put("serializationTimeMs", totalSerializationTime / 1_000_000.0 / iterations);
            results.put("deserializationTimeMs", totalDeserializationTime / 1_000_000.0 / iterations);
            results.put("totalSizeBytes", totalSize / iterations);
            results.put("userCount", users.size());
            results.put("success", true);

        } catch (Exception e) {
            results.put("error", e.getMessage());
            e.printStackTrace();
        }

        return results;
    }

    @PostMapping("/benchmark/compression")
    public Map<String, Object> runCompressionBenchmark(@RequestBody Map<String, Object> request) {
        String complexity = (String) request.getOrDefault("complexity", "SMALL");
        int iterations = (int) request.getOrDefault("iterations", 1);

        System.out.println("Running gRPC compression benchmark with complexity: " + complexity);

        Map<String, Object> results = new HashMap<>();
        try {
            int userCount = getComplexityCount(complexity);
            List<User> users = PayloadGenerator.generateUsers(userCount);

            long totalSerializationTime = 0;
            long totalCompressionTime = 0;
            long totalCompressedSize = 0;

            for (int i = 0; i < iterations; i++) {
                long startTime = System.nanoTime();
                byte[] serialized = serializationService.serializeUsers(users);
                totalSerializationTime += (System.nanoTime() - startTime);

                startTime = System.nanoTime();
                byte[] compressed = serializationService.compressWithGzip(serialized);
                totalCompressionTime += (System.nanoTime() - startTime);
                totalCompressedSize += compressed.length;
            }

            results.put("serializationTimeMs", totalSerializationTime / 1_000_000.0 / iterations);
            results.put("compressionTimeMs", totalCompressionTime / 1_000_000.0 / iterations);
            results.put("compressedSizeBytes", totalCompressedSize / iterations);
            results.put("userCount", users.size());
            results.put("success", true);

        } catch (Exception e) {
            results.put("error", e.getMessage());
            e.printStackTrace();
        }

        return results;
    }

    @PostMapping("/benchmark/performance")
    public Map<String, Object> runPerformanceBenchmark(@RequestBody Map<String, Object> request) {
        String complexity = (String) request.getOrDefault("complexity", "SMALL");
        int iterations = (int) request.getOrDefault("iterations", 1);

        System.out.println("Running gRPC performance benchmark with complexity: " + complexity);

        Map<String, Object> results = new HashMap<>();
        try {
            int userCount = getComplexityCount(complexity);
            List<User> users = PayloadGenerator.generateUsers(userCount);

            long totalTime = 0;
            long totalSize = 0;

            for (int i = 0; i < iterations; i++) {
                long startTime = System.nanoTime();
                byte[] serialized = serializationService.serializeUsers(users);
                byte[] compressed = serializationService.compressWithGzip(serialized);
                List<User> deserialized = serializationService.deserializeUsers(serialized);
                totalTime += (System.nanoTime() - startTime);
                totalSize += compressed.length;
            }

            results.put("totalTimeMs", totalTime / 1_000_000.0 / iterations);
            results.put("compressedSizeBytes", totalSize / iterations);
            results.put("userCount", users.size());
            results.put("success", true);

        } catch (Exception e) {
            results.put("error", e.getMessage());
            e.printStackTrace();
        }

        return results;
    }

    @GetMapping("/info")
    public Map<String, Object> getFrameworkInfo() {
        return Map.of(
            "framework", "gRPC",
            "version", "1.58.0",
            "description", "High-performance RPC framework",
            "features", List.of(
                "HTTP/2 based",
                "Protocol Buffers",
                "Bidirectional streaming",
                "Cross-language support"
            )
        );
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
            "status", "UP",
            "framework", "gRPC",
            "timestamp", System.currentTimeMillis()
        );
    }

    private int getComplexityCount(String complexity) {
        return switch (complexity.toUpperCase()) {
            case "SMALL" -> 10;
            case "MEDIUM" -> 100;
            case "LARGE" -> 1000;
            default -> 10;
        };
    }
}
