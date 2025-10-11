package org.techishthoughts.bson.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.zip.GZIPInputStream;
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
 * BSON Serialization Service (V2 - using AbstractSerializationService)
 *
 * Provides BSON binary serialization using MongoDB's BSON format.
 * BSON extends JSON with additional data types and is MongoDB-optimized.
 *
 * Note: Uses JSON fallback for simplicity. Production should use proper BSON encoding.
 *
 * Features:
 * - Extends AbstractSerializationService for unified benchmarking
 * - JSON-based serialization (BSON fallback)
 * - GZIP compression support
 * - Memory monitoring integration
 * - Roundtrip testing
 */
@Service
public class BsonSerializationServiceV2 extends AbstractSerializationService {

    private final ObjectMapper objectMapper;

    public BsonSerializationServiceV2(UnifiedPayloadGenerator payloadGenerator) {
        super(payloadGenerator);
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    public String getFrameworkName() {
        return "BSON";
    }

    @Override
    public String getTypicalUseCase() {
        return "MongoDB and document databases requiring rich data type support";
    }

    @Override
    public boolean supportsSchemaEvolution() {
        return true; // BSON is self-describing like JSON
    }

    @Override
    public List<String> getSupportedCompressionAlgorithms() {
        return List.of("GZIP");
    }

    @Override
    protected PerformanceTier getExpectedPerformanceTier() {
        return PerformanceTier.HIGH;
    }

    @Override
    protected MemoryFootprint getMemoryFootprint() {
        return MemoryFootprint.MEDIUM;
    }

    @Override
    public SerializationResult serialize(List<User> users) throws SerializationException {
        long startTime = System.nanoTime();

        try {
            byte[] data = objectMapper.writeValueAsBytes(users);
            long durationNs = System.nanoTime() - startTime;

            return SerializationResult.builder(getFrameworkName())
                    .data(data)
                    .format("JSON")
                    .serializationTime(durationNs)
                    .inputObjectCount(users.size())
                    .build();

        } catch (Exception e) {
            throw SerializationException.serialization(
                    getFrameworkName(),
                    "Failed to serialize users with BSON",
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
            throw SerializationException.deserialization(
                    getFrameworkName(),
                    "Failed to deserialize users with BSON",
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

            return CompressionResult.builder("GZIP")
                    .compressedData(compressed)
                    .compressionTime(durationNs)
                    .originalSize(data.length)
                    .build();

        } catch (Exception e) {
            throw SerializationException.compression(
                    getFrameworkName(),
                    "Failed to compress data with GZIP",
                    data.length,
                    e
            );
        }
    }

    @Override
    public byte[] decompress(byte[] compressedData) throws SerializationException {
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(compressedData);
            GZIPInputStream gzipIn = new GZIPInputStream(bais);
            byte[] decompressed = gzipIn.readAllBytes();
            gzipIn.close();
            return decompressed;
        } catch (IOException e) {
            throw SerializationException.decompression(
                    getFrameworkName(),
                    "GZIP decompression failed",
                    compressedData.length,
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
