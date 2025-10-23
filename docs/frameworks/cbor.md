# CBOR (Concise Binary Object Representation) - Deep Dive

![Speed](https://img.shields.io/badge/Speed-3_stars-yellow)
![Compression](https://img.shields.io/badge/Compression-4_stars-brightgreen)
![Ease of Use](https://img.shields.io/badge/Ease%20of%20Use-4_stars-brightgreen)

## Overview

CBOR (Concise Binary Object Representation) is a binary data serialization format defined in RFC 8949. It combines the simplicity and flexibility of JSON with the efficiency of binary encoding, making it ideal for IoT devices, constrained environments, and scenarios requiring both human-debuggable structure and network efficiency.

**Port**: 8093
**Category**: Binary Schema-less
**Official Site**: https://cbor.io/

## Key Characteristics

### Strengths
- **Extremely Small Code Size**: Minimal implementation footprint for embedded systems
- **Compact Message Size**: Binary encoding reduces payload size significantly
- **Extensibility**: Tag system allows custom data types without version negotiation
- **JSON Compatibility**: Structure mirrors JSON for easy migration
- **Native Binary Data**: First-class support for byte arrays and binary data
- **Standardized Format**: IETF RFC 8949 ensures interoperability

### Weaknesses
- **Limited Tooling**: Fewer development tools compared to JSON
- **Not Human-Readable**: Binary format requires tools for inspection
- **Schema-less**: No built-in schema validation
- **Less Compact Than Some**: MessagePack achieves slightly better compression
- **Adoption**: Smaller ecosystem compared to Protocol Buffers or JSON

## Performance Benchmarks

### Serialization Performance
| Metric | Value | Rank |
|--------|-------|------|
| **Avg Serialization Time** | ~4.5ms | 9/13 |
| **Throughput** | ~220 ops/sec | 9/13 |
| **Payload Size (MEDIUM)** | 5.5KB | 5/13 |
| **Compression Ratio** | 0.45 | 5/13 |

### Resource Utilization
| Metric | Value |
|--------|-------|
| **CPU Usage** | 3.2% |
| **Memory** | 245 MB |
| **Memory Delta** | 10 MB |
| **Threads** | 42 |

### Performance by Payload Size
| Size | Avg Time | Throughput | Size (bytes) |
|------|----------|------------|--------------|
| SMALL (10 users) | 18ms | 55 ops/s | 550 |
| MEDIUM (100 users) | 35ms | 28 ops/s | 5,500 |
| LARGE (1000 users) | 180ms | 5.5 ops/s | 55,000 |
| HUGE (10000 users) | 950ms | 1 ops/s | 550,000 |

## Implementation Details

### Dependencies

```xml
<dependency>
    <groupId>com.fasterxml.jackson.dataformat</groupId>
    <artifactId>jackson-dataformat-cbor</artifactId>
    <version>2.15.2</version>
</dependency>
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
    <version>2.15.2</version>
</dependency>
```

### Basic Usage

```java
@Service
public class CborSerializationService {
    private final ObjectMapper objectMapper;

    public CborSerializationService() {
        // Use CBORFactory for binary encoding
        CBORFactory cborFactory = new CBORFactory();
        this.objectMapper = new ObjectMapper(cborFactory);

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
public class CborConfiguration {

    @Bean
    public ObjectMapper cborObjectMapper() {
        // Create CBOR factory with custom settings
        CBORFactory cborFactory = CBORFactory.builder()
            .enable(CBORGenerator.Feature.WRITE_MINIMAL_INTS)
            .enable(CBORGenerator.Feature.STRINGREF)
            .build();

        ObjectMapper mapper = new ObjectMapper(cborFactory);

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

### IoT-Optimized Configuration

```java
@Configuration
public class IoTCborConfiguration {

    @Bean
    public ObjectMapper iotCborMapper() {
        CBORFactory factory = CBORFactory.builder()
            // Minimize integer encoding size
            .enable(CBORGenerator.Feature.WRITE_MINIMAL_INTS)
            // Enable string deduplication for repeated strings
            .enable(CBORGenerator.Feature.STRINGREF)
            // Use type prefixes for better efficiency
            .enable(CBORGenerator.Feature.WRITE_TYPE_HEADER)
            .build();

        ObjectMapper mapper = new ObjectMapper(factory);

        // Optimize for constrained devices
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, true);

        return mapper;
    }
}
```

## Use Cases

### Ideal For

**IoT & Embedded Systems**
- Extremely small code footprint fits in constrained devices
- Binary efficiency reduces network bandwidth
- Native support for sensor data (binary, timestamps)
- Standardized format ensures interoperability

**Microservices Communication**
- Smaller payloads than JSON reduce network traffic
- Compatible with REST APIs (binary body)
- Easy migration from JSON (same structure)
- No schema compilation required

**Mobile Applications**
- Reduced data transfer saves battery
- Smaller payloads improve user experience
- Native binary data support for media
- Works well with cellular connections

**Edge Computing**
- Efficient data aggregation from sensors
- Minimal processing overhead
- Standardized format for heterogeneous devices
- Good balance of efficiency and flexibility

### Not Ideal For

**Web Browser Communication**
- Limited JavaScript support
- JSON more standard for web APIs
- Developer tools less mature

**Schema Evolution Requirements**
- No built-in schema versioning
- Consider Avro or Protocol Buffers instead
- Runtime errors on structure mismatch

**Maximum Compression Needed**
- MessagePack slightly more compact
- Protocol Buffers better with schema
- Consider if bandwidth absolutely critical

**Human Debugging Priority**
- Binary format not human-readable
- Use JSON if readability critical
- Requires hex editors or specialized tools

## Optimization Tips

### 1. Enable CBOR-Specific Features

```java
// Minimize integer encoding
CBORFactory factory = CBORFactory.builder()
    .enable(CBORGenerator.Feature.WRITE_MINIMAL_INTS)
    .build();

// This encodes 100 as 1 byte instead of 5
```

### 2. Use String References

```java
// Enable string deduplication
CBORFactory factory = CBORFactory.builder()
    .enable(CBORGenerator.Feature.STRINGREF)
    .build();

// Repeated strings stored once, referenced later
// Saves significant space for repeated field names
```

### 3. Reuse ObjectMapper

```java
// Bad: Creates new instance every time
public byte[] serialize(Object obj) throws IOException {
    return new ObjectMapper(new CBORFactory()).writeValueAsBytes(obj);
}

// Good: Reuse singleton instance
private static final ObjectMapper CBOR_MAPPER =
    new ObjectMapper(new CBORFactory());

public byte[] serialize(Object obj) throws IOException {
    return CBOR_MAPPER.writeValueAsBytes(obj);
}
```

### 4. Streaming for Large Data

```java
// For large arrays/collections in IoT scenarios
CBORFactory factory = new CBORFactory();
JsonGenerator generator = factory.createGenerator(outputStream);

generator.writeStartArray();
for (SensorReading reading : readings) {
    mapper.writeValue(generator, reading);
}
generator.writeEndArray();
generator.close();
```

### 5. Optimize for IoT Devices

```java
// Use minimal configuration for embedded systems
ObjectMapper mapper = new ObjectMapper(
    CBORFactory.builder()
        .enable(CBORGenerator.Feature.WRITE_MINIMAL_INTS)
        .enable(CBORGenerator.Feature.STRINGREF)
        .build()
);

// Exclude nulls to reduce payload
mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

// Use timestamps for dates (smaller than ISO strings)
mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, true);
```

## Spring Boot Integration

### Auto-Configuration

```java
@Configuration
public class CborAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ObjectMapper cborObjectMapper() {
        return new ObjectMapper(new CBORFactory());
    }

    @Bean
    public HttpMessageConverter<Object> cborHttpMessageConverter(
            ObjectMapper cborObjectMapper) {
        return new MappingJackson2HttpMessageConverter(cborObjectMapper);
    }
}
```

### Custom Media Type Support

```java
@RestController
@RequestMapping("/api/sensors")
public class SensorController {

