package org.techishthoughts.grpc.service;

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
 * gRPC Serialization Service (V2 - using AbstractSerializationService)
 *
 * Provides gRPC-style serialization benchmarking using JSON as the underlying format
 * for testing purposes. Migrated to V2 architecture with unified benchmark support.
 *
 * Note: This uses JSON fallback as a placeholder for actual gRPC protobuf serialization.
 * For production use, implement proper protobuf schema-based serialization with gRPC.
 *
 * Features:
 * - Extends AbstractSerializationService for unified benchmarking
 * - JSON-based serialization (gRPC placeholder)
 * - GZIP compression support
 * - Memory monitoring integration
 * - Roundtrip testing
 */
@Service
public class GrpcSerializationServiceV2 extends AbstractSerializationService {

    private final ObjectMapper objectMapper;

    public GrpcSerializationServiceV2(UnifiedPayloadGenerator payloadGenerator) {
        super(payloadGenerator);
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    public String getFrameworkName() {
        return "gRPC";
    }

    @Override
    public String getTypicalUseCase() {
        return "High-performance RPC framework for microservices and distributed systems";
    }

    @Override
    public boolean supportsSchemaEvolution() {
        return true; // gRPC with protobuf supports schema evolution
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
        return MemoryFootprint.MEDIUM;
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
                    "Failed to serialize users with gRPC",
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
                    "Failed to deserialize users with gRPC",
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
