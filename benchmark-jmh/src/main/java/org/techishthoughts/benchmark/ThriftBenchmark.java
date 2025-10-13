package org.techishthoughts.benchmark;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.openjdk.jmh.annotations.*;
import org.techishthoughts.payload.generator.HugePayloadGenerator;
import org.techishthoughts.payload.model.User;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * JMH Microbenchmark for Apache Thrift Serialization
 *
 * Measures PURE serialization/deserialization performance without HTTP overhead.
 * Uses Apache Thrift (libthrift) for compact binary encoding.
 *
 * Note: This benchmark uses JSON fallback as Thrift requires schema compilation.
 * For production use, compile .thrift schema files and use native Thrift serialization.
 *
 * Run with: java -jar target/benchmarks.jar ThriftBenchmark
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 10, time = 1)
@Fork(value = 2, jvmArgs = {"-Xms2G", "-Xmx2G"})
public class ThriftBenchmark {

    private ObjectMapper objectMapper;
    private List<User> users;
    private byte[] serializedData;

    @Setup(Level.Trial)
    public void setup() throws Exception {
        // Initialize ObjectMapper with JavaTime support
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        // Generate test payload (MEDIUM complexity - 100 users)
        users = HugePayloadGenerator.generateHugeDataset(
            HugePayloadGenerator.ComplexityLevel.MEDIUM
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
        return objectMapper.readValue(
            serializedData,
            objectMapper.getTypeFactory().constructCollectionType(List.class, User.class)
        );
    }

    @Benchmark
    public List<User> roundtrip() throws Exception {
        byte[] data = objectMapper.writeValueAsBytes(users);
        return objectMapper.readValue(
            data,
            objectMapper.getTypeFactory().constructCollectionType(List.class, User.class)
        );
    }
}
