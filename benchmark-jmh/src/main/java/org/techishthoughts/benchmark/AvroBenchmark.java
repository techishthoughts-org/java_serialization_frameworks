package org.techishthoughts.benchmark;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.*;
import org.openjdk.jmh.annotations.*;
import org.techishthoughts.payload.generator.UnifiedPayloadGenerator;
import org.techishthoughts.payload.model.User;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * JMH Microbenchmark for Apache Avro Serialization
 *
 * Measures PURE serialization/deserialization performance without HTTP overhead.
 * Uses Avro binary encoding with schema-based serialization.
 *
 * Run with: java -jar target/benchmarks.jar AvroBenchmark
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 10, time = 1)
@Fork(value = 2, jvmArgs = {"-Xms2G", "-Xmx2G"})
public class AvroBenchmark {

    private Schema userSchema;
    private DatumWriter<GenericRecord> userWriter;
    private DatumReader<GenericRecord> userReader;
    private List<User> users;
    private byte[] serializedData;

    @Setup(Level.Trial)
    public void setup() throws Exception {
        // Create Avro schema for User with basic fields
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

        userSchema = new Schema.Parser().parse(schemaJson);
        userWriter = new GenericDatumWriter<>(userSchema);
        userReader = new GenericDatumReader<>(userSchema);

        // Generate test payload (MEDIUM complexity - 100 users)
        UnifiedPayloadGenerator generator = new UnifiedPayloadGenerator();
        users = generator.generateUsers(
            UnifiedPayloadGenerator.ComplexityLevel.MEDIUM,
            100
        );

        // Pre-serialize for deserialization benchmark
        serializedData = serializeToBytes(users);
    }

    private byte[] serializeToBytes(List<User> users) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BinaryEncoder encoder = EncoderFactory.get().binaryEncoder(baos, null);

        for (User user : users) {
            GenericRecord record = new GenericData.Record(userSchema);
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

    private List<User> deserializeFromBytes(byte[] data) throws Exception {
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        BinaryDecoder decoder = DecoderFactory.get().binaryDecoder(bais, null);

        List<User> result = new ArrayList<>();
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
                result.add(user);
            } catch (Exception e) {
                // End of stream
                break;
            }
        }
        return result;
    }

    @Benchmark
    public byte[] serialize() throws Exception {
        return serializeToBytes(users);
    }

    @Benchmark
    public List<User> deserialize() throws Exception {
        return deserializeFromBytes(serializedData);
    }

    @Benchmark
    public List<User> roundtrip() throws Exception {
        byte[] data = serializeToBytes(users);
        return deserializeFromBytes(data);
    }
}
