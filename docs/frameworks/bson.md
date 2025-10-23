# BSON (Binary JSON) - Deep Dive

![Speed](https://img.shields.io/badge/Speed-3_stars-yellow)
![Compression](https://img.shields.io/badge/Compression-3_stars-yellow)
![Ease of Use](https://img.shields.io/badge/Ease%20of%20Use-4_stars-green)

## Overview

BSON (Binary JSON) is a binary-encoded serialization format for JSON-like documents. Originally developed as MongoDB's internal data representation format, BSON extends JSON's data model with additional types like Date and BinData while maintaining efficient traversability and a lightweight structure.

**Port**: 8094
**Category**: Binary Schema-less
**Official Site**: http://bsonspec.org/

## Key Characteristics

### Strengths
- **Schema-less Flexibility**: Like JSON but with binary efficiency
- **Rich Type System**: Native support for Date, Binary, ObjectId, Decimal128
- **Traversable**: Efficient document traversal without full parsing
- **Lightweight**: Compact binary representation
- **Easy Integration**: Works seamlessly with MongoDB ecosystem
- **No Schema Required**: No compilation or code generation needed

### Weaknesses
- **Includes Field Names**: Larger than schema-based formats like Protocol Buffers
- **MongoDB-Specific**: Primarily designed for MongoDB use cases
- **Limited Ecosystem**: Fewer libraries compared to JSON or Protocol Buffers
- **No Built-in Compression**: Requires external compression
- **Not Human-Readable**: Binary format requires tools to inspect

## Performance Benchmarks

### Serialization Performance
| Metric | Value | Rank |
|--------|-------|------|
| **Avg Serialization Time** | 3.52ms | 7/13 |
| **Throughput** | 286 ops/sec | 7/13 |
| **Payload Size (MEDIUM)** | 4.2KB | 6/13 |
| **Compression Ratio** | 0.42 | 6/13 |

### Resource Utilization
| Metric | Value |
|--------|-------|
| **CPU Usage** | 2.8% |
| **Memory** | 245.6 MB |
| **Memory Delta** | 8.4 MB |
| **Threads** | 42 |

### Performance by Payload Size
| Size | Avg Time | Throughput | Size (bytes) |
|------|----------|------------|--------------|
| SMALL (10 users) | 14.23ms | 70 ops/s | 456 |
| MEDIUM (100 users) | 28.45ms | 35 ops/s | 4,321 |
| LARGE (1000 users) | 142.34ms | 7 ops/s | 43,210 |
| HUGE (10000 users) | 712.45ms | 1.4 ops/s | 432,109 |

## Implementation Details

### Dependencies

```xml
<dependency>
    <groupId>org.mongodb</groupId>
    <artifactId>bson</artifactId>
    <version>4.11.1</version>
</dependency>
```

### Basic Usage

```java
@Service
public class BsonSerializationService {
    private final Codec<UserPayload> codec;
    private final CodecRegistry codecRegistry;

    public BsonSerializationService() {
        // Create codec registry with POJO support
        CodecProvider pojoCodecProvider = PojoCodecProvider.builder()
            .automatic(true)
            .build();

        this.codecRegistry = CodecRegistries.fromRegistries(
            MongoClientSettings.getDefaultCodecRegistry(),
            CodecRegistries.fromProviders(pojoCodecProvider)
        );

        this.codec = codecRegistry.get(UserPayload.class);
    }

    public byte[] serialize(UserPayload payload) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        BsonBinaryWriter writer = new BsonBinaryWriter(outputStream);

        try {
            codec.encode(writer, payload, EncoderContext.builder().build());
            return outputStream.toByteArray();
        } finally {
            writer.close();
        }
    }

    public UserPayload deserialize(byte[] data) {
        ByteBuffer buffer = ByteBuffer.wrap(data);
        BsonBinaryReader reader = new BsonBinaryReader(buffer);

        try {
            return codec.decode(reader, DecoderContext.builder().build());
        } finally {
            reader.close();
        }
    }
}
```

### Advanced Configuration

```java
@Configuration
public class BsonConfiguration {

    @Bean
    public CodecRegistry codecRegistry() {
        // Custom POJO codec provider with conventions
        Convention convention = Convention.builder()
            .discriminatorKey("_type")
            .discriminatorEnabled(true)
            .build();

        CodecProvider pojoCodecProvider = PojoCodecProvider.builder()
            .automatic(true)
            .conventions(Arrays.asList(convention))
            .build();

        // Combine with default codecs
        return CodecRegistries.fromRegistries(
            MongoClientSettings.getDefaultCodecRegistry(),
            CodecRegistries.fromProviders(pojoCodecProvider)
        );
    }

    @Bean
    public BsonSerializationService bsonService(CodecRegistry registry) {
        return new BsonSerializationService(registry);
    }
}
```

### Working with BsonDocument

```java
// Manual document creation
BsonDocument document = new BsonDocument()
    .append("name", new BsonString("John Doe"))
    .append("age", new BsonInt32(30))
    .append("email", new BsonString("john@example.com"))
    .append("createdAt", new BsonDateTime(System.currentTimeMillis()))
    .append("active", new BsonBoolean(true));

// Serialize to bytes
byte[] bytes = document.toByteArray();

// Deserialize from bytes
BsonDocument parsed = new BsonDocument(ByteBuffer.wrap(bytes));

// Access values
String name = parsed.getString("name").getValue();
int age = parsed.getInt32("age").getValue();
```

## Use Cases

### Ideal For

**MongoDB Integration**
- Native data format for MongoDB
- Zero-overhead when working with MongoDB
- Efficient storage and retrieval
- Direct document manipulation

**Document-Based Storage**
- Flexible schema evolution
- Nested document structures
- Rich type support for dates and binaries
- Fast document traversal

**Microservices with MongoDB**
- Consistent format across services
- Native MongoDB driver support
- Schema-less flexibility for rapid development
- Efficient binary transmission

**Event Sourcing**
- Rich type system for events
- Efficient binary storage
- Fast serialization for event streams
- Flexible schema for evolving events

### Not Ideal For

**Cross-Language Systems**
- Limited library support outside MongoDB ecosystem
- Consider Protocol Buffers or Avro
- Not widely adopted outside MongoDB

**Maximum Compression**
- Field names included in payload
- Use Protocol Buffers or FlatBuffers
- Consider schema-based formats

**Human Debugging**
- Binary format not readable
- Use JSON for development
- Requires tools to inspect

**Public APIs**
- Not standard format for REST APIs
- Use JSON or Protocol Buffers
- Limited client support

## Optimization Tips

### 1. Reuse Codecs and Registries
```java
// Bad: Creates new registry every time
public byte[] serialize(UserPayload payload) {
    CodecRegistry registry = CodecRegistries.fromProviders(
        PojoCodecProvider.builder().automatic(true).build()
    );
    // ... serialize
}

// Good: Reuse singleton registry
private static final CodecRegistry REGISTRY = CodecRegistries.fromProviders(
    PojoCodecProvider.builder().automatic(true).build()
);

public byte[] serialize(UserPayload payload) {
    // Use REGISTRY
}
```

### 2. Use Binary Writers for Direct Control
```java
// For maximum performance with known structure
public void serializeOptimized(User user, BsonBinaryWriter writer) {
    writer.writeStartDocument();
    writer.writeName("name");
    writer.writeString(user.getName());
    writer.writeName("age");
    writer.writeInt32(user.getAge());
    writer.writeName("email");
    writer.writeString(user.getEmail());
    writer.writeEndDocument();
}
```

### 3. Pool ByteArrayOutputStream
```java
// Reuse byte buffers for better performance
private static final ThreadLocal<ByteArrayOutputStream> BUFFER_POOL =
    ThreadLocal.withInitial(() -> new ByteArrayOutputStream(4096));

public byte[] serialize(UserPayload payload) {
    ByteArrayOutputStream buffer = BUFFER_POOL.get();
    buffer.reset();

    BsonBinaryWriter writer = new BsonBinaryWriter(buffer);
    try {
        codec.encode(writer, payload, EncoderContext.builder().build());
        return buffer.toByteArray();
    } finally {
        writer.close();
    }
}
```

### 4. Use Appropriate Data Types
```java
// Optimize for data types
public class OptimizedDocument {
    // Use appropriate BSON types
    @BsonId
    private ObjectId id;  // More efficient than String

    @BsonDateTime
    private Date timestamp;  // Native BSON type

    @BsonInt32
    private int count;  // Smaller than Int64

    @BsonBinary
    private byte[] data;  // Efficient binary storage
}
```

## Spring Boot Integration

### Configuration

```java
@Configuration
public class MongoConfiguration {

    @Bean
    public MongoClient mongoClient() {
        return MongoClients.create("mongodb://localhost:27017");
    }

    @Bean
    public MongoTemplate mongoTemplate(MongoClient mongoClient) {
        return new MongoTemplate(mongoClient, "bson_benchmark");
    }

    @Bean
    public CodecRegistry codecRegistry() {
        CodecProvider pojoCodecProvider = PojoCodecProvider.builder()
            .automatic(true)
            .build();

        return CodecRegistries.fromRegistries(
            MongoClientSettings.getDefaultCodecRegistry(),
            CodecRegistries.fromProviders(pojoCodecProvider)
        );
    }
}
```

### Custom Codec Example

```java
public class UserCodec implements Codec<User> {

    @Override
    public void encode(BsonWriter writer, User user, EncoderContext context) {
        writer.writeStartDocument();
        writer.writeName("_id");
        writer.writeString(user.getId());
        writer.writeName("name");
        writer.writeString(user.getName());
        writer.writeName("email");
        writer.writeString(user.getEmail());
        writer.writeName("createdAt");
        writer.writeDateTime(user.getCreatedAt().getTime());
        writer.writeEndDocument();
    }

    @Override
    public User decode(BsonReader reader, DecoderContext context) {
        User user = new User();
        reader.readStartDocument();

        while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
            String fieldName = reader.readName();

            switch (fieldName) {
                case "_id":
                    user.setId(reader.readString());
                    break;
                case "name":
                    user.setName(reader.readString());
                    break;
                case "email":
                    user.setEmail(reader.readString());
                    break;
                case "createdAt":
                    user.setCreatedAt(new Date(reader.readDateTime()));
                    break;
                default:
                    reader.skipValue();
            }
        }

        reader.readEndDocument();
        return user;
    }

    @Override
    public Class<User> getEncoderClass() {
        return User.class;
    }
}
```

## API Endpoints

### Benchmark Endpoint
```bash
POST http://localhost:8094/api/bson/v2/benchmark
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
GET http://localhost:8094/api/bson/v2/info
```

**Response:**
```json
{
  "framework": "BSON",
  "version": "2.0",
  "supportedCompressionAlgorithms": ["GZIP"],
  "supportsSchemaEvolution": true,
  "typicalUseCase": "MongoDB data representation, document storage"
}
```

## Real-World Examples

### MongoDB Repository Integration

```java
@Repository
public class UserRepository {
    private final MongoCollection<User> collection;

    public UserRepository(MongoDatabase database) {
        this.collection = database.getCollection("users", User.class);
    }

    public void save(User user) {
        // BSON serialization happens automatically
        collection.insertOne(user);
    }

    public User findById(String id) {
        // BSON deserialization happens automatically
        return collection.find(eq("_id", id)).first();
    }

    public List<User> findByAge(int minAge) {
        // Complex queries with BSON
        return collection.find(gte("age", minAge))
            .into(new ArrayList<>());
    }
}
```

### Event Store with BSON

```java
@Service
public class EventStore {
    private final CodecRegistry codecRegistry;

    public void storeEvent(Event event) throws IOException {
        Codec<Event> codec = codecRegistry.get(Event.class);

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        BsonBinaryWriter writer = new BsonBinaryWriter(buffer);

        try {
            codec.encode(writer, event, EncoderContext.builder().build());
            byte[] bsonData = buffer.toByteArray();

            // Store in file, database, or message queue
            persistEvent(event.getId(), bsonData);
        } finally {
            writer.close();
        }
    }

    public Event loadEvent(String eventId) throws IOException {
        byte[] bsonData = retrieveEvent(eventId);

        Codec<Event> codec = codecRegistry.get(Event.class);
        BsonBinaryReader reader = new BsonBinaryReader(ByteBuffer.wrap(bsonData));

        try {
            return codec.decode(reader, DecoderContext.builder().build());
        } finally {
            reader.close();
        }
    }
}
```

### Custom Document Builder

```java
public class BsonDocumentBuilder {

    public static BsonDocument buildUserDocument(User user) {
        return new BsonDocument()
            .append("_id", new BsonObjectId(new ObjectId()))
            .append("name", new BsonString(user.getName()))
            .append("email", new BsonString(user.getEmail()))
            .append("age", new BsonInt32(user.getAge()))
            .append("roles", toBsonArray(user.getRoles()))
            .append("metadata", toBsonDocument(user.getMetadata()))
            .append("createdAt", new BsonDateTime(System.currentTimeMillis()))
            .append("active", new BsonBoolean(true));
    }

    private static BsonArray toBsonArray(List<String> list) {
        BsonArray array = new BsonArray();
        list.forEach(item -> array.add(new BsonString(item)));
        return array;
    }

    private static BsonDocument toBsonDocument(Map<String, Object> map) {
        BsonDocument doc = new BsonDocument();
        map.forEach((key, value) -> {
            if (value instanceof String) {
                doc.append(key, new BsonString((String) value));
            } else if (value instanceof Integer) {
                doc.append(key, new BsonInt32((Integer) value));
            }
            // Add more type handlers as needed
        });
        return doc;
    }
}
```

## Alternatives & When to Switch

| Switch To | When | Why |
|-----------|------|-----|
| **Protocol Buffers** | Need smaller payloads | No field names, better compression |
| **Avro** | Need schema evolution | Better versioning, more compact |
| **MessagePack** | Similar needs, better ecosystem | More language support, smaller size |
| **JSON** | Need human readability | Debugging, development, browser APIs |
| **FlatBuffers** | Zero-copy access needed | No parsing required, ultra-fast |

## Common Patterns

### Polymorphic Documents

```java
@BsonDiscriminator(key = "_type", value = "base")
public abstract class BaseEntity {
    @BsonId
    private ObjectId id;

    @BsonProperty("createdAt")
    private Date createdAt;
}

@BsonDiscriminator("user")
public class User extends BaseEntity {
    private String name;
    private String email;
}

@BsonDiscriminator("admin")
public class Admin extends BaseEntity {
    private String name;
    private List<String> permissions;
}
```

### Embedded Documents

```java
public class Order {
    @BsonId
    private ObjectId id;

    private Customer customer;  // Embedded document

    private List<LineItem> items;  // Array of embedded documents

    @BsonDateTime
    private Date orderDate;

    private Address shippingAddress;  // Nested embedded document
}
```

### Custom Field Naming

```java
public class User {
    @BsonId
    private ObjectId id;

    @BsonProperty("full_name")
    private String fullName;

    @BsonProperty("email_address")
    private String email;

    @BsonIgnore
    private String temporaryField;  // Not serialized
}
```

### Handling Null Values

```java
@Configuration
public class BsonNullHandling {

    public CodecRegistry nullAwareRegistry() {
        Convention nullConvention = Convention.builder()
            .objectIdGenerators(new ObjectIdGenerator())
            .discriminatorEnabled(false)
            .build();

        CodecProvider provider = PojoCodecProvider.builder()
            .conventions(Arrays.asList(nullConvention))
            .automatic(true)
            .build();

        return CodecRegistries.fromProviders(provider);
    }
}
```

## Troubleshooting

### Issue: Codec Not Found
**Problem**: `CodecConfigurationException: Can't find a codec for class X`

**Solution**:
```java
// Register POJO codec provider
CodecProvider pojoCodecProvider = PojoCodecProvider.builder()
    .automatic(true)
    .register("com.example.model")  // Specify package
    .build();

CodecRegistry registry = CodecRegistries.fromRegistries(
    MongoClientSettings.getDefaultCodecRegistry(),
    CodecRegistries.fromProviders(pojoCodecProvider)
);
```

### Issue: BsonInvalidOperationException
**Problem**: `BsonInvalidOperationException: Invalid state`

**Solution**:
```java
// Ensure proper document structure
writer.writeStartDocument();
try {
    writer.writeName("field1");
    writer.writeString("value1");
    // All fields...
} finally {
    writer.writeEndDocument();  // Always close document
}
```

### Issue: Memory Overhead
**Problem**: High memory usage with large documents

**Solutions**:
1. Use streaming API for large arrays
2. Pool ByteArrayOutputStream instances
3. Process in batches
4. Use projection to load only needed fields

### Issue: Circular References
**Problem**: Stack overflow with circular object references

**Solution**:
```java
// Use @BsonIgnore on one side of the relationship
public class User {
    private List<Order> orders;
}

public class Order {
    @BsonIgnore  // Prevent circular serialization
    private User user;

    @BsonId
    private ObjectId userId;  // Store reference instead
}
```

## Benchmarking Results

### Comparison with Other Formats
BSON performs in the middle of the pack for speed and compression:

**vs JSON**: ~1.6x faster, ~2x smaller
**vs MessagePack**: ~0.9x speed, ~1.1x larger
**vs Protocol Buffers**: ~0.5x speed, ~2x larger
**vs Avro**: ~0.8x speed, ~1.2x larger

### Memory Footprint
Moderate memory usage:
- Base overhead: ~240 MB
- Per-operation increase: 8-10 MB for MEDIUM payload
- Lower than JSON, higher than schema-based formats

### Schema Evolution
While schema-less, BSON supports evolution:
- Add new fields without breaking old readers
- Optional fields handled naturally
- No versioning required
- Compatible with MongoDB schema evolution

## Best Practices

1. **Reuse Codec Registry**: Create once, use throughout application
2. **Use Appropriate Types**: Leverage BSON's rich type system (Date, ObjectId, Binary)
3. **Index Strategy**: Structure documents for query patterns
4. **Avoid Deep Nesting**: Keep document structure flat when possible
5. **Field Name Length**: Use shorter field names to reduce payload size
6. **Binary for Large Data**: Use BsonBinary for large byte arrays
7. **Pool Resources**: Reuse writers and buffers
8. **Test Roundtrip**: Ensure serialization/deserialization preserves data
9. **Monitor Size**: Track document sizes to avoid 16MB limit
10. **Use Projections**: Load only needed fields to reduce overhead

## Additional Resources

- **Official BSON Spec**: http://bsonspec.org/
- **MongoDB BSON Guide**: https://www.mongodb.com/docs/drivers/java/sync/current/fundamentals/data-formats/documents/
- **Java BSON Library**: https://mongodb.github.io/mongo-java-driver/
- **BSON Types Reference**: https://www.mongodb.com/docs/manual/reference/bson-types/
- **Performance Best Practices**: https://www.mongodb.com/docs/manual/core/data-model-operations/

## Source Code

Implementation: [`bson-poc/`](../../bson-poc/)

Key Files:
- `BsonBenchmarkControllerV2.java` - REST endpoints
- `BsonSerializationServiceV2.java` - Core serialization logic
- `BsonConfiguration.java` - Codec registry configuration

---

**Last Updated**: 2025-10-22
**Benchmark Version**: 2.0.0
