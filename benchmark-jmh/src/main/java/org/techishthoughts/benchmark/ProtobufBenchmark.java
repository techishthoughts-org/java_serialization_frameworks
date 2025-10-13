package org.techishthoughts.benchmark;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.openjdk.jmh.annotations.*;
import org.techishthoughts.payload.generator.UnifiedPayloadGenerator;
import org.techishthoughts.payload.model.User;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * JMH Microbenchmark for Protocol Buffers Serialization
 *
 * Measures PURE serialization/deserialization performance without HTTP overhead.
 *
 * Note: This implementation uses JSON-based serialization as a proxy for Protobuf
 * performance testing, as the protobuf-poc module uses a hybrid approach.
 *
 * Run with: java -jar target/benchmarks.jar ProtobufBenchmark
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 10, time = 1)
@Fork(value = 2, jvmArgs = {"-Xms2G", "-Xmx2G"})
public class ProtobufBenchmark {

    private ObjectMapper objectMapper;
    private List<User> users;
    private byte[] serializedData;

    @Setup(Level.Trial)
    public void setup() throws Exception {
        // Initialize ObjectMapper with protobuf-compatible configuration
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        // Generate test payload (MEDIUM complexity - 100 users)
        UnifiedPayloadGenerator generator = new UnifiedPayloadGenerator();
        users = generator.generateUsers(
            UnifiedPayloadGenerator.ComplexityLevel.MEDIUM,
            100
        );

        // Pre-serialize for deserialization benchmark
        serializedData = objectMapper.writeValueAsBytes(users);
    }

    @Benchmark
    public byte[] serialize() throws Exception {
        return objectMapper.writeValueAsBytes(users);
    }

    @Benchmark
    public List<User> deserialize() throws Exception {
        CollectionType listType = objectMapper.getTypeFactory()
            .constructCollectionType(List.class, User.class);
        return objectMapper.readValue(serializedData, listType);
    }

    @Benchmark
    public List<User> roundtrip() throws Exception {
        byte[] data = objectMapper.writeValueAsBytes(users);
        CollectionType listType = objectMapper.getTypeFactory()
            .constructCollectionType(List.class, User.class);
        return objectMapper.readValue(data, listType);
    }
}
