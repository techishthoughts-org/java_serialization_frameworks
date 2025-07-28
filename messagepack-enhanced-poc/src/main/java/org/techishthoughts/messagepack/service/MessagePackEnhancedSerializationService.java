package org.techishthoughts.messagepack.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// Native MessagePack
import org.msgpack.core.MessageBufferPacker;
import org.msgpack.core.MessagePack;
// MessagePack Jackson integration - 2025 FIXED imports
import org.msgpack.jackson.dataformat.MessagePackFactory;
import org.springframework.stereotype.Service;
import org.techishthoughts.messagepack.service.MessagePackEnhancedSerializationService.CompressionAlgorithm;
import org.techishthoughts.messagepack.service.MessagePackEnhancedSerializationService.SerializationResult;
import org.techishthoughts.payload.generator.PayloadGenerator;
import org.techishthoughts.payload.model.User;
import org.xerial.snappy.Snappy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.github.luben.zstd.Zstd;

// Compression
import net.jpountz.lz4.LZ4Compressor;
import net.jpountz.lz4.LZ4Factory;
import net.jpountz.lz4.LZ4FastDecompressor;

/**
 * Enhanced MessagePack Serialization Service (2025)
 *
 * FIXED: Uses correct 2025 MessagePack dependencies and imports
 * Features: Jackson integration, native MessagePack, multi-compression support
 */
@Service
public class MessagePackEnhancedSerializationService {

    private final PayloadGenerator payloadGenerator;
    private final ObjectMapper jacksonMessagePackMapper;
    private final ObjectMapper jsonMapper;
    private final LZ4Compressor lz4Compressor;
    private final LZ4FastDecompressor lz4Decompressor;
    private final Map<String, Long> performanceMetrics = new ConcurrentHashMap<>();

    public MessagePackEnhancedSerializationService(PayloadGenerator payloadGenerator) {
        this.payloadGenerator = payloadGenerator;

        // Initialize Jackson with MessagePack factory - 2025 FIXED
        this.jacksonMessagePackMapper = new ObjectMapper(new MessagePackFactory());
        this.jacksonMessagePackMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

        // Regular JSON mapper for comparison
        this.jsonMapper = new ObjectMapper();

        // Initialize LZ4 compression
        LZ4Factory factory = LZ4Factory.fastestInstance();
        this.lz4Compressor = factory.fastCompressor();
        this.lz4Decompressor = factory.fastDecompressor();

        System.out.println("✅ Enhanced MessagePack Service initialized (2025 FIXED)");
    }

    /**
     * Serialize using Jackson MessagePack integration
     */
    public byte[] serializeUsersJackson(List<User> users) {
        long startTime = System.nanoTime();

        try {
            byte[] result = jacksonMessagePackMapper.writeValueAsBytes(users);

            long serializationTime = System.nanoTime() - startTime;
            performanceMetrics.put("lastJacksonSerializationNs", serializationTime);

            System.out.println("MessagePack Jackson serialization took: " + (serializationTime / 1_000_000.0) + " ms");
            return result;

        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize users with MessagePack Jackson", e);
        }
    }

    /**
     * Deserialize using Jackson MessagePack integration
     */
    @SuppressWarnings("unchecked")
    public List<User> deserializeUsersJackson(byte[] data) {
        long startTime = System.nanoTime();

        try {
            List<User> users = jacksonMessagePackMapper.readValue(data,
                jacksonMessagePackMapper.getTypeFactory().constructCollectionType(List.class, User.class));

            long deserializationTime = System.nanoTime() - startTime;
            performanceMetrics.put("lastJacksonDeserializationNs", deserializationTime);

            System.out.println("MessagePack Jackson deserialization took: " + (deserializationTime / 1_000_000.0) + " ms");
            return users;

        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize users with MessagePack Jackson", e);
        }
    }

