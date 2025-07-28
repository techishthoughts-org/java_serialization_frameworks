package org.techishthoughts.fst.controller;

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
import org.techishthoughts.fst.service.FstEnhancedSerializationService;
import org.techishthoughts.fst.service.FstEnhancedSerializationService.CompressionAlgorithm;
import org.techishthoughts.fst.service.FstEnhancedSerializationService.SerializationResult;
import org.techishthoughts.payload.generator.PayloadGenerator;
import org.techishthoughts.payload.model.User;

/**
 * Enhanced FST Benchmark Controller (2025)
 *
 * Enhanced FST (Fast Serialization) implementation with:
 * - GraalVM native image support for ultra-fast startup
 * - Off-heap storage with Chronicle Map integration
 * - Advanced compression algorithms
 * - Performance monitoring and metrics
 */
@RestController
@RequestMapping("/api/fst-enhanced")
public class FstEnhancedBenchmarkController {

    @Autowired
    private FstEnhancedSerializationService fstService;

    @Autowired
    private PayloadGenerator payloadGenerator;

    @GetMapping("/test")
    public ResponseEntity<Map<String, Object>> testFst(@RequestParam(defaultValue = "100") int userCount) {
        try {
            // Generate test users
            List<User> users = payloadGenerator.generateUsers(userCount);

            // Test standard serialization
            long startTime = System.nanoTime();
            byte[] standardData = fstService.serializeUsers(users);
            long standardSerializationTime = System.nanoTime() - startTime;

            startTime = System.nanoTime();
            List<User> standardDeserializedUsers = fstService.deserializeUsers(standardData);
            long standardDeserializationTime = System.nanoTime() - startTime;

            // Test compression
            SerializationResult lz4Result = fstService.serializeUsersCompressed(users, CompressionAlgorithm.LZ4);
            SerializationResult zstdResult = fstService.serializeUsersCompressed(users, CompressionAlgorithm.ZSTD);

            // Test off-heap caching
            String cacheKey = "test-users-" + userCount;
            fstService.cacheUsers(cacheKey, users);
            List<User> cachedUsers = fstService.getCachedUsers(cacheKey);

            return ResponseEntity.ok(Map.of(
                "framework", "Enhanced FST (2025)",
                "status", "✅ SUCCESS",
                "userCount", userCount,
                "originalUsers", users.size(),
                "deserializedUsers", standardDeserializedUsers.size(),
                "standard", Map.of(
                    "sizeBytes", standardData.length,
                    "serializationTimeMs", standardSerializationTime / 1_000_000.0,
                    "deserializationTimeMs", standardDeserializationTime / 1_000_000.0
                ),
                "compression", Map.of(
                    "lz4", Map.of(
                        "sizeBytes", lz4Result.getCompressedSize(),
                        "compressionRatio", lz4Result.getCompressionRatio(),
                        "compressionTimeMs", lz4Result.getSerializationTime() / 1_000_000.0
                    ),
                    "zstd", Map.of(
                        "sizeBytes", zstdResult.getCompressedSize(),
                        "compressionRatio", zstdResult.getCompressionRatio(),
                        "compressionTimeMs", zstdResult.getSerializationTime() / 1_000_000.0
                    )
                ),
                "offHeapCache", Map.of(
                    "cachedSuccessfully", cachedUsers != null,
                    "retrievedCount", cachedUsers != null ? cachedUsers.size() : 0
                ),
                "stats", fstService.getEnhancedStats()
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "framework", "Enhanced FST",
                "status", "❌ ERROR",
                "error", e.getMessage()
            ));
        }
    }

