package org.techishthoughts.bson.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.techishthoughts.bson.service.BsonSerializationService;
import org.techishthoughts.payload.generator.PayloadGenerator;
import org.techishthoughts.payload.model.User;

import java.util.List;
import java.util.Map;

/**
 * BSON Benchmark Controller
 *
 * Provides REST endpoints for BSON serialization benchmarking.
 * BSON is MongoDB's binary format that extends JSON with additional data types.
 *
 * Endpoints:
 * - /api/bson/benchmark/serialization - Test BSON serialization performance
 * - /api/bson/benchmark/compression - Test compression ratios with BSON
 * - /api/bson/benchmark/performance - Comprehensive performance benchmark
 *
 * @author TechishThoughts
 * @version 1.0.0
 */
@RestController
@RequestMapping("/api/bson/benchmark")
@CrossOrigin(origins = "*")
public class BsonBenchmarkController {

    private final BsonSerializationService bsonService;
    private final PayloadGenerator payloadGenerator;

    @Autowired
    public BsonBenchmarkController(BsonSerializationService bsonService, PayloadGenerator payloadGenerator) {
        this.bsonService = bsonService;
        this.payloadGenerator = payloadGenerator;
    }

    /**
     * Test BSON serialization performance
     *
     * @param request Benchmark request with complexity and iterations
     * @return Serialization benchmark results
     */
    @PostMapping("/serialization")
    public ResponseEntity<Map<String, Object>> benchmarkSerialization(
            @RequestBody BenchmarkRequest request) {

        try {
            List<User> users = payloadGenerator.generateUsers(request.getIterations());
            BsonSerializationService.SerializationResult result = bsonService.serializeUsers(users);

            return ResponseEntity.ok(Map.of(
                    "userCount", result.getUserCount(),
                    "success", result.isSuccess(),
                    "serializationTimeMs", result.getSerializationTimeMs(),
                    "totalSizeBytes", result.getTotalSizeBytes()
            ));

        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Test compression ratios with BSON data
     *
     * @param request Benchmark request with complexity and iterations
     * @return Compression benchmark results
     */
    @PostMapping("/compression")
    public ResponseEntity<Map<String, Object>> benchmarkCompression(
            @RequestBody BenchmarkRequest request) {

        try {
            BsonSerializationService.CompressionResult result =
                    bsonService.compressData(request.getComplexity(), request.getIterations());

            return ResponseEntity.ok(Map.of(
                    "complexity", result.getComplexity(),
                    "userCount", result.getUserCount(),
                    "compression", result.getCompression(),
                    "iterations", result.getIterations()
            ));

        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Run comprehensive performance benchmark
     *
     * @param request Benchmark request with complexity and iterations
     * @return Performance benchmark results
     */
    @PostMapping("/performance")
    public ResponseEntity<Map<String, Object>> benchmarkPerformance(
            @RequestBody BenchmarkRequest request) {

        try {
            BsonSerializationService.PerformanceResult result =
                    bsonService.runPerformanceBenchmark(request.getComplexity(), request.getIterations());

            return ResponseEntity.ok(Map.of(
                    "performance", Map.of(
                            "avgDeserializationTime", result.getPerformance().getAvgDeserializationTime(),
                            "avgCompressionTime", result.getPerformance().getAvgCompressionTime(),
                            "totalIterations", result.getPerformance().getTotalIterations(),
                            "avgSerializationTime", result.getPerformance().getAvgSerializationTime()
                    ),
                    "payloadSize", result.getPayloadSize(),
                    "userCount", result.getUserCount(),
                    "iterations", result.getIterations()
            ));

        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Health check endpoint
     *
     * @return Health status
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "framework", "BSON",
                "description", "BSON (Binary JSON) serialization framework for MongoDB ecosystem",
                "version", "1.0.0"
        ));
    }

    /**
     * Benchmark request model
     */
    public static class BenchmarkRequest {
        private String complexity = "MEDIUM";
        private int iterations = 10;

        public String getComplexity() {
            return complexity;
        }

        public void setComplexity(String complexity) {
            this.complexity = complexity;
        }

        public int getIterations() {
            return iterations;
        }

        public void setIterations(int iterations) {
            this.iterations = iterations;
        }
    }
}
