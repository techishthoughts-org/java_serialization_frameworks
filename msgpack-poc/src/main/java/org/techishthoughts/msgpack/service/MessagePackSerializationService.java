package org.techishthoughts.msgpack.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import net.jpountz.lz4.LZ4Compressor;
import net.jpountz.lz4.LZ4Factory;
import net.jpountz.lz4.LZ4FastDecompressor;
import org.msgpack.jackson.dataformat.MessagePackFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.techishthoughts.payload.generator.UnifiedPayloadGenerator;
import org.techishthoughts.payload.model.User;
import org.techishthoughts.payload.service.AbstractSerializationService;
import org.techishthoughts.payload.service.SerializationException;
import org.techishthoughts.payload.service.result.CompressionResult;
import org.techishthoughts.payload.service.result.SerializationResult;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * MessagePack Serialization Service
 *
 * MessagePack provides fast binary serialization with excellent performance and
 * cross-language compatibility. It's particularly effective for mobile and IoT
 * applications where bandwidth and processing power are limited.
 */
@Service
public class MessagePackSerializationService extends AbstractSerializationService {

    private static final Logger logger = LoggerFactory.getLogger(MessagePackSerializationService.class);
    private static final String FRAMEWORK_NAME = "MessagePack";

    private final ObjectMapper messagePackMapper;
    private final LZ4Compressor lz4Compressor;
    private final LZ4FastDecompressor lz4Decompressor;

    public MessagePackSerializationService(UnifiedPayloadGenerator payloadGenerator) {
        super(payloadGenerator);

        // Initialize LZ4 compression
        LZ4Factory factory = LZ4Factory.fastestInstance();
        this.lz4Compressor = factory.fastCompressor();
        this.lz4Decompressor = factory.fastDecompressor();

        // Create custom module for BigDecimal handling
        SimpleModule bigDecimalModule = new SimpleModule();
        bigDecimalModule.addSerializer(BigDecimal.class, new BigDecimalSerializer());
        bigDecimalModule.addDeserializer(BigDecimal.class, new BigDecimalDeserializer());

        this.messagePackMapper = new ObjectMapper(new MessagePackFactory())
            .registerModule(new JavaTimeModule())
            .registerModule(bigDecimalModule)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        logger.info("Initialized MessagePack serialization service with LZ4 and GZIP compression");
    }

    /**
     * Custom serializer for BigDecimal to handle MessagePack limitations
     */
    public static class BigDecimalSerializer extends StdSerializer<BigDecimal> {
        public BigDecimalSerializer() {
            super(BigDecimal.class);
        }

        @Override
        public void serialize(BigDecimal value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeString(value.toString());
        }
    }

    /**
     * Custom deserializer for BigDecimal from MessagePack
     */
    public static class BigDecimalDeserializer extends StdDeserializer<BigDecimal> {
        public BigDecimalDeserializer() {
            super(BigDecimal.class);
        }

        @Override
        public BigDecimal deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            String value = p.getValueAsString();
            return new BigDecimal(value);
        }
    }

    @Override
    public String getFrameworkName() {
        return FRAMEWORK_NAME;
    }

    @Override
    public SerializationResult serialize(List<User> users) throws SerializationException {
        try {
            logger.debug("Serializing {} users to MessagePack format", users.size());

            long startTime = System.nanoTime();
            byte[] data = messagePackMapper.writeValueAsBytes(users);
            long serializationTime = System.nanoTime() - startTime;

            logger.debug("Successfully serialized {} users to {} bytes in {:.2f} ms",
                    users.size(), data.length, serializationTime / 1_000_000.0);

            return SerializationResult.builder(FRAMEWORK_NAME)
                    .format("MessagePack Binary")
                    .data(data)
                    .serializationTime(serializationTime)
                    .inputObjectCount(users.size())
                    .build();

        } catch (Exception e) {
            logger.error("Failed to serialize {} users", users.size(), e);
            throw SerializationException.serialization(FRAMEWORK_NAME, "MessagePack serialization failed", e);
        }
    }

    @Override
    public List<User> deserialize(byte[] data) throws SerializationException {
        try {
            logger.debug("Deserializing {} bytes from MessagePack format", data.length);

            long startTime = System.nanoTime();
            List<User> users = messagePackMapper.readValue(data,
                    messagePackMapper.getTypeFactory().constructCollectionType(List.class, User.class));
            long deserializationTime = System.nanoTime() - startTime;

            logger.debug("Successfully deserialized {} users from {} bytes in {:.2f} ms",
                    users.size(), data.length, deserializationTime / 1_000_000.0);

            return users;

        } catch (Exception e) {
            logger.error("Failed to deserialize {} bytes", data.length, e);
            throw SerializationException.deserialization(FRAMEWORK_NAME, "MessagePack deserialization failed", e);
        }
    }

    @Override
    public CompressionResult compress(byte[] data) throws SerializationException {
        // Use GZIP as the default compression (matches decompress method)
        return compressWithGzip(data);
    }

    @Override
    public byte[] decompress(byte[] compressedData) throws SerializationException {
        try {
            // For simplicity, default to GZIP decompression
            // In a real implementation, you'd store compression metadata
            return decompressGzip(compressedData);
        } catch (Exception e) {
            throw SerializationException.decompression(FRAMEWORK_NAME, "Default decompression failed", compressedData.length, e);
        }
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
    public List<String> getSupportedCompressionAlgorithms() {
        return Arrays.asList("LZ4", "GZIP");
    }

    @Override
    public boolean supportsSchemaEvolution() {
        return false; // MessagePack requires consistent schema
    }

    @Override
    public String getTypicalUseCase() {
        return "Cross-language serialization, network protocols, efficient data storage";
    }

    // Additional compression methods
    public CompressionResult compressWithLZ4(byte[] data) throws SerializationException {
        try {
            long startTime = System.nanoTime();
            int maxCompressedLength = lz4Compressor.maxCompressedLength(data.length);
            byte[] compressed = new byte[maxCompressedLength];
            int compressedLength = lz4Compressor.compress(data, 0, data.length, compressed, 0, maxCompressedLength);

            // Trim the array to actual compressed size
            byte[] result = new byte[compressedLength];
            System.arraycopy(compressed, 0, result, 0, compressedLength);

            long compressionTime = System.nanoTime() - startTime;

            return CompressionResult.builder("LZ4")
                    .compressedData(result)
                    .compressionTime(compressionTime)
                    .originalSize(data.length)
                    .build();

        } catch (Exception e) {
            throw SerializationException.compression(FRAMEWORK_NAME, "LZ4 compression failed", data.length, e);
        }
    }

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

    /**
     * Legacy methods for backward compatibility
     */
    @Deprecated
    public byte[] serialize(User user) throws IOException {
        return messagePackMapper.writeValueAsBytes(user);
    }

    @Deprecated
    public User deserializeUser(byte[] data) throws IOException {
        return messagePackMapper.readValue(data, User.class);
    }

    @Deprecated
    public byte[] serializeList(List<User> users) throws IOException {
        return messagePackMapper.writeValueAsBytes(users);
    }

    @Deprecated
    public List<User> deserializeList(byte[] data) throws IOException {
        return messagePackMapper.readValue(data,
                messagePackMapper.getTypeFactory().constructCollectionType(List.class, User.class));
    }
}
