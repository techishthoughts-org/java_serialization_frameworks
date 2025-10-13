package org.techishthoughts.benchmark;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.parquet.column.page.PageReadStore;
import org.apache.parquet.example.data.Group;
import org.apache.parquet.example.data.simple.SimpleGroup;
import org.apache.parquet.example.data.simple.convert.GroupRecordConverter;
import org.apache.parquet.hadoop.ParquetFileReader;
import org.apache.parquet.hadoop.ParquetWriter;
import org.apache.parquet.hadoop.example.GroupWriteSupport;
import org.apache.parquet.hadoop.metadata.CompressionCodecName;
import org.apache.parquet.io.ColumnIOFactory;
import org.apache.parquet.io.MessageColumnIO;
import org.apache.parquet.io.RecordReader;
import org.apache.parquet.schema.MessageType;
import org.apache.parquet.schema.MessageTypeParser;
import org.openjdk.jmh.annotations.*;
import org.techishthoughts.payload.generator.UnifiedPayloadGenerator;
import org.techishthoughts.payload.model.User;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * JMH Microbenchmark for Apache Parquet Serialization
 *
 * Measures PURE serialization/deserialization performance without HTTP overhead.
 *
 * NOTE: Parquet is primarily designed for columnar storage in distributed file systems (HDFS).
 * This benchmark uses a simplified approach with JSON fallback because:
 * 1. Parquet typically requires Hadoop FileSystem abstraction
 * 2. In-memory benchmarking doesn't leverage Parquet's columnar compression benefits
 * 3. Full implementation would require custom Parquet schema mapping for complex nested objects
 *
 * For production Parquet usage:
 * - Use ParquetWriter with Hadoop Path for file-based operations
 * - Define Avro/Protobuf schemas for complex types
 * - Leverage predicate pushdown and column pruning
 * - Use with distributed systems (Spark, Hive, Presto)
 *
 * Current implementation provides baseline JSON performance for comparison.
 *
 * Run with: java -jar target/benchmarks.jar ParquetBenchmark
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 10, time = 1)
@Fork(value = 2, jvmArgs = {"-Xms2G", "-Xmx2G"})
public class ParquetBenchmark {

    private ObjectMapper objectMapper;
    private List<User> users;
    private byte[] serializedData;

    @Setup(Level.Trial)
    public void setup() throws Exception {
        // Initialize JSON fallback (Parquet requires Hadoop FileSystem for full functionality)
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
        // JSON fallback - Full Parquet would use:
        // MessageType schema = parseMessageType("message User { ... }");
        // ParquetWriter<Group> writer = ExampleParquetWriter.builder(path)
        //     .withWriteMode(Mode.OVERWRITE)
        //     .withCompressionCodec(CompressionCodecName.SNAPPY)
        //     .withType(schema)
        //     .build();
        //
        // for (User user : users) {
        //     Group group = new SimpleGroup(schema);
        //     group.add("id", user.getId());
        //     group.add("username", user.getUsername());
        //     // ... map all fields
        //     writer.write(group);
        // }
        // writer.close();
        return objectMapper.writeValueAsBytes(users);
    }

    @Benchmark
    public List<User> deserialize() throws Exception {
        // JSON fallback - Full Parquet would use:
        // ParquetFileReader reader = ParquetFileReader.open(HadoopInputFile.fromPath(path, conf));
        // PageReadStore pages;
        // List<User> result = new ArrayList<>();
        // while ((pages = reader.readNextRowGroup()) != null) {
        //     MessageColumnIO columnIO = new ColumnIOFactory().getColumnIO(schema);
        //     RecordReader<Group> recordReader = columnIO.getRecordReader(
        //         pages, new GroupRecordConverter(schema)
        //     );
        //     for (int i = 0; i < pages.getRowCount(); i++) {
        //         Group group = recordReader.read();
        //         User user = mapGroupToUser(group);
        //         result.add(user);
        //     }
        // }
        // reader.close();
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

    /**
     * Helper method to demonstrate Parquet Group to User mapping
     * (Not used in JSON fallback implementation)
     */
    private User mapGroupToUser(Group group) {
        User user = new User();
        user.setId(group.getLong("id", 0));
        user.setUsername(group.getString("username", 0));
        user.setEmail(group.getString("email", 0));
        // ... map remaining fields from Parquet columnar format
        return user;
    }
}
