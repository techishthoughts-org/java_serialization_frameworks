package org.techishthoughts.flatbuffers.service;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.jpountz.lz4.LZ4Compressor;
import net.jpountz.lz4.LZ4Factory;
import net.jpountz.lz4.LZ4FastDecompressor;
import org.springframework.stereotype.Service;
import org.techishthoughts.payload.generator.PayloadGenerator;
import org.techishthoughts.payload.model.User;
import org.xerial.snappy.Snappy;

import com.github.luben.zstd.Zstd;
import com.google.flatbuffers.FlatBufferBuilder;

/**
 * FlatBuffers Serialization Service
 *
 * 2025 UPDATE: Google FlatBuffers provides zero-copy serialization with
 * exceptional performance for high-throughput applications.
 *
 * Features:
 * - Zero-copy serialization/deserialization
 * - Schema-based validation
 * - Cross-language compatibility
 * - Memory-efficient operations
 * - Integration with modern compression algorithms
 */
@Service
public class FlatBuffersSerializationService {

    private final PayloadGenerator payloadGenerator;
    private final LZ4Compressor lz4Compressor;
    private final LZ4FastDecompressor lz4Decompressor;
    private final Map<String, ByteBuffer> cache = new ConcurrentHashMap<>();

    public FlatBuffersSerializationService(PayloadGenerator payloadGenerator) {
        this.payloadGenerator = payloadGenerator;

        // Initialize LZ4 compression
        LZ4Factory factory = LZ4Factory.fastestInstance();
        this.lz4Compressor = factory.fastCompressor();
        this.lz4Decompressor = factory.fastDecompressor();
    }

    /**
     * Serialize users using FlatBuffers with zero-copy operations
     */
    public SerializationResult serializeUsers(List<User> users) {
        long startTime = System.nanoTime();

        try {
            // For simplicity, serialize the first user as root
            // In a real implementation, you might want to create a wrapper table
            if (users.isEmpty()) {
                throw new IllegalArgumentException("Cannot serialize empty user list");
            }

            // Create FlatBuffer builder
            FlatBufferBuilder builder = new FlatBufferBuilder(1024);

            // Convert the first user to FlatBuffers format
            int userOffset = convertUserToFlatBuffer(users.get(0), builder);

            // Finish building with the user as root
            org.techishthoughts.flatbuffers.User.finishUserBuffer(builder, userOffset);

            // Get the serialized data
            byte[] data = builder.sizedByteArray();

            long serializationTime = System.nanoTime() - startTime;

            return new SerializationResult("FlatBuffers", data, serializationTime, data.length);

        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize users with FlatBuffers", e);
        }
    }

    /**
     * Deserialize users using FlatBuffers zero-copy operations
     */
    public List<User> deserializeUsers(byte[] data) {
        long startTime = System.nanoTime();

        try {
            // Create ByteBuffer from data
            ByteBuffer buffer = ByteBuffer.wrap(data);

            // Get root object (single user)
            org.techishthoughts.flatbuffers.User fbUser = org.techishthoughts.flatbuffers.User.getRootAsUser(buffer);

            // Convert back to Java object
            List<User> users = new java.util.ArrayList<>();
            users.add(convertFlatBufferToUser(fbUser));

            long deserializationTime = System.nanoTime() - startTime;
            System.out.println("FlatBuffers deserialization took: " + (deserializationTime / 1_000_000.0) + " ms");

            return users;

        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize users with FlatBuffers", e);
        }
    }

    /**
     * Compress FlatBuffers data with LZ4 (extremely fast)
     */
    public CompressionResult compressWithLZ4(byte[] data) {
        long startTime = System.nanoTime();

        try {
            byte[] compressed = lz4Compressor.compress(data);
            long compressionTime = System.nanoTime() - startTime;

            return new CompressionResult("LZ4", compressed, compressionTime, data.length, compressed.length);
        } catch (Exception e) {
            throw new RuntimeException("Failed to compress with LZ4", e);
        }
    }

    /**
     * Compress FlatBuffers data with Snappy (Google's fast compression)
     */
    public CompressionResult compressWithSnappy(byte[] data) {
        long startTime = System.nanoTime();

        try {
            byte[] compressed = Snappy.compress(data);
            long compressionTime = System.nanoTime() - startTime;

            return new CompressionResult("Snappy", compressed, compressionTime, data.length, compressed.length);
        } catch (IOException e) {
            throw new RuntimeException("Failed to compress with Snappy", e);
        }
    }

    /**
     * Compress FlatBuffers data with Zstandard
     */
    public CompressionResult compressWithZstd(byte[] data) {
        long startTime = System.nanoTime();

        try {
            byte[] compressed = Zstd.compress(data);
            long compressionTime = System.nanoTime() - startTime;

            return new CompressionResult("Zstandard", compressed, compressionTime, data.length, compressed.length);
        } catch (Exception e) {
            throw new RuntimeException("Failed to compress with Zstandard", e);
        }
    }

    /**
     * Decompress and deserialize with LZ4
     */
    public List<User> decompressAndDeserializeLZ4(byte[] compressedData) {
        long startTime = System.nanoTime();

        try {
            // Decompress - we need to know the original size
            // For simplicity, we'll use a fixed buffer size (in real scenarios, you'd store the original size)
            byte[] decompressed = lz4Decompressor.decompress(compressedData, 1024 * 1024); // 1MB buffer
            long decompressionTime = System.nanoTime() - startTime;

            System.out.println("LZ4 decompression took: " + (decompressionTime / 1_000_000.0) + " ms");

            // Deserialize
            return deserializeUsers(decompressed);

        } catch (Exception e) {
            throw new RuntimeException("Failed to decompress and deserialize with LZ4", e);
        }
    }

