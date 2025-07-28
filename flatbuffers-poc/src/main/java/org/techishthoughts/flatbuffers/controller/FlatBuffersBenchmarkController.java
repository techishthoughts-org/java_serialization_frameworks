package org.techishthoughts.flatbuffers.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.techishthoughts.flatbuffers.service.FlatBuffersSerializationService;
import org.techishthoughts.payload.generator.PayloadGenerator;
import org.techishthoughts.payload.model.User;

@RestController
@RequestMapping("/api/flatbuffers")
public class FlatBuffersBenchmarkController {

    private final FlatBuffersSerializationService serializationService;

    public FlatBuffersBenchmarkController(FlatBuffersSerializationService serializationService) {
        this.serializationService = serializationService;
    }

    @GetMapping("/benchmark")
    public Map<String, Object> runBenchmark(@RequestParam(defaultValue = "1000") int userCount) {
        Map<String, Object> results = new HashMap<>();

        try {
            System.out.println("Running FlatBuffers benchmark with " + userCount + " users...");

            // Generate test data
            List<User> users = PayloadGenerator.generateUsers(userCount);

            // Run serialization benchmark
            long startTime = System.nanoTime();
            FlatBuffersSerializationService.SerializationResult result = serializationService.serializeUsers(users);
            long serializationTime = System.nanoTime() - startTime;
            byte[] serialized = result.getData();

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

        System.out.println("Running FlatBuffers serialization benchmark with complexity: " + complexity);

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
                FlatBuffersSerializationService.SerializationResult result = serializationService.serializeUsers(users);
                totalSerializationTime += (System.nanoTime() - startTime);
                byte[] serialized = result.getData();
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

        System.out.println("Running FlatBuffers compression benchmark with complexity: " + complexity);

        Map<String, Object> results = new HashMap<>();
        try {
            int userCount = getComplexityCount(complexity);
            List<User> users = PayloadGenerator.generateUsers(userCount);

            long totalSerializationTime = 0;
            long totalCompressionTime = 0;
            long totalCompressedSize = 0;

            for (int i = 0; i < iterations; i++) {
                long startTime = System.nanoTime();
                FlatBuffersSerializationService.SerializationResult result = serializationService.serializeUsers(users);
                totalSerializationTime += (System.nanoTime() - startTime);
                byte[] serialized = result.getData();

                startTime = System.nanoTime();
                FlatBuffersSerializationService.CompressionResult compressionResult = serializationService.compressWithLZ4(serialized);
                totalCompressionTime += (System.nanoTime() - startTime);
                totalCompressedSize += compressionResult.getCompressedData().length;
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

        System.out.println("Running FlatBuffers performance benchmark with complexity: " + complexity);

        Map<String, Object> results = new HashMap<>();
        try {
            int userCount = getComplexityCount(complexity);
            List<User> users = PayloadGenerator.generateUsers(userCount);

            long totalTime = 0;
            long totalSize = 0;

            for (int i = 0; i < iterations; i++) {
                long startTime = System.nanoTime();
                FlatBuffersSerializationService.SerializationResult result = serializationService.serializeUsers(users);
                byte[] serialized = result.getData();
                FlatBuffersSerializationService.CompressionResult compressionResult = serializationService.compressWithLZ4(serialized);
                List<User> deserialized = serializationService.deserializeUsers(serialized);
                totalTime += (System.nanoTime() - startTime);
                totalSize += compressionResult.getCompressedData().length;
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
            "framework", "Google FlatBuffers",
            "version", "23.5.26",
            "description", "Cross-platform serialization library",
            "features", List.of(
                "Zero-copy deserialization",
                "Schema evolution",
                "Cross-language support",
                "Memory efficient"
            )
        );
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
            "status", "UP",
            "framework", "Google FlatBuffers",
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
