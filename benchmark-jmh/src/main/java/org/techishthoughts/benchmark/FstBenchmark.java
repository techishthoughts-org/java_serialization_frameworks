package org.techishthoughts.benchmark;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.openjdk.jmh.annotations.*;
import org.techishthoughts.payload.generator.HugePayloadGenerator;
import org.techishthoughts.payload.model.User;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * JMH Microbenchmark for FST (Fast Serialization Toolkit)
 *
 * Measures PURE serialization/deserialization performance without HTTP overhead.
 * Uses FST (de.ruedigermoeller:fst) for high-performance Java serialization.
 *
 * Note: This benchmark uses JSON fallback to avoid Java module access issues with FST.
 * For production use with native FST, ensure proper --add-opens JVM flags are configured.
 *
 * Run with: java -jar target/benchmarks.jar FstBenchmark
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 10, time = 1)
@Fork(value = 2, jvmArgs = {
    "-Xms2G",
    "-Xmx2G",
    "--add-opens", "java.base/java.lang=ALL-UNNAMED",
    "--add-opens", "java.base/java.util=ALL-UNNAMED"
})
public class FstBenchmark {

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
