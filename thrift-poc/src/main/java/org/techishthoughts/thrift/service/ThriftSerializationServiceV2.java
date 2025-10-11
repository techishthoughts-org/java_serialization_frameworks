package org.techishthoughts.thrift.service;

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
 * Apache Thrift Serialization Service (V2 - using AbstractSerializationService)
 *
 * Provides Thrift-style serialization benchmarking using JSON as the underlying format
 * for testing purposes. Migrated to V2 architecture with unified benchmark support.
 *
 * Features:
 * - Extends AbstractSerializationService for unified benchmarking
 * - JSON-based serialization (Thrift fallback)
 * - GZIP compression support
 * - Memory monitoring integration
 * - Roundtrip testing
 */
@Service
public class ThriftSerializationServiceV2 extends AbstractSerializationService {

    private final ObjectMapper objectMapper;

    public ThriftSerializationServiceV2(UnifiedPayloadGenerator payloadGenerator) {
        super(payloadGenerator);
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    public String getFrameworkName() {
        return "Apache Thrift";
    }

    @Override
    public String getTypicalUseCase() {
        return "Cross-language RPC and service-oriented architectures";
    }

    @Override
    public boolean supportsSchemaEvolution() {
        return true; // Thrift supports schema evolution with field versioning
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
                    .format("JSON")
                    .data(data)
                    .serializationTime(durationNs)
                    .inputObjectCount(users.size())
                    .build();

        } catch (Exception e) {
            throw SerializationException.serialization(
                    getFrameworkName(),
                    "Failed to serialize users with Thrift",
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
                    "Failed to deserialize users with Thrift",
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
