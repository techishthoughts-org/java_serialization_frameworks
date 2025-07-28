package org.techishthoughts.jackson.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.msgpack.jackson.dataformat.MessagePackFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.techishthoughts.jackson.service.JacksonSerializationService.CompressionResult;
import org.techishthoughts.jackson.service.JacksonSerializationService.SerializationResult;
import org.techishthoughts.payload.model.User;

import com.aayushatharva.brotli4j.Brotli4jLoader;
import com.aayushatharva.brotli4j.decoder.Decoder;
import com.aayushatharva.brotli4j.encoder.Encoder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import com.fasterxml.jackson.dataformat.smile.SmileFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.luben.zstd.Zstd;

@Service
public class JacksonSerializationService {

    private final ObjectMapper standardMapper;
    private final ObjectMapper compactMapper;
    private final ObjectMapper cborMapper;
    private final ObjectMapper messagePackMapper;
    private final ObjectMapper smileMapper;

    static {
        // Initialize Brotli
        if (!Brotli4jLoader.isAvailable()) {
            throw new RuntimeException("Brotli4j not available");
        }
    }

    public JacksonSerializationService(ObjectMapper standardMapper,
                                     @Qualifier("compactObjectMapper") ObjectMapper compactMapper) {
        this.standardMapper = standardMapper;
        this.compactMapper = compactMapper;

        // Create format-specific mappers with JSR310 support
        this.cborMapper = new ObjectMapper(new CBORFactory())
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        this.messagePackMapper = new ObjectMapper(new MessagePackFactory())
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        this.smileMapper = new ObjectMapper(new SmileFactory())
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public SerializationResult serializeUsersToJson(List<User> users) throws IOException {
        return serializeUsers(users, standardMapper, "JSON");
    }

    public SerializationResult serializeUsersToCompactJson(List<User> users) throws IOException {
        return serializeUsers(users, compactMapper, "Compact JSON");
    }

    public SerializationResult serializeUsersToCbor(List<User> users) throws IOException {
        try {
            return serializeUsers(users, cborMapper, "CBOR");
        } catch (NoSuchMethodError e) {
            // CBOR dependency issue - fallback to JSON
            System.out.println("CBOR serialization failed due to dependency issue, falling back to JSON");
            return serializeUsers(users, standardMapper, "JSON (CBOR fallback)");
        }
    }

    public SerializationResult serializeUsersToMessagePack(List<User> users) throws IOException {
        try {
            return serializeUsers(users, messagePackMapper, "MessagePack");
        } catch (Exception e) {
            // MessagePack serialization issue - fallback to JSON
            System.out.println("MessagePack serialization failed: " + e.getMessage() + ", falling back to JSON");
            return serializeUsers(users, standardMapper, "JSON (MessagePack fallback)");
        }
    }

    public SerializationResult serializeUsersToSmile(List<User> users) throws IOException {
        try {
            return serializeUsers(users, smileMapper, "Smile");
        } catch (Exception e) {
            // Smile serialization issue - fallback to JSON
            System.out.println("Smile serialization failed: " + e.getMessage() + ", falling back to JSON");
            return serializeUsers(users, standardMapper, "JSON (Smile fallback)");
        }
    }

    private SerializationResult serializeUsers(List<User> users, ObjectMapper mapper, String format) throws IOException {
        long startTime = System.nanoTime();
        byte[] data = mapper.writeValueAsBytes(users);
        long serializationTime = System.nanoTime() - startTime;

        return new SerializationResult(format, data, serializationTime, data.length);
    }

    public CompressionResult compressWithGzip(byte[] data) throws IOException {
        long startTime = System.nanoTime();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (GZIPOutputStream gzipOut = new GZIPOutputStream(baos)) {
            gzipOut.write(data);
        }

        byte[] compressed = baos.toByteArray();
        long compressionTime = System.nanoTime() - startTime;

        return new CompressionResult("GZIP", compressed, compressionTime, data.length, compressed.length);
    }

    public CompressionResult compressWithZstd(byte[] data) {
        long startTime = System.nanoTime();
        byte[] compressed = Zstd.compress(data);
        long compressionTime = System.nanoTime() - startTime;

        return new CompressionResult("Zstandard", compressed, compressionTime, data.length, compressed.length);
    }

    public CompressionResult compressWithBrotli(byte[] data) throws IOException {
        long startTime = System.nanoTime();

        Encoder.Parameters params = new Encoder.Parameters().setQuality(4);
        byte[] compressed = Encoder.compress(data, params);

        long compressionTime = System.nanoTime() - startTime;

        return new CompressionResult("Brotli", compressed, compressionTime, data.length, compressed.length);
    }

    public List<User> deserializeUsersFromJson(byte[] data) throws IOException {
        return deserializeUsers(data, standardMapper);
    }

    public List<User> deserializeUsersFromCompactJson(byte[] data) throws IOException {
        return deserializeUsers(data, compactMapper);
    }

    public List<User> deserializeUsersFromCbor(byte[] data) throws IOException {
        try {
            return deserializeUsers(data, cborMapper);
        } catch (NoSuchMethodError e) {
            // CBOR dependency issue - fallback to JSON
            System.out.println("CBOR deserialization failed due to dependency issue, falling back to JSON");
            return deserializeUsers(data, standardMapper);
        }
    }

    public List<User> deserializeUsersFromMessagePack(byte[] data) throws IOException {
        return deserializeUsers(data, messagePackMapper);
    }

    public List<User> deserializeUsersFromSmile(byte[] data) throws IOException {
        return deserializeUsers(data, smileMapper);
    }

    private List<User> deserializeUsers(byte[] data, ObjectMapper mapper) throws IOException {
        long startTime = System.nanoTime();
        List<User> users = mapper.readValue(data, mapper.getTypeFactory().constructCollectionType(List.class, User.class));
        long deserializationTime = System.nanoTime() - startTime;

        // Log deserialization time (could be stored for analysis)
        System.out.println("Deserialization took: " + (deserializationTime / 1_000_000.0) + " ms");

        return users;
    }

    public List<User> decompressAndDeserializeGzip(byte[] compressedData) throws IOException {
        long startTime = System.nanoTime();

        ByteArrayInputStream bais = new ByteArrayInputStream(compressedData);
        GZIPInputStream gzipIn = new GZIPInputStream(bais);
        byte[] decompressed = gzipIn.readAllBytes();
        gzipIn.close();

        long decompressionTime = System.nanoTime() - startTime;
        System.out.println("GZIP decompression took: " + (decompressionTime / 1_000_000.0) + " ms");

        return deserializeUsersFromJson(decompressed);
    }

    public List<User> decompressAndDeserializeZstd(byte[] compressedData) throws IOException {
        long startTime = System.nanoTime();
        byte[] decompressed = Zstd.decompress(compressedData, (int) Zstd.decompressedSize(compressedData));
        long decompressionTime = System.nanoTime() - startTime;

        System.out.println("Zstd decompression took: " + (decompressionTime / 1_000_000.0) + " ms");

        return deserializeUsersFromJson(decompressed);
    }

    public List<User> decompressAndDeserializeBrotli(byte[] compressedData) throws IOException {
        long startTime = System.nanoTime();
        // Use the correct Brotli decompression API
        byte[] decompressed = Decoder.decompress(compressedData).getDecompressedData();
        long decompressionTime = System.nanoTime() - startTime;

        System.out.println("Brotli decompression took: " + (decompressionTime / 1_000_000.0) + " ms");

        return deserializeUsersFromJson(decompressed);
    }

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

        public CompressionResult(String algorithm, byte[] compressedData, long compressionTimeNs,
                               int originalSize, int compressedSize) {
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
        public double getCompressionRatio() { return (double) compressedSize / originalSize; }
        public double getSpaceSavings() { return 1.0 - getCompressionRatio(); }
        public double getOriginalSizeMB() { return originalSize / (1024.0 * 1024.0); }
        public double getCompressedSizeMB() { return compressedSize / (1024.0 * 1024.0); }
    }
}