    /**
     * Serialize using native MessagePack
     */
    public byte[] serializeUsersNative(List<User> users) {
        long startTime = System.nanoTime();

        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            MessageBufferPacker packer = MessagePack.newDefaultBufferPacker();

            // Pack users array
            packer.packArrayHeader(users.size());
            for (User user : users) {
                packUser(packer, user);
            }

            byte[] result = packer.toByteArray();
            packer.close();

            long serializationTime = System.nanoTime() - startTime;
            performanceMetrics.put("lastNativeSerializationNs", serializationTime);

            System.out.println("MessagePack Native serialization took: " + (serializationTime / 1_000_000.0) + " ms");
            return result;

        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize users with Native MessagePack", e);
        }
    }

    /**
     * Serialize with compression
     */
    public SerializationResult serializeUsersCompressed(List<User> users, CompressionAlgorithm algorithm) {
        long startTime = System.nanoTime();

        try {
            // First serialize with Jackson MessagePack
            byte[] messagePackData = serializeUsersJackson(users);

            // Then compress
            byte[] compressedData;
            switch (algorithm) {
                case LZ4:
                    compressedData = lz4Compressor.compress(messagePackData);
                    break;
                case SNAPPY:
                    compressedData = Snappy.compress(messagePackData);
                    break;
                case ZSTD:
                    compressedData = Zstd.compress(messagePackData);
                    break;
                default:
                    compressedData = messagePackData;
            }

            long serializationTime = System.nanoTime() - startTime;
            performanceMetrics.put("lastCompressedSerializationNs", serializationTime);

            return new SerializationResult(
                algorithm,
                compressedData,
                messagePackData.length,
                compressedData.length,
                serializationTime
            );

        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize users with compressed MessagePack", e);
        }
    }

    /**
     * Deserialize compressed data
     */
    public List<User> deserializeUsersCompressed(SerializationResult result) {
        long startTime = System.nanoTime();

        try {
            // First decompress
            byte[] messagePackData;
            switch (result.getAlgorithm()) {
                case LZ4:
                    messagePackData = lz4Decompressor.decompress(result.getCompressedData(), result.getOriginalSize());
                    break;
                case SNAPPY:
                    messagePackData = Snappy.uncompress(result.getCompressedData());
                    break;
                case ZSTD:
                    messagePackData = Zstd.decompress(result.getCompressedData(), result.getOriginalSize());
                    break;
                default:
                    messagePackData = result.getCompressedData();
            }

            // Then deserialize
            List<User> users = deserializeUsersJackson(messagePackData);

            long deserializationTime = System.nanoTime() - startTime;
            performanceMetrics.put("lastCompressedDeserializationNs", deserializationTime);

            return users;

        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize compressed MessagePack data", e);
        }
    }

    /**
     * Get enhanced statistics
     */
    public Map<String, Object> getEnhancedStats() {
        return Map.of(
            "framework", "MessagePack Enhanced (2025 FIXED)",
            "version", "0.9.8",
            "features", List.of(
                "Jackson integration",
                "Native MessagePack support",
                "Multi-compression algorithms",
                "Schema evolution support",
                "Cross-language compatibility",
                "Compact binary format"
            ),
            "compressionAlgorithms", List.of("LZ4", "Snappy", "ZSTD"),
            "performanceMetrics", performanceMetrics,
            "status", "✅ All dependencies resolved"
        );
    }

    /**
     * Helper method to pack a User object
     */
    private void packUser(MessageBufferPacker packer, User user) throws IOException {
        packer.packMapHeader(8); // Adjust based on number of fields

        packer.packString("id");
        packer.packLong(user.getId());

        packer.packString("username");
        packer.packString(user.getUsername() != null ? user.getUsername() : "");

        packer.packString("email");
        packer.packString(user.getEmail() != null ? user.getEmail() : "");

        packer.packString("firstName");
        packer.packString(user.getFirstName() != null ? user.getFirstName() : "");

        packer.packString("lastName");
        packer.packString(user.getLastName() != null ? user.getLastName() : "");

        packer.packString("isActive");
        packer.packBoolean(user.getIsActive() != null ? user.getIsActive() : false);

        packer.packString("loyaltyPoints");
        packer.packDouble(user.getLoyaltyPoints() != null ? user.getLoyaltyPoints() : 0.0);

        packer.packString("createdAt");
        packer.packString(user.getCreatedAt() != null ? user.getCreatedAt().toString() : "");
    }

    /**
     * Compression algorithms
     */
    public enum CompressionAlgorithm {
        NONE, LZ4, SNAPPY, ZSTD
    }

    /**
     * Serialization result with compression info
     */
    public static class SerializationResult {
        private final CompressionAlgorithm algorithm;
        private final byte[] compressedData;
        private final int originalSize;
        private final int compressedSize;
        private final long serializationTime;

        public SerializationResult(CompressionAlgorithm algorithm, byte[] compressedData,
                                   int originalSize, int compressedSize, long serializationTime) {
            this.algorithm = algorithm;
            this.compressedData = compressedData;
            this.originalSize = originalSize;
            this.compressedSize = compressedSize;
            this.serializationTime = serializationTime;
        }

        public CompressionAlgorithm getAlgorithm() { return algorithm; }
        public byte[] getCompressedData() { return compressedData; }
        public int getOriginalSize() { return originalSize; }
        public int getCompressedSize() { return compressedSize; }
        public long getSerializationTime() { return serializationTime; }

        public double getCompressionRatio() {
            return originalSize > 0 ? (double) compressedSize / originalSize : 1.0;
        }
    }
}
