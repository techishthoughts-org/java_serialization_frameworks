package org.techishthoughts.benchmark;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.openjdk.jmh.annotations.*;
import org.techishthoughts.payload.generator.UnifiedPayloadGenerator;
import org.techishthoughts.payload.model.User;
import org.techishthoughts.payload.config.BenchmarkProperties;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPOutputStream;

/**
 * JMH Microbenchmark for gRPC Serialization
 *
 * Measures PURE serialization/deserialization performance without HTTP overhead.
 *
 * Note: gRPC typically uses Protocol Buffers for serialization.
 * For benchmarking purposes, uses JSON with gRPC-style optimization.
 * This allows testing gRPC integration patterns without full protobuf schema generation.
 *
 * Run with: java -jar target/benchmarks.jar GrpcBenchmark
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 10, time = 1)
@Fork(value = 2, jvmArgs = {"-Xms2G", "-Xmx2G"})
public class GrpcBenchmark {

    private ObjectMapper objectMapper;
    private List<User> users;
    private byte[] serializedData;
    private byte[] compressedData;

    @Setup(Level.Trial)
    public void setup() throws Exception {
        // Initialize ObjectMapper for JSON serialization
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        // Generate test payload (MEDIUM complexity - 100 users)
        BenchmarkProperties properties = new BenchmarkProperties();
        UnifiedPayloadGenerator generator = new UnifiedPayloadGenerator(properties);
        users = generator.generateDataset(100);

        // Pre-serialize for deserialization benchmark
        serializedData = objectMapper.writeValueAsBytes(users);

        // Pre-compress for compression benchmark
        compressedData = compressWithGzip(serializedData);
    }

    @Benchmark
    public byte[] serialize() throws Exception {
        // gRPC-style serialization (JSON fallback)
        return objectMapper.writeValueAsBytes(users);
    }

    @Benchmark
    public List<User> deserialize() throws Exception {
        // gRPC-style deserialization (JSON fallback)
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

    /**
     * Compress data using GZIP (common with gRPC)
     */
    private byte[] compressWithGzip(byte[] data) throws Exception {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        try (GZIPOutputStream gzipStream = new GZIPOutputStream(byteStream)) {
            gzipStream.write(data);
        }
        return byteStream.toByteArray();
    }
}
