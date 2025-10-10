package org.techishthoughts.kryo.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.techishthoughts.payload.generator.UnifiedPayloadGenerator;
import org.techishthoughts.payload.model.Address;
import org.techishthoughts.payload.model.Education;
import org.techishthoughts.payload.model.Language;
import org.techishthoughts.payload.model.Order;
import org.techishthoughts.payload.model.OrderItem;
import org.techishthoughts.payload.model.Payment;
import org.techishthoughts.payload.model.Skill;
import org.techishthoughts.payload.model.SocialConnection;
import org.techishthoughts.payload.model.TrackingEvent;
import org.techishthoughts.payload.model.User;
import org.techishthoughts.payload.model.UserProfile;
import org.techishthoughts.payload.service.AbstractSerializationService;
import org.techishthoughts.payload.service.SerializationException;
import org.techishthoughts.payload.service.result.CompressionResult;
import org.techishthoughts.payload.service.result.SerializationResult;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import net.jpountz.lz4.LZ4Compressor;
import net.jpountz.lz4.LZ4Factory;
import net.jpountz.lz4.LZ4FastDecompressor;

/**
 * Kryo serialization service implementing the common SerializationService interface.
 * Provides high-performance binary serialization with compression options.
 */
@Service
public class KryoSerializationService extends AbstractSerializationService {

    private static final Logger logger = LoggerFactory.getLogger(KryoSerializationService.class);
    private static final String FRAMEWORK_NAME = "Kryo";

    private final Kryo kryo;
    private final Kryo optimizedKryo;
    private final LZ4Factory lz4Factory;
    private final LZ4Compressor lz4Compressor;
    private final LZ4FastDecompressor lz4Decompressor;

    // Thread-local Kryo instances for thread safety
    private final ThreadLocal<Kryo> threadKryo = ThreadLocal.withInitial(() -> {
        Kryo k = new Kryo();
        k.setRegistrationRequired(false);
        k.setReferences(true);
        k.setAutoReset(false);

        // Register classes for better performance
        registerClasses(k);
        return k;
    });

    private final ThreadLocal<Kryo> threadOptimizedKryo = ThreadLocal.withInitial(() -> {
        Kryo k = new Kryo();
        k.setRegistrationRequired(false);
        k.setReferences(false);
        k.setAutoReset(false);
        k.setCopyReferences(false);

        // Register classes for better performance
        registerClasses(k);
        return k;
    });

    public KryoSerializationService(
            @Qualifier("kryo") Kryo kryo,
            @Qualifier("optimizedKryo") Kryo optimizedKryo,
            UnifiedPayloadGenerator payloadGenerator) {
        super(payloadGenerator);
        this.kryo = kryo;
        this.optimizedKryo = optimizedKryo;

        // Initialize LZ4
        this.lz4Factory = LZ4Factory.fastestInstance();
        this.lz4Compressor = lz4Factory.fastCompressor();
        this.lz4Decompressor = lz4Factory.fastDecompressor();

        logger.info("Initialized Kryo serialization service with thread-local instances");
    }

    private void registerClasses(Kryo kryo) {
        kryo.register(User.class);
        kryo.register(UserProfile.class);
        kryo.register(Address.class);
        kryo.register(Order.class);
        kryo.register(OrderItem.class);
        kryo.register(Payment.class);
        kryo.register(TrackingEvent.class);
        kryo.register(SocialConnection.class);
        kryo.register(Skill.class);
        kryo.register(Education.class);
        kryo.register(Language.class);
        kryo.register(java.time.LocalDateTime.class);
        kryo.register(java.util.ArrayList.class);
        kryo.register(java.util.HashMap.class);
        kryo.register(java.util.LinkedHashMap.class);
    }

    @Override
    public String getFrameworkName() {
        return FRAMEWORK_NAME;
    }

