package org.techishthoughts.benchmark;

import org.bson.BsonBinaryReader;
import org.bson.BsonBinaryWriter;
import org.bson.Document;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.DocumentCodec;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.bson.io.BasicOutputBuffer;
import org.bson.io.ByteBufferBsonInput;
import org.openjdk.jmh.annotations.*;
import org.techishthoughts.payload.generator.UnifiedPayloadGenerator;
import org.techishthoughts.payload.model.User;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;
import static com.mongodb.MongoClientSettings.getDefaultCodecRegistry;

/**
 * JMH Microbenchmark for BSON (Binary JSON) Serialization
 *
 * Measures PURE serialization/deserialization performance without HTTP overhead.
 * BSON is a binary-encoded serialization format used by MongoDB.
 *
 * Run with: java -jar target/benchmarks.jar BsonBenchmark
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 10, time = 1)
@Fork(value = 2, jvmArgs = {"-Xms2G", "-Xmx2G"})
public class BsonBenchmark {

    private CodecRegistry codecRegistry;
    private List<User> users;
    private byte[] serializedData;

    @Setup(Level.Trial)
    public void setup() throws Exception {
        // Initialize BSON codec registry with POJO support
        codecRegistry = fromRegistries(
            getDefaultCodecRegistry(),
            fromProviders(PojoCodecProvider.builder().automatic(true).build())
        );

        // Generate test payload (MEDIUM complexity - 100 users)
        UnifiedPayloadGenerator generator = new UnifiedPayloadGenerator();
        users = generator.generateUsers(
            UnifiedPayloadGenerator.ComplexityLevel.MEDIUM,
            100
        );

        // Pre-serialize for deserialization benchmark
        serializedData = serializeUsers(users);
    }

    @Benchmark
    public byte[] serialize() throws Exception {
        return serializeUsers(users);
    }

    @Benchmark
    public List<User> deserialize() throws Exception {
        return deserializeUsers(serializedData);
    }

    @Benchmark
    public List<User> roundtrip() throws Exception {
        byte[] data = serializeUsers(users);
        return deserializeUsers(data);
    }

    private byte[] serializeUsers(List<User> userList) {
        BasicOutputBuffer outputBuffer = new BasicOutputBuffer();
        BsonBinaryWriter writer = new BsonBinaryWriter(outputBuffer);

        try {
            writer.writeStartDocument();
            writer.writeStartArray("users");

            for (User user : userList) {
                Document doc = convertUserToDocument(user);
                DocumentCodec codec = new DocumentCodec(codecRegistry);
                codec.encode(writer, doc, EncoderContext.builder().build());
            }

            writer.writeEndArray();
            writer.writeEndDocument();

            return outputBuffer.toByteArray();
        } finally {
            writer.close();
        }
    }

    private List<User> deserializeUsers(byte[] data) {
        List<User> result = new ArrayList<>();
        ByteBufferBsonInput input = new ByteBufferBsonInput(new ByteBuffer[] {ByteBuffer.wrap(data)});
        BsonBinaryReader reader = new BsonBinaryReader(input);

        try {
            reader.readStartDocument();
            reader.readName("users");
            reader.readStartArray();

            DocumentCodec codec = new DocumentCodec(codecRegistry);

            while (reader.readBsonType() != org.bson.BsonType.END_OF_DOCUMENT) {
                Document doc = codec.decode(reader, DecoderContext.builder().build());
                User user = convertDocumentToUser(doc);
                result.add(user);
            }

            reader.readEndArray();
            reader.readEndDocument();

            return result;
        } finally {
            reader.close();
        }
    }

    private Document convertUserToDocument(User user) {
        Document doc = new Document();
        doc.put("id", user.getId());
        doc.put("username", user.getUsername());
        doc.put("email", user.getEmail());
        doc.put("firstName", user.getFirstName());
        doc.put("lastName", user.getLastName());
        doc.put("isActive", user.getIsActive());
        doc.put("loyaltyPoints", user.getLoyaltyPoints());
        doc.put("createdAt", user.getCreatedAt() != null ? user.getCreatedAt().toString() : null);
        doc.put("lastLoginAt", user.getLastLoginAt() != null ? user.getLastLoginAt().toString() : null);
        doc.put("tags", user.getTags());
        doc.put("metadata", user.getMetadata());
        doc.put("preferences", user.getPreferences());
        return doc;
    }

    private User convertDocumentToUser(Document doc) {
        User user = new User();
        user.setId(doc.getLong("id"));
        user.setUsername(doc.getString("username"));
        user.setEmail(doc.getString("email"));
        user.setFirstName(doc.getString("firstName"));
        user.setLastName(doc.getString("lastName"));
        user.setIsActive(doc.getBoolean("isActive"));
        user.setLoyaltyPoints(doc.getDouble("loyaltyPoints"));

        @SuppressWarnings("unchecked")
        List<String> tags = (List<String>) doc.get("tags");
        user.setTags(tags);

        @SuppressWarnings("unchecked")
        java.util.Map<String, String> metadata = (java.util.Map<String, String>) doc.get("metadata");
        user.setMetadata(metadata);

        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> preferences = (java.util.Map<String, Object>) doc.get("preferences");
        user.setPreferences(preferences);

        return user;
    }
}