    @PostMapping(
        consumes = "application/cbor",
        produces = "application/cbor"
    )
    public SensorResponse processSensorData(@RequestBody SensorData data) {
        // CBOR automatically handled by Jackson
        return sensorService.process(data);
    }
}
```

### Configuration Properties

```yaml
# Custom CBOR configuration
cbor:
  enabled: true
  minimal-ints: true
  string-refs: true

spring:
  http:
    converters:
      preferred-json-mapper: cbor
```

## API Endpoints

### Benchmark Endpoint
```bash
POST http://localhost:8093/api/cbor/v2/benchmark
Content-Type: application/cbor

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
GET http://localhost:8093/api/cbor/v2/info
```

**Response:**
```json
{
  "framework": "CBOR",
  "version": "2.0",
  "supportedCompressionAlgorithms": ["GZIP"],
  "supportsSchemaEvolution": false,
  "typicalUseCase": "IoT devices, microservices, mobile apps, edge computing"
}
```

## Real-World Examples

### IoT Sensor Data Collection

```java
@Service
public class SensorDataService {
    private final ObjectMapper cborMapper;

    public SensorDataService() {
        CBORFactory factory = CBORFactory.builder()
            .enable(CBORGenerator.Feature.WRITE_MINIMAL_INTS)
            .enable(CBORGenerator.Feature.STRINGREF)
            .build();
        this.cborMapper = new ObjectMapper(factory);
    }

