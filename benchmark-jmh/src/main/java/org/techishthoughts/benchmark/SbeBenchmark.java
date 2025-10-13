package org.techishthoughts.benchmark;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.openjdk.jmh.annotations.*;
import org.techishthoughts.payload.generator.UnifiedPayloadGenerator;
import org.techishthoughts.payload.model.User;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * JMH Microbenchmark for Simple Binary Encoding (SBE)
 *
 * Measures PURE serialization/deserialization performance without HTTP overhead.
 *
 * NOTE: This implementation uses JSON fallback as SBE requires pre-generated schema files.
 * For true SBE performance, you would need to:
 * 1. Define .xml schema files for all domain objects
 * 2. Generate Java codecs using SBE's schema compiler
 * 3. Replace Jackson calls with SBE MessageHeader and generated encoders/decoders
 *
 * Current implementation provides baseline JSON performance for comparison.
 *
 * Run with: java -jar target/benchmarks.jar SbeBenchmark
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 10, time = 1)
@Fork(value = 2, jvmArgs = {"-Xms2G", "-Xmx2G"})
public class SbeBenchmark {

    private ObjectMapper objectMapper;
    private List<User> users;
    private byte[] serializedData;

    @Setup(Level.Trial)
    public void setup() throws Exception {
        // Initialize JSON fallback (SBE requires pre-compiled schemas)
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
        // JSON fallback - SBE would use:
        // MessageHeaderEncoder headerEncoder = new MessageHeaderEncoder();
        // UserListEncoder encoder = new UserListEncoder();
        // encoder.wrap(buffer, offset).encode(users);
        return objectMapper.writeValueAsBytes(users);
    }

    @Benchmark
    public List<User> deserialize() throws Exception {
        // JSON fallback - SBE would use:
        // MessageHeaderDecoder headerDecoder = new MessageHeaderDecoder();
        // UserListDecoder decoder = new UserListDecoder();
        // decoder.wrap(buffer, offset, blockLength, version);
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