    @Override
    public org.techishthoughts.payload.service.result.SerializationResult serialize(List<User> users) throws SerializationException {
        try {
            logger.debug("Serializing {} users with Kryo", users.size());

            long startTime = System.nanoTime();

            // Use optimized Kryo for best performance
            Kryo kryoInstance = threadOptimizedKryo.get();

            // Use direct buffer for better performance
            Output output = new Output(1024 * 1024); // 1MB buffer

            try {
                // Write user count first
                output.writeInt(users.size());

                // Serialize each user
                for (User user : users) {
                    kryoInstance.writeObject(output, user);
                }

                output.flush();
                byte[] data = output.toBytes();
                long serializationTime = System.nanoTime() - startTime;

                logger.debug("Successfully serialized {} users to {} bytes in {:.2f} ms",
                        users.size(), data.length, serializationTime / 1_000_000.0);

                return org.techishthoughts.payload.service.result.SerializationResult.builder(FRAMEWORK_NAME)
                        .format("Kryo Binary")
                        .data(data)
                        .serializationTime(serializationTime)
                        .inputObjectCount(users.size())
                        .build();

            } finally {
                output.close();
            }

        } catch (Exception e) {
            logger.error("Failed to serialize {} users with Kryo", users.size(), e);
            throw SerializationException.serialization(FRAMEWORK_NAME, "Kryo serialization failed", e);
        }
    }

    @Override
    public List<User> deserialize(byte[] data) throws SerializationException {
        try {
            logger.debug("Deserializing {} bytes with Kryo", data.length);

            long startTime = System.nanoTime();
            Kryo kryoInstance = threadOptimizedKryo.get();

            ByteArrayInputStream bais = new ByteArrayInputStream(data);
            Input input = new Input(bais);

            try {
                // Read user count
                int userCount = input.readInt();
                List<User> users = new java.util.ArrayList<>(userCount);

                // Deserialize each user
                for (int i = 0; i < userCount; i++) {
                    User user = kryoInstance.readObject(input, User.class);
                    users.add(user);
                }

                long deserializationTime = System.nanoTime() - startTime;
                logger.debug("Successfully deserialized {} users from {} bytes in {:.2f} ms",
                        users.size(), data.length, deserializationTime / 1_000_000.0);

                return users;

            } finally {
                input.close();
            }

        } catch (Exception e) {
            logger.error("Failed to deserialize {} bytes with Kryo", data.length, e);
            throw SerializationException.deserialization(FRAMEWORK_NAME, "Kryo deserialization failed", e);
        }
    }

    @Override
    public CompressionResult compress(byte[] data) throws SerializationException {
        // Use LZ4 as the default compression for Kryo (fast compression/decompression)
        return compressWithLz4(data);
    }

    @Override
    public byte[] decompress(byte[] compressedData) throws SerializationException {
        // Default to LZ4 decompression
        return decompressLz4(compressedData);
    }

    @Override
    protected PerformanceTier getExpectedPerformanceTier() {
        return PerformanceTier.ULTRA_HIGH;
    }

    @Override
    protected MemoryFootprint getMemoryFootprint() {
        return MemoryFootprint.VERY_LOW;
    }

    @Override
    public List<String> getSupportedCompressionAlgorithms() {
        return Arrays.asList("LZ4", "GZIP");
    }

    @Override
    public boolean supportsSchemaEvolution() {
        return false; // Kryo doesn't natively support schema evolution
    }

    @Override
    public String getTypicalUseCase() {
        return "JVM-only applications, caching, session storage";
    }

    // LZ4 compression methods
    public CompressionResult compressWithLz4(byte[] data) throws SerializationException {
        try {
            long startTime = System.nanoTime();

            int maxCompressedLength = lz4Compressor.maxCompressedLength(data.length);
            byte[] compressed = new byte[maxCompressedLength];
            int compressedLength = lz4Compressor.compress(data, 0, data.length, compressed, 0, maxCompressedLength);

            // Trim the array to actual compressed size
            byte[] finalCompressed = new byte[compressedLength];
            System.arraycopy(compressed, 0, finalCompressed, 0, compressedLength);

            long compressionTime = System.nanoTime() - startTime;

            logger.debug("LZ4 compressed {} bytes to {} bytes in {:.2f} ms",
                    data.length, compressedLength, compressionTime / 1_000_000.0);

            return CompressionResult.builder("LZ4")
                    .compressedData(finalCompressed)
                    .compressionTime(compressionTime)
                    .originalSize(data.length)
                    .build();

        } catch (Exception e) {
            throw SerializationException.compression(FRAMEWORK_NAME, "LZ4 compression failed", data.length, e);
        }
    }