    /**
     * Decompress and deserialize with Snappy
     */
    public List<User> decompressAndDeserializeSnappy(byte[] compressedData) {
        long startTime = System.nanoTime();

        try {
            // Decompress
            byte[] decompressed = Snappy.uncompress(compressedData);
            long decompressionTime = System.nanoTime() - startTime;

            System.out.println("Snappy decompression took: " + (decompressionTime / 1_000_000.0) + " ms");

            // Deserialize
            return deserializeUsers(decompressed);

        } catch (IOException e) {
            throw new RuntimeException("Failed to decompress and deserialize with Snappy", e);
        }
    }

    /**
     * Decompress and deserialize with Zstandard
     */
    public List<User> decompressAndDeserializeZstd(byte[] compressedData) {
        long startTime = System.nanoTime();

        try {
            // Decompress
            byte[] decompressed = Zstd.decompress(compressedData, (int) Zstd.decompressedSize(compressedData));
            long decompressionTime = System.nanoTime() - startTime;

            System.out.println("Zstandard decompression took: " + (decompressionTime / 1_000_000.0) + " ms");

            // Deserialize
            return deserializeUsers(decompressed);

        } catch (Exception e) {
            throw new RuntimeException("Failed to decompress and deserialize with Zstandard", e);
        }
    }

    /**
     * Get serialization statistics
     */
    public Map<String, Object> getSerializationStats() {
        return Map.of(
            "framework", "Google FlatBuffers",
            "zero_copy_enabled", true,
            "schema_based", true,
            "cross_language_support", true,
            "memory_efficient", true,
            "compression_support", "LZ4, Snappy, Zstandard",
            "version", "24.3.25"
        );
    }

    // Helper methods for conversion (simplified for demonstration)
    private int convertUserToFlatBuffer(User user, FlatBufferBuilder builder) {
        // This is a simplified conversion - in a real implementation,
        // you would convert all fields from Java User to FlatBuffers User

        int username = builder.createString(user.getUsername());
        int email = builder.createString(user.getEmail());
        int firstName = builder.createString(user.getFirstName());
        int lastName = builder.createString(user.getLastName());
        int createdAt = builder.createString(user.getCreatedAt().toString());
        int lastLoginAt = builder.createString(user.getLastLoginAt().toString());

        org.techishthoughts.flatbuffers.User.startUser(builder);
        org.techishthoughts.flatbuffers.User.addId(builder, user.getId());
        org.techishthoughts.flatbuffers.User.addUsername(builder, username);
        org.techishthoughts.flatbuffers.User.addEmail(builder, email);
        org.techishthoughts.flatbuffers.User.addFirstName(builder, firstName);
        org.techishthoughts.flatbuffers.User.addLastName(builder, lastName);
        org.techishthoughts.flatbuffers.User.addIsActive(builder, user.getIsActive());
        org.techishthoughts.flatbuffers.User.addCreatedAt(builder, createdAt);
        org.techishthoughts.flatbuffers.User.addLastLoginAt(builder, lastLoginAt);

        return org.techishthoughts.flatbuffers.User.endUser(builder);
    }

    private User convertFlatBufferToUser(org.techishthoughts.flatbuffers.User fbUser) {
        // This is a simplified conversion - in a real implementation,
        // you would convert all fields from FlatBuffers User to Java User

        User user = new User();
        user.setId(fbUser.id());
        user.setUsername(fbUser.username());
        user.setEmail(fbUser.email());
        user.setFirstName(fbUser.firstName());
        user.setLastName(fbUser.lastName());
        user.setIsActive(fbUser.isActive());
        // Note: In a real implementation, you would parse the date strings

        return user;
    }

    // Result classes
    public static class SerializationResult {
        private final String format;
        private final byte[] data;
        private final long serializationTimeNs;
        private final int sizeBytes;

        public SerializationResult(String format, byte[] data, long serializationTimeNs, int sizeBytes) {
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

    public static class CompressionResult {
        private final String algorithm;
        private final byte[] compressedData;
        private final long compressionTimeNs;
        private final int originalSize;
        private final int compressedSize;

        public CompressionResult(String algorithm, byte[] compressedData, long compressionTimeNs, int originalSize, int compressedSize) {
            this.algorithm = algorithm;
            this.compressedData = compressedData;
            this.compressionTimeNs = compressionTimeNs;
            this.originalSize = originalSize;
            this.compressedSize = compressedSize;
        }

        public String getAlgorithm() { return algorithm; }
        public byte[] getCompressedData() { return compressedData; }
        public long getCompressionTimeNs() { return compressionTimeNs; }
        public double getCompressionTimeMs() { return compressionTimeNs / 1_000_000.0; }
        public int getOriginalSize() { return originalSize; }
        public int getCompressedSize() { return compressedSize; }
        public double getCompressionRatio() { return (double) originalSize / compressedSize; }
        public double getCompressionPercentage() { return (1.0 - (double) compressedSize / originalSize) * 100.0; }
    }
}
