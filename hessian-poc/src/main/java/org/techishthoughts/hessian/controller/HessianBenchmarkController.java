package org.techishthoughts.hessian.controller;

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
import org.techishthoughts.hessian.service.HessianSerializationService;
import org.techishthoughts.payload.generator.PayloadGenerator;
import org.techishthoughts.payload.model.User;

/**
 * Fixed Hessian Benchmark Controller (2025)
 *
 * Simplified controller for the fixed Hessian implementation
 */
@RestController
@RequestMapping("/api/hessian")
public class HessianBenchmarkController {

    @Autowired
    private HessianSerializationService serializationService;

    @Autowired
    private PayloadGenerator payloadGenerator;

    @GetMapping("/test")
    public ResponseEntity<Map<String, Object>> testHessian(@RequestParam(defaultValue = "100") int userCount) {
        try {
            // Generate test users
            List<User> users = payloadGenerator.generateUsers(userCount);

            // Test serialization and deserialization
            long startTime = System.nanoTime();
            byte[] serializedData = serializationService.serializeUsers(users);
            long serializationTime = System.nanoTime() - startTime;

            startTime = System.nanoTime();
            List<User> deserializedUsers = serializationService.deserializeUsers(serializedData);
            long deserializationTime = System.nanoTime() - startTime;

            return ResponseEntity.ok(Map.of(
                "framework", "Fixed Hessian (2025)",
                "status", "✅ SUCCESS",
                "userCount", userCount,
                "originalUsers", users.size(),
                "deserializedUsers", deserializedUsers.size(),
                "serializedSize", serializedData.length,
                "serializationTimeMs", serializationTime / 1_000_000.0,
                "deserializationTimeMs", deserializationTime / 1_000_000.0,
                "stats", serializationService.getStats()
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "framework", "Fixed Hessian",
                "status", "❌ ERROR",
                "error", e.getMessage()
            ));
        }
    }

    @PostMapping("/benchmark")
    public ResponseEntity<Map<String, Object>> benchmarkHessian(@RequestBody Map<String, Object> request) {
        try {
            int iterations = (Integer) request.getOrDefault("iterations", 1000);
            int userCount = (Integer) request.getOrDefault("userCount", 100);

            List<User> users = payloadGenerator.generateUsers(userCount);

            long totalSerializationTime = 0;
            long totalDeserializationTime = 0;
            long totalSize = 0;

            for (int i = 0; i < iterations; i++) {
                long startTime = System.nanoTime();
                byte[] serializedData = serializationService.serializeUsers(users);
                totalSerializationTime += (System.nanoTime() - startTime);

                startTime = System.nanoTime();
                serializationService.deserializeUsers(serializedData);
                totalDeserializationTime += (System.nanoTime() - startTime);

                totalSize += serializedData.length;
            }

            double avgSerializationMs = (totalSerializationTime / 1_000_000.0) / iterations;
            double avgDeserializationMs = (totalDeserializationTime / 1_000_000.0) / iterations;
            double avgSize = (double) totalSize / iterations;

            return ResponseEntity.ok(Map.of(
                "framework", "Fixed Hessian",
                "status", "✅ BENCHMARK_COMPLETE",
                "iterations", iterations,
                "userCount", userCount,
                "averageSerializationMs", avgSerializationMs,
                "averageDeserializationMs", avgDeserializationMs,
                "averageSize", avgSize,
                "stats", serializationService.getStats()
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "framework", "Fixed Hessian",
                "status", "❌ BENCHMARK_ERROR",
                "error", e.getMessage()
            ));
        }
    }

    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getHessianInfo() {
        return ResponseEntity.ok(serializationService.getStats());
    }
}