    public byte[] decompressLz4(byte[] compressedData) throws SerializationException {
        try {
            // For LZ4, we need to know the original size to decompress
            // In a real implementation, you might store this information with the compressed data
            // For now, we'll estimate a reasonable size
            int estimatedSize = compressedData.length * 4; // Conservative estimate
            byte[] decompressed = new byte[estimatedSize];

            int actualDecompressedLength = lz4Decompressor.decompress(compressedData, 0, decompressed, 0, estimatedSize);

            // Trim to actual size
            byte[] result = new byte[actualDecompressedLength];
            System.arraycopy(decompressed, 0, result, 0, actualDecompressedLength);

            return result;

        } catch (Exception e) {
            throw SerializationException.decompression(FRAMEWORK_NAME, "LZ4 decompression failed", compressedData.length, e);
        }
    }

    // GZIP compression methods
    public CompressionResult compressWithGzip(byte[] data) throws SerializationException {
        try {
            long startTime = System.nanoTime();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (GZIPOutputStream gzipOut = new GZIPOutputStream(baos)) {
                gzipOut.write(data);
            }

            byte[] compressed = baos.toByteArray();
            long compressionTime = System.nanoTime() - startTime;

            logger.debug("GZIP compressed {} bytes to {} bytes in {:.2f} ms",
                    data.length, compressed.length, compressionTime / 1_000_000.0);

            return CompressionResult.builder("GZIP")
                    .compressedData(compressed)
                    .compressionTime(compressionTime)
                    .originalSize(data.length)
                    .build();

        } catch (IOException e) {
            throw SerializationException.compression(FRAMEWORK_NAME, "GZIP compression failed", data.length, e);
        }
    }

