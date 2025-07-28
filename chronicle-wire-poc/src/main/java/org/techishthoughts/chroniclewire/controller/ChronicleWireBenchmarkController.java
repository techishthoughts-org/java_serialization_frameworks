package org.techishthoughts.chroniclewire.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.techishthoughts.chroniclewire.service.ChronicleWireSerializationService;
import org.techishthoughts.payload.generator.PayloadGenerator;
import org.techishthoughts.payload.model.User;

/**
 * Chronicle Wire Benchmark Controller (2025)
 *
 * Ultra-low latency serialization framework controller designed for:
 * - High-frequency trading applications
 * - Real-time streaming systems
 * - Zero-garbage-collection serialization
 * - Sub-microsecond performance
 */
@RestController
@RequestMapping("/api/chronicle-wire")
public class ChronicleWireBenchmarkController {

    @Autowired
    private ChronicleWireSerializationService chronicleWireService;

    @Autowired
    private PayloadGenerator payloadGenerator;

    @GetMapping("/test")
    public ResponseEntity<Map<String, Object>> testChronicleWire(@RequestParam(defaultValue = "100") int userCount) {
        try {
            // Generate test users
            List<User> users = payloadGenerator.generateUsers(userCount);

            // Test different serialization formats
            long startTime = System.nanoTime();
            byte[] binaryData = chronicleWireService.serializeToBinary(users);
            long binarySerializationTime = System.nanoTime() - startTime;

            startTime = System.nanoTime();
            String yamlData = chronicleWireService.serializeToYaml(users);
            long yamlSerializationTime = System.nanoTime() - startTime;

            startTime = System.nanoTime();
            List<User> deserializedUsers = chronicleWireService.deserializeFromBinary(binaryData);
            long binaryDeserializationTime = System.nanoTime() - startTime;

            // Gather performance stats
            Map<String, Object> stats = chronicleWireService.getPerformanceStats();

            return ResponseEntity.ok(Map.of(
                "framework", "Chronicle Wire (2025)",
                "status", "✅ SUCCESS",
                "userCount", userCount,
                "originalUsers", users.size(),
                "deserializedUsers", deserializedUsers.size(),
                "binary", Map.of(
                    "sizeBytes", binaryData.length,
                    "serializationTimeNs", binarySerializationTime,
                    "deserializationTimeNs", binaryDeserializationTime,
                    "serializationTimeMicros", binarySerializationTime / 1_000.0,
                    "deserializationTimeMicros", binaryDeserializationTime / 1_000.0
                ),
                "yaml", Map.of(
                    "sizeBytes", yamlData.length(),
                    "serializationTimeNs", yamlSerializationTime,
                    "serializationTimeMicros", yamlSerializationTime / 1_000.0
                ),
                "stats", stats
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "framework", "Chronicle Wire",
                "status", "❌ ERROR",
                "error", e.getMessage()
            ));
        }
    }

    @PostMapping("/benchmark")
    public ResponseEntity<Map<String, Object>> benchmarkChronicleWire(@RequestBody Map<String, Object> request) {
        try {
            int iterations = (Integer) request.getOrDefault("iterations", 1000);
            int userCount = (Integer) request.getOrDefault("userCount", 100);
            String format = (String) request.getOrDefault("format", "binary");

            List<User> users = payloadGenerator.generateUsers(userCount);

            long totalSerializationTime = 0;
            long totalDeserializationTime = 0;
            long totalSize = 0;

            if ("binary".equals(format)) {
                for (int i = 0; i < iterations; i++) {
                    long startTime = System.nanoTime();
                    byte[] serializedData = chronicleWireService.serializeToBinary(users);
                    totalSerializationTime += (System.nanoTime() - startTime);

                    startTime = System.nanoTime();
                    chronicleWireService.deserializeFromBinary(serializedData);
                    totalDeserializationTime += (System.nanoTime() - startTime);

                    totalSize += serializedData.length;
                }
            } else if ("yaml".equals(format)) {
                for (int i = 0; i < iterations; i++) {
                    long startTime = System.nanoTime();
                    String serializedData = chronicleWireService.serializeToYaml(users);
                    totalSerializationTime += (System.nanoTime() - startTime);

                    startTime = System.nanoTime();
                    chronicleWireService.deserializeFromYaml(serializedData);
                    totalDeserializationTime += (System.nanoTime() - startTime);

                    totalSize += serializedData.length();
                }
            }

            double avgSerializationNs = (double) totalSerializationTime / iterations;
            double avgDeserializationNs = (double) totalDeserializationTime / iterations;
            double avgSize = (double) totalSize / iterations;

            Map<String, Object> response = new HashMap<>();
            response.put("framework", "Chronicle Wire");
            response.put("status", "✅ BENCHMARK_COMPLETE");
            response.put("format", format);
            response.put("iterations", iterations);
            response.put("userCount", userCount);
            response.put("averageSerializationNs", avgSerializationNs);
            response.put("averageDeserializationNs", avgDeserializationNs);
            response.put("averageSerializationMicros", avgSerializationNs / 1_000.0);
            response.put("averageDeserializationMicros", avgDeserializationNs / 1_000.0);
            response.put("averageSize", avgSize);
            response.put("stats", chronicleWireService.getPerformanceStats());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "framework", "Chronicle Wire",
                "status", "❌ BENCHMARK_ERROR",
                "error", e.getMessage()
            ));
        }
    }

    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getChronicleWireInfo() {
        return ResponseEntity.ok(chronicleWireService.getPerformanceStats());
    }

    @PostMapping("/streaming-test")
    public ResponseEntity<Map<String, Object>> testStreaming(@RequestBody Map<String, Object> request) {
        try {
            int messageCount = (Integer) request.getOrDefault("messageCount", 1000);
            int userCount = (Integer) request.getOrDefault("userCount", 10);

            List<User> users = payloadGenerator.generateUsers(userCount);
            Map<String, Object> streamingResults = chronicleWireService.testStreaming(users, messageCount);

            return ResponseEntity.ok(Map.of(
                "framework", "Chronicle Wire",
                "status", "✅ STREAMING_TEST_COMPLETE",
                "messageCount", messageCount,
                "userCount", userCount,
                "results", streamingResults
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "framework", "Chronicle Wire",
                "status", "❌ STREAMING_ERROR",
                "error", e.getMessage()
            ));
        }
    }
}
