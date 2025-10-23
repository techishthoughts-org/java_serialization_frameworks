# Apache Avro - Deep Dive

![Speed](https://img.shields.io/badge/Speed-3_stars-yellow)
![Compression](https://img.shields.io/badge/Compression-5_stars-brightgreen)
![Ease of Use](https://img.shields.io/badge/Ease%20of%20Use-4_stars-yellow)

## Overview

Apache Avro is a data serialization system that provides rich data structures, a compact binary format, and schema evolution capabilities. Data is always stored with its corresponding schema, enabling backward and forward compatibility across system versions.

**Port**: 8083
**Category**: Binary Schema
**Official Site**: https://avro.apache.org/

## Key Characteristics

### Strengths
- **Schema Evolution**: Backward and forward compatibility built-in
- **Excellent Compression**: 68% size reduction with compression
- **Dynamic Typing**: Read data without code generation
- **Rich Data Types**: Support for complex types, unions, and enums
- **Hadoop Integration**: Native support across Apache ecosystem

### Weaknesses
- **Schema Overhead**: Schema must be available at read time
- **Slower Than Binary**: Not as fast as Kryo or FST for Java-only
- **Learning Curve**: Schema definition language requires understanding
- **Code Generation**: Optional but adds build complexity

## Performance Benchmarks

### Serialization Performance
| Metric | Value | Rank |
|--------|-------|------|
| **Avg Serialization Time** | 3.12ms | 8/13 |
| **Throughput** | 321 ops/sec | 8/13 |
| **Payload Size (MEDIUM)** | 3.2KB | 6/13 |
| **Compression Ratio** | 0.32 | 2/13 |

### Resource Utilization
| Metric | Value |
|--------|-------|
| **CPU Usage** | 3.8% |
| **Memory** | 278.3 MB |
| **Memory Delta** | 14.2 MB |
| **Threads** | 44 |

### Performance by Payload Size
| Size | Avg Time | Throughput | Size (bytes) |
|------|----------|------------|--------------|
| SMALL (10 users) | 12.34ms | 81 ops/s | 412 |
| MEDIUM (100 users) | 28.67ms | 35 ops/s | 3,156 |
| LARGE (1000 users) | 145.23ms | 7 ops/s | 31,542 |
| HUGE (10000 users) | 723.45ms | 1.4 ops/s | 315,421 |

## Implementation Details

### Dependencies

```xml
<dependency>
    <groupId>org.apache.avro</groupId>
    <artifactId>avro</artifactId>
    <version>1.11.3</version>
</dependency>
```

### Schema Definition

```json
{
  "type": "record",
  "name": "User",
  "namespace": "com.example",
  "fields": [
    {"name": "id", "type": "long"},
    {"name": "name", "type": "string"},
    {"name": "email", "type": ["null", "string"], "default": null},
    {"name": "age", "type": "int"},
    {"name": "status", "type": {
      "type": "enum",
      "name": "Status",
      "symbols": ["ACTIVE", "INACTIVE", "PENDING"]
    }}
  ]
}
```

### Basic Usage (Specific API)

```java
@Service
public class AvroSerializationService {

    public byte[] serialize(User user) throws IOException {
        DatumWriter<User> writer = new SpecificDatumWriter<>(User.class);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        BinaryEncoder encoder = EncoderFactory.get().binaryEncoder(out, null);
        writer.write(user, encoder);
        encoder.flush();

        return out.toByteArray();
    }

    public User deserialize(byte[] data) throws IOException {
        DatumReader<User> reader = new SpecificDatumReader<>(User.class);
        BinaryDecoder decoder = DecoderFactory.get()
            .binaryDecoder(data, null);

        return reader.read(null, decoder);
    }
}
```

### Generic API Usage

```java
@Service
public class GenericAvroService {

    public byte[] serialize(GenericRecord record) throws IOException {
        Schema schema = record.getSchema();
        DatumWriter<GenericRecord> writer =
            new GenericDatumWriter<>(schema);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        BinaryEncoder encoder = EncoderFactory.get().binaryEncoder(out, null);

        writer.write(record, encoder);
        encoder.flush();

        return out.toByteArray();
    }
}
```

### Advanced Configuration

```java
@Configuration
public class AvroConfiguration {

    @Bean
    public DataFileWriter<User> dataFileWriter() {
        DatumWriter<User> writer = new SpecificDatumWriter<>(User.class);
        DataFileWriter<User> fileWriter = new DataFileWriter<>(writer);

        // Enable compression
        fileWriter.setCodec(CodecFactory.snappyCodec());

        // Set sync interval
        fileWriter.setSyncInterval(16 * 1024); // 16KB

        return fileWriter;
    }
}
```

## Use Cases

### Ideal For

**Schema Evolution Requirements**
- Systems where data structure changes over time
- Backward compatibility with older clients
- Forward compatibility for gradual rollouts

**Hadoop Ecosystem Integration**
- Native support in Hive, Pig, Spark
- Efficient storage format for HDFS
- Standard format for Kafka message serialization

**Cross-Language Communication**
- Implementations in Java, Python, C++, C#, Ruby
- Standardized schema definition
- Consistent serialization across platforms

**Long-Term Data Storage**
- Self-describing data with embedded schemas
- Schema registry for version management
- Data remains readable across system versions

### Not Ideal For

**Ultra-Low Latency**
- Schema overhead adds processing time
- Use SBE or Cap'n Proto instead

**Simple Java Serialization**
- Overkill for Java-only systems
- Consider Kryo or FST for better performance

**Human Readability**
- Binary format not human-readable
- Use Jackson JSON for debugging needs

## Optimization Tips

### 1. Reuse Encoders and Decoders

```java
// Bad: Creates new instances
public byte[] serialize(User user) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    BinaryEncoder encoder = EncoderFactory.get().binaryEncoder(out, null);
    writer.write(user, encoder);
    return out.toByteArray();
}

// Good: Reuse instances
private BinaryEncoder encoder;
private ByteArrayOutputStream out = new ByteArrayOutputStream();

public byte[] serialize(User user) throws IOException {
    out.reset();
    encoder = EncoderFactory.get().binaryEncoder(out, encoder);
    writer.write(user, encoder);
    encoder.flush();
    return out.toByteArray();
}
```

### 2. Enable Compression

```java
// Use Snappy for best speed/compression balance
DataFileWriter<User> writer = new DataFileWriter<>(datumWriter);
writer.setCodec(CodecFactory.snappyCodec());

// Or use Deflate for better compression
writer.setCodec(CodecFactory.deflateCodec(6));
```

### 3. Use Binary Encoding

```java
// Binary encoding is faster than JSON
BinaryEncoder encoder = EncoderFactory.get().binaryEncoder(out, null);

// Avoid JSON encoding in production
// JsonEncoder encoder = EncoderFactory.get().jsonEncoder(schema, out);
```

### 4. Schema Registry Integration

```java
@Service
public class SchemaRegistryService {
    private final CachedSchemaRegistryClient client;

    public int registerSchema(Schema schema) {
        return client.register("user-value", schema);
    }

    public Schema getSchema(int id) {
        return client.getById(id);
    }
}
```

## Kafka Integration

### Producer Configuration

```java
@Configuration
public class KafkaAvroProducerConfig {

    @Bean
    public ProducerFactory<String, User> producerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
            StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
            KafkaAvroSerializer.class);
        props.put("schema.registry.url", "http://localhost:8081");

        return new DefaultKafkaProducerFactory<>(props);
    }
}
```

### Consumer Configuration

```java
@Configuration
public class KafkaAvroConsumerConfig {

    @Bean
    public ConsumerFactory<String, User> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
            StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
            KafkaAvroDeserializer.class);
        props.put("schema.registry.url", "http://localhost:8081");
        props.put("specific.avro.reader", "true");

        return new DefaultKafkaConsumerFactory<>(props);
    }
}
```

## API Endpoints

### Benchmark Endpoint

```bash
POST http://localhost:8083/api/avro/v2/benchmark
Content-Type: application/json

{
  "complexity": "MEDIUM",
  "iterations": 50,
  "enableWarmup": true,
  "enableCompression": true,
  "enableRoundtrip": true,
  "enableMemoryMonitoring": true
}
```

### Framework Information

```bash
GET http://localhost:8083/api/avro/v2/info
```

**Response:**
```json
{
  "framework": "Apache Avro",
  "version": "2.0",
  "supportedCompressionAlgorithms": ["SNAPPY", "DEFLATE", "BZIP2", "XZ"],
  "supportsSchemaEvolution": true,
  "typicalUseCase": "Kafka messaging, Hadoop storage, schema evolution"
}
```

## Real-World Examples

### Schema Evolution

```java
// Version 1 schema
{
  "type": "record",
  "name": "User",
  "fields": [
    {"name": "id", "type": "long"},
    {"name": "name", "type": "string"}
  ]
}

// Version 2 schema (backward compatible)
{
  "type": "record",
  "name": "User",
  "fields": [
    {"name": "id", "type": "long"},
    {"name": "name", "type": "string"},
    {"name": "email", "type": ["null", "string"], "default": null}
  ]
}
```

### File Storage

```java
@Service
public class AvroFileService {

    public void writeToFile(List<User> users, String path) throws IOException {
        DatumWriter<User> writer = new SpecificDatumWriter<>(User.class);

        try (DataFileWriter<User> fileWriter = new DataFileWriter<>(writer)) {
            fileWriter.setCodec(CodecFactory.snappyCodec());
            fileWriter.create(User.getClassSchema(), new File(path));

            for (User user : users) {
                fileWriter.append(user);
            }
        }
    }

    public List<User> readFromFile(String path) throws IOException {
        DatumReader<User> reader = new SpecificDatumReader<>(User.class);
        List<User> users = new ArrayList<>();

        try (DataFileReader<User> fileReader =
                new DataFileReader<>(new File(path), reader)) {

            while (fileReader.hasNext()) {
                users.add(fileReader.next());
            }
        }

        return users;
    }
}
```

## Alternatives & When to Switch

| Switch To | When | Why |
|-----------|------|-----|
| **Protocol Buffers** | Need gRPC support | Better RPC integration |
| **Parquet** | Analytics workloads | Columnar format, better for queries |
| **Thrift** | Multi-language RPC | More mature RPC framework |
| **Kryo** | Java-only, need speed | 2-3x faster serialization |

## Schema Evolution Patterns

### Adding Fields

```java
// Always provide defaults for new fields
{"name": "phone", "type": ["null", "string"], "default": null}
```

### Removing Fields

```java
// Mark as deprecated, remove in future version
// Readers with old schema will ignore missing field
```

### Changing Types

```java
// Use union types for safe evolution
{"name": "id", "type": ["long", "string"]}
```

## Troubleshooting

### Issue: Schema Not Found

**Problem**: Reader cannot find writer schema

**Solution**:
```java
// Embed schema in data file
DataFileWriter<User> writer = new DataFileWriter<>(datumWriter);
writer.create(schema, outputStream);

// Or use Schema Registry
schemaRegistry.register("user-value", schema);
```

### Issue: Incompatible Schema Changes

**Problem**: New schema breaks old readers

**Solution**:
```java
// Always test schema compatibility
SchemaCompatibility.checkReaderWriterCompatibility(
    readerSchema, writerSchema
).getType(); // Should be COMPATIBLE
```

### Issue: Slow Performance

**Problem**: Serialization slower than expected

**Solutions**:
1. Reuse encoders and decoders
2. Use binary encoding instead of JSON
3. Enable Snappy compression
4. Use specific API instead of generic

## Best Practices

1. **Use Schema Registry**: Centralize schema management
2. **Test Compatibility**: Validate schema evolution changes
3. **Reuse Objects**: Minimize object allocation
4. **Enable Compression**: Use Snappy for best balance
5. **Version Schemas**: Track schema versions carefully
6. **Provide Defaults**: Always add defaults to new fields
7. **Document Changes**: Maintain schema changelog
8. **Use Specific API**: Better performance than generic

## Additional Resources

- **Official Documentation**: https://avro.apache.org/docs/current/
- **Schema Evolution**: https://docs.confluent.io/platform/current/schema-registry/avro.html
- **Kafka Integration**: https://docs.confluent.io/kafka/documentation.html
- **Best Practices**: https://www.baeldung.com/java-apache-avro

## Source Code

Implementation: [`avro-poc/`](../../avro-poc/)

Key Files:
- `AvroBenchmarkControllerV2.java` - REST endpoints
- `AvroSerializationServiceV2.java` - Core serialization logic
- `AvroConfiguration.java` - Encoder/decoder configuration

---

**Last Updated**: 2025-10-22
**Benchmark Version**: 2.0.0