    @PostMapping("/benchmark")
    public ResponseEntity<Map<String, Object>> benchmarkFst(@RequestBody Map<String, Object> request) {
        try {
            int iterations = (Integer) request.getOrDefault("iterations", 1000);
            int userCount = (Integer) request.getOrDefault("userCount", 100);
            String method = (String) request.getOrDefault("method", "standard");
            String compression = (String) request.getOrDefault("compression", "none");

            List<User> users = payloadGenerator.generateUsers(userCount);

            long totalSerializationTime = 0;
            long totalDeserializationTime = 0;
            long totalSize = 0;

            CompressionAlgorithm compressionAlgorithm = CompressionAlgorithm.valueOf(compression.toUpperCase());

            if ("standard".equals(method)) {
                for (int i = 0; i < iterations; i++) {
                    long startTime = System.nanoTime();
                    byte[] serializedData = fstService.serializeUsers(users);
                    totalSerializationTime += (System.nanoTime() - startTime);

                    startTime = System.nanoTime();
                    fstService.deserializeUsers(serializedData);
                    totalDeserializationTime += (System.nanoTime() - startTime);

                    totalSize += serializedData.length;
                }
            } else if ("compressed".equals(method)) {
                for (int i = 0; i < iterations; i++) {
                    long startTime = System.nanoTime();
                    SerializationResult result = fstService.serializeUsersCompressed(users, compressionAlgorithm);
                    totalSerializationTime += (System.nanoTime() - startTime);

                    startTime = System.nanoTime();
                    fstService.deserializeUsersCompressed(result);
                    totalDeserializationTime += (System.nanoTime() - startTime);

                    totalSize += result.getCompressedSize();
                }
            } else if ("cached".equals(method)) {
                String baseCacheKey = "benchmark-users-" + userCount + "-";
                for (int i = 0; i < iterations; i++) {
                    String cacheKey = baseCacheKey + i;

                    long startTime = System.nanoTime();
                    fstService.cacheUsers(cacheKey, users);
                    totalSerializationTime += (System.nanoTime() - startTime);

                    startTime = System.nanoTime();
                    List<User> cachedUsers = fstService.getCachedUsers(cacheKey);
                    totalDeserializationTime += (System.nanoTime() - startTime);

                    if (cachedUsers != null) {
                        totalSize += cachedUsers.size() * 100; // approximate size
                    }
                }
            }

            double avgSerializationMs = (totalSerializationTime / 1_000_000.0) / iterations;
            double avgDeserializationMs = (totalDeserializationTime / 1_000_000.0) / iterations;
            double avgSize = (double) totalSize / iterations;

            return ResponseEntity.ok(Map.of(
                "framework", "Enhanced FST",
                "status", "✅ BENCHMARK_COMPLETE",
                "method", method,
                "compression", compression,
                "iterations", iterations,
                "userCount", userCount,
                "averageSerializationMs", avgSerializationMs,
                "averageDeserializationMs", avgDeserializationMs,
                "averageSize", avgSize,
                "stats", fstService.getEnhancedStats()
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "framework", "Enhanced FST",
                "status", "❌ BENCHMARK_ERROR",
                "error", e.getMessage()
            ));
        }
    }

    @PostMapping("/cache-test")
    public ResponseEntity<Map<String, Object>> testOffHeapCache(@RequestBody Map<String, Object> request) {
        try {
            int userCount = (Integer) request.getOrDefault("userCount", 100);
            int cacheOperations = (Integer) request.getOrDefault("cacheOperations", 1000);

            List<User> users = payloadGenerator.generateUsers(userCount);

            long totalCacheTime = 0;
            long totalRetrievalTime = 0;
            int successfulCaches = 0;
            int successfulRetrievals = 0;

            for (int i = 0; i < cacheOperations; i++) {
                String cacheKey = "cache-test-" + i;

                // Cache operation
                long startTime = System.nanoTime();
                fstService.cacheUsers(cacheKey, users);
                totalCacheTime += (System.nanoTime() - startTime);
                successfulCaches++;

                // Retrieval operation
                startTime = System.nanoTime();
                List<User> cachedUsers = fstService.getCachedUsers(cacheKey);
                totalRetrievalTime += (System.nanoTime() - startTime);

                if (cachedUsers != null && cachedUsers.size() == userCount) {
                    successfulRetrievals++;
                }
            }

            double avgCacheTimeMs = (totalCacheTime / 1_000_000.0) / cacheOperations;
            double avgRetrievalTimeMs = (totalRetrievalTime / 1_000_000.0) / cacheOperations;

            return ResponseEntity.ok(Map.of(
                "framework", "Enhanced FST",
                "status", "✅ CACHE_TEST_COMPLETE",
                "userCount", userCount,
                "cacheOperations", cacheOperations,
                "successfulCaches", successfulCaches,
                "successfulRetrievals", successfulRetrievals,
                "averageCacheTimeMs", avgCacheTimeMs,
                "averageRetrievalTimeMs", avgRetrievalTimeMs,
                "cacheHitRate", (double) successfulRetrievals / cacheOperations,
                "stats", fstService.getEnhancedStats()
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "framework", "Enhanced FST",
                "status", "❌ CACHE_ERROR",
                "error", e.getMessage()
            ));
        }
    }

    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getFstInfo() {
        return ResponseEntity.ok(fstService.getEnhancedStats());
    }
}