    public byte[] encodeSensorReading(SensorReading reading)
            throws JsonProcessingException {
        // Efficient binary encoding for IoT transmission
        return cborMapper.writeValueAsBytes(reading);
    }

    public SensorReading decodeSensorReading(byte[] data)
            throws IOException {
        return cborMapper.readValue(data, SensorReading.class);
    }
}

@Data
public class SensorReading {
    private String sensorId;
    private long timestamp;
    private double temperature;
    private double humidity;
    private byte[] rawData; // Native binary support
}
```

### Microservice Communication

```java
@Service
public class CborMicroserviceClient {
    private final RestTemplate restTemplate;
    private final ObjectMapper cborMapper;

    public CborMicroserviceClient() {
        // Configure RestTemplate with CBOR support
        this.cborMapper = new ObjectMapper(new CBORFactory());
        this.restTemplate = new RestTemplate();

        // Add CBOR message converter
        List<HttpMessageConverter<?>> converters = restTemplate.getMessageConverters();
        converters.add(new MappingJackson2HttpMessageConverter(cborMapper));
        restTemplate.setMessageConverters(converters);
    }

    public UserData fetchUserData(String userId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/cbor"));

        return restTemplate.exchange(
            "http://user-service/api/users/" + userId,
            HttpMethod.GET,
            new HttpEntity<>(headers),
            UserData.class
        ).getBody();
    }
}
```

### Edge Computing Aggregation

```java
@Service
public class EdgeAggregationService {
    private final ObjectMapper cborMapper;
    private final ByteArrayOutputStream buffer;

    public EdgeAggregationService() {
        CBORFactory factory = CBORFactory.builder()
            .enable(CBORGenerator.Feature.WRITE_MINIMAL_INTS)
            .enable(CBORGenerator.Feature.STRINGREF)
            .build();
        this.cborMapper = new ObjectMapper(factory);
        this.buffer = new ByteArrayOutputStream();
    }

    public byte[] aggregateSensorData(List<SensorReading> readings)
            throws IOException {
        buffer.reset();

        try (JsonGenerator generator = cborMapper.getFactory()
                .createGenerator(buffer)) {
            generator.writeStartObject();
            generator.writeFieldName("timestamp");
            generator.writeNumber(System.currentTimeMillis());
            generator.writeFieldName("readings");
            generator.writeStartArray();

            for (SensorReading reading : readings) {
                cborMapper.writeValue(generator, reading);
            }

            generator.writeEndArray();
            generator.writeEndObject();
        }

        return buffer.toByteArray();
    }
}
```

### Mobile App Data Sync

```java
@Service
public class MobileDataSyncService {
    private final ObjectMapper cborMapper;

