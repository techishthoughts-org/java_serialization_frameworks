package org.techishthoughts.jackson.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.techishthoughts.payload.generator.UnifiedPayloadGenerator;
import org.techishthoughts.payload.model.User;
import org.techishthoughts.payload.service.BenchmarkConfig;
import org.techishthoughts.payload.service.SerializationException;
import org.techishthoughts.payload.service.AbstractSerializationService;
import org.techishthoughts.payload.service.result.BenchmarkResult;
import org.techishthoughts.payload.service.result.CompressionResult;
import org.techishthoughts.payload.service.result.SerializationResult;

import com.aayushatharva.brotli4j.Brotli4jLoader;
import com.aayushatharva.brotli4j.decoder.Decoder;
import com.aayushatharva.brotli4j.encoder.Encoder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.luben.zstd.Zstd;

/**
 * Jackson JSON serialization service implementing the common SerializationService interface.
 * Provides JSON serialization with multiple compression options and comprehensive benchmarking.
 */
@Service
public class JacksonSerializationServiceImpl extends AbstractSerializationService {

    private static final Logger logger = LoggerFactory.getLogger(JacksonSerializationServiceImpl.class);
    private static final String FRAMEWORK_NAME = "Jackson JSON";

    private final ObjectMapper standardMapper;
    private final ObjectMapper compactMapper;

    static {
        // Initialize Brotli
        if (!Brotli4jLoader.isAvailable()) {
            throw new RuntimeException("Brotli4j not available");
        }
    }

    public JacksonSerializationServiceImpl(
            ObjectMapper standardMapper,
            @Qualifier("compactObjectMapper") ObjectMapper compactMapper,
            UnifiedPayloadGenerator payloadGenerator) {
        super(payloadGenerator);
        this.standardMapper = configureMapper(standardMapper);
        this.compactMapper = configureMapper(compactMapper);

        logger.info("Initialized Jackson serialization service with standard and compact mappers");
    }

    private ObjectMapper configureMapper(ObjectMapper mapper) {
        return mapper
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Override
    public String getFrameworkName() {
        return FRAMEWORK_NAME;
    }

    @Override
    public SerializationResult serialize(List<User> users) throws SerializationException {
        try {
            logger.debug("Serializing {} users to JSON", users.size());

            long startTime = System.nanoTime();
            byte[] data = standardMapper.writeValueAsBytes(users);
            long serializationTime = System.nanoTime() - startTime;

            logger.debug("Successfully serialized {} users to {} bytes in {:.2f} ms",
                    users.size(), data.length, serializationTime / 1_000_000.0);

            return SerializationResult.builder(FRAMEWORK_NAME)
                    .format("JSON")
                    .data(data)
                    .serializationTime(serializationTime)
                    .inputObjectCount(users.size())
                    .build();

        } catch (Exception e) {
            logger.error("Failed to serialize {} users", users.size(), e);
            throw SerializationException.serialization(FRAMEWORK_NAME, "JSON serialization failed", e);
        }
    }

    @Override
    public List<User> deserialize(byte[] data) throws SerializationException {
        try {
            logger.debug("Deserializing {} bytes from JSON", data.length);

            long startTime = System.nanoTime();
            List<User> users = standardMapper.readValue(data,
                    standardMapper.getTypeFactory().constructCollectionType(List.class, User.class));
            long deserializationTime = System.nanoTime() - startTime;

            logger.debug("Successfully deserialized {} users from {} bytes in {:.2f} ms",
                    users.size(), data.length, deserializationTime / 1_000_000.0);

            return users;

        } catch (Exception e) {
            logger.error("Failed to deserialize {} bytes", data.length, e);
            throw SerializationException.deserialization(FRAMEWORK_NAME, "JSON deserialization failed", e);
        }
    }

    @Override
    public CompressionResult compress(byte[] data) throws SerializationException {
        // Use GZIP as the default compression for Jackson
        return compressWithGzip(data);
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
            throw SerializationException.decompression(FRAMEWORK_NAME, "GZIP decompression failed", compressedData.length, e);
        }
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
    public List<String> getSupportedCompressionAlgorithms() {
        return Arrays.asList("GZIP", "Zstandard", "Brotli");
    }

    @Override
    public boolean supportsSchemaEvolution() {
        return true; // JSON is schema-flexible
    }

    @Override
    public String getTypicalUseCase() {
        return "Web APIs, REST services, configuration files, human-readable data exchange";
    }

    // Additional compression methods
    public CompressionResult compressWithGzip(byte[] data) throws SerializationException {
        try {
            long startTime = System.nanoTime();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (GZIPOutputStream gzipOut = new GZIPOutputStream(baos)) {
                gzipOut.write(data);
            }

            byte[] compressed = baos.toByteArray();
            long compressionTime = System.nanoTime() - startTime;

            return CompressionResult.builder("GZIP")
                    .compressedData(compressed)
                    .compressionTime(compressionTime)
                    .originalSize(data.length)
                    .build();

        } catch (IOException e) {
            throw SerializationException.compression(FRAMEWORK_NAME, "GZIP compression failed", data.length, e);
        }
    }

    public CompressionResult compressWithZstd(byte[] data) throws SerializationException {
        try {
            long startTime = System.nanoTime();
            byte[] compressed = Zstd.compress(data);
            long compressionTime = System.nanoTime() - startTime;

            return CompressionResult.builder("Zstandard")
                    .compressedData(compressed)
                    .compressionTime(compressionTime)
                    .originalSize(data.length)
                    .build();

        } catch (Exception e) {
            throw SerializationException.compression(FRAMEWORK_NAME, "Zstd compression failed", data.length, e);
        }
    }

    public CompressionResult compressWithBrotli(byte[] data) throws SerializationException {
        try {
            long startTime = System.nanoTime();

            Encoder.Parameters params = new Encoder.Parameters().setQuality(4);
            byte[] compressed = Encoder.compress(data, params);

            long compressionTime = System.nanoTime() - startTime;

            return CompressionResult.builder("Brotli")
                    .compressedData(compressed)
                    .compressionTime(compressionTime)
                    .originalSize(data.length)
                    .build();

        } catch (Exception e) {
            throw SerializationException.compression(FRAMEWORK_NAME, "Brotli compression failed", data.length, e);
        }
    }

    // Compact JSON serialization
    public SerializationResult serializeCompact(List<User> users) throws SerializationException {
        try {
            logger.debug("Serializing {} users to compact JSON", users.size());

            long startTime = System.nanoTime();
            byte[] data = compactMapper.writeValueAsBytes(users);
            long serializationTime = System.nanoTime() - startTime;

            return SerializationResult.builder(FRAMEWORK_NAME)
                    .format("Compact JSON")
                    .data(data)
                    .serializationTime(serializationTime)
                    .inputObjectCount(users.size())
                    .build();

        } catch (Exception e) {
            throw SerializationException.serialization(FRAMEWORK_NAME, "Compact JSON serialization failed", e);
        }
    }
}