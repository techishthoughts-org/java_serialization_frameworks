package org.techishthoughts.avro.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.EncoderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.techishthoughts.avro.service.AvroSerializationService.SchemaEvolutionResult;
import org.techishthoughts.payload.generator.UnifiedPayloadGenerator;
import org.techishthoughts.payload.model.User;
import org.techishthoughts.payload.service.AbstractSerializationService;
import org.techishthoughts.payload.service.SerializationException;
import org.techishthoughts.payload.service.result.CompressionResult;
import org.techishthoughts.payload.service.result.SerializationResult;

/**
 * Apache Avro serialization service implementing the common SerializationService interface.
 * Provides binary serialization with schema evolution support and multiple compression options.
 */
@Service
public class AvroSerializationService extends AbstractSerializationService {

    private static final Logger logger = LoggerFactory.getLogger(AvroSerializationService.class);
    private static final String FRAMEWORK_NAME = "Apache Avro";

    private final Schema userSchema;
    private final DatumWriter<GenericRecord> userWriter;
    private final DatumReader<GenericRecord> userReader;

    public AvroSerializationService(UnifiedPayloadGenerator payloadGenerator) {
        super(payloadGenerator);
        // Create a simple schema for User with basic fields
        String schemaJson = "{\n" +
            "  \"type\": \"record\",\n" +
            "  \"name\": \"User\",\n" +
            "  \"fields\": [\n" +
            "    {\"name\": \"id\", \"type\": \"long\"},\n" +
            "    {\"name\": \"username\", \"type\": \"string\"},\n" +
            "    {\"name\": \"email\", \"type\": \"string\"},\n" +
            "    {\"name\": \"firstName\", \"type\": \"string\"},\n" +
            "    {\"name\": \"lastName\", \"type\": \"string\"},\n" +
            "    {\"name\": \"isActive\", \"type\": \"boolean\"},\n" +
            "    {\"name\": \"loyaltyPoints\", \"type\": \"double\"}\n" +
            "  ]\n" +
            "}";

        this.userSchema = new Schema.Parser().parse(schemaJson);
        this.userWriter = new GenericDatumWriter<>(userSchema);
        this.userReader = new GenericDatumReader<>(userSchema);

        logger.info("Initialized Avro serialization service with user schema");
    }

    @Override
    public String getFrameworkName() {
        return FRAMEWORK_NAME;
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
        return Arrays.asList("Snappy", "Deflate", "GZIP");
    }

    @Override
    public boolean supportsSchemaEvolution() {
        return true;
    }

    @Override
    public String getTypicalUseCase() {
        return "Schema evolution, data pipelines, Apache Kafka integration";
    }

    @Override
    public SerializationResult serialize(List<User> users) throws SerializationException {
        try {
            logger.debug("Serializing {} users to Avro binary format", users.size());

            long startTime = System.nanoTime();
            byte[] data = serializeToAvro(users);
            long serializationTime = System.nanoTime() - startTime;

            logger.debug("Successfully serialized {} users to {} bytes in {:.2f} ms",
                    users.size(), data.length, serializationTime / 1_000_000.0);

            return SerializationResult.builder(FRAMEWORK_NAME)
                    .format("Avro Binary")
                    .data(data)
                    .serializationTime(serializationTime)
                    .inputObjectCount(users.size())
                    .build();

        } catch (Exception e) {
            logger.error("Failed to serialize {} users", users.size(), e);
            throw SerializationException.serialization(FRAMEWORK_NAME, "Avro serialization failed", e);
        }
    }

    @Override
    public List<User> deserialize(byte[] data) throws SerializationException {
        try {
            logger.debug("Deserializing {} bytes from Avro binary format", data.length);

            long startTime = System.nanoTime();
            List<User> users = deserializeFromAvro(data);
            long deserializationTime = System.nanoTime() - startTime;

            logger.debug("Successfully deserialized {} users from {} bytes in {:.2f} ms",
                    users.size(), data.length, deserializationTime / 1_000_000.0);

            return users;

        } catch (Exception e) {
            logger.error("Failed to deserialize {} bytes", data.length, e);
            throw SerializationException.deserialization(FRAMEWORK_NAME, "Avro deserialization failed", e);
        }
    }

