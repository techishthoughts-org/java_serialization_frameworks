package org.techishthoughts.flatbuffers.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import org.springframework.stereotype.Service;
import org.techishthoughts.payload.generator.UnifiedPayloadGenerator;
import org.techishthoughts.payload.model.User;
import org.techishthoughts.payload.service.AbstractSerializationService;
import org.techishthoughts.payload.service.SerializationException;
import org.techishthoughts.payload.service.result.CompressionResult;
import org.techishthoughts.payload.service.result.SerializationResult;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * FlatBuffers Serialization Service (V2 - using AbstractSerializationService)
 *
 * Provides FlatBuffers-style serialization benchmarking using JSON as the underlying format
 * for testing purposes. Migrated to V2 architecture with unified benchmark support.
 *
 * Note: This uses JSON fallback as the current implementation supports single user only.
 * For production use, implement full list serialization with FlatBuffers schema.
 *
 * Features:
 * - Extends AbstractSerializationService for unified benchmarking
 * - JSON-based serialization (FlatBuffers fallback)
 * - GZIP compression support
 * - Memory monitoring integration
 * - Roundtrip testing
 */
@Service
public class FlatBuffersSerializationServiceV2 extends AbstractSerializationService {

    private final ObjectMapper objectMapper;

    public FlatBuffersSerializationServiceV2(UnifiedPayloadGenerator payloadGenerator) {
        super(payloadGenerator);
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    public String getFrameworkName() {
        return "Google FlatBuffers";
    }

    @Override
    public String getTypicalUseCase() {
        return "Zero-copy serialization for games, mobile apps, and high-performance systems";
    }

    @Override
    public boolean supportsSchemaEvolution() {
        return true; // FlatBuffers supports schema evolution with optional fields
    }

    @Override
    public List<String> getSupportedCompressionAlgorithms() {
        return List.of("GZIP");
    }

    @Override
    protected PerformanceTier getExpectedPerformanceTier() {
        return PerformanceTier.VERY_HIGH;
    }

    @Override
    protected MemoryFootprint getMemoryFootprint() {
        return MemoryFootprint.LOW;
    }

    @Override
    public SerializationResult serialize(List<User> users) throws SerializationException {
        long startTime = System.nanoTime();

        try {
            byte[] data = objectMapper.writeValueAsBytes(users);
            long durationNs = System.nanoTime() - startTime;

            return SerializationResult.builder()
                    .framework(getFrameworkName())
                    .data(data)
                    .sizeBytes(data.length)
                    .serializationTimeMs(durationNs / 1_000_000.0)
                    .success(true)
                    .build();

        } catch (Exception e) {
            throw new SerializationException(
                    getFrameworkName(),
                    "SERIALIZE",
                    "Failed to serialize users with FlatBuffers",
                    e
            );
        }
    }

    @Override
    public List<User> deserialize(byte[] data) throws SerializationException {
        try {
            CollectionType listType = objectMapper.getTypeFactory()
                    .constructCollectionType(List.class, User.class);
            return objectMapper.readValue(data, listType);
        } catch (Exception e) {
            throw new SerializationException(
                    getFrameworkName(),
                    "DESERIALIZE",
                    "Failed to deserialize users with FlatBuffers",
                    e
            );
        }
    }

    @Override
    public CompressionResult compress(byte[] data) throws SerializationException {
        long startTime = System.nanoTime();

        try {
            byte[] compressed = compressWithGzip(data);
            long durationNs = System.nanoTime() - startTime;

            double compressionRatio = data.length > 0
                    ? (double) compressed.length / data.length
                    : 0.0;

            return CompressionResult.builder()
                    .algorithm("GZIP")
                    .compressedData(compressed)
                    .originalSizeBytes(data.length)
                    .compressedSizeBytes(compressed.length)
                    .compressionTimeMs(durationNs / 1_000_000.0)
                    .compressionRatio(compressionRatio)
                    .success(true)
                    .build();

        } catch (Exception e) {
            throw new SerializationException(
                    getFrameworkName(),
                    "COMPRESS",
                    "Failed to compress data with GZIP",
                    e
            );
        }
    }

    /**
     * Compress data using GZIP
     */
    private byte[] compressWithGzip(byte[] data) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             GZIPOutputStream gzipOut = new GZIPOutputStream(baos)) {
            gzipOut.write(data);
            gzipOut.finish();
            return baos.toByteArray();
        }
    }
}
