package org.techishthoughts.cbor.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import com.github.luben.zstd.Zstd;
import org.techishthoughts.payload.generator.PayloadGenerator;
import org.techishthoughts.payload.model.User;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * CBOR Serialization Service
 *
 * Provides CBOR serialization and deserialization capabilities using Jackson CBOR.
 * CBOR is an IETF standard binary format optimized for IoT and constrained environments.
 *
 * Key features:
 * - Compact binary representation
 * - Self-describing format
 * - Language-agnostic
 * - Excellent for IoT devices
 *
 * @author TechishThoughts
 * @version 1.0.0
 */
@Service
public class CborSerializationService {

    private final ObjectMapper cborMapper;
    private final PayloadGenerator payloadGenerator;
    private final Map<String, byte[]> serializationCache;

    public CborSerializationService() {
        this.cborMapper = new ObjectMapper(new CBORFactory());
        this.payloadGenerator = new PayloadGenerator();
        this.serializationCache = new ConcurrentHashMap<>();
    }

    /**
     * Serialize a list of users to CBOR format
     *
     * @param users List of users to serialize
     * @return Serialization result with timing and size information
     */
    public SerializationResult serializeUsers(List<User> users) {
        long startTime = System.nanoTime();

        try {
            byte[] cborData = cborMapper.writeValueAsBytes(users);
            long serializationTime = (System.nanoTime() - startTime) / 1_000_000; // Convert to ms

            return SerializationResult.builder()
                    .success(true)
                    .serializationTimeMs(serializationTime)
                    .totalSizeBytes(cborData.length)
                    .userCount(users.size())
                    .build();

        } catch (IOException e) {
            return SerializationResult.builder()
                    .success(false)
                    .error(e.getMessage())
                    .build();
        }
    }

    /**
     * Deserialize CBOR data back to a list of users
     *
     * @param cborData CBOR byte array
     * @return Deserialization result with timing information
     */
    public DeserializationResult deserializeUsers(byte[] cborData) {
        long startTime = System.nanoTime();

        try {
            List<User> users = cborMapper.readValue(cborData,
                    cborMapper.getTypeFactory().constructCollectionType(List.class, User.class));
            long deserializationTime = (System.nanoTime() - startTime) / 1_000_000; // Convert to ms

            return DeserializationResult.builder()
                    .success(true)
                    .deserializationTimeMs(deserializationTime)
                    .userCount(users.size())
                    .build();

        } catch (IOException e) {
            return DeserializationResult.builder()
                    .success(false)
                    .error(e.getMessage())
                    .build();
        }
    }

    /**
     * Compress CBOR data using different compression algorithms
     *
     * @param complexity Payload complexity level
     * @param iterations Number of iterations for testing
     * @return Compression result with ratios and timing
     */
    public CompressionResult compressData(String complexity, int iterations) {
        List<User> users = payloadGenerator.generateUsers(iterations);

        try {
            byte[] cborData = cborMapper.writeValueAsBytes(users);
            int originalSize = cborData.length;

            // GZIP compression
            long gzipStart = System.nanoTime();
            byte[] gzipCompressed = compressGzip(cborData);
            long gzipTime = (System.nanoTime() - gzipStart) / 1_000_000;
            double gzipRatio = (double) gzipCompressed.length / originalSize;

            // Zstandard compression
            long zstdStart = System.nanoTime();
            byte[] zstdCompressed = Zstd.compress(cborData);
            long zstdTime = (System.nanoTime() - zstdStart) / 1_000_000;
            double zstdRatio = (double) zstdCompressed.length / originalSize;

            return CompressionResult.builder()
                    .complexity(complexity)
                    .iterations(iterations)
                    .userCount(users.size())
                    .compression(Map.of(
                            "gzip", CompressionInfo.builder()
                                    .compressionRatio(gzipRatio)
                                    .compressedSize(gzipCompressed.length)
                                    .compressionTime(gzipTime)
                                    .originalSize(originalSize)
                                    .build(),
                            "zstandard", CompressionInfo.builder()
                                    .compressionRatio(zstdRatio)
                                    .compressedSize(zstdCompressed.length)
                                    .compressionTime(zstdTime)
                                    .originalSize(originalSize)
                                    .build()
                    ))
                    .build();

        } catch (IOException e) {
            return CompressionResult.builder()
                    .success(false)
                    .error(e.getMessage())
                    .build();
        }
    }

