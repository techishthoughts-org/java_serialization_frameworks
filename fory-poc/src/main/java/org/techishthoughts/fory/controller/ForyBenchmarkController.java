package org.techishthoughts.fory.controller;

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
import org.techishthoughts.fory.service.ForySerializationService;
import org.techishthoughts.payload.generator.PayloadGenerator;
import org.techishthoughts.payload.model.User;

/**
 * Apache Fory Benchmark Controller (2025)
 *
 * Handles HTTP endpoints for testing Apache Fory serialization performance.
 * Note: Apache Fury was renamed to Apache Fory in June 2025.
 */
@RestController
@RequestMapping("/api/fory")
public class ForyBenchmarkController {

    @Autowired
    private ForySerializationService forySerializationService;

    @Autowired
    private PayloadGenerator payloadGenerator;

    @GetMapping("/test")
    public ResponseEntity<Map<String, Object>> testFory(@RequestParam(defaultValue = "100") int userCount) {
        try {
            // Simple test first
            return ResponseEntity.ok(Map.of(
                "framework", "Apache Fory (2025)",
                "status", "✅ SUCCESS",
                "message", "Basic endpoint working",
                "userCount", userCount
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "framework", "Apache Fory",
                "status", "❌ ERROR",
                "error", e.getMessage(),
                "migration_note", "Framework renamed from Fury to Fory in June 2025"
            ));
        }
    }

    @PostMapping("/benchmark")
    public ResponseEntity<Map<String, Object>> benchmarkFory(@RequestBody Map<String, Object> request) {
        try {
            int iterations = (Integer) request.getOrDefault("iterations", 1000);
            int userCount = (Integer) request.getOrDefault("userCount", 100);

            List<User> users = payloadGenerator.generateUsers(userCount);

            long totalSerializationTime = 0;
            long totalDeserializationTime = 0;
            int totalSerializedSize = 0;

            for (int i = 0; i < iterations; i++) {
                long startTime = System.nanoTime();
                byte[] serializedData = forySerializationService.serializeUsers(users);
                totalSerializationTime += (System.nanoTime() - startTime);

                startTime = System.nanoTime();
                forySerializationService.deserializeUsers(serializedData);
                totalDeserializationTime += (System.nanoTime() - startTime);

                totalSerializedSize += serializedData.length;
            }

            double avgSerializationMs = (totalSerializationTime / 1_000_000.0) / iterations;
            double avgDeserializationMs = (totalDeserializationTime / 1_000_000.0) / iterations;
            double avgSerializedSize = (double) totalSerializedSize / iterations;

            return ResponseEntity.ok(Map.of(
                "framework", "Apache Fory",
                "status", "✅ BENCHMARK_COMPLETE",
                "iterations", iterations,
                "userCount", userCount,
                "avgSerializationMs", avgSerializationMs,
                "avgDeserializationMs", avgDeserializationMs,
                "avgSerializedSize", avgSerializedSize,
                "totalSerializedSizeKB", totalSerializedSize / 1024.0,
                "serializationThroughput", (iterations * userCount) / (avgSerializationMs / 1000.0),
                "deserializationThroughput", (iterations * userCount) / (avgDeserializationMs / 1000.0)
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "framework", "Apache Fory",
                "status", "❌ ERROR",
                "error", e.getMessage()
            ));
        }
    }

    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getForyInfo() {
        try {
            Map<String, Object> stats = forySerializationService.getSerializationStats();
            return ResponseEntity.ok(Map.of(
                "framework", "Apache Fory (2025)",
                "version", "0.11.2",
                "status", "✅ ACTIVE",
                "features", List.of(
                    "JIT compilation for maximum performance",
                    "Cross-language serialization without IDL",
                    "Zero-copy operations",
                    "Schema evolution support",
                    "Backward compatibility with Fury 0.10.x"
                ),
                "stats", stats
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "framework", "Apache Fory",
                "status", "❌ ERROR",
                "error", e.getMessage()
            ));
        }
    }
}
