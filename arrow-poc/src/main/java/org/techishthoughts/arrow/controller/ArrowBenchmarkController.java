package org.techishthoughts.arrow.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.techishthoughts.arrow.service.ArrowSerializationService;
import org.techishthoughts.payload.generator.PayloadGenerator;
import org.techishthoughts.payload.model.User;

import java.util.List;
import java.util.Map;

/**
 * Apache Arrow Benchmark Controller
 *
 * Provides REST endpoints for Apache Arrow serialization benchmarking.
 * Apache Arrow is optimized for analytical workloads and big data processing.
 *
 * Endpoints:
 * - /api/arrow/benchmark/serialization - Test Arrow serialization performance
 * - /api/arrow/benchmark/compression - Test compression ratios with Arrow
 * - /api/arrow/benchmark/performance - Comprehensive performance benchmark
 *
 * @author TechishThoughts
 * @version 1.0.0
 */
@RestController
@RequestMapping("/api/arrow/benchmark")
@CrossOrigin(origins = "*")
public class ArrowBenchmarkController {

    private final ArrowSerializationService arrowService;
    private final PayloadGenerator payloadGenerator;

    @Autowired
    public ArrowBenchmarkController(ArrowSerializationService arrowService, PayloadGenerator payloadGenerator) {
        this.arrowService = arrowService;
        this.payloadGenerator = payloadGenerator;
    }

    /**
     * Test Apache Arrow serialization performance
     *
     * @param request Benchmark request with complexity and iterations
     * @return Serialization benchmark results
     */
    @PostMapping("/serialization")
    public ResponseEntity<Map<String, Object>> benchmarkSerialization(
            @RequestBody BenchmarkRequest request) {

        try {
            List<User> users = payloadGenerator.generateUsers(request.getIterations());
            ArrowSerializationService.SerializationResult result = arrowService.serializeUsers(users);

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
     * Test compression ratios with Arrow data
     *
     * @param request Benchmark request with complexity and iterations
     * @return Compression benchmark results
     */
    @PostMapping("/compression")
    public ResponseEntity<Map<String, Object>> benchmarkCompression(
            @RequestBody BenchmarkRequest request) {

        try {
            ArrowSerializationService.CompressionResult result =
                    arrowService.compressData(request.getComplexity(), request.getIterations());

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
            ArrowSerializationService.PerformanceResult result =
                    arrowService.runPerformanceBenchmark(request.getComplexity(), request.getIterations());

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
                "framework", "Apache Arrow",
                "description", "Apache Arrow columnar format for big data and analytics",
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
