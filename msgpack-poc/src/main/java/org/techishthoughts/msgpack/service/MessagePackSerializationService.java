package org.techishthoughts.msgpack.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.msgpack.jackson.dataformat.MessagePackFactory;
import org.springframework.stereotype.Service;
import org.techishthoughts.msgpack.service.MessagePackSerializationService.BigDecimalSerializer;
import org.techishthoughts.payload.model.User;

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
 * 2025 UPDATE: MessagePack provides fast binary serialization with excellent
 * performance and cross-language compatibility. It's particularly effective
 * for mobile and IoT applications.
 */
@Service
public class MessagePackSerializationService {

    private final ObjectMapper messagePackMapper;

    public MessagePackSerializationService() {
        // Create custom module for BigDecimal handling
        SimpleModule bigDecimalModule = new SimpleModule();
        bigDecimalModule.addSerializer(BigDecimal.class, new BigDecimalSerializer());
        bigDecimalModule.addDeserializer(BigDecimal.class, new BigDecimalDeserializer());

        this.messagePackMapper = new ObjectMapper(new MessagePackFactory())
            .registerModule(new JavaTimeModule())
            .registerModule(bigDecimalModule)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
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

    /**
     * Serialize user to MessagePack format
     */
    public byte[] serialize(User user) throws IOException {
        return messagePackMapper.writeValueAsBytes(user);
    }

    /**
     * Deserialize user from MessagePack format
     */
    public User deserialize(byte[] data) throws IOException {
        return messagePackMapper.readValue(data, User.class);
    }

    /**
     * Serialize user list to MessagePack format
     */
    public byte[] serializeList(List<User> users) throws IOException {
        return messagePackMapper.writeValueAsBytes(users);
    }

    /**
     * Deserialize user list from MessagePack format
     */
    @SuppressWarnings("unchecked")
    public List<User> deserializeList(byte[] data) throws IOException {
        return messagePackMapper.readValue(data, List.class);
    }

    /**
     * Serialize with GZIP compression
     */
    public byte[] serializeWithGzip(User user) throws IOException {
        byte[] serialized = serialize(user);
        return compressGzip(serialized);
    }

    /**
     * Deserialize with GZIP decompression
     */
    public User deserializeWithGzip(byte[] compressedData) throws IOException {
        byte[] decompressed = decompressGzip(compressedData);
        return deserialize(decompressed);
    }

    /**
     * Serialize list with GZIP compression
     */
    public byte[] serializeListWithGzip(List<User> users) throws IOException {
        byte[] serialized = serializeList(users);
        return compressGzip(serialized);
    }

    /**
     * Deserialize list with GZIP decompression
     */
    public List<User> deserializeListWithGzip(byte[] compressedData) throws IOException {
        byte[] decompressed = decompressGzip(compressedData);
        return deserializeList(decompressed);
    }

    /**
     * Compress data using GZIP
     */
    private byte[] compressGzip(byte[] data) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (GZIPOutputStream gzipOut = new GZIPOutputStream(baos)) {
            gzipOut.write(data);
        }
        return baos.toByteArray();
    }

    /**
     * Decompress data using GZIP
     */
    private byte[] decompressGzip(byte[] compressedData) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(compressedData);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (GZIPInputStream gzipIn = new GZIPInputStream(bais)) {
            byte[] buffer = new byte[1024];
            int len;
            while ((len = gzipIn.read(buffer)) > 0) {
                baos.write(buffer, 0, len);
            }
        }
        return baos.toByteArray();
    }

    /**
     * Get serialization format info
     */
    public String getFormatInfo() {
        return "MessagePack - Fast binary serialization format with excellent performance and cross-language compatibility";
    }

    /**
     * Get compression info
     */
    public String getCompressionInfo() {
        return "GZIP - Standard compression with good compression ratio and reasonable speed";
    }
}
