package org.techishthoughts.sbe.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.techishthoughts.sbe.service.SbeSerializationService;
import org.techishthoughts.payload.generator.PayloadGenerator;
import org.techishthoughts.payload.model.User;

import java.util.List;
import java.util.Map;

/**
 * SBE Benchmark Controller
 *
 * Provides REST endpoints for SBE serialization benchmarking.
 * SBE is optimized for ultra-low latency applications, particularly in financial trading.
 *
 * Endpoints:
 * - /api/sbe/benchmark/serialization - Test SBE serialization performance
 * - /api/sbe/benchmark/compression - Test compression ratios with SBE
 * - /api/sbe/benchmark/performance - Comprehensive performance benchmark
 *
 * @author TechishThoughts
 * @version 1.0.0
 */
@RestController
@RequestMapping("/api/sbe/benchmark")
@CrossOrigin(origins = "*")
public class SbeBenchmarkController {

    private final SbeSerializationService sbeService;
    private final PayloadGenerator payloadGenerator;

    @Autowired
    public SbeBenchmarkController(SbeSerializationService sbeService, PayloadGenerator payloadGenerator) {
        this.sbeService = sbeService;
        this.payloadGenerator = payloadGenerator;
    }

    /**
     * Test SBE serialization performance
     *
     * @param request Benchmark request with complexity and iterations
     * @return Serialization benchmark results
     */
    @PostMapping("/serialization")
    public ResponseEntity<Map<String, Object>> benchmarkSerialization(
            @RequestBody BenchmarkRequest request) {

        try {
            List<User> users = payloadGenerator.generateUsers(request.getIterations());
            SbeSerializationService.SerializationResult result = sbeService.serializeUsers(users);

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
     * Test compression ratios with SBE data
     *
     * @param request Benchmark request with complexity and iterations
     * @return Compression benchmark results
     */
    @PostMapping("/compression")
    public ResponseEntity<Map<String, Object>> benchmarkCompression(
            @RequestBody BenchmarkRequest request) {

        try {
            SbeSerializationService.CompressionResult result =
                    sbeService.compressData(request.getComplexity(), request.getIterations());

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
            SbeSerializationService.PerformanceResult result =
                    sbeService.runPerformanceBenchmark(request.getComplexity(), request.getIterations());

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
                "framework", "SBE",
                "description", "SBE (Simple Binary Encoding) for ultra-low latency applications",
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
