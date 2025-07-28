package org.techishthoughts.messagepack.controller;

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
import org.techishthoughts.messagepack.service.MessagePackEnhancedSerializationService;
import org.techishthoughts.messagepack.service.MessagePackEnhancedSerializationService.CompressionAlgorithm;
import org.techishthoughts.messagepack.service.MessagePackEnhancedSerializationService.SerializationResult;
import org.techishthoughts.payload.generator.PayloadGenerator;
import org.techishthoughts.payload.model.User;

/**
 * Enhanced MessagePack Benchmark Controller (2025)
 *
 * Enhanced implementation with:
 * - Jackson integration for better object mapping
 * - Multiple compression algorithms (LZ4, Snappy, ZSTD)
 * - Streaming support for large datasets
 * - Performance monitoring and metrics
 */
@RestController
@RequestMapping("/api/messagepack-enhanced")
public class MessagePackEnhancedBenchmarkController {

    @Autowired
    private MessagePackEnhancedSerializationService messagePackService;

    @Autowired
    private PayloadGenerator payloadGenerator;

    @GetMapping("/test")
    public ResponseEntity<Map<String, Object>> testMessagePack(@RequestParam(defaultValue = "100") int userCount) {
        try {
            // Generate test users
            List<User> users = payloadGenerator.generateUsers(userCount);

            // Test Jackson-based serialization
            long startTime = System.nanoTime();
            byte[] jacksonData = messagePackService.serializeUsersJackson(users);
            long jacksonSerializationTime = System.nanoTime() - startTime;

            startTime = System.nanoTime();
            List<User> jacksonDeserializedUsers = messagePackService.deserializeUsersJackson(jacksonData);
            long jacksonDeserializationTime = System.nanoTime() - startTime;

            // Test native serialization
            startTime = System.nanoTime();
            byte[] nativeData = messagePackService.serializeUsersNative(users);
            long nativeSerializationTime = System.nanoTime() - startTime;

            // Test compression
            SerializationResult lz4Result = messagePackService.serializeUsersCompressed(users, CompressionAlgorithm.LZ4);
            SerializationResult zstdResult = messagePackService.serializeUsersCompressed(users, CompressionAlgorithm.ZSTD);

            return ResponseEntity.ok(Map.of(
                "framework", "Enhanced MessagePack (2025)",
                "status", "✅ SUCCESS",
                "userCount", userCount,
                "originalUsers", users.size(),
                "deserializedUsers", jacksonDeserializedUsers.size(),
                "jackson", Map.of(
                    "sizeBytes", jacksonData.length,
                    "serializationTimeMs", jacksonSerializationTime / 1_000_000.0,
                    "deserializationTimeMs", jacksonDeserializationTime / 1_000_000.0
                ),
                "native", Map.of(
                    "sizeBytes", nativeData.length,
                    "serializationTimeMs", nativeSerializationTime / 1_000_000.0
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
                "stats", messagePackService.getEnhancedStats()
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "framework", "Enhanced MessagePack",
                "status", "❌ ERROR",
                "error", e.getMessage()
            ));
        }
    }

    @PostMapping("/benchmark")
    public ResponseEntity<Map<String, Object>> benchmarkMessagePack(@RequestBody Map<String, Object> request) {
        try {
            int iterations = (Integer) request.getOrDefault("iterations", 1000);
            int userCount = (Integer) request.getOrDefault("userCount", 100);
            String method = (String) request.getOrDefault("method", "jackson");
            String compression = (String) request.getOrDefault("compression", "none");

            List<User> users = payloadGenerator.generateUsers(userCount);

            long totalSerializationTime = 0;
            long totalDeserializationTime = 0;
            long totalSize = 0;

            CompressionAlgorithm compressionAlgorithm = CompressionAlgorithm.valueOf(compression.toUpperCase());

            if ("jackson".equals(method)) {
                for (int i = 0; i < iterations; i++) {
                    long startTime = System.nanoTime();
                    byte[] serializedData = messagePackService.serializeUsersJackson(users);
                    totalSerializationTime += (System.nanoTime() - startTime);

                    startTime = System.nanoTime();
                    messagePackService.deserializeUsersJackson(serializedData);
                    totalDeserializationTime += (System.nanoTime() - startTime);

                    totalSize += serializedData.length;
                }
            } else if ("native".equals(method)) {
                for (int i = 0; i < iterations; i++) {
                    long startTime = System.nanoTime();
                    byte[] serializedData = messagePackService.serializeUsersNative(users);
                    totalSerializationTime += (System.nanoTime() - startTime);

                    totalSize += serializedData.length;
                }
            } else if ("compressed".equals(method)) {
                for (int i = 0; i < iterations; i++) {
                    long startTime = System.nanoTime();
                    SerializationResult result = messagePackService.serializeUsersCompressed(users, compressionAlgorithm);
                    totalSerializationTime += (System.nanoTime() - startTime);

                    startTime = System.nanoTime();
                    messagePackService.deserializeUsersCompressed(result);
                    totalDeserializationTime += (System.nanoTime() - startTime);

                    totalSize += result.getCompressedSize();
                }
            }

            double avgSerializationMs = (totalSerializationTime / 1_000_000.0) / iterations;
            double avgDeserializationMs = (totalDeserializationTime / 1_000_000.0) / iterations;
            double avgSize = (double) totalSize / iterations;

            return ResponseEntity.ok(Map.of(
                "framework", "Enhanced MessagePack",
                "status", "✅ BENCHMARK_COMPLETE",
                "method", method,
                "compression", compression,
                "iterations", iterations,
                "userCount", userCount,
                "averageSerializationMs", avgSerializationMs,
                "averageDeserializationMs", avgDeserializationMs,
                "averageSize", avgSize,
                "stats", messagePackService.getEnhancedStats()
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "framework", "Enhanced MessagePack",
                "status", "❌ BENCHMARK_ERROR",
                "error", e.getMessage()
            ));
        }
    }

    @PostMapping("/compression-comparison")
    public ResponseEntity<Map<String, Object>> compareCompressionAlgorithms(@RequestBody Map<String, Object> request) {
        try {
            int userCount = (Integer) request.getOrDefault("userCount", 100);
            List<User> users = payloadGenerator.generateUsers(userCount);

            SerializationResult noneResult = messagePackService.serializeUsersCompressed(users, CompressionAlgorithm.NONE);
            SerializationResult lz4Result = messagePackService.serializeUsersCompressed(users, CompressionAlgorithm.LZ4);
            SerializationResult snappyResult = messagePackService.serializeUsersCompressed(users, CompressionAlgorithm.SNAPPY);
            SerializationResult zstdResult = messagePackService.serializeUsersCompressed(users, CompressionAlgorithm.ZSTD);

            return ResponseEntity.ok(Map.of(
                "framework", "Enhanced MessagePack",
                "status", "✅ COMPRESSION_COMPARISON_COMPLETE",
                "userCount", userCount,
                "results", Map.of(
                    "none", Map.of(
                        "size", noneResult.getCompressedSize(),
                        "ratio", noneResult.getCompressionRatio(),
                        "timeMs", noneResult.getSerializationTime() / 1_000_000.0
                    ),
                    "lz4", Map.of(
                        "size", lz4Result.getCompressedSize(),
                        "ratio", lz4Result.getCompressionRatio(),
                        "timeMs", lz4Result.getSerializationTime() / 1_000_000.0
                    ),
                    "snappy", Map.of(
                        "size", snappyResult.getCompressedSize(),
                        "ratio", snappyResult.getCompressionRatio(),
                        "timeMs", snappyResult.getSerializationTime() / 1_000_000.0
                    ),
                    "zstd", Map.of(
                        "size", zstdResult.getCompressedSize(),
                        "ratio", zstdResult.getCompressionRatio(),
                        "timeMs", zstdResult.getSerializationTime() / 1_000_000.0
                    )
                )
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "framework", "Enhanced MessagePack",
                "status", "❌ COMPRESSION_ERROR",
                "error", e.getMessage()
            ));
        }
    }

    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getMessagePackInfo() {
        return ResponseEntity.ok(messagePackService.getEnhancedStats());
    }
}