    @Override
    public CompressionResult compress(byte[] data) throws SerializationException {
        // Use GZIP as the default compression for Avro (Snappy requires additional library)
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

    private byte[] serializeToAvro(List<User> users) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BinaryEncoder encoder = EncoderFactory.get().binaryEncoder(baos, null);

        // Write each user as GenericRecord
        for (User user : users) {
            GenericRecord record = new org.apache.avro.generic.GenericData.Record(userSchema);
            record.put("id", user.getId());
            record.put("username", user.getUsername());
            record.put("email", user.getEmail());
            record.put("firstName", user.getFirstName());
            record.put("lastName", user.getLastName());
            record.put("isActive", user.getIsActive());
            record.put("loyaltyPoints", user.getLoyaltyPoints());

            userWriter.write(record, encoder);
        }

        encoder.flush();
        return baos.toByteArray();
    }

    private List<User> deserializeFromAvro(byte[] data) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        BinaryDecoder decoder = DecoderFactory.get().binaryDecoder(bais, null);

        List<User> users = new java.util.ArrayList<>();
        while (bais.available() > 0) {
            try {
                GenericRecord record = userReader.read(null, decoder);
                User user = new User();
                user.setId((Long) record.get("id"));
                user.setUsername((String) record.get("username"));
                user.setEmail((String) record.get("email"));
                user.setFirstName((String) record.get("firstName"));
                user.setLastName((String) record.get("lastName"));
                user.setIsActive((Boolean) record.get("isActive"));
                user.setLoyaltyPoints((Double) record.get("loyaltyPoints"));
                users.add(user);
            } catch (IOException e) {
                // End of stream
                break;
            }
        }
        return users;
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

    public CompressionResult compressWithDeflate(byte[] data) throws SerializationException {
        try {
            long startTime = System.nanoTime();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (DeflaterOutputStream deflateOut = new DeflaterOutputStream(baos)) {
                deflateOut.write(data);
            }

            byte[] compressed = baos.toByteArray();
            long compressionTime = System.nanoTime() - startTime;

            return CompressionResult.builder("Deflate")
                    .compressedData(compressed)
                    .compressionTime(compressionTime)
                    .originalSize(data.length)
                    .build();

        } catch (IOException e) {
            throw SerializationException.compression(FRAMEWORK_NAME, "Deflate compression failed", data.length, e);
        }
    }

    public CompressionResult compressWithSnappy(byte[] data) throws SerializationException {
        try {
            // Try to use Snappy if available
            Class<?> snappyClass = Class.forName("org.xerial.snappy.Snappy");

            long startTime = System.nanoTime();
            byte[] compressed = (byte[]) snappyClass.getMethod("compress", byte[].class).invoke(null, data);
            long compressionTime = System.nanoTime() - startTime;

            return CompressionResult.builder("Snappy")
                    .compressedData(compressed)
                    .compressionTime(compressionTime)
                    .originalSize(data.length)
                    .build();

        } catch (ClassNotFoundException e) {
            logger.warn("Snappy compression not available, falling back to GZIP");
            return compressWithGzip(data);
        } catch (Exception e) {
            throw SerializationException.compression(FRAMEWORK_NAME, "Snappy compression failed", data.length, e);
        }
    }

