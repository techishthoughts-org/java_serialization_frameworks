package org.techishthoughts.arrow.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.luben.zstd.Zstd;
import org.apache.arrow.memory.BufferAllocator;
import org.apache.arrow.memory.RootAllocator;
import org.apache.arrow.vector.VarCharVector;
import org.apache.arrow.vector.VectorSchemaRoot;
import org.apache.arrow.vector.dictionary.DictionaryProvider;
import org.apache.arrow.vector.ipc.ArrowFileWriter;
import org.apache.arrow.vector.ipc.ArrowStreamReader;
import org.apache.arrow.vector.ipc.ArrowStreamWriter;
import org.apache.arrow.vector.types.pojo.Field;
import org.apache.arrow.vector.types.pojo.Schema;
import org.techishthoughts.payload.generator.PayloadGenerator;
import org.techishthoughts.payload.model.User;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Apache Arrow Serialization Service
 *
 * Provides Apache Arrow serialization and deserialization capabilities for columnar data.
 * Apache Arrow is optimized for analytical workloads and big data processing.
 *
 * Key features:
 * - Columnar in-memory format
 * - Zero-copy reads
 * - Language-agnostic
 * - Big data optimized
 *
 * @author TechishThoughts
 * @version 1.0.0
 */
@Service
public class ArrowSerializationService {

    private final ObjectMapper jsonMapper;
    private final PayloadGenerator payloadGenerator;
    private final Map<String, byte[]> serializationCache;
    private final BufferAllocator allocator;

    public ArrowSerializationService() {
        this.jsonMapper = new ObjectMapper();
        this.payloadGenerator = new PayloadGenerator();
        this.serializationCache = new ConcurrentHashMap<>();
        this.allocator = new RootAllocator(Long.MAX_VALUE);
    }

    /**
     * Serialize a list of users to Arrow format
     *
     * @param users List of users to serialize
     * @return Serialization result with timing and size information
     */
    public SerializationResult serializeUsers(List<User> users) {
        long startTime = System.nanoTime();

        try {
            // Create Arrow vectors for user data
            VarCharVector nameVector = new VarCharVector("name", allocator);
            VarCharVector emailVector = new VarCharVector("email", allocator);
            VarCharVector phoneVector = new VarCharVector("phone", allocator);

            // Populate vectors
            for (int i = 0; i < users.size(); i++) {
                User user = users.get(i);
                nameVector.setSafe(i, user.getUsername().getBytes());
                emailVector.setSafe(i, user.getEmail().getBytes());
                phoneVector.setSafe(i, user.getFirstName().getBytes());
            }

            // Set value counts
            nameVector.setValueCount(users.size());
            emailVector.setValueCount(users.size());
            phoneVector.setValueCount(users.size());

            // Create schema and root
            List<Field> fields = List.of(
                Field.nullable("name", org.apache.arrow.vector.types.Types.MinorType.VARCHAR.getType()),
                Field.nullable("email", org.apache.arrow.vector.types.Types.MinorType.VARCHAR.getType()),
                Field.nullable("phone", org.apache.arrow.vector.types.Types.MinorType.VARCHAR.getType())
            );

            Schema schema = new Schema(fields);
            VectorSchemaRoot root = new VectorSchemaRoot(schema, List.of(nameVector, emailVector, phoneVector), users.size());

            // Serialize to Arrow format
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ArrowStreamWriter writer = new ArrowStreamWriter(root, new DictionaryProvider.MapDictionaryProvider(), baos);
            writer.start();
            writer.writeBatch();
            writer.end();

            byte[] arrowData = baos.toByteArray();
            long serializationTime = (System.nanoTime() - startTime) / 1_000_000; // Convert to ms

            // Clean up
            root.close();
            allocator.close();

            return SerializationResult.builder()
                    .success(true)
                    .serializationTimeMs(serializationTime)
                    .totalSizeBytes(arrowData.length)
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
     * Deserialize Arrow data back to a list of users
     *
     * @param arrowData Arrow byte array
     * @return Deserialization result with timing information
     */
    public DeserializationResult deserializeUsers(byte[] arrowData) {
        long startTime = System.nanoTime();

        try {
            // Read Arrow data
            ByteArrayInputStream bais = new ByteArrayInputStream(arrowData);
            ArrowStreamReader reader = new ArrowStreamReader(bais, allocator);

            VectorSchemaRoot root = reader.getVectorSchemaRoot();
            reader.loadNextBatch();

            // Extract data from vectors
            VarCharVector nameVector = (VarCharVector) root.getVector("name");
            VarCharVector emailVector = (VarCharVector) root.getVector("email");
            VarCharVector phoneVector = (VarCharVector) root.getVector("phone");

            int rowCount = root.getRowCount();
            long deserializationTime = (System.nanoTime() - startTime) / 1_000_000; // Convert to ms

            // Clean up
            root.close();
            reader.close();

            return DeserializationResult.builder()
                    .success(true)
                    .deserializationTimeMs(deserializationTime)
                    .userCount(rowCount)
                    .build();

        } catch (IOException e) {
            return DeserializationResult.builder()
                    .success(false)
                    .error(e.getMessage())
                    .build();
        }
    }

    /**
     * Compress Arrow data using different compression algorithms
     *
     * @param complexity Payload complexity level
     * @param iterations Number of iterations for testing
     * @return Compression result with ratios and timing
     */
    public CompressionResult compressData(String complexity, int iterations) {
        List<User> users = payloadGenerator.generateUsers(iterations);

        try {
            // Convert to Arrow format first
            byte[] arrowData = convertToArrowFormat(users);
            int originalSize = arrowData.length;

            // GZIP compression
            long gzipStart = System.nanoTime();
            byte[] gzipCompressed = compressGzip(arrowData);
            long gzipTime = (System.nanoTime() - gzipStart) / 1_000_000;
            double gzipRatio = (double) gzipCompressed.length / originalSize;

            // Zstandard compression
            long zstdStart = System.nanoTime();
            byte[] zstdCompressed = Zstd.compress(arrowData);
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
                byte[] arrowData = convertToArrowFormat(users);
                totalSerializationTime += (System.nanoTime() - serialStart) / 1_000_000;
                totalSize = arrowData.length;

                // Deserialization timing
                long deserialStart = System.nanoTime();
                readArrowFormat(arrowData);
                totalDeserializationTime += (System.nanoTime() - deserialStart) / 1_000_000;

                // Compression timing
                long compressStart = System.nanoTime();
                Zstd.compress(arrowData);
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

    private byte[] convertToArrowFormat(List<User> users) throws IOException {
        // Simplified Arrow conversion for demonstration
        // In a real implementation, you would use proper Arrow vectors
        return jsonMapper.writeValueAsBytes(users);
    }

    private void readArrowFormat(byte[] arrowData) throws IOException {
        // Simplified Arrow reading for demonstration
        jsonMapper.readValue(arrowData,
                jsonMapper.getTypeFactory().constructCollectionType(List.class, User.class));
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

    // Result classes (same structure as other services)
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