    /**
     * Run performance benchmark with multiple iterations
     *
     * @param complexity Payload complexity level
     * @param iterations Number of iterations
     * @return Performance benchmark results
     */
    public PerformanceResult runPerformanceBenchmark(String complexity, int iterations) {
        List<User> users = payloadGenerator.generateUsers(iterations);

        long totalSerializationTime = 0;
        long totalDeserializationTime = 0;
        long totalCompressionTime = 0;
        int totalSize = 0;

        for (int i = 0; i < iterations; i++) {
            try {
                // Serialization timing
                long serialStart = System.nanoTime();
                byte[] cborData = cborMapper.writeValueAsBytes(users);
                totalSerializationTime += (System.nanoTime() - serialStart) / 1_000_000;
                totalSize = cborData.length;

                // Deserialization timing
                long deserialStart = System.nanoTime();
                cborMapper.readValue(cborData,
                        cborMapper.getTypeFactory().constructCollectionType(List.class, User.class));
                totalDeserializationTime += (System.nanoTime() - deserialStart) / 1_000_000;

                // Compression timing
                long compressStart = System.nanoTime();
                Zstd.compress(cborData);
                totalCompressionTime += (System.nanoTime() - compressStart) / 1_000_000;

            } catch (IOException e) {
                return PerformanceResult.builder()
                        .success(false)
                        .error(e.getMessage())
                        .build();
            }
        }

        return PerformanceResult.builder()
                .success(true)
                .payloadSize(getPayloadSizeCategory(complexity))
                .userCount(users.size())
                .iterations(iterations)
                .performance(PerformanceInfo.builder()
                        .avgSerializationTime(totalSerializationTime / iterations)
                        .avgDeserializationTime(totalDeserializationTime / iterations)
                        .avgCompressionTime(totalCompressionTime / iterations)
                        .totalIterations(iterations)
                        .build())
                .build();
    }

