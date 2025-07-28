package org.techishthoughts.parquet.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.techishthoughts.parquet.service.ParquetSerializationService;
import org.techishthoughts.payload.generator.PayloadGenerator;
import org.techishthoughts.payload.model.User;

import java.util.List;
import java.util.Map;

/**
 * Apache Parquet Benchmark Controller
 *
 * Provides REST endpoints for Apache Parquet serialization benchmarking.
 * Apache Parquet is optimized for data warehousing and analytical workloads.
 *
 * Endpoints:
 * - /api/parquet/benchmark/serialization - Test Parquet serialization performance
 * - /api/parquet/benchmark/compression - Test compression ratios with Parquet
 * - /api/parquet/benchmark/performance - Comprehensive performance benchmark
 *
 * @author TechishThoughts
 * @version 1.0.0
 */
@RestController
@RequestMapping("/api/parquet/benchmark")
@CrossOrigin(origins = "*")
public class ParquetBenchmarkController {

    private final ParquetSerializationService parquetService;
    private final PayloadGenerator payloadGenerator;

    @Autowired
    public ParquetBenchmarkController(ParquetSerializationService parquetService, PayloadGenerator payloadGenerator) {
        this.parquetService = parquetService;
        this.payloadGenerator = payloadGenerator;
    }

    /**
     * Test Apache Parquet serialization performance
     *
     * @param request Benchmark request with complexity and iterations
     * @return Serialization benchmark results
     */
    @PostMapping("/serialization")
    public ResponseEntity<Map<String, Object>> benchmarkSerialization(
            @RequestBody BenchmarkRequest request) {

        try {
            List<User> users = payloadGenerator.generateUsers(request.getIterations());
            ParquetSerializationService.SerializationResult result = parquetService.serializeUsers(users);

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
     * Test compression ratios with Parquet data
     *
     * @param request Benchmark request with complexity and iterations
     * @return Compression benchmark results
     */
    @PostMapping("/compression")
    public ResponseEntity<Map<String, Object>> benchmarkCompression(
            @RequestBody BenchmarkRequest request) {

        try {
            ParquetSerializationService.CompressionResult result =
                    parquetService.compressData(request.getComplexity(), request.getIterations());

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
            ParquetSerializationService.PerformanceResult result =
                    parquetService.runPerformanceBenchmark(request.getComplexity(), request.getIterations());

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
                "framework", "Apache Parquet",
                "description", "Apache Parquet columnar format for data warehousing and analytics",
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
