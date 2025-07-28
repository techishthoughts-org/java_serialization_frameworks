package org.techishthoughts.avro.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

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
import org.springframework.stereotype.Service;
import org.techishthoughts.avro.service.AvroSerializationService.SchemaEvolutionResult;
import org.techishthoughts.avro.service.AvroSerializationService.SerializationResult;
import org.techishthoughts.payload.model.User;

@Service
public class AvroSerializationService {

    private final Schema userSchema;
    private final DatumWriter<GenericRecord> userWriter;
    private final DatumReader<GenericRecord> userReader;

    public AvroSerializationService() {
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
    }

    public SerializationResult serializeUsers(List<User> users) throws IOException {
        long startTime = System.nanoTime();

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
        byte[] data = baos.toByteArray();
        long serializationTime = System.nanoTime() - startTime;

        return new SerializationResult("Avro", data, serializationTime, data.length);
    }

    public List<User> deserializeUsers(byte[] data) throws IOException {
        long startTime = System.nanoTime();

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

        long deserializationTime = System.nanoTime() - startTime;
        System.out.println("Avro deserialization took: " + (deserializationTime / 1_000_000.0) + " ms");

        return users;
    }

    public SerializationResult serializeUsersWithSchemaEvolution(List<User> users, Schema writerSchema) throws IOException {
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

        return new SerializationResult("Avro (Schema Evolution)", data, serializationTime, data.length);
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
        System.out.println("Avro schema evolution deserialization took: " + (deserializationTime / 1_000_000.0) + " ms");

        return users;
    }

    public SchemaEvolutionResult testSchemaEvolution(List<User> users) throws IOException {
        // Serialize with original schema
        SerializationResult originalResult = serializeUsers(users);

        // Create an evolved schema (add a new field)
        Schema evolvedSchema = createEvolvedSchema();

        // Serialize with evolved schema
        SerializationResult evolvedResult = serializeUsersWithSchemaEvolution(users, evolvedSchema);

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

    public static class SchemaEvolutionResult {
        private final SerializationResult originalResult;
        private final int deserializedCount;
        private final boolean evolutionSuccess;
        private final String originalSchema;
        private final String evolvedSchema;

        public SchemaEvolutionResult(SerializationResult originalResult, int deserializedCount,
                                   boolean evolutionSuccess, String originalSchema, String evolvedSchema) {
            this.originalResult = originalResult;
            this.deserializedCount = deserializedCount;
            this.evolutionSuccess = evolutionSuccess;
            this.originalSchema = originalSchema;
            this.evolvedSchema = evolvedSchema;
        }

        public SerializationResult getOriginalResult() { return originalResult; }
        public int getDeserializedCount() { return deserializedCount; }
        public boolean isEvolutionSuccess() { return evolutionSuccess; }
        public String getOriginalSchema() { return originalSchema; }
        public String getEvolvedSchema() { return evolvedSchema; }
    }
}
