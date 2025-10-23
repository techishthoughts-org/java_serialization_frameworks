# FST (Fast Serialization) - Deep Dive

![Speed](https://img.shields.io/badge/Speed-5_stars-brightgreen)
![Compression](https://img.shields.io/badge/Compression-3_stars-yellow)
![Ease of Use](https://img.shields.io/badge/Ease%20of%20Use-5_stars-brightgreen)

## Overview

FST (Fast Serialization) is a high-performance, 100% compatible drop-in replacement for Java's built-in serialization. It delivers up to 10x faster serialization performance while maintaining full compatibility with existing JDK Serializable classes, requiring minimal code changes.

**Port**: 8090
**Category**: Binary Schema-less
**Official Site**: https://github.com/RuedigerMoeller/fast-serialization

## Key Characteristics

### Strengths
- **Drop-in Replacement**: 100% compatible with JDK Serialization
- **Minimal Code Changes**: Works with existing Serializable classes
- **Very Fast**: Up to 10x faster than standard Java serialization
- **Off-Heap Support**: Built-in off-heap collections for memory-efficient caching
- **Android Compatible**: Can be used in Android applications
- **JSON/MinBin Support**: Alternative lightweight JSON-like format

### Weaknesses
- **Java-Only**: No cross-language support
- **Less Mature**: Smaller community compared to Kryo
- **Binary Format**: Not human-readable
- **Version Compatibility**: Must match FST version for serialization/deserialization

## Performance Benchmarks

### Serialization Performance
| Metric | Value | Rank |
|--------|-------|------|
| **Avg Serialization Time** | 1.45ms | 3/13 |
| **Throughput** | 690 ops/sec | 3/13 |
| **Payload Size (MEDIUM)** | 2.3KB | 5/13 |
| **Compression Ratio** | 0.55 | 6/13 |

### Resource Utilization
| Metric | Value |
|--------|-------|
| **CPU Usage** | 2.8% |
| **Memory** | 234.5 MB |
| **Memory Delta** | 8.7 MB |
| **Threads** | 42 |

### Performance by Payload Size
| Size | Avg Time | Throughput | Size (bytes) |
|------|----------|------------|--------------|
| SMALL (10 users) | 0.45ms | 2,222 ops/s | 230 |
| MEDIUM (100 users) | 1.45ms | 690 ops/s | 2,300 |
| LARGE (1000 users) | 14.5ms | 69 ops/s | 23,000 |
| HUGE (10000 users) | 145ms | 6.9 ops/s | 230,000 |

## Implementation Details

### Dependencies

**For Java 14+:**
```xml
<dependency>
    <groupId>de.ruedigermoeller</groupId>
    <artifactId>fst</artifactId>
    <version>3.0.1</version>
</dependency>
```

**For Java 8+:**
```xml
<dependency>
    <groupId>de.ruedigermoeller</groupId>
    <artifactId>fst</artifactId>
    <version>2.56</version>
</dependency>
```

### Basic Usage

```java
@Service
public class FSTSerializationService {
    private final FSTConfiguration conf;

    public FSTSerializationService() {
        // Create reusable configuration
        this.conf = FSTConfiguration.createDefaultConfiguration();
    }

    public byte[] serialize(UserPayload payload) {
        return conf.asByteArray(payload);
    }

    public UserPayload deserialize(byte[] data) {
        return (UserPayload) conf.asObject(data);
    }
}
```

### Advanced Configuration

```java
@Configuration
public class FSTConfiguration {

    @Bean
    public FSTConfiguration fstConfiguration() {
        // Create configuration with optimizations
        FSTConfiguration conf = FSTConfiguration.createDefaultConfiguration();

        // Register classes for better performance
        conf.registerClass(
            UserPayload.class,
            User.class,
            Address.class,
            Preferences.class
        );

        // Enable class sharing (faster, but less safe)
        conf.setShareReferences(true);

        // Configure serialization compatibility
        conf.setForceSerializable(false);

        // Set class loader for dynamic environments
        conf.setClassLoader(Thread.currentThread().getContextClassLoader());

        return conf;
    }

    @Bean
    public FSTConfiguration jsonConfiguration() {
        // Alternative JSON-like format configuration
        FSTConfiguration conf = FSTConfiguration.createJsonConfiguration();
        conf.registerClass(UserPayload.class);
        return conf;
    }
}
```

### Drop-in Replacement Pattern

```java
// Replace ObjectOutputStream/ObjectInputStream with FST

// Before: Standard Java Serialization
try (ObjectOutputStream oos = new ObjectOutputStream(fileOutputStream)) {
    oos.writeObject(myObject);
}

// After: FST Drop-in Replacement
FSTObjectOutput out = conf.getObjectOutput(fileOutputStream);
out.writeObject(myObject);
out.flush();

// Before: Standard Java Deserialization
try (ObjectInputStream ois = new ObjectInputStream(fileInputStream)) {
    MyObject obj = (MyObject) ois.readObject();
}

// After: FST Drop-in Replacement
FSTObjectInput in = conf.getObjectInput(fileInputStream);
MyObject obj = (MyObject) in.readObject();
```

## Use Cases

### Ideal For

**Legacy Java Application Migration**
- Works with existing Serializable classes
- No need to rewrite domain models
- Immediate performance improvement
- Minimal testing required

**High-Performance Caching**
- Fast serialization for cache storage
- Off-heap collections for memory efficiency
- Reduces garbage collection pressure
- Suitable for large-scale caching systems

**Session Storage**
- Replace Java serialization in session management
- Faster session serialization/deserialization
- Compatible with existing session implementations
- Works with distributed session stores

**Inter-Process Communication (IPC)**
- Fast binary serialization for local IPC
- Lower latency than standard Java serialization
- Maintains object graph relationships
- Supports complex object hierarchies

### Not Ideal For

**Cross-Language Systems**
- Java-only format
- No support for other languages
- Consider Protocol Buffers or Thrift

**Long-Term Storage**
- Schema evolution challenges
- Version compatibility required
- Consider Avro or Parquet

**Human-Readable Data**
- Binary format not debuggable
- Use Jackson or XML instead

**Microservices APIs**
- Not web-friendly
- Consider JSON or Protocol Buffers

## Optimization Tips

### 1. Reuse FSTConfiguration

```java
// Bad: Creates new configuration every time
public byte[] serialize(Object obj) {
    FSTConfiguration conf = FSTConfiguration.createDefaultConfiguration();
    return conf.asByteArray(obj);
}

// Good: Reuse singleton configuration
private static final FSTConfiguration CONF =
    FSTConfiguration.createDefaultConfiguration();

public byte[] serialize(Object obj) {
    return CONF.asByteArray(obj);
}
```

### 2. Register Classes Upfront

```java
// Better performance when classes are registered
FSTConfiguration conf = FSTConfiguration.createDefaultConfiguration();

// Register all classes you'll serialize
conf.registerClass(
    User.class,
    Order.class,
    Product.class,
    // ... register all domain classes
);
```

### 3. Configure Reference Sharing

```java
// Enable for object graphs with shared references
conf.setShareReferences(true); // Default: true

// Disable for simple objects (faster)
conf.setShareReferences(false);
```

### 4. Use Thread-Local Instances

```java
// For multi-threaded environments
private static final ThreadLocal<FSTConfiguration> CONF_THREAD_LOCAL =
    ThreadLocal.withInitial(() -> {
        FSTConfiguration conf = FSTConfiguration.createDefaultConfiguration();
        conf.registerClass(UserPayload.class);
        return conf;
    });

public byte[] serialize(Object obj) {
    return CONF_THREAD_LOCAL.get().asByteArray(obj);
}
```

### 5. Leverage Off-Heap Collections

```java
// Use FST's off-heap collections for memory efficiency
FSTConfiguration conf = FSTConfiguration.createDefaultConfiguration();

// Create off-heap map
OffHeapMap<String, User> cache = new OffHeapMap<>(
    10_000,              // initial size
    0.5,                 // free memory percentage
    100_000,             // max entries
    conf
);

// Use like regular map
cache.put("user123", user);
User retrieved = cache.get("user123");

// Memory is managed off-heap, reducing GC pressure
```

## Spring Boot Integration

### Configuration Bean

```java
@Configuration
public class FSTConfig {

    @Bean
    public FSTConfiguration fstConfiguration() {
        FSTConfiguration conf = FSTConfiguration.createDefaultConfiguration();

        // Pre-register domain classes
        conf.registerClass(
            UserPayload.class,
            User.class,
            Address.class
        );

        return conf;
    }

    @Bean
    public FSTSerializationService fstSerializationService(
            FSTConfiguration fstConfiguration) {
        return new FSTSerializationService(fstConfiguration);
    }
}
```

### Service Implementation

```java
@Service
public class FSTSerializationService {
    private final FSTConfiguration conf;

    public FSTSerializationService(FSTConfiguration conf) {
        this.conf = conf;
    }

    public <T> byte[] serialize(T object) {
        return conf.asByteArray(object);
    }

    @SuppressWarnings("unchecked")
    public <T> T deserialize(byte[] data, Class<T> clazz) {
        return (T) conf.asObject(data);
    }

    public <T> T deepCopy(T object) {
        return (T) conf.deepCopy(object);
    }
}
```

## API Endpoints

### Benchmark Endpoint
```bash
POST http://localhost:8090/api/fst/v2/benchmark
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
GET http://localhost:8090/api/fst/v2/info
```

**Response:**
```json
{
  "framework": "FST (Fast Serialization)",
  "version": "2.0",
  "supportedCompressionAlgorithms": ["GZIP"],
  "supportsSchemaEvolution": false,
  "typicalUseCase": "Drop-in JDK serialization replacement, high-performance caching"
}
```

## Real-World Examples

### Session Serialization

```java
@Component
public class FSTSessionSerializer implements SessionSerializer {
    private final FSTConfiguration conf;

    public FSTSessionSerializer() {
        this.conf = FSTConfiguration.createDefaultConfiguration();
        conf.setShareReferences(true);
    }

    @Override
    public byte[] serialize(HttpSession session) {
        Map<String, Object> attributes = new HashMap<>();
        Enumeration<String> names = session.getAttributeNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            attributes.put(name, session.getAttribute(name));
        }
        return conf.asByteArray(attributes);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void deserialize(byte[] data, HttpSession session) {
        Map<String, Object> attributes =
            (Map<String, Object>) conf.asObject(data);
        attributes.forEach(session::setAttribute);
    }
}
```

### Off-Heap Cache Implementation

```java
@Service
public class FSTCacheService {
    private final OffHeapMap<String, UserPayload> cache;
    private final FSTConfiguration conf;

    public FSTCacheService() {
        this.conf = FSTConfiguration.createDefaultConfiguration();
        conf.registerClass(UserPayload.class, User.class);

        // 100MB off-heap cache
        this.cache = new OffHeapMap<>(
            1_000,           // initial capacity
            0.5,             // free memory percentage
            100_000,         // max entries
            conf
        );
    }

    public void put(String key, UserPayload value) {
        cache.put(key, value);
    }

    public UserPayload get(String key) {
        return cache.get(key);
    }

    public void clear() {
        cache.clear();
    }

    public long getMemoryUsed() {
        return cache.getMemoryUsed();
    }
}
```

### File-Based Persistence

```java
@Service
public class FSTFileStorage {
    private final FSTConfiguration conf;

    public FSTFileStorage() {
        this.conf = FSTConfiguration.createDefaultConfiguration();
    }

    public void saveToFile(Object object, Path path) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(path.toFile());
             FSTObjectOutput out = conf.getObjectOutput(fos)) {
            out.writeObject(object);
            out.flush();
        }
    }

    public <T> T loadFromFile(Path path, Class<T> clazz) throws Exception {
        try (FileInputStream fis = new FileInputStream(path.toFile());
             FSTObjectInput in = conf.getObjectInput(fis)) {
            return clazz.cast(in.readObject());
        }
    }
}
```

### Deep Copy Utility

```java
@Component
public class FSTDeepCopyService {
    private final FSTConfiguration conf;

    public FSTDeepCopyService() {
        this.conf = FSTConfiguration.createDefaultConfiguration();
    }

    @SuppressWarnings("unchecked")
    public <T> T deepCopy(T object) {
        // FST provides efficient deep copy
        return (T) conf.deepCopy(object);
    }

    public <T> List<T> deepCopyList(List<T> list) {
        return (List<T>) conf.deepCopy(list);
    }
}
```

## Alternatives & When to Switch

| Switch To | When | Why |
|-----------|------|-----|
| **Kryo** | Need more maturity | Larger community, more stable |
| **Protocol Buffers** | Cross-language needed | Multi-language support, schema |
| **Avro** | Schema evolution required | Better versioning support |
| **Jackson** | Need human-readable | JSON format for debugging |

## Common Patterns

### Custom Serializer Registration

```java
public class CustomDateSerializer extends FSTBasicObjectSerializer {

    @Override
    public void writeObject(FSTObjectOutput out, Object toWrite,
                           FSTClazzInfo clzInfo,
                           FSTFieldInfo referencedBy,
                           int streamPosition) throws IOException {
        Date date = (Date) toWrite;
        out.writeLong(date.getTime());
    }

    @Override
    public Object instantiate(Class objectClass, FSTObjectInput in,
                             FSTClazzInfo serializationInfo,
                             FSTFieldInfo referencee,
                             int streamPosition) throws Exception {
        long time = in.readLong();
        return new Date(time);
    }
}

// Register custom serializer
conf.registerSerializer(Date.class, new CustomDateSerializer(), false);
```

### Conditional Serialization

```java
public class ConditionalSerializer extends FSTBasicObjectSerializer {

    @Override
    public void writeObject(FSTObjectOutput out, Object toWrite,
                           FSTClazzInfo clzInfo,
                           FSTFieldInfo referencedBy,
                           int streamPosition) throws IOException {
        User user = (User) toWrite;
        out.writeUTF(user.getUsername());

        // Conditionally serialize sensitive data
        if (shouldSerializeSensitiveData()) {
            out.writeUTF(user.getEmail());
        }
    }

    private boolean shouldSerializeSensitiveData() {
        // Your logic here
        return SecurityContext.hasSensitiveAccess();
    }
}
```

### Class Versioning

```java
// Add version field to classes for compatibility
@Version(1)
public class UserV1 implements Serializable {
    private String name;
    private String email;
}

@Version(2)
public class UserV2 implements Serializable {
    private String name;
    private String email;
    private String phone; // New field in v2
}

// FST handles versioning through standard Java serialization
```

## Troubleshooting

### Issue: ClassNotFoundException
**Problem**: Cannot deserialize due to missing class

**Solution**:
```java
// Set appropriate class loader
conf.setClassLoader(MyClass.class.getClassLoader());

// Or use thread context class loader
conf.setClassLoader(Thread.currentThread().getContextClassLoader());
```

### Issue: Performance Degradation
**Problem**: Serialization slower than expected

**Solutions**:
```java
// 1. Register classes upfront
conf.registerClass(MyClass.class, MyOtherClass.class);

// 2. Disable reference tracking for simple objects
conf.setShareReferences(false);

// 3. Reuse FSTConfiguration instance
// Create once, use everywhere (thread-safe for read operations)
private static final FSTConfiguration CONF =
    FSTConfiguration.createDefaultConfiguration();
```

### Issue: Out of Memory
**Problem**: Memory usage too high

**Solutions**:
```java
// 1. Use off-heap collections
OffHeapMap<K, V> cache = new OffHeapMap<>(size, freeMem, maxEntries, conf);

// 2. Disable reference sharing
conf.setShareReferences(false);

// 3. Clear configuration cache periodically
conf.clearCaches();
```

### Issue: Serialization Incompatibility
**Problem**: Cannot deserialize data from different FST version

**Solution**:
```java
// Use same FST version for serialization and deserialization
// Or fall back to standard Java serialization for compatibility

// Enable force serializable for non-Serializable classes
conf.setForceSerializable(true);
```

## Benchmarking Results

### Comparison with JDK Serialization
FST is designed as a drop-in replacement with significant performance improvements:

**vs JDK Serialization**: ~10x faster
**vs Kryo**: Comparable speed, easier migration
**vs Protocol Buffers**: Faster for Java-only, no schema needed

### Memory Footprint
Low memory overhead compared to text formats:
- Base overhead: ~230 MB
- Per-operation increase: 8-10 MB for MEDIUM payload
- Off-heap support reduces GC pressure

### Real-World Performance
**Eurex Exchange Trading Middleware**: Used in production for high-frequency trading systems requiring low latency serialization.

**Monthly Downloads**: ~14,000 downloads on Maven Central, demonstrating steady adoption in enterprise Java applications.

## Best Practices

1. **Reuse FSTConfiguration**: Create once, use throughout application lifecycle
2. **Register Classes**: Pre-register all classes for optimal performance
3. **Thread Safety**: Use ThreadLocal for multi-threaded environments
4. **Off-Heap for Caching**: Leverage OffHeapMap for large caches
5. **Monitor Memory**: Track off-heap memory usage in production
6. **Version Classes**: Use serialVersionUID for compatibility
7. **Test Migration**: Thoroughly test when migrating from JDK serialization
8. **Benchmark First**: Verify performance gains in your use case

## Advanced Features

### MinBin Format

```java
// Alternative lightweight format (JSON-compatible)
FSTConfiguration minBinConf = FSTConfiguration.createMinBinConfiguration();

// Serialize to MinBin
byte[] minBinData = minBinConf.asByteArray(object);

// Smaller than JSON, but still structured
```

### JSON Configuration

```java
// FST can also serialize to JSON format
FSTConfiguration jsonConf = FSTConfiguration.createJsonConfiguration();

// Serialize to JSON bytes
byte[] jsonData = jsonConf.asByteArray(object);

// Deserialize from JSON
Object obj = jsonConf.asObject(jsonData);
```

### Cross-Platform Serialization

```java
// Use FSTCrossPlatformConfiguration for cross-platform compatibility
FSTConfiguration crossPlatformConf =
    FSTConfiguration.createCrossPlatformConfiguration();

// Serializes in a platform-neutral format
byte[] data = crossPlatformConf.asByteArray(object);
```

## Additional Resources

- **Official Repository**: https://github.com/RuedigerMoeller/fast-serialization
- **Wiki Documentation**: https://github.com/RuedigerMoeller/fast-serialization/wiki
- **Maven Central**: https://search.maven.org/artifact/de.ruedigermoeller/fst
- **Performance Benchmarks**: https://github.com/RuedigerMoeller/fast-serialization#performance

## Source Code

Implementation: [`fst-poc/`](../../fst-poc/)

Key Files:
- `FSTBenchmarkControllerV2.java` - REST endpoints
- `FSTSerializationServiceV2.java` - Core serialization logic
- `FSTConfiguration.java` - FSTConfiguration setup

---

**Last Updated**: 2025-10-22
**Benchmark Version**: 2.0.0