    public MobileDataSyncService() {
        this.cborMapper = new ObjectMapper(new CBORFactory());
        // Optimize for mobile bandwidth
        cborMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    public byte[] prepareSyncPayload(List<DataChange> changes)
            throws JsonProcessingException {
        SyncPayload payload = SyncPayload.builder()
            .deviceId(getDeviceId())
            .timestamp(System.currentTimeMillis())
            .changes(changes)
            .build();

        // Compact binary format saves mobile data
        return cborMapper.writeValueAsBytes(payload);
    }

    public SyncPayload parseSyncPayload(byte[] data) throws IOException {
        return cborMapper.readValue(data, SyncPayload.class);
    }
}
```

## Alternatives & When to Switch

| Switch To | When | Why |
|-----------|------|-----|
| **JSON** | Web APIs, debugging | Human-readable, universal |
| **MessagePack** | Need max compression | Slightly more compact |
| **Protocol Buffers** | Schema evolution needed | Strong typing, versioning |
| **Avro** | Hadoop ecosystem | Better compression with schema |
| **FlatBuffers** | Zero-copy needed | Direct memory access |

## Common Patterns

### Working with Binary Data

```java
@Data
public class ImageData {
    private String imageId;
    private String format;
    private int width;
    private int height;

    // CBOR natively handles byte arrays efficiently
    private byte[] imageBytes;
    private byte[] thumbnail;
}

// Serialization preserves binary data efficiently
byte[] cbor = cborMapper.writeValueAsBytes(imageData);
ImageData decoded = cborMapper.readValue(cbor, ImageData.class);
```

### Using CBOR Tags for Custom Types

```java
// CBOR supports extensible tags for custom data types
public class TimestampSerializer extends JsonSerializer<Instant> {
    @Override
    public void serialize(Instant value, JsonGenerator gen,
                         SerializerProvider provider) throws IOException {
        // CBOR tag 1 indicates Unix timestamp
        gen.writeNumber(value.getEpochSecond());
    }
}

@Data
public class Event {
    private String eventId;

    @JsonSerialize(using = TimestampSerializer.class)
    private Instant timestamp;
}
```

### Efficient Collection Encoding

```java
public class BatchProcessor {
    private final ObjectMapper cborMapper = new ObjectMapper(
        CBORFactory.builder()
            .enable(CBORGenerator.Feature.STRINGREF)
            .build()
    );

    public byte[] encodeReadings(List<SensorReading> readings)
            throws IOException {
        // STRINGREF deduplicates field names across array elements
        // Dramatically reduces size for large collections
        return cborMapper.writeValueAsBytes(readings);
    }
}
```

### Hybrid JSON/CBOR API

```java
@RestController
@RequestMapping("/api/data")
public class HybridController {

    @PostMapping(
        consumes = {"application/json", "application/cbor"},
        produces = {"application/json", "application/cbor"}
    )
    public ResponseEntity<?> processData(
            @RequestBody DataPayload payload,
            @RequestHeader("Content-Type") String contentType) {

        // Same code handles both JSON and CBOR
        DataResult result = dataService.process(payload);

        // Response format matches request format
        return ResponseEntity.ok(result);
    }
}
```

## Troubleshooting

### Issue: Payload Larger Than Expected
**Problem**: CBOR output larger than anticipated

**Solution**:
```java
// Enable compression features
CBORFactory factory = CBORFactory.builder()
    .enable(CBORGenerator.Feature.WRITE_MINIMAL_INTS)
    .enable(CBORGenerator.Feature.STRINGREF)
    .build();

// Exclude nulls
mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

// Use numeric timestamps instead of ISO strings
mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, true);
```

### Issue: Deserialization Fails from Other Languages
**Problem**: CBOR generated by other libraries won't deserialize

**Solution**:
```java
// Ensure lenient parsing
mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
mapper.configure(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT, true);

// Check CBOR tag compatibility
CBORFactory factory = CBORFactory.builder()
    .enable(CBORParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES)
    .build();
```

### Issue: Cannot Debug Binary Data
**Problem**: Cannot inspect CBOR payloads

**Solution**:
```java
// Convert CBOR to JSON for debugging
public String cborToJson(byte[] cborData) throws IOException {
    Object obj = cborMapper.readValue(cborData, Object.class);
    ObjectMapper jsonMapper = new ObjectMapper();
    return jsonMapper.writerWithDefaultPrettyPrinter()
        .writeValueAsString(obj);
}

