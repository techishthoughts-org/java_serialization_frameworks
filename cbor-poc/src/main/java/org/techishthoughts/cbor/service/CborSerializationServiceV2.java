package org.techishthoughts.cbor.service;

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
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * CBOR Serialization Service (V2 - using AbstractSerializationService)
 *
 * Provides CBOR binary serialization using Jackson CBOR format.
 * CBOR is optimized for IoT and constrained environments.
 *
 * Features:
 * - Extends AbstractSerializationService for unified benchmarking
 * - Native CBOR binary serialization
 * - GZIP compression support
 * - Memory monitoring integration
 * - Roundtrip testing
 */
@Service
public class CborSerializationServiceV2 extends AbstractSerializationService {

    private final ObjectMapper cborMapper;

    public CborSerializationServiceV2(UnifiedPayloadGenerator payloadGenerator) {
        super(payloadGenerator);
        this.cborMapper = new ObjectMapper(new CBORFactory());
        this.cborMapper.registerModule(new JavaTimeModule());
    }

    @Override
    public String getFrameworkName() {
        return "CBOR";
    }

    @Override
    public String getTypicalUseCase() {
        return "IoT and constrained environments requiring compact binary format";
    }

    @Override
    public boolean supportsSchemaEvolution() {
        return true; // CBOR is self-describing
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
        return MemoryFootprint.LOW;
    }

    @Override
    public SerializationResult serialize(List<User> users) throws SerializationException {
        long startTime = System.nanoTime();

        try {
            byte[] data = cborMapper.writeValueAsBytes(users);
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
                    "Failed to serialize users with CBOR",
                    e
            );
        }
    }

    @Override
    public List<User> deserialize(byte[] data) throws SerializationException {
        try {
            CollectionType listType = cborMapper.getTypeFactory()
                    .constructCollectionType(List.class, User.class);
            return cborMapper.readValue(data, listType);
        } catch (Exception e) {
            throw SerializationException.deserialization(
                    getFrameworkName(),
                    "Failed to deserialize users with CBOR",
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
