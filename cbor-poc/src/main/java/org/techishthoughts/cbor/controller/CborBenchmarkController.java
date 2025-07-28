package org.techishthoughts.cbor.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.techishthoughts.cbor.service.CborSerializationService;
import org.techishthoughts.payload.generator.PayloadGenerator;
import org.techishthoughts.payload.model.User;

import java.util.List;
import java.util.Map;

/**
 * CBOR Benchmark Controller
 *
 * Provides REST endpoints for CBOR serialization benchmarking.
 * CBOR is an IETF standard binary format optimized for IoT and constrained environments.
 *
 * Endpoints:
 * - /api/cbor/benchmark/serialization - Test CBOR serialization performance
 * - /api/cbor/benchmark/compression - Test compression ratios with CBOR
 * - /api/cbor/benchmark/performance - Comprehensive performance benchmark
 *
 * @author TechishThoughts
 * @version 1.0.0
 */
@RestController
@RequestMapping("/api/cbor/benchmark")
@CrossOrigin(origins = "*")
public class CborBenchmarkController {

    private final CborSerializationService cborService;
    private final PayloadGenerator payloadGenerator;

    @Autowired
    public CborBenchmarkController(CborSerializationService cborService, PayloadGenerator payloadGenerator) {
        this.cborService = cborService;
        this.payloadGenerator = payloadGenerator;
    }

    /**
     * Test CBOR serialization performance
     *
     * @param request Benchmark request with complexity and iterations
     * @return Serialization benchmark results
     */
    @PostMapping("/serialization")
    public ResponseEntity<Map<String, Object>> benchmarkSerialization(
            @RequestBody BenchmarkRequest request) {

        try {
            List<User> users = payloadGenerator.generateUsers(request.getIterations());
            CborSerializationService.SerializationResult result = cborService.serializeUsers(users);

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
     * Test compression ratios with CBOR data
     *
     * @param request Benchmark request with complexity and iterations
     * @return Compression benchmark results
     */
    @PostMapping("/compression")
    public ResponseEntity<Map<String, Object>> benchmarkCompression(
            @RequestBody BenchmarkRequest request) {

        try {
            CborSerializationService.CompressionResult result =
                    cborService.compressData(request.getComplexity(), request.getIterations());

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
            CborSerializationService.PerformanceResult result =
                    cborService.runPerformanceBenchmark(request.getComplexity(), request.getIterations());

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
                "framework", "CBOR",
                "description", "CBOR (Concise Binary Object Representation) serialization framework",
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