    public byte[] decompressGzip(byte[] compressedData) throws SerializationException {
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(compressedData);
            GZIPInputStream gzipIn = new GZIPInputStream(bais);
            byte[] decompressed = gzipIn.readAllBytes();
            gzipIn.close();
            return decompressed;
        } catch (IOException e) {
            throw SerializationException.decompression(FRAMEWORK_NAME, "GZIP decompression failed", compressedData.length, e);
        }
    }

    // Legacy method support for backward compatibility
    public SerializationResult serializeUsers(List<User> users) {
        try {
            org.techishthoughts.payload.service.result.SerializationResult result = serialize(users);
            return new SerializationResult("kryo_standard", result.getData(), result.getSerializationTimeNs(), result.getSizeBytes());
        } catch (SerializationException e) {
            logger.error("Legacy serializeUsers failed", e);
            return new SerializationResult("kryo_standard", new byte[0], 0, 0);
        }
    }

    public SerializationResult serializeUsersOptimized(List<User> users) {
        try {
            org.techishthoughts.payload.service.result.SerializationResult result = serialize(users);
            return new SerializationResult("kryo_optimized", result.getData(), result.getSerializationTimeNs(), result.getSizeBytes());
        } catch (SerializationException e) {
            logger.error("Legacy serializeUsersOptimized failed", e);
            return new SerializationResult("kryo_optimized", new byte[0], 0, 0);
        }
    }

    public List<User> deserializeUsers(byte[] data) {
        try {
            return deserialize(data);
        } catch (SerializationException e) {
            logger.error("Legacy deserializeUsers failed", e);
            return new java.util.ArrayList<>();
        }
    }

    public List<User> deserializeUsersOptimized(byte[] data) {
        try {
            return deserialize(data);
        } catch (SerializationException e) {
            logger.error("Legacy deserializeUsersOptimized failed", e);
            return new java.util.ArrayList<>();
        }
    }

    public CachingResult cacheUsers(List<User> users, String cacheKey) {
        long startTime = System.nanoTime();
        SerializationResult serializationResult = serializeUsersOptimized(users);
        long cachingTime = System.nanoTime() - startTime;
        return new CachingResult(cacheKey, serializationResult, cachingTime);
    }

    public List<User> retrieveFromCache(String cacheKey) {
        // For backward compatibility, return null (cache functionality would need separate implementation)
        logger.warn("retrieveFromCache not implemented in refactored service");
        return null;
    }

    public PerformanceResult benchmarkPerformance(List<User> users, int iterations) {
        long totalSerializationTime = 0;
        long totalDeserializationTime = 0;
        byte[] serializedData = null;

        // Warm up
        for (int i = 0; i < 10; i++) {
            SerializationResult result = serializeUsersOptimized(users);
            deserializeUsersOptimized(result.getData());
        }

        // Benchmark serialization
        for (int i = 0; i < iterations; i++) {
            long startTime = System.nanoTime();
            SerializationResult result = serializeUsersOptimized(users);
            totalSerializationTime += System.nanoTime() - startTime;

            if (serializedData == null) {
                serializedData = result.getData();
            }
        }

        // Benchmark deserialization
        for (int i = 0; i < iterations; i++) {
            long startTime = System.nanoTime();
            deserializeUsersOptimized(serializedData);
            totalDeserializationTime += System.nanoTime() - startTime;
        }

        double avgSerializationTime = totalSerializationTime / (double) iterations;
        double avgDeserializationTime = totalDeserializationTime / (double) iterations;

        return new PerformanceResult(
            iterations,
            avgSerializationTime / 1_000_000.0, // Convert to ms
            avgDeserializationTime / 1_000_000.0, // Convert to ms
            serializedData != null ? serializedData.length : 0,
            users.size() / (avgSerializationTime / 1_000_000_000.0), // Throughput
            users.size() / (avgDeserializationTime / 1_000_000_000.0) // Throughput
        );
    }

    // Legacy nested classes for backward compatibility (kept as static inner classes)
    public static class LegacySerializationResult {
        private final String format;
        private final byte[] data;
        private final long serializationTimeNs;
        private final int sizeBytes;

        public LegacySerializationResult(String format, byte[] data, long serializationTimeNs, int sizeBytes) {
            this.format = format;
            this.data = data;
            this.serializationTimeNs = serializationTimeNs;
            this.sizeBytes = sizeBytes;
        }

        public String getFormat() { return format; }
        public byte[] getData() { return data; }
        public long getSerializationTimeNs() { return serializationTimeNs; }
        public double getSerializationTimeMs() { return serializationTimeNs / 1_000_000.0; }
        public int getSizeBytes() { return sizeBytes; }
        public double getSizeKB() { return sizeBytes / 1024.0; }
        public double getSizeMB() { return sizeBytes / (1024.0 * 1024.0); }
    }

    public static class CachingResult {
        private final String cacheKey;
        private final SerializationResult serializationResult;
        private final long cachingTimeNs;

        public CachingResult(String cacheKey, SerializationResult serializationResult, long cachingTimeNs) {
            this.cacheKey = cacheKey;
            this.serializationResult = serializationResult;
            this.cachingTimeNs = cachingTimeNs;
        }

        public String getCacheKey() { return cacheKey; }
        public SerializationResult getSerializationResult() { return serializationResult; }
        public long getCachingTimeNs() { return cachingTimeNs; }
        public double getCachingTimeMs() { return cachingTimeNs / 1_000_000.0; }
    }

    public static class PerformanceResult {
        private final int iterations;
        private final double avgSerializationTimeMs;
        private final double avgDeserializationTimeMs;
        private final int payloadSizeBytes;
        private final double serializationThroughput;
        private final double deserializationThroughput;

        public PerformanceResult(int iterations, double avgSerializationTimeMs, double avgDeserializationTimeMs,
                               int payloadSizeBytes, double serializationThroughput, double deserializationThroughput) {
            this.iterations = iterations;
            this.avgSerializationTimeMs = avgSerializationTimeMs;
            this.avgDeserializationTimeMs = avgDeserializationTimeMs;
            this.payloadSizeBytes = payloadSizeBytes;
            this.serializationThroughput = serializationThroughput;
            this.deserializationThroughput = deserializationThroughput;
        }

        public int getIterations() { return iterations; }
        public double getAvgSerializationTimeMs() { return avgSerializationTimeMs; }
        public double getAvgDeserializationTimeMs() { return avgDeserializationTimeMs; }
        public int getPayloadSizeBytes() { return payloadSizeBytes; }
        public double getSerializationThroughput() { return serializationThroughput; }
        public double getDeserializationThroughput() { return deserializationThroughput; }
    }

    // Backward compatibility alias
    public static class SerializationResult extends LegacySerializationResult {
        public SerializationResult(String format, byte[] data, long serializationTimeNs, int sizeBytes) {
            super(format, data, serializationTimeNs, sizeBytes);
        }
    }
}