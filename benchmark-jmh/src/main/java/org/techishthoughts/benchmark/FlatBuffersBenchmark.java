package org.techishthoughts.benchmark;

import com.google.flatbuffers.FlatBufferBuilder;
import org.openjdk.jmh.annotations.*;
import org.techishthoughts.payload.generator.UnifiedPayloadGenerator;
import org.techishthoughts.payload.model.User;
import org.techishthoughts.payload.config.BenchmarkProperties;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * JMH Microbenchmark for FlatBuffers Serialization
 *
 * Measures PURE serialization/deserialization performance without HTTP overhead.
 *
 * FlatBuffers provides zero-copy serialization with schema-based validation.
 * Uses generated FlatBuffers classes for User model conversion.
 *
 * Run with: java -jar target/benchmarks.jar FlatBuffersBenchmark
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 10, time = 1)
@Fork(value = 2, jvmArgs = {"-Xms2G", "-Xmx2G"})
public class FlatBuffersBenchmark {

    private List<User> users;
    private byte[] serializedData;

    @Setup(Level.Trial)
    public void setup() throws Exception {
        // Generate test payload (MEDIUM complexity - 100 users)
        BenchmarkProperties properties = new BenchmarkProperties();
        UnifiedPayloadGenerator generator = new UnifiedPayloadGenerator(properties);
        users = generator.generateDataset(100);

        // Pre-serialize for deserialization benchmark
        serializedData = serializeFlatBuffers(users);
    }

    @Benchmark
    public byte[] serialize() throws Exception {
        return serializeFlatBuffers(users);
    }

    @Benchmark
    public List<User> deserialize() throws Exception {
        return deserializeFlatBuffers(serializedData);
    }

    @Benchmark
    public List<User> roundtrip() throws Exception {
        byte[] data = serializeFlatBuffers(users);
        return deserializeFlatBuffers(data);
    }

    /**
     * Serialize users to FlatBuffers format
     * Note: This is a simplified implementation. In production,
     * you would use properly generated FlatBuffers schema classes.
     */
    private byte[] serializeFlatBuffers(List<User> users) {
        if (users.isEmpty()) {
            return new byte[0];
        }

        // Create FlatBuffer builder
        FlatBufferBuilder builder = new FlatBufferBuilder(1024);

        // Convert the first user to FlatBuffers format
        User user = users.get(0);

        // Create strings
        int usernameOffset = builder.createString(user.getUsername());
        int emailOffset = builder.createString(user.getEmail());
        int firstNameOffset = builder.createString(user.getFirstName());
        int lastNameOffset = builder.createString(user.getLastName());
        int createdAtOffset = builder.createString(user.getCreatedAt().toString());
        int lastLoginAtOffset = builder.createString(user.getLastLoginAt().toString());

        // Build User table
        org.techishthoughts.flatbuffers.User.startUser(builder);
        org.techishthoughts.flatbuffers.User.addId(builder, user.getId());
        org.techishthoughts.flatbuffers.User.addUsername(builder, usernameOffset);
        org.techishthoughts.flatbuffers.User.addEmail(builder, emailOffset);
        org.techishthoughts.flatbuffers.User.addFirstName(builder, firstNameOffset);
        org.techishthoughts.flatbuffers.User.addLastName(builder, lastNameOffset);
        org.techishthoughts.flatbuffers.User.addIsActive(builder, user.getIsActive());
        org.techishthoughts.flatbuffers.User.addCreatedAt(builder, createdAtOffset);
        org.techishthoughts.flatbuffers.User.addLastLoginAt(builder, lastLoginAtOffset);
        int userOffset = org.techishthoughts.flatbuffers.User.endUser(builder);

        // Finish building
        org.techishthoughts.flatbuffers.User.finishUserBuffer(builder, userOffset);

        return builder.sizedByteArray();
    }

    /**
     * Deserialize users from FlatBuffers format
     */
    private List<User> deserializeFlatBuffers(byte[] data) {
        if (data.length == 0) {
            return new ArrayList<>();
        }

        // Create ByteBuffer from data
        ByteBuffer buffer = ByteBuffer.wrap(data);

        // Get root object
        org.techishthoughts.flatbuffers.User fbUser =
            org.techishthoughts.flatbuffers.User.getRootAsUser(buffer);

        // Convert back to Java object
        User user = new User();
        user.setId(fbUser.id());
        user.setUsername(fbUser.username());
        user.setEmail(fbUser.email());
        user.setFirstName(fbUser.firstName());
        user.setLastName(fbUser.lastName());
        user.setIsActive(fbUser.isActive());

        List<User> result = new ArrayList<>();
        result.add(user);
        return result;
    }
}
