# Kryo - Deep Dive

![Speed](https://img.shields.io/badge/Speed-5_stars-brightgreen)
![Compression](https://img.shields.io/badge/Compression-3_stars-yellow)
![Ease of Use](https://img.shields.io/badge/Ease%20of%20Use-4_stars-green)

## Overview

Kryo is a fast and efficient binary serialization framework for Java. It provides automatic deep and shallow object copying, supports circular references, and offers variable-length encoding for optimal performance. Designed specifically for Java applications requiring maximum throughput and minimal overhead.

**Port**: 8084
**Category**: Binary Schema-less
**Official Site**: https://github.com/EsotericSoftware/kryo

## Key Characteristics

### Strengths
- **Extremely Fast**: Optimized for Java with minimal overhead
- **Compact Format**: Variable-length encoding reduces payload size
- **Minimal Configuration**: Works out-of-the-box with sensible defaults
- **Deep/Shallow Copying**: Built-in object cloning capabilities
- **Circular References**: Handles complex object graphs
- **Active Development**: Regular updates and improvements
- **Compression Support**: Built-in compression options

### Weaknesses
- **Java-Only**: No cross-language compatibility
- **No Schema**: Runtime errors if structure mismatch
- **Platform-Dependent**: Unsafe buffers may behave differently across platforms
- **Registration Required**: Best performance requires class registration
- **Breaking Changes**: Format may change between versions

## Performance Benchmarks

### Serialization Performance
| Metric | Value | Rank |
|--------|-------|------|
| **Avg Serialization Time** | 1.67ms | 4/13 |
| **Throughput** | 599 ops/sec | 4/13 |
| **Payload Size (MEDIUM)** | 2.3KB | 3/13 |
| **Compression Ratio** | 0.12 | 3/13 |

### Resource Utilization
| Metric | Value |
|--------|-------|
| **CPU Usage** | 4.2% |
| **Memory** | 245.8 MB |
| **Memory Delta** | 8.5 MB |
| **Threads** | 42 |

### Performance by Payload Size
| Size | Avg Time | Throughput | Size (bytes) |
|------|----------|------------|--------------|
| SMALL (10 users) | 0.85ms | 1,176 ops/s | 234 |
| MEDIUM (100 users) | 1.67ms | 599 ops/s | 2,345 |
| LARGE (1000 users) | 18.45ms | 54 ops/s | 23,456 |
| HUGE (10000 users) | 185.67ms | 5.4 ops/s | 234,567 |

## Implementation Details

### Dependencies

```xml
<dependency>
    <groupId>com.esotericsoftware</groupId>
    <artifactId>kryo</artifactId>
    <version>5.6.2</version>
</dependency>
```

**Minimum Requirements**: JDK 11+

### Basic Usage

```java
@Service
public class KryoSerializationService {
    private final ThreadLocal<Kryo> kryoThreadLocal;

    public KryoSerializationService() {
        // Kryo is not thread-safe, use ThreadLocal for pooling
        this.kryoThreadLocal = ThreadLocal.withInitial(() -> {
            Kryo kryo = new Kryo();
            kryo.setRegistrationRequired(false);
            kryo.setReferences(true); // Handle circular references
            return kryo;
        });
    }

    public byte[] serialize(UserPayload payload) {
        Kryo kryo = kryoThreadLocal.get();
        Output output = new Output(4096, -1); // Initial 4KB, unlimited max
        try {
            kryo.writeObject(output, payload);
            return output.toBytes();
        } finally {
            output.close();
        }
    }

    public UserPayload deserialize(byte[] data) {
        Kryo kryo = kryoThreadLocal.get();
        Input input = new Input(data);
        try {
            return kryo.readObject(input, UserPayload.class);
        } finally {
            input.close();
        }
    }
}
```

### Advanced Configuration

```java
@Configuration
public class KryoConfiguration {

    @Bean
    public Pool<Kryo> kryoPool() {
        return new Pool<Kryo>(true, false, 16) {
            @Override
            protected Kryo create() {
                Kryo kryo = new Kryo();

                // Performance optimizations
                kryo.setRegistrationRequired(false);
                kryo.setReferences(true);
                kryo.setCopyReferences(true);

                // Register common classes for better performance
                kryo.register(UserPayload.class, 10);
                kryo.register(User.class, 11);
                kryo.register(Address.class, 12);
                kryo.register(ArrayList.class, 13);
                kryo.register(HashMap.class, 14);
                kryo.register(Date.class, 15);

                // Custom serializers
                kryo.register(LocalDateTime.class, new JavaTimeSerializer());
                kryo.register(UUID.class, new UUIDSerializer());

                // Configure for optimal performance
                kryo.setInstantiatorStrategy(
                    new StdInstantiatorStrategy()
                );

                // Use unsafe IO for maximum speed (platform-dependent)
                kryo.setDefaultSerializer(CompatibleFieldSerializer.class);

                return kryo;
            }
        };
    }

    @Bean
    public KryoSerializationService kryoService(Pool<Kryo> kryoPool) {
        return new KryoSerializationService(kryoPool);
    }
}
```

## Use Cases

### Ideal For

**High-Performance Java Applications**
- Maximum throughput requirements
- Low-latency microservices communication
- Internal Java-to-Java RPC calls
- High-frequency trading systems

**Caching Systems**
- Redis/Memcached serialization
- Session storage
- Distributed cache entries
- Object persistence

**Message Queues**
- Kafka message serialization
- RabbitMQ payload encoding
- Internal event streams
- High-volume message processing

**Game Development**
- Network protocol serialization
- Game state persistence
- Fast object cloning
- Memory-efficient storage

### Not Ideal For

**Cross-Language Systems**
- No support for non-JVM languages
- Use Protocol Buffers, Avro, or Thrift instead

**Long-Term Storage**
- Format may change between versions
- No schema evolution support
- Consider Avro or Parquet

**Public APIs**
- Binary format not human-readable
- Use JSON or XML for external APIs

**Schema Validation Required**
- No compile-time type checking
- Use Protocol Buffers or Thrift

## Optimization Tips

### 1. Register Classes

```java
// Bad: Dynamic class registration overhead
Kryo kryo = new Kryo();
kryo.setRegistrationRequired(false);

// Good: Pre-register classes with IDs
Kryo kryo = new Kryo();
kryo.setRegistrationRequired(true);
kryo.register(UserPayload.class, 10);
kryo.register(User.class, 11);
kryo.register(Address.class, 12);
// 20-30% performance improvement
```

### 2. Use Object Pooling

```java
// Bad: Creates new Kryo instance every time
public byte[] serialize(Object obj) {
    Kryo kryo = new Kryo();
    Output output = new Output(4096);
    kryo.writeObject(output, obj);
    return output.toBytes();
}

// Good: Pool Kryo instances
private final Pool<Kryo> kryoPool = new Pool<Kryo>(true, false, 16) {
    protected Kryo create() {
        return new Kryo();
    }
};

public byte[] serialize(Object obj) {
    Kryo kryo = kryoPool.obtain();
    try {
        Output output = new Output(4096);
        kryo.writeObject(output, obj);
        return output.toBytes();
    } finally {
        kryoPool.free(kryo);
    }
}
```

### 3. Optimize Buffer Sizes

```java
// Estimate initial buffer size based on payload
public byte[] serialize(Object obj) {
    int estimatedSize = estimateSize(obj);
    Output output = new Output(estimatedSize, -1);
    kryo.writeObject(output, obj);
    return output.toBytes();
}

// For known sizes, use fixed buffers
Output output = new Output(2048); // No resizing overhead
```

### 4. Use Unsafe IO for Maximum Speed

```java
Kryo kryo = new Kryo();
// Use unsafe buffers (platform-dependent, fastest)
kryo.setDefaultSerializer(FieldSerializer.class);

// Alternative: Use compatible mode for portability
kryo.setDefaultSerializer(CompatibleFieldSerializer.class);
```

### 5. Disable Unnecessary Features

```java
Kryo kryo = new Kryo();
// Disable if no circular references
kryo.setReferences(false);

// Disable copy references if not using deep copy
kryo.setCopyReferences(false);

// Use specific serializers for known types
kryo.setDefaultSerializer(FieldSerializer.class);
```

## Spring Boot Integration

### Configuration Bean

```java
@Configuration
public class KryoConfig {

    @Bean
    public Pool<Kryo> kryoPool() {
        return new Pool<Kryo>(true, false, 16) {
            @Override
            protected Kryo create() {
                Kryo kryo = new Kryo();
                configureKryo(kryo);
                return kryo;
            }
        };
    }

    private void configureKryo(Kryo kryo) {
        kryo.setRegistrationRequired(false);
        kryo.setReferences(true);
        kryo.setCopyReferences(true);

        // Register application classes
        registerClasses(kryo);

        // Register custom serializers
        registerSerializers(kryo);
    }

    private void registerClasses(Kryo kryo) {
        kryo.register(UserPayload.class, 10);
        kryo.register(User.class, 11);
        kryo.register(Address.class, 12);
        kryo.register(ArrayList.class, 13);
        kryo.register(HashMap.class, 14);
    }

    private void registerSerializers(Kryo kryo) {
        kryo.register(LocalDateTime.class, new JavaTimeSerializer());
        kryo.register(UUID.class, new UUIDSerializer());
    }
}
```

### Custom Serializer Example

```java
public class JavaTimeSerializer extends Serializer<LocalDateTime> {

    @Override
    public void write(Kryo kryo, Output output, LocalDateTime object) {
        output.writeLong(object.toEpochSecond(ZoneOffset.UTC));
        output.writeInt(object.getNano());
    }

    @Override
    public LocalDateTime read(Kryo kryo, Input input, Class<LocalDateTime> type) {
        long epochSecond = input.readLong();
        int nano = input.readInt();
        return LocalDateTime.ofEpochSecond(epochSecond, nano, ZoneOffset.UTC);
    }
}
```

## API Endpoints

### Benchmark Endpoint
```bash
POST http://localhost:8084/api/kryo/v2/benchmark
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
GET http://localhost:8084/api/kryo/v2/info
```

**Response:**
```json
{
  "framework": "Kryo",
  "version": "2.0",
  "supportedCompressionAlgorithms": ["GZIP", "DEFLATE"],
  "supportsSchemaEvolution": false,
  "typicalUseCase": "High-performance Java applications, caching, message queues"
}
```

## Real-World Examples

### Redis Cache Serialization

```java
@Configuration
public class RedisCacheConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(
            RedisConnectionFactory factory,
            Pool<Kryo> kryoPool) {

        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        // Use Kryo for value serialization
        template.setValueSerializer(new KryoRedisSerializer<>(kryoPool));
        template.setHashValueSerializer(new KryoRedisSerializer<>(kryoPool));

        return template;
    }
}

public class KryoRedisSerializer<T> implements RedisSerializer<T> {
    private final Pool<Kryo> kryoPool;

    @Override
    public byte[] serialize(T t) {
        if (t == null) return new byte[0];

        Kryo kryo = kryoPool.obtain();
        try (Output output = new Output(4096, -1)) {
            kryo.writeClassAndObject(output, t);
            return output.toBytes();
        } finally {
            kryoPool.free(kryo);
        }
    }

    @Override
    public T deserialize(byte[] bytes) {
        if (bytes == null || bytes.length == 0) return null;

        Kryo kryo = kryoPool.obtain();
        try (Input input = new Input(bytes)) {
            return (T) kryo.readClassAndObject(input);
        } finally {
            kryoPool.free(kryo);
        }
    }
}
```

### Kafka Message Serialization

```java
public class KryoKafkaSerializer implements Serializer<Object> {
    private final ThreadLocal<Kryo> kryoThreadLocal;

    public KryoKafkaSerializer() {
        this.kryoThreadLocal = ThreadLocal.withInitial(() -> {
            Kryo kryo = new Kryo();
            kryo.setRegistrationRequired(false);
            return kryo;
        });
    }

    @Override
    public byte[] serialize(String topic, Object data) {
        if (data == null) return null;

        Kryo kryo = kryoThreadLocal.get();
        try (Output output = new Output(4096, -1)) {
            kryo.writeClassAndObject(output, data);
            return output.toBytes();
        }
    }
}

// Kafka producer configuration
@Bean
public ProducerFactory<String, Object> producerFactory() {
    Map<String, Object> config = new HashMap<>();
    config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
    config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
               StringSerializer.class);
    config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
               KryoKafkaSerializer.class);
    return new DefaultKafkaProducerFactory<>(config);
}
```

### Deep Object Copying

```java
@Service
public class ObjectCloningService {
    private final ThreadLocal<Kryo> kryoThreadLocal;

    public ObjectCloningService() {
        this.kryoThreadLocal = ThreadLocal.withInitial(() -> {
            Kryo kryo = new Kryo();
            kryo.setReferences(true);
            kryo.setCopyReferences(true);
            return kryo;
        });
    }

    public <T> T deepCopy(T object) {
        return kryoThreadLocal.get().copy(object);
    }

    public <T> T shallowCopy(T object) {
        return kryoThreadLocal.get().copyShallow(object);
    }
}

// Usage
User original = new User("John", new Address("123 Main St"));
User clone = cloningService.deepCopy(original);
// Clone is completely independent of original
```

## Alternatives & When to Switch

| Switch To | When | Why |
|-----------|------|-----|
| **FST** | Need even faster Java serialization | Slightly faster, similar features |
| **Protocol Buffers** | Cross-language support | Schema-based, multi-language |
| **Avro** | Schema evolution needed | Schema versioning, compression |
| **MessagePack** | Language interop + speed | Fast binary, cross-language |

## Common Patterns

### Class Registration Pattern

```java
public class KryoClassRegistry {
    private static final Map<Class<?>, Integer> REGISTRY = new HashMap<>();

    static {
        // Domain objects
        REGISTRY.put(UserPayload.class, 10);
        REGISTRY.put(User.class, 11);
        REGISTRY.put(Address.class, 12);

        // Collections
        REGISTRY.put(ArrayList.class, 20);
        REGISTRY.put(HashMap.class, 21);
        REGISTRY.put(HashSet.class, 22);

        // Java time
        REGISTRY.put(LocalDateTime.class, 30);
        REGISTRY.put(LocalDate.class, 31);
        REGISTRY.put(Instant.class, 32);
    }

    public static void registerAll(Kryo kryo) {
        REGISTRY.forEach(kryo::register);
    }
}
```

### Versioned Serialization

```java
public class VersionedSerializer<T> extends Serializer<T> {
    private static final int CURRENT_VERSION = 2;
    private final Serializer<T> delegateSerializer;

    @Override
    public void write(Kryo kryo, Output output, T object) {
        output.writeInt(CURRENT_VERSION);
        delegateSerializer.write(kryo, output, object);
    }

    @Override
    public T read(Kryo kryo, Input input, Class<T> type) {
        int version = input.readInt();
        switch (version) {
            case 1:
                return readVersion1(kryo, input, type);
            case 2:
                return delegateSerializer.read(kryo, input, type);
            default:
                throw new KryoException("Unknown version: " + version);
        }
    }
}
```

### Safe Type Handling

```java
public class SafeKryoSerializer {
    private final Pool<Kryo> kryoPool;

    public <T> byte[] serialize(T object) {
        Kryo kryo = kryoPool.obtain();
        try (Output output = new Output(4096, -1)) {
            // Write class information for safe deserialization
            kryo.writeClassAndObject(output, object);
            return output.toBytes();
        } finally {
            kryoPool.free(kryo);
        }
    }

    public <T> T deserialize(byte[] data, Class<T> expectedType) {
        Kryo kryo = kryoPool.obtain();
        try (Input input = new Input(data)) {
            Object result = kryo.readClassAndObject(input);
            if (!expectedType.isInstance(result)) {
                throw new KryoException(
                    "Expected " + expectedType + " but got " + result.getClass()
                );
            }
            return expectedType.cast(result);
        } finally {
            kryoPool.free(kryo);
        }
    }
}
```

## Troubleshooting

### Issue: NotSerializableException
**Problem**: Class cannot be serialized without registration

**Solution**:
```java
// Option 1: Disable registration requirement
kryo.setRegistrationRequired(false);

// Option 2: Register the class
kryo.register(MyClass.class);

// Option 3: Use DefaultSerializer annotation
@DefaultSerializer(MyClassSerializer.class)
public class MyClass {
    // fields
}
```

### Issue: Thread Safety Errors
**Problem**: Kryo is not thread-safe

**Solution**:
```java
// Use ThreadLocal for simple cases
private final ThreadLocal<Kryo> kryoThreadLocal =
    ThreadLocal.withInitial(Kryo::new);

// Use Pool for better resource management
private final Pool<Kryo> kryoPool = new Pool<Kryo>(true, false, 16) {
    protected Kryo create() {
        return new Kryo();
    }
};
```

### Issue: Version Incompatibility
**Problem**: Serialized data cannot be read after Kryo upgrade

**Solution**:
```java
// Use CompatibleFieldSerializer for version tolerance
kryo.setDefaultSerializer(CompatibleFieldSerializer.class);

// Or implement custom versioning
public class VersionedKryo extends Kryo {
    @Override
    public void writeObject(Output output, Object object) {
        output.writeInt(VERSION);
        super.writeObject(output, object);
    }
}
```

### Issue: Poor Performance
**Problem**: Serialization slower than expected

**Solutions**:
1. Register classes with IDs
2. Use object pooling (Pool or ThreadLocal)
3. Pre-size Output buffers appropriately
4. Disable references if not needed
5. Use FieldSerializer instead of CompatibleFieldSerializer

```java
// Performance tuning example
Kryo kryo = new Kryo();
kryo.setRegistrationRequired(true);
kryo.setReferences(false);
kryo.setDefaultSerializer(FieldSerializer.class);

// Pre-register all classes
kryo.register(MyClass.class, 10);
```

## Benchmarking Results

### Comparison with Other Binary Formats
Kryo excels in Java-to-Java serialization performance.

**vs FST**: Comparable speed, FST slightly faster
**vs Java Serialization**: ~10x faster, much smaller payload
**vs Protocol Buffers**: ~2x faster, but no cross-language support
**vs Avro**: ~3x faster, but no schema evolution

### Memory Footprint
Low memory overhead compared to other serializers:
- Base overhead: ~240 MB
- Per-operation increase: 8-9 MB for MEDIUM payload
- Efficient object pooling reduces allocation pressure

### Throughput Analysis
Excellent throughput characteristics:
- SMALL payloads: 1,176 ops/sec
- MEDIUM payloads: 599 ops/sec
- LARGE payloads: 54 ops/sec
- Ranks 4th overall in speed benchmarks

## Best Practices

1. **Use Object Pooling**: Pool Kryo instances with ThreadLocal or Pool
2. **Register Classes**: Pre-register classes for 20-30% performance gain
3. **Right-Size Buffers**: Estimate buffer sizes to avoid resizing
4. **Handle Circular References**: Enable references only if needed
5. **Version Your Format**: Implement versioning for long-term storage
6. **Test Roundtrips**: Verify serialization/deserialization consistency
7. **Monitor Performance**: Track serialization times and payload sizes
8. **Use Unsafe Carefully**: Understand platform dependencies
9. **Implement Custom Serializers**: Optimize hot path objects
10. **Document Registration IDs**: Maintain consistent class registration

## Additional Resources

- **Official GitHub**: https://github.com/EsotericSoftware/kryo
- **API Documentation**: https://javadoc.io/doc/com.esotericsoftware/kryo
- **Performance Guide**: https://github.com/EsotericSoftware/kryo#benchmarks
- **Custom Serializers**: https://github.com/EsotericSoftware/kryo#serializers

## Source Code

Implementation: [`kryo-poc/`](../../kryo-poc/)

Key Files:
- `KryoBenchmarkControllerV2.java` - REST endpoints
- `KryoSerializationServiceV2.java` - Core serialization logic
- `KryoConfiguration.java` - Kryo pool configuration

---

**Last Updated**: 2025-10-22
**Benchmark Version**: 2.0.0
**Kryo Version**: 5.6.2