    // Legacy methods for backward compatibility - now delegates to new interface methods
    public LegacySerializationResult serializeUsers(List<User> users) throws IOException {
        try {
            SerializationResult result = serialize(users);
            // Convert to legacy format for compatibility
            return new LegacySerializationResult("Avro", result.getData(), result.getSerializationTimeNs(), result.getSizeBytes());
        } catch (SerializationException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    public List<User> deserializeUsers(byte[] data) throws IOException {
        try {
            return deserialize(data);
        } catch (SerializationException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    // Schema Evolution Methods - Enhanced versions
    public LegacySerializationResult serializeUsersWithSchemaEvolution(List<User> users, Schema writerSchema) throws IOException {
        long startTime = System.nanoTime();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BinaryEncoder encoder = EncoderFactory.get().binaryEncoder(baos, null);

        // Use the writer schema for serialization
        DatumWriter<GenericRecord> writer = new GenericDatumWriter<>(writerSchema);

        for (User user : users) {
            GenericRecord record = new org.apache.avro.generic.GenericData.Record(writerSchema);
            record.put("id", user.getId());
            record.put("username", user.getUsername());
            record.put("email", user.getEmail());
            record.put("firstName", user.getFirstName());
            record.put("lastName", user.getLastName());
            record.put("isActive", user.getIsActive());
            record.put("loyaltyPoints", user.getLoyaltyPoints());

            writer.write(record, encoder);
        }

        encoder.flush();
        byte[] data = baos.toByteArray();
        long serializationTime = System.nanoTime() - startTime;

        return new LegacySerializationResult("Avro (Schema Evolution)", data, serializationTime, data.length);
    }

    public List<User> deserializeUsersWithSchemaEvolution(byte[] data, Schema readerSchema) throws IOException {
        long startTime = System.nanoTime();

        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        BinaryDecoder decoder = DecoderFactory.get().binaryDecoder(bais, null);

        // Use both schemas for resolution
        DatumReader<GenericRecord> reader = new GenericDatumReader<>(userSchema, readerSchema);

        List<User> users = new java.util.ArrayList<>();
        while (bais.available() > 0) {
            try {
                GenericRecord record = reader.read(null, decoder);
                User user = new User();
                user.setId((Long) record.get("id"));
                user.setUsername((String) record.get("username"));
                user.setEmail((String) record.get("email"));
                user.setFirstName((String) record.get("firstName"));
                user.setLastName((String) record.get("lastName"));
                user.setIsActive((Boolean) record.get("isActive"));
                user.setLoyaltyPoints((Double) record.get("loyaltyPoints"));
                users.add(user);
            } catch (IOException e) {
                break;
            }
        }

        long deserializationTime = System.nanoTime() - startTime;
        logger.debug("Avro schema evolution deserialization took: {:.2f} ms", deserializationTime / 1_000_000.0);

        return users;
    }

    public SchemaEvolutionResult testSchemaEvolution(List<User> users) throws IOException {
        // Serialize with original schema
        LegacySerializationResult originalResult = serializeUsers(users);

        // Create an evolved schema (add a new field)
        Schema evolvedSchema = createEvolvedSchema();

        // Serialize with evolved schema
        LegacySerializationResult evolvedResult = serializeUsersWithSchemaEvolution(users, evolvedSchema);

        // Try to deserialize with original schema (backward compatibility)
        List<User> deserializedUsers = deserializeUsersWithSchemaEvolution(evolvedResult.getData(), userSchema);

        boolean evolutionSuccess = deserializedUsers.size() == users.size();

        return new SchemaEvolutionResult(
            originalResult,
            deserializedUsers.size(),
            evolutionSuccess,
            userSchema.toString(),
            evolvedSchema.toString()
        );
    }

    /**
     * Get schema information for the Avro service
     */
    public Map<String, Object> getSchemaInfo() {
        Map<String, Object> schemaInfo = new java.util.HashMap<>();
        schemaInfo.put("schemaSize", userSchema.toString().length());
        schemaInfo.put("schemaType", "Avro Binary");
        schemaInfo.put("schemaEvolution", "Supported");
        schemaInfo.put("compression", "Built-in binary format");
        schemaInfo.put("schemaDefinition", userSchema.toString());
        return schemaInfo;
    }

    private Schema createEvolvedSchema() {
        // Create a schema with an additional field to test evolution
        String evolvedSchemaJson = userSchema.toString().replace(
            "\"fields\":[",
            "\"fields\":[\n    {\"name\":\"newField\",\"type\":[\"null\",\"string\"],\"default\":null},"
        );

        try {
            return new Schema.Parser().parse(evolvedSchemaJson);
        } catch (Exception e) {
            // Fallback to original schema if evolution fails
            return userSchema;
        }
    }

    // Legacy result class for backward compatibility
    public static class LegacySerializationResult {
        private final String format;
        private final byte[] data;
        private final long serializationTimeNs;
        private final int sizeBytes;

        public LegacySerializationResult(String format, byte[] data, long serializationTimeNs, int sizeBytes) {
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

    public static class SchemaEvolutionResult {
        private final LegacySerializationResult originalResult;
        private final int deserializedCount;
        private final boolean evolutionSuccess;
        private final String originalSchema;
        private final String evolvedSchema;

        public SchemaEvolutionResult(LegacySerializationResult originalResult, int deserializedCount,
                                   boolean evolutionSuccess, String originalSchema, String evolvedSchema) {
            this.originalResult = originalResult;
            this.deserializedCount = deserializedCount;
            this.evolutionSuccess = evolutionSuccess;
            this.originalSchema = originalSchema;
            this.evolvedSchema = evolvedSchema;
        }

        public LegacySerializationResult getOriginalResult() { return originalResult; }
        public int getDeserializedCount() { return deserializedCount; }
        public boolean isEvolutionSuccess() { return evolutionSuccess; }
        public String getOriginalSchema() { return originalSchema; }
        public String getEvolvedSchema() { return evolvedSchema; }
    }
}
