package org.techishthoughts.fst.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.techishthoughts.fst.service.FstSerializationService;
import org.techishthoughts.payload.generator.PayloadGenerator;
import org.techishthoughts.payload.model.User;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * FST (Fast Serialization Toolkit) Benchmark Controller
 *
 * Provides REST endpoints for benchmarking FST serialization performance
 * against other frameworks in the compression strategy project.
 */
@RestController
@RequestMapping("/api/fst/benchmark")
public class FstBenchmarkController {

    @Autowired
    private FstSerializationService fstService;

    @Autowired
    private PayloadGenerator payloadGenerator;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Run serialization benchmark with FST
     */
            @PostMapping("/serialization")
    public ResponseEntity<Map<String, Object>> runSerializationBenchmark(
            @RequestBody Map<String, Object> request) {

        String complexity = (String) request.getOrDefault("complexity", "SMALL");
        int iterations = ((Number) request.getOrDefault("iterations", 1)).intValue();

        System.out.println("Running FST serialization benchmark with complexity: " + complexity);

        try {
            // Generate test data
            int userCount = getComplexityCount(complexity);
            List<User> users = payloadGenerator.generateUsers(userCount);
            System.out.println("Generated " + users.size() + " users for testing");

            // Run benchmark
            Map<String, Object> results = fstService.benchmarkSerialization(users, iterations);

            // Add metadata
            results.put("complexity", complexity);
            results.put("userCount", users.size());
            results.put("iterations", iterations);

            return ResponseEntity.ok(results);

        } catch (Exception e) {
            System.err.println("Error in FST serialization benchmark: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Serialization benchmark failed: " + e.getMessage()));
        }
    }

    /**
     * Run compression benchmark with FST
     */
        @PostMapping("/compression")
    public ResponseEntity<Map<String, Object>> runCompressionBenchmark(
            @RequestBody Map<String, Object> request) {

        String complexity = (String) request.getOrDefault("complexity", "SMALL");
        int iterations = ((Number) request.getOrDefault("iterations", 1)).intValue();

        System.out.println("Running FST compression benchmark with complexity: " + complexity);

        try {
            // Generate test data
            int userCount = getComplexityCount(complexity);
            List<User> users = payloadGenerator.generateUsers(userCount);
            System.out.println("Generated " + users.size() + " users for testing");

            // Run benchmark
            Map<String, Object> results = fstService.benchmarkCompression(users, iterations);

            // Add metadata
            results.put("complexity", complexity);
            results.put("userCount", users.size());
            results.put("iterations", iterations);

            return ResponseEntity.ok(results);

        } catch (Exception e) {
            System.err.println("Error in FST compression benchmark: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Compression benchmark failed: " + e.getMessage()));
        }
    }

    /**
     * Run performance benchmark with FST
     */
        @PostMapping("/performance")
    public ResponseEntity<Map<String, Object>> runPerformanceBenchmark(
            @RequestBody Map<String, Object> request) {

        String complexity = (String) request.getOrDefault("complexity", "SMALL");
        int iterations = ((Number) request.getOrDefault("iterations", 1)).intValue();

        System.out.println("Running FST performance benchmark with complexity: " + complexity);

        try {
            // Generate test data
            int userCount = getComplexityCount(complexity);
            List<User> users = payloadGenerator.generateUsers(userCount);
            System.out.println("Generated " + users.size() + " users for testing");

            // Run benchmark
            Map<String, Object> results = fstService.benchmarkPerformance(users, iterations);

            // Add metadata
            results.put("complexity", complexity);
            results.put("userCount", users.size());
            results.put("iterations", iterations);

            return ResponseEntity.ok(results);

        } catch (Exception e) {
            System.err.println("Error in FST performance benchmark: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Performance benchmark failed: " + e.getMessage()));
        }
    }

    /**
     * Get FST framework information
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getFrameworkInfo() {
        return ResponseEntity.ok(Map.of(
            "framework", "FST (Fast Serialization Toolkit)",
            "version", "2.57",
            "description", "High-performance Java serialization library",
            "features", List.of(
                "Zero-copy serialization",
                "Schema evolution support",
                "Cross-language compatibility",
                "Memory efficient"
            ),
            "status", "active"
        ));
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "framework", "FST",
            "timestamp", System.currentTimeMillis()
        ));
    }

    /**
     * Convert complexity string to user count
     */
    private int getComplexityCount(String complexity) {
        return switch (complexity.toUpperCase()) {
            case "TINY" -> 1;
            case "SMALL" -> 10;
            case "MEDIUM" -> 100;
            case "LARGE" -> 1000;
            case "HUGE" -> 10000;
            default -> 10;
        };
    }
}