// Or use online tools: http://cbor.me/
```

### Issue: Out of Memory on Large Payloads
**Problem**: Large sensor data streams cause memory issues

**Solution**:
```java
// Use streaming API
public void processLargeStream(InputStream input, OutputStream output)
        throws IOException {
    JsonParser parser = cborMapper.getFactory().createParser(input);
    JsonGenerator generator = cborMapper.getFactory().createGenerator(output);

    // Process incrementally without loading entire payload
    while (parser.nextToken() != null) {
        generator.copyCurrentEvent(parser);
    }

    parser.close();
    generator.close();
}
```

## Benchmarking Results

### Comparison with Similar Formats

**CBOR Position**: Mid-tier performance, good compression

**vs JSON**: ~1.3x faster, ~35% smaller
**vs MessagePack**: Similar speed, ~5% larger
**vs Protocol Buffers**: ~1.5x slower, ~10% larger
**vs Kryo**: ~1.8x slower, more portable

### Size Comparison (100 user objects)
- JSON: 12.3 KB
- CBOR: 5.5 KB (45% of JSON)
- MessagePack: 5.2 KB
- Protocol Buffers: 4.8 KB
- Avro: 4.5 KB

### IoT Scenario (1000 sensor readings)
- Transmission time (3G): ~2.5 seconds
- Payload size: 55 KB
- Battery impact: Low
- Processing time: ~180ms

## Best Practices

1. **Enable Compression Features**: Use WRITE_MINIMAL_INTS and STRINGREF
2. **Exclude Nulls**: Reduces payload size significantly
3. **Reuse ObjectMapper**: Thread-safe singleton
4. **Use Streaming**: For large datasets or constrained memory
5. **Binary Data**: Leverage native byte array support
6. **Timestamps**: Use numeric format for smaller size
7. **Test Interoperability**: Verify compatibility with other languages
8. **Monitor Payload Size**: Compare with JSON baseline
9. **Profile Memory**: Important for embedded systems
10. **Document Binary Endpoints**: Not obvious from API alone

## CBOR vs JSON Migration

### When to Migrate

**Good Candidates**:
- Mobile apps with high data usage
- IoT devices with bandwidth constraints
- Microservices with high traffic
- APIs serving binary data

**Poor Candidates**:
- Public web APIs (ecosystem)
- Browser-based apps
- APIs requiring human inspection
- Low-traffic services

### Migration Strategy

```java
// Phase 1: Support both formats
@RestController
public class HybridController {

    @PostMapping(
        path = "/api/data",
        consumes = {"application/json", "application/cbor"},
        produces = {"application/json", "application/cbor"}
    )
    public ResponseEntity<?> handleData(@RequestBody DataPayload payload) {
        // Works with both formats
        return ResponseEntity.ok(processData(payload));
    }
}

// Phase 2: Measure adoption
@Component
public class FormatMetrics {
    private final Counter jsonRequests = Counter.build()
        .name("api_requests_json").register();
    private final Counter cborRequests = Counter.build()
        .name("api_requests_cbor").register();
}

// Phase 3: Deprecate JSON once CBOR adoption high
```

## Additional Resources

- **Official Specification**: https://www.rfc-editor.org/rfc/rfc8949.html
- **CBOR Website**: https://cbor.io/
- **Jackson CBOR Module**: https://github.com/FasterXML/jackson-dataformats-binary
- **Online CBOR Debugger**: http://cbor.me/
- **CBOR Implementation List**: https://cbor.io/impls.html
- **IoT Best Practices**: https://www.cbor.io/spec.html

## Source Code

Implementation: [`cbor-poc/`](../../cbor-poc/)

Key Files:
- `CborBenchmarkControllerV2.java` - REST endpoints
- `CborSerializationServiceV2.java` - Core serialization logic
- `CborConfiguration.java` - CBORFactory configuration

---

**Last Updated**: 2025-10-22
**Benchmark Version**: 2.0.0
**RFC**: 8949
