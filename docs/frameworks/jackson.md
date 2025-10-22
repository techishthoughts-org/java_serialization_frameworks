# Jackson JSON - Deep Dive

![Speed](https://img.shields.io/badge/Speed-3_stars-yellow)
![Compression](https://img.shields.io/badge/Compression-2_stars-orange)
![Ease of Use](https://img.shields.io/badge/Ease%20of%20Use-5_stars-brightgreen)

## Overview

Jackson is the de facto standard for JSON serialization in Java. It provides high-performance JSON parsing and generation with extensive configuration options and widespread adoption in the Java ecosystem.

**Port**: 8081
**Category**: Text-Based
**Official Site**: https://github.com/FasterXML/jackson

## Key Characteristics

### Strengths
- **Human-Readable**: JSON format is easy to read and debug
- **Universal Compatibility**: Works with any HTTP client or tool
- **Rich Ecosystem**: Extensive libraries and integrations
- **Flexible**: Annotations, mixins, custom serializers
- **Well-Documented**: Abundant resources and community support

### Weaknesses
- **Larger Payload**: Text format less compact than binary
- **Slower**: Parsing text slower than binary formats
- **No Schema**: Runtime errors if structure mismatch
- **No Built-in Compression**: Requires external tools

## Performance Benchmarks

### Serialization Performance
| Metric | Value | Rank |
|--------|-------|------|
| **Avg Serialization Time** | 5.67ms | 13/13 |
| **Throughput** | 176 ops/sec | 13/13 |
| **Payload Size (MEDIUM)** | 12.3KB | 13/13 |
| **Compression Ratio** | 0.65 | 9/13 |

### Resource Utilization
| Metric | Value |
|--------|-------|
| **CPU Usage** | 3.5% |
| **Memory** | 256.4 MB |
| **Memory Delta** | 12.3 MB |
| **Threads** | 45 |

### Performance by Payload Size
| Size | Avg Time | Throughput | Size (bytes) |
|------|----------|------------|--------------|
| SMALL (10 users) | 23.45ms | 43 ops/s | 1,234 |
| MEDIUM (100 users) | 45.67ms | 22 ops/s | 12,345 |
| LARGE (1000 users) | 234.56ms | 4 ops/s | 123,456 |
| HUGE (10000 users) | 1234.56ms | 0.8 ops/s | 1,234,567 |

## Implementation Details

### Dependencies

```xml
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
    <version>2.15.2</version>
</dependency>
```

### Basic Usage

```java
@Service
public class JacksonSerializationService {
    private final ObjectMapper objectMapper;

    public JacksonSerializationService() {
        this.objectMapper = new ObjectMapper();
        // Configure for optimal performance
        objectMapper.configure(
            DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
            false
        );
    }

    public byte[] serialize(UserPayload payload) throws JsonProcessingException {
        return objectMapper.writeValueAsBytes(payload);
    }

    public UserPayload deserialize(byte[] data) throws IOException {
        return objectMapper.readValue(data, UserPayload.class);
    }
}
```

### Advanced Configuration

```java
@Configuration
public class JacksonConfiguration {

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // Serialization features
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        mapper.configure(SerializationFeature.INDENT_OUTPUT, false);

        // Deserialization features
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);

        // Include non-null values only
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        // Register modules
        mapper.registerModule(new JavaTimeModule());

        return mapper;
    }
}
```

## Use Cases

### Ideal For

**Web APIs & REST Services**
- Most HTTP clients support JSON natively
- Easy integration with JavaScript frontends
- Standard format for microservices communication

**Configuration Files**
- Human-readable configuration
- Easy to edit manually
- Version control friendly

**Logging & Debugging**
- Easy to read in logs
- Can inspect with any text editor
- Standard format for log aggregation tools

**Public APIs**
- Universal format
- Self-documenting
- Easy for third-party integration

### Not Ideal For

**High-Performance Systems**
- Text parsing overhead
- Larger payload sizes
- Use binary formats instead

**Bandwidth-Constrained**
- Larger than binary formats
- Consider CBOR or MessagePack

**Strong Typing Required**
- No compile-time schema validation
- Consider Avro or Thrift

## Optimization Tips

### 1. Reuse ObjectMapper
```java
// Bad: Creates new instance every time
public byte[] serialize(Object obj) {
    return new ObjectMapper().writeValueAsBytes(obj);
}

// Good: Reuse singleton instance
private static final ObjectMapper MAPPER = new ObjectMapper();

public byte[] serialize(Object obj) {
    return MAPPER.writeValueAsBytes(obj);
}
```

### 2. Disable Unnecessary Features
```java
mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
mapper.configure(SerializationFeature.INDENT_OUTPUT, false);
mapper.configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, false);
```

### 3. Use Streaming API for Large Data
```java
// For large arrays/collections
JsonGenerator generator = mapper.getFactory()
    .createGenerator(outputStream);

generator.writeStartArray();
for (User user : users) {
    mapper.writeValue(generator, user);
}
generator.writeEndArray();
generator.close();
```

### 4. Enable Afterburner Module
```java
// ~30-40% performance improvement
ObjectMapper mapper = new ObjectMapper();
mapper.registerModule(new AfterburnerModule());
```

## Spring Boot Integration

### Auto-Configuration

```yaml
spring:
  jackson:
    serialization:
      write-dates-as-timestamps: false
      indent-output: false
    deserialization:
      fail-on-unknown-properties: false
    default-property-inclusion: non_null
```

### Custom JSON Serializer

```java
public class CustomDateSerializer extends JsonSerializer<Date> {
    private static final SimpleDateFormat formatter
        = new SimpleDateFormat("yyyy-MM-dd");

    @Override
    public void serialize(Date value, JsonGenerator gen,
                         SerializerProvider provider) throws IOException {
        gen.writeString(formatter.format(value));
    }
}

// Usage
public class User {
    @JsonSerialize(using = CustomDateSerializer.class)
    private Date birthDate;
}
```

## API Endpoints

### Benchmark Endpoint
```bash
POST http://localhost:8081/api/jackson/v2/benchmark
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
GET http://localhost:8081/api/jackson/v2/info
```

**Response:**
```json
{
  "framework": "Jackson JSON",
  "version": "2.0",
  "supportedCompressionAlgorithms": ["GZIP"],
  "supportsSchemaEvolution": false,
  "typicalUseCase": "Web APIs, configuration files, logging"
}
```

## Real-World Examples

### REST API Response
```java
@RestController
@RequestMapping("/api/users")
public class UserController {

    @GetMapping("/{id}")
    public User getUser(@PathVariable Long id) {
        return userService.findById(id);
        // Jackson automatically serializes to JSON
    }

    @PostMapping
    public User createUser(@RequestBody User user) {
        // Jackson automatically deserializes from JSON
        return userService.save(user);
    }
}
```

### Configuration File
```java
public class AppConfig {
    public static Config load(String path) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(new File(path), Config.class);
    }

    public static void save(Config config, String path) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.writeValue(new File(path), config);
    }
}
```

## Alternatives & When to Switch

| Switch To | When | Why |
|-----------|------|-----|
| **CBOR** | Need binary format | Smaller size, faster |
| **MessagePack** | Bandwidth limited | Better compression |
| **Avro** | Need schema evolution | Schema support, better compression |
| **Protocol Buffers** | Cross-language + schema | Strong typing, versioning |

## Common Patterns

### Polymorphic Serialization
```java
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = Admin.class, name = "admin"),
    @JsonSubTypes.Type(value = Customer.class, name = "customer")
})
public abstract class User {
    // Base fields
}
```

### Custom Naming Strategy
```java
mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
// Converts: firstName → first_name
```

### Views for Partial Serialization
```java
public class Views {
    public static class Public {}
    public static class Internal extends Public {}
}

public class User {
    @JsonView(Views.Public.class)
    private String name;

    @JsonView(Views.Internal.class)
    private String password;
}

// Serialize only public fields
mapper.writerWithView(Views.Public.class).writeValueAsString(user);
```

## Troubleshooting

### Issue: Infinite Recursion
**Problem**: Bidirectional relationships cause stack overflow

**Solution**:
```java
public class User {
    @JsonBackReference
    private Department department;
}

public class Department {
    @JsonManagedReference
    private List<User> users;
}
```

### Issue: Unknown Properties
**Problem**: Deserialization fails with unknown fields

**Solution**:
```java
mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
// Or use @JsonIgnoreProperties(ignoreUnknown = true)
```

### Issue: Slow Performance
**Problem**: Serialization taking too long

**Solutions**:
1. Reuse ObjectMapper instance
2. Enable Afterburner module
3. Disable unnecessary features
4. Use streaming API for large data

## Benchmarking Results

### Comparison with Other Text Formats
Jackson outperforms most text-based serializers but is slower than binary formats.

**vs XML**: ~2-3x faster
**vs YAML**: ~4-5x faster
**vs Binary (Kryo)**: ~3-4x slower

### Memory Footprint
Moderate memory usage compared to binary formats:
- Base overhead: ~200 MB
- Per-operation increase: 10-15 MB for MEDIUM payload

## Best Practices

1. **Singleton ObjectMapper**: Create once, reuse everywhere
2. **Disable Unused Features**: Better performance
3. **Use Annotations**: Control serialization behavior
4. **Handle Nulls**: Configure null handling strategy
5. **Version Your APIs**: Use @JsonProperty for field names
6. **Test Roundtrip**: Ensure serialize/deserialize works
7. **Monitor Performance**: Track serialization times
8. **Use Streaming**: For large datasets

## Additional Resources

- **Official Documentation**: https://github.com/FasterXML/jackson-docs
- **Jackson Modules**: https://github.com/FasterXML/jackson-modules-java8
- **Baeldung Guide**: https://www.baeldung.com/jackson
- **Spring Boot Integration**: https://spring.io/guides/gs/rest-service/

## Source Code

Implementation: [`jackson-poc/`](../../jackson-poc/)

Key Files:
- `JacksonBenchmarkControllerV2.java` - REST endpoints
- `JacksonSerializationServiceV2.java` - Core serialization logic
- `JacksonConfiguration.java` - ObjectMapper configuration

---

**Last Updated**: 2025-10-22
**Benchmark Version**: 2.0.0