    private byte[] compressGzip(byte[] data) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            java.util.zip.GZIPOutputStream gzipOut = new java.util.zip.GZIPOutputStream(baos);
            gzipOut.write(data);
            gzipOut.finish();
            return baos.toByteArray();
        }
    }

    private String getPayloadSizeCategory(String complexity) {
        return switch (complexity.toUpperCase()) {
            case "SMALL" -> "small";
            case "MEDIUM" -> "medium";
            case "LARGE" -> "large";
            case "HUGE" -> "huge";
            default -> "medium";
        };
    }

    // Result classes
    public static class SerializationResult {
        private boolean success;
        private long serializationTimeMs;
        private int totalSizeBytes;
        private int userCount;
        private String error;

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private SerializationResult result = new SerializationResult();

            public Builder success(boolean success) {
                result.success = success;
                return this;
            }

            public Builder serializationTimeMs(long time) {
                result.serializationTimeMs = time;
                return this;
            }

            public Builder totalSizeBytes(int size) {
                result.totalSizeBytes = size;
                return this;
            }

            public Builder userCount(int count) {
                result.userCount = count;
                return this;
            }

            public Builder error(String error) {
                result.error = error;
                return this;
            }

            public SerializationResult build() {
                return result;
            }
        }

        // Getters
        public boolean isSuccess() { return success; }
        public long getSerializationTimeMs() { return serializationTimeMs; }
        public int getTotalSizeBytes() { return totalSizeBytes; }
        public int getUserCount() { return userCount; }
        public String getError() { return error; }
    }

    public static class DeserializationResult {
        private boolean success;
        private long deserializationTimeMs;
        private int userCount;
        private String error;

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private DeserializationResult result = new DeserializationResult();

            public Builder success(boolean success) {
                result.success = success;
                return this;
            }

            public Builder deserializationTimeMs(long time) {
                result.deserializationTimeMs = time;
                return this;
            }

            public Builder userCount(int count) {
                result.userCount = count;
                return this;
            }

            public Builder error(String error) {
                result.error = error;
                return this;
            }

            public DeserializationResult build() {
                return result;
            }
        }

        // Getters
        public boolean isSuccess() { return success; }
        public long getDeserializationTimeMs() { return deserializationTimeMs; }
        public int getUserCount() { return userCount; }
        public String getError() { return error; }
    }

    public static class CompressionResult {
        private boolean success = true;
        private String complexity;
        private int iterations;
        private int userCount;
        private Map<String, CompressionInfo> compression;
        private String error;

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private CompressionResult result = new CompressionResult();

            public Builder success(boolean success) {
                result.success = success;
                return this;
            }

            public Builder complexity(String complexity) {
                result.complexity = complexity;
                return this;
            }

            public Builder iterations(int iterations) {
                result.iterations = iterations;
                return this;
            }

            public Builder userCount(int count) {
                result.userCount = count;
                return this;
            }

            public Builder compression(Map<String, CompressionInfo> compression) {
                result.compression = compression;
                return this;
            }

            public Builder error(String error) {
                result.error = error;
                return this;
            }

            public CompressionResult build() {
                return result;
            }
        }

        // Getters
        public boolean isSuccess() { return success; }
        public String getComplexity() { return complexity; }
        public int getIterations() { return iterations; }
        public int getUserCount() { return userCount; }
        public Map<String, CompressionInfo> getCompression() { return compression; }
        public String getError() { return error; }
    }

    public static class CompressionInfo {
        private double compressionRatio;
        private int compressedSize;
        private long compressionTime;
        private int originalSize;

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private CompressionInfo info = new CompressionInfo();

            public Builder compressionRatio(double ratio) {
                info.compressionRatio = ratio;
                return this;
            }

            public Builder compressedSize(int size) {
                info.compressedSize = size;
                return this;
            }

            public Builder compressionTime(long time) {
                info.compressionTime = time;
                return this;
            }

            public Builder originalSize(int size) {
                info.originalSize = size;
                return this;
            }

            public CompressionInfo build() {
                return info;
            }
        }

        // Getters
        public double getCompressionRatio() { return compressionRatio; }
        public int getCompressedSize() { return compressedSize; }
        public long getCompressionTime() { return compressionTime; }
        public int getOriginalSize() { return originalSize; }
    }

    public static class PerformanceResult {
        private boolean success = true;
        private String payloadSize;
        private int userCount;
        private int iterations;
        private PerformanceInfo performance;
        private String error;

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private PerformanceResult result = new PerformanceResult();

            public Builder success(boolean success) {
                result.success = success;
                return this;
            }

            public Builder payloadSize(String size) {
                result.payloadSize = size;
                return this;
            }

            public Builder userCount(int count) {
                result.userCount = count;
                return this;
            }

            public Builder iterations(int iterations) {
                result.iterations = iterations;
                return this;
            }

            public Builder performance(PerformanceInfo performance) {
                result.performance = performance;
                return this;
            }

            public Builder error(String error) {
                result.error = error;
                return this;
            }

            public PerformanceResult build() {
                return result;
            }
        }

        // Getters
        public boolean isSuccess() { return success; }
        public String getPayloadSize() { return payloadSize; }
        public int getUserCount() { return userCount; }
        public int getIterations() { return iterations; }
        public PerformanceInfo getPerformance() { return performance; }
        public String getError() { return error; }
    }

    public static class PerformanceInfo {
        private long avgSerializationTime;
        private long avgDeserializationTime;
        private long avgCompressionTime;
        private int totalIterations;

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private PerformanceInfo info = new PerformanceInfo();

            public Builder avgSerializationTime(long time) {
                info.avgSerializationTime = time;
                return this;
            }

            public Builder avgDeserializationTime(long time) {
                info.avgDeserializationTime = time;
                return this;
            }

            public Builder avgCompressionTime(long time) {
                info.avgCompressionTime = time;
                return this;
            }

            public Builder totalIterations(int iterations) {
                info.totalIterations = iterations;
                return this;
            }

            public PerformanceInfo build() {
                return info;
            }
        }

        // Getters
        public long getAvgSerializationTime() { return avgSerializationTime; }
        public long getAvgDeserializationTime() { return avgDeserializationTime; }
        public long getAvgCompressionTime() { return avgCompressionTime; }
        public int getTotalIterations() { return totalIterations; }
    }
}
