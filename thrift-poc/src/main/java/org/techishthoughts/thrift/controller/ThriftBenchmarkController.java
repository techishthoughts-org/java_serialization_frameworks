package org.techishthoughts.thrift.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.techishthoughts.thrift.service.ThriftSerializationService;
import org.techishthoughts.payload.generator.PayloadGenerator;
import org.techishthoughts.payload.model.User;

/**
 * Thrift Benchmark Controller
 *
 * Provides REST endpoints for benchmarking Thrift serialization performance
 * against other frameworks in the compression strategy project.
 */
@RestController
@RequestMapping("/api/thrift/benchmark")
public class ThriftBenchmarkController {

    @Autowired
    private ThriftSerializationService thriftService;

    @Autowired
    private PayloadGenerator payloadGenerator;

    /**
     * Run serialization benchmark with Thrift
     */
    @PostMapping("/serialization")
    public ResponseEntity<Map<String, Object>> runSerializationBenchmark(
            @RequestBody Map<String, Object> request) {

        String complexity = (String) request.getOrDefault("complexity", "SMALL");
        int iterations = ((Number) request.getOrDefault("iterations", 1)).intValue();

        System.out.println("Running Thrift serialization benchmark with complexity: " + complexity);

        try {
            // Generate test data
            int userCount = getComplexityCount(complexity);
            List<User> users = payloadGenerator.generateUsers(userCount);
            System.out.println("Generated " + users.size() + " users for testing");

            // Run benchmark
            Map<String, Object> results = thriftService.benchmarkSerialization(users, iterations);

            // Add metadata
            results.put("complexity", complexity);
            results.put("userCount", users.size());
            results.put("iterations", iterations);

            return ResponseEntity.ok(results);

        } catch (Exception e) {
            System.err.println("Error in Thrift serialization benchmark: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Serialization benchmark failed: " + e.getMessage()));
        }
    }

    /**
     * Run compression benchmark with Thrift
     */
    @PostMapping("/compression")
    public ResponseEntity<Map<String, Object>> runCompressionBenchmark(
            @RequestBody Map<String, Object> request) {

        String complexity = (String) request.getOrDefault("complexity", "SMALL");
        int iterations = ((Number) request.getOrDefault("iterations", 1)).intValue();

        System.out.println("Running Thrift compression benchmark with complexity: " + complexity);

        try {
            // Generate test data
            int userCount = getComplexityCount(complexity);
            List<User> users = payloadGenerator.generateUsers(userCount);
            System.out.println("Generated " + users.size() + " users for testing");

            // Run benchmark
            Map<String, Object> results = thriftService.benchmarkCompression(users, iterations);

            // Add metadata
            results.put("complexity", complexity);
            results.put("userCount", users.size());
            results.put("iterations", iterations);

            return ResponseEntity.ok(results);

        } catch (Exception e) {
            System.err.println("Error in Thrift compression benchmark: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Compression benchmark failed: " + e.getMessage()));
        }
    }

    /**
     * Run performance benchmark with Thrift
     */
    @PostMapping("/performance")
    public ResponseEntity<Map<String, Object>> runPerformanceBenchmark(
            @RequestBody Map<String, Object> request) {

        String complexity = (String) request.getOrDefault("complexity", "SMALL");
        int iterations = ((Number) request.getOrDefault("iterations", 1)).intValue();

        System.out.println("Running Thrift performance benchmark with complexity: " + complexity);

        try {
            // Generate test data
            int userCount = getComplexityCount(complexity);
            List<User> users = payloadGenerator.generateUsers(userCount);
            System.out.println("Generated " + users.size() + " users for testing");

            // Run benchmark
            Map<String, Object> results = thriftService.benchmarkPerformance(users, iterations);

            // Add metadata
            results.put("complexity", complexity);
            results.put("userCount", users.size());
            results.put("iterations", iterations);

            return ResponseEntity.ok(results);

        } catch (Exception e) {
            System.err.println("Error in Thrift performance benchmark: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Performance benchmark failed: " + e.getMessage()));
        }
    }

    /**
     * Get Thrift framework information
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getFrameworkInfo() {
        return ResponseEntity.ok(Map.of(
            "framework", "Apache Thrift",
            "version", "0.22.0",
            "description", "Cross-language RPC framework with efficient serialization",
            "features", List.of(
                "Cross-language compatibility",
                "Binary protocol",
                "Compact protocol",
                "JSON protocol",
                "High performance"
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
            "framework", "Apache Thrift",
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
