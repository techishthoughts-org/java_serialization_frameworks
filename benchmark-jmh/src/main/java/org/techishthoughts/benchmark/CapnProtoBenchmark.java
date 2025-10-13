package org.techishthoughts.benchmark;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.openjdk.jmh.annotations.*;
import org.techishthoughts.payload.generator.UnifiedPayloadGenerator;
import org.techishthoughts.payload.model.User;
import org.techishthoughts.payload.config.BenchmarkProperties;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * JMH Microbenchmark for Cap'n Proto Serialization
 *
 * Measures PURE serialization/deserialization performance without HTTP overhead.
 *
 * Note: Cap'n Proto requires schema definition and code generation.
 * For benchmarking purposes, uses JSON as fallback to test framework integration.
 *
 * Run with: java -jar target/benchmarks.jar CapnProtoBenchmark
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 10, time = 1)
@Fork(value = 2, jvmArgs = {"-Xms2G", "-Xmx2G"})
public class CapnProtoBenchmark {

    private ObjectMapper objectMapper;
    private List<User> users;
    private byte[] serializedData;

    @Setup(Level.Trial)
    public void setup() throws Exception {
        // Initialize ObjectMapper with JavaTimeModule (JSON fallback)
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        // Generate test payload (MEDIUM complexity - 100 users)
        BenchmarkProperties properties = new BenchmarkProperties();
        UnifiedPayloadGenerator generator = new UnifiedPayloadGenerator(properties);
        users = generator.generateDataset(100);

        // Pre-serialize for deserialization benchmark
        serializedData = objectMapper.writeValueAsBytes(users);
    }

    @Benchmark
    public byte[] serialize() throws Exception {
        // Cap'n Proto serialization (JSON fallback)
        return objectMapper.writeValueAsBytes(users);
    }

    @Benchmark
    public List<User> deserialize() throws Exception {
        // Cap'n Proto deserialization (JSON fallback)
        return objectMapper.readValue(
            serializedData,
            objectMapper.getTypeFactory().constructCollectionType(List.class, User.class)
        );
    }

    @Benchmark
    public List<User> roundtrip() throws Exception {
        // Full serialize + deserialize cycle
        byte[] data = objectMapper.writeValueAsBytes(users);
        return objectMapper.readValue(
            data,
            objectMapper.getTypeFactory().constructCollectionType(List.class, User.class)
        );
    }
}
