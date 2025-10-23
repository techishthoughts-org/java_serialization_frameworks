# gRPC - Deep Dive

![Speed](https://img.shields.io/badge/Speed-3_stars-yellow)
![Compression](https://img.shields.io/badge/Compression-3_stars-yellow)
![Ease of Use](https://img.shields.io/badge/Ease%20of%20Use-3_stars-yellow)

## Overview

gRPC is a high-performance, open-source RPC (Remote Procedure Call) framework developed by Google. Built on HTTP/2 and Protocol Buffers, it provides efficient communication between services with support for multiple streaming patterns and cross-platform interoperability.

**Port**: 8092
**Category**: RPC Framework
**Official Site**: https://grpc.io/

## Key Characteristics

### Strengths
- **HTTP/2 Based**: Multiplexing, header compression, binary framing
- **Streaming Support**: Unary, server-streaming, client-streaming, bidirectional
- **Strong Typing**: Protocol Buffers provide compile-time type safety
- **Code Generation**: Auto-generates client and server code from .proto files
- **Multi-Language**: Native support for 10+ programming languages
- **Load Balancing**: Built-in support for client-side load balancing
- **Deadline Propagation**: Automatic timeout handling across service calls

### Weaknesses
- **Complex Setup**: Requires .proto files, code generation, and build tooling
- **HTTP/2 Required**: Not all infrastructure supports HTTP/2
- **Limited Browser Support**: No native browser support without grpc-web proxy
- **Steeper Learning Curve**: More complex than REST/JSON
- **Debugging Challenges**: Binary protocol harder to inspect than text formats
- **Payload Size Overhead**: Protocol overhead for small messages

## Performance Benchmarks

### Serialization Performance
| Metric | Value | Rank |
|--------|-------|------|
| **Avg Serialization Time** | 4.56ms | 10/13 |
| **Throughput** | 219 ops/sec | 10/13 |
| **Payload Size (MEDIUM)** | 8.2KB | 8/13 |
| **Compression Ratio** | 0.48 | 5/13 |

### Resource Utilization
| Metric | Value |
|--------|-------|
| **CPU Usage** | 4.2% |
| **Memory** | 312.5 MB |
| **Memory Delta** | 18.7 MB |
| **Threads** | 52 |

### Performance by Payload Size
| Size | Avg Time | Throughput | Size (bytes) |
|------|----------|------------|--------------|
| SMALL (10 users) | 18.23ms | 55 ops/s | 892 |
| MEDIUM (100 users) | 42.34ms | 24 ops/s | 8,920 |
| LARGE (1000 users) | 198.45ms | 5 ops/s | 89,200 |
| HUGE (10000 users) | 1045.67ms | 0.96 ops/s | 892,000 |

## Implementation Details

### Dependencies

```xml
<!-- gRPC Netty (transport) -->
<dependency>
    <groupId>io.grpc</groupId>
    <artifactId>grpc-netty-shaded</artifactId>
    <version>1.58.0</version>
</dependency>

<!-- gRPC Protocol Buffers -->
<dependency>
    <groupId>io.grpc</groupId>
    <artifactId>grpc-protobuf</artifactId>
    <version>1.58.0</version>
</dependency>

<!-- gRPC Stub (client/server stubs) -->
<dependency>
    <groupId>io.grpc</groupId>
    <artifactId>grpc-stub</artifactId>
    <version>1.58.0</version>
</dependency>

<!-- Protocol Buffers -->
<dependency>
    <groupId>com.google.protobuf</groupId>
    <artifactId>protobuf-java</artifactId>
    <version>3.24.0</version>
</dependency>

<!-- Annotation API (for @Generated) -->
<dependency>
    <groupId>javax.annotation</groupId>
    <artifactId>javax.annotation-api</artifactId>
    <version>1.3.2</version>
</dependency>
```

### Maven Plugin Configuration

```xml
<build>
    <extensions>
        <extension>
            <groupId>kr.motd.maven</groupId>
            <artifactId>os-maven-plugin</artifactId>
            <version>1.7.1</version>
        </extension>
    </extensions>
    <plugins>
        <plugin>
            <groupId>org.xolstice.maven.plugins</groupId>
            <artifactId>protobuf-maven-plugin</artifactId>
            <version>0.6.1</version>
            <configuration>
                <protocArtifact>
                    com.google.protobuf:protoc:3.24.0:exe:${os.detected.classifier}
                </protocArtifact>
                <pluginId>grpc-java</pluginId>
                <pluginArtifact>
                    io.grpc:protoc-gen-grpc-java:1.58.0:exe:${os.detected.classifier}
                </pluginArtifact>
            </configuration>
            <executions>
                <execution>
                    <goals>
                        <goal>compile</goal>
                        <goal>compile-custom</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

### Protocol Buffer Definition

```protobuf
syntax = "proto3";

package com.example.grpc;

option java_multiple_files = true;
option java_package = "com.example.grpc.proto";
option java_outer_classname = "UserServiceProto";

// Service definition
service UserService {
  // Unary RPC: Single request, single response
  rpc GetUser (UserRequest) returns (UserResponse);

  // Server streaming: Single request, stream of responses
  rpc ListUsers (ListUsersRequest) returns (stream UserResponse);

  // Client streaming: Stream of requests, single response
  rpc CreateUsers (stream UserRequest) returns (BatchResponse);

  // Bidirectional streaming: Stream of requests and responses
  rpc SyncUsers (stream UserRequest) returns (stream UserResponse);
}

// Message definitions
message UserRequest {
  int64 id = 1;
  string email = 2;
}

message UserResponse {
  int64 id = 1;
  string name = 2;
  string email = 3;
  int32 age = 4;
  repeated string roles = 5;
  Address address = 6;
  int64 created_at = 7;
}

message Address {
  string street = 1;
  string city = 2;
  string state = 3;
  string zip_code = 4;
  string country = 5;
}

message ListUsersRequest {
  int32 page = 1;
  int32 page_size = 2;
  string filter = 3;
}

message BatchResponse {
  int32 total_created = 1;
  repeated int64 user_ids = 2;
  string status = 3;
}
```

### Server Implementation

```java
@Service
public class UserServiceImpl extends UserServiceGrpc.UserServiceImplBase {

    private final UserRepository userRepository;

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void getUser(UserRequest request,
                       StreamObserver<UserResponse> responseObserver) {
        try {
            User user = userRepository.findById(request.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

            UserResponse response = UserResponse.newBuilder()
                .setId(user.getId())
                .setName(user.getName())
                .setEmail(user.getEmail())
                .setAge(user.getAge())
                .addAllRoles(user.getRoles())
                .setAddress(convertAddress(user.getAddress()))
                .setCreatedAt(user.getCreatedAt().toEpochMilli())
                .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL
                .withDescription(e.getMessage())
                .asRuntimeException());
        }
    }

    @Override
    public void listUsers(ListUsersRequest request,
                         StreamObserver<UserResponse> responseObserver) {
        try {
            List<User> users = userRepository.findAll(
                request.getPage(),
                request.getPageSize()
            );

            for (User user : users) {
                UserResponse response = buildUserResponse(user);
                responseObserver.onNext(response);
            }

            responseObserver.onCompleted();

        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL
                .withDescription(e.getMessage())
                .asRuntimeException());
        }
    }

    @Override
    public StreamObserver<UserRequest> createUsers(
            StreamObserver<BatchResponse> responseObserver) {

        return new StreamObserver<UserRequest>() {
            private final List<Long> createdIds = new ArrayList<>();

            @Override
            public void onNext(UserRequest request) {
                User user = createUserFromRequest(request);
                User saved = userRepository.save(user);
                createdIds.add(saved.getId());
            }

            @Override
            public void onError(Throwable t) {
                responseObserver.onError(t);
            }

            @Override
            public void onCompleted() {
                BatchResponse response = BatchResponse.newBuilder()
                    .setTotalCreated(createdIds.size())
                    .addAllUserIds(createdIds)
                    .setStatus("SUCCESS")
                    .build();

                responseObserver.onNext(response);
                responseObserver.onCompleted();
            }
        };
    }

    private Address convertAddress(UserAddress userAddress) {
        return Address.newBuilder()
            .setStreet(userAddress.getStreet())
            .setCity(userAddress.getCity())
            .setState(userAddress.getState())
            .setZipCode(userAddress.getZipCode())
            .setCountry(userAddress.getCountry())
            .build();
    }
}
```

### Server Configuration

```java
@Configuration
public class GrpcServerConfiguration {

    @Value("${grpc.server.port:8092}")
    private int grpcPort;

    @Bean
    public Server grpcServer(UserServiceImpl userService) throws IOException {
        Server server = ServerBuilder.forPort(grpcPort)
            .addService(userService)
            .maxInboundMessageSize(10 * 1024 * 1024) // 10MB
            .maxInboundMetadataSize(8192)
            .executor(grpcExecutor())
            .intercept(new LoggingInterceptor())
            .build();

        server.start();

        // Shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            server.shutdown();
            try {
                if (!server.awaitTermination(30, TimeUnit.SECONDS)) {
                    server.shutdownNow();
                }
            } catch (InterruptedException e) {
                server.shutdownNow();
            }
        }));

        return server;
    }

    @Bean
    public Executor grpcExecutor() {
        return new ThreadPoolExecutor(
            10,  // core pool size
            50,  // max pool size
            60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(1000),
            new ThreadFactoryBuilder()
                .setNameFormat("grpc-server-%d")
                .build()
        );
    }
}
```

### Client Implementation

```java
@Service
public class UserGrpcClient {

    private final ManagedChannel channel;
    private final UserServiceGrpc.UserServiceBlockingStub blockingStub;
    private final UserServiceGrpc.UserServiceStub asyncStub;

    public UserGrpcClient(@Value("${grpc.server.host:localhost}") String host,
                          @Value("${grpc.server.port:8092}") int port) {

        this.channel = ManagedChannelBuilder
            .forAddress(host, port)
            .usePlaintext() // Disable TLS for development
            .maxInboundMessageSize(10 * 1024 * 1024)
            .keepAliveTime(30, TimeUnit.SECONDS)
            .keepAliveTimeout(10, TimeUnit.SECONDS)
            .build();

        this.blockingStub = UserServiceGrpc.newBlockingStub(channel);
        this.asyncStub = UserServiceGrpc.newStub(channel);
    }

    // Unary call (blocking)
    public UserResponse getUser(long userId) {
        UserRequest request = UserRequest.newBuilder()
            .setId(userId)
            .build();

        return blockingStub
            .withDeadlineAfter(5, TimeUnit.SECONDS)
            .getUser(request);
    }

    // Server streaming (blocking)
    public List<UserResponse> listUsers(int page, int pageSize) {
        ListUsersRequest request = ListUsersRequest.newBuilder()
            .setPage(page)
            .setPageSize(pageSize)
            .build();

        List<UserResponse> users = new ArrayList<>();
        Iterator<UserResponse> iterator = blockingStub.listUsers(request);

        iterator.forEachRemaining(users::add);

        return users;
    }

    // Client streaming (async)
    public CompletableFuture<BatchResponse> createUsers(List<UserRequest> requests) {
        CompletableFuture<BatchResponse> future = new CompletableFuture<>();

        StreamObserver<BatchResponse> responseObserver = new StreamObserver<>() {
            @Override
            public void onNext(BatchResponse response) {
                future.complete(response);
            }

            @Override
            public void onError(Throwable t) {
                future.completeExceptionally(t);
            }

            @Override
            public void onCompleted() {
                // Response already sent in onNext
            }
        };

        StreamObserver<UserRequest> requestObserver =
            asyncStub.createUsers(responseObserver);

        try {
            for (UserRequest request : requests) {
                requestObserver.onNext(request);
            }
            requestObserver.onCompleted();
        } catch (Exception e) {
            requestObserver.onError(e);
            future.completeExceptionally(e);
        }

        return future;
    }

    @PreDestroy
    public void shutdown() {
        channel.shutdown();
        try {
            if (!channel.awaitTermination(5, TimeUnit.SECONDS)) {
                channel.shutdownNow();
            }
        } catch (InterruptedException e) {
            channel.shutdownNow();
        }
    }
}
```

## Use Cases

### Ideal For

**Microservices Communication**
- Efficient inter-service communication
- Built-in load balancing and service discovery
- Strong typing prevents API mismatches
- Streaming support for real-time data

**Real-Time Applications**
- Bidirectional streaming for chat applications
- Server streaming for live updates
- Low latency communication
- Efficient connection multiplexing

**Polyglot Environments**
- Same .proto files across all languages
- Consistent API contracts
- Auto-generated client/server code
- Native performance in each language

**High-Performance APIs**
- Binary protocol reduces bandwidth
- HTTP/2 multiplexing improves throughput
- Streaming reduces memory overhead
- Better than REST for high-frequency calls

### Not Ideal For

**Public Web APIs**
- Limited browser support without grpc-web
- REST/JSON more accessible for third parties
- Binary format harder to debug
- Less tooling than REST

**Simple CRUD Applications**
- Overhead not justified for simple operations
- REST/JSON easier to implement
- More complex setup than needed
- Better alternatives for basic use cases

**Legacy System Integration**
- Requires HTTP/2 support
- May need proxies for older systems
- Binary protocol incompatible with text-based systems
- Migration complexity

**Small Messages**
- Protocol overhead for tiny payloads
- JSON may be more efficient
- Setup complexity not worth it
- Consider MessagePack or CBOR

## Optimization Tips

### 1. Connection Pooling and Reuse

```java
// Bad: Creating new channel for each request
public UserResponse getUser(long id) {
    ManagedChannel channel = ManagedChannelBuilder
        .forAddress("localhost", 8092)
        .build();
    UserServiceBlockingStub stub = UserServiceGrpc.newBlockingStub(channel);
    return stub.getUser(UserRequest.newBuilder().setId(id).build());
}

// Good: Reuse channel across requests
private final ManagedChannel channel;
private final UserServiceBlockingStub stub;

public UserGrpcClient() {
    this.channel = ManagedChannelBuilder
        .forAddress("localhost", 8092)
        .build();
    this.stub = UserServiceGrpc.newBlockingStub(channel);
}

public UserResponse getUser(long id) {
    return stub.getUser(UserRequest.newBuilder().setId(id).build());
}
```

### 2. Use Streaming for Large Datasets

```java
// Instead of returning List<User> in single response
@Override
public void listUsers(ListUsersRequest request,
                     StreamObserver<UserResponse> responseObserver) {
    // Stream users one at a time - lower memory, faster TTFB
    userRepository.streamAll().forEach(user -> {
        responseObserver.onNext(buildUserResponse(user));
    });
    responseObserver.onCompleted();
}
```

### 3. Configure Thread Pools Appropriately

```java
Server server = ServerBuilder.forPort(8092)
    .addService(userService)
    .executor(Executors.newFixedThreadPool(
        Runtime.getRuntime().availableProcessors() * 2
    ))
    .build();
```

### 4. Set Appropriate Timeouts

```java
// Client-side deadline
UserResponse response = blockingStub
    .withDeadlineAfter(5, TimeUnit.SECONDS)
    .getUser(request);

// Server-side timeout check
if (Context.current().isCancelled()) {
    return; // Client already disconnected
}
```

### 5. Enable Compression

```java
// Server side
Server server = ServerBuilder.forPort(8092)
    .addService(ServerInterceptors.intercept(
        userService,
        new ServerInterceptor() {
            @Override
            public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
                    ServerCall<ReqT, RespT> call,
                    Metadata headers,
                    ServerCallHandler<ReqT, RespT> next) {
                call.setCompression("gzip");
                return next.startCall(call, headers);
            }
        }
    ))
    .build();

// Client side
UserResponse response = blockingStub
    .withCompression("gzip")
    .getUser(request);
```

### 6. Use Async Stubs for Concurrent Calls

```java
// Blocking (slow for multiple calls)
UserResponse user1 = blockingStub.getUser(request1);
UserResponse user2 = blockingStub.getUser(request2);

// Async (parallel execution)
CompletableFuture<UserResponse> future1 = new CompletableFuture<>();
CompletableFuture<UserResponse> future2 = new CompletableFuture<>();

asyncStub.getUser(request1, createObserver(future1));
asyncStub.getUser(request2, createObserver(future2));

CompletableFuture.allOf(future1, future2).join();
```

## Advanced Patterns

### Interceptors for Cross-Cutting Concerns

```java
public class LoggingInterceptor implements ServerInterceptor {

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {

        String methodName = call.getMethodDescriptor().getFullMethodName();
        long startTime = System.currentTimeMillis();

        ServerCall<ReqT, RespT> loggingCall = new ForwardingServerCall
                .SimpleForwardingServerCall<ReqT, RespT>(call) {
            @Override
            public void close(Status status, Metadata trailers) {
                long duration = System.currentTimeMillis() - startTime;
                log.info("Method: {}, Status: {}, Duration: {}ms",
                    methodName, status.getCode(), duration);
                super.close(status, trailers);
            }
        };

        return next.startCall(loggingCall, headers);
    }
}
```

### Custom Error Handling

```java
public class ErrorHandlingInterceptor implements ServerInterceptor {

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {

        return new ForwardingServerCallListener
                .SimpleForwardingServerCallListener<ReqT>(
                    next.startCall(call, headers)) {
            @Override
            public void onHalfClose() {
                try {
                    super.onHalfClose();
                } catch (IllegalArgumentException e) {
                    call.close(Status.INVALID_ARGUMENT
                        .withDescription(e.getMessage()), new Metadata());
                } catch (Exception e) {
                    call.close(Status.INTERNAL
                        .withDescription("Internal error"), new Metadata());
                }
            }
        };
    }
}
```

### Load Balancing

```java
@Configuration
public class GrpcClientConfiguration {

    @Bean
    public ManagedChannel userServiceChannel() {
        return ManagedChannelBuilder
            .forTarget("dns:///user-service:8092")
            .defaultLoadBalancingPolicy("round_robin")
            .usePlaintext()
            .build();
    }
}
```

### Service Discovery with Consul

```java
public class ConsulNameResolverProvider extends NameResolverProvider {

    @Override
    public NameResolver newNameResolver(URI targetUri, Args args) {
        return new ConsulNameResolver(targetUri, consulClient);
    }

    @Override
    protected boolean isAvailable() {
        return true;
    }

    @Override
    protected int priority() {
        return 5;
    }

    @Override
    public String getDefaultScheme() {
        return "consul";
    }
}

// Usage
ManagedChannel channel = ManagedChannelBuilder
    .forTarget("consul:///user-service")
    .defaultLoadBalancingPolicy("round_robin")
    .nameResolverFactory(new ConsulNameResolverProvider())
    .build();
```

## Spring Boot Integration

### Application Properties

```yaml
grpc:
  server:
    port: 8092
    max-inbound-message-size: 10485760  # 10MB
    max-inbound-metadata-size: 8192
    keep-alive-time: 30s
    keep-alive-timeout: 10s
    permit-keep-alive-time: 5s
    enable-reflection: true

  client:
    user-service:
      address: 'static://localhost:8092'
      negotiation-type: plaintext
      max-inbound-message-size: 10485760
      keep-alive-time: 30s
      keep-alive-timeout: 10s
```

### Auto-Configuration

```java
@Configuration
@EnableConfigurationProperties(GrpcServerProperties.class)
public class GrpcServerAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public Server grpcServer(GrpcServerProperties properties,
                            List<BindableService> services,
                            List<ServerInterceptor> interceptors) {

        ServerBuilder<?> serverBuilder = ServerBuilder.forPort(properties.getPort());

        // Add all services
        services.forEach(service -> {
            ServerServiceDefinition definition = service.bindService();
            if (!interceptors.isEmpty()) {
                definition = ServerInterceptors.intercept(
                    definition,
                    interceptors
                );
            }
            serverBuilder.addService(definition);
        });

        // Configure server
        serverBuilder
            .maxInboundMessageSize(properties.getMaxInboundMessageSize())
            .maxInboundMetadataSize(properties.getMaxInboundMetadataSize());

        return serverBuilder.build();
    }

    @Bean
    public GrpcServerLifecycle grpcServerLifecycle(Server server) {
        return new GrpcServerLifecycle(server);
    }
}
```

## API Endpoints

### Benchmark Endpoint

```bash
POST http://localhost:8092/api/grpc/v2/benchmark
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
GET http://localhost:8092/api/grpc/v2/info
```

**Response:**
```json
{
  "framework": "gRPC",
  "version": "2.0",
  "supportedCompressionAlgorithms": ["GZIP", "DEFLATE"],
  "supportsSchemaEvolution": true,
  "typicalUseCase": "Microservices communication, real-time streaming, polyglot systems"
}
```

### Health Check

```bash
# Using grpc_health_probe
grpc_health_probe -addr=localhost:8092

# Using grpcurl
grpcurl -plaintext localhost:8092 grpc.health.v1.Health/Check
```

## Real-World Examples

### Microservices Communication

```java
// Order Service calls User Service via gRPC
@Service
public class OrderService {

    private final UserGrpcClient userClient;

    public Order createOrder(CreateOrderRequest request) {
        // Verify user exists via gRPC
        UserResponse user = userClient.getUser(request.getUserId());

        // Validate user can place order
        if (!user.getRolesList().contains("CUSTOMER")) {
            throw new IllegalStateException("User cannot place orders");
        }

        // Create order
        Order order = new Order();
        order.setUserId(user.getId());
        order.setUserEmail(user.getEmail());
        order.setItems(request.getItems());

        return orderRepository.save(order);
    }
}
```

### Real-Time Chat Application

```java
@Override
public StreamObserver<ChatMessage> chat(
        StreamObserver<ChatMessage> responseObserver) {

    String sessionId = UUID.randomUUID().toString();

    return new StreamObserver<ChatMessage>() {
        @Override
        public void onNext(ChatMessage message) {
            // Broadcast to all other clients
            chatService.broadcast(message, sessionId);

            // Echo back confirmation
            ChatMessage confirmation = ChatMessage.newBuilder()
                .setMessageId(message.getMessageId())
                .setStatus("DELIVERED")
                .setTimestamp(System.currentTimeMillis())
                .build();

            responseObserver.onNext(confirmation);
        }

        @Override
        public void onError(Throwable t) {
            chatService.removeSession(sessionId);
        }

        @Override
        public void onCompleted() {
            chatService.removeSession(sessionId);
            responseObserver.onCompleted();
        }
    };
}
```

### File Upload with Streaming

```java
@Override
public StreamObserver<FileChunk> uploadFile(
        StreamObserver<UploadResponse> responseObserver) {

    return new StreamObserver<FileChunk>() {
        private FileOutputStream outputStream;
        private String filename;
        private long totalBytes = 0;

        @Override
        public void onNext(FileChunk chunk) {
            try {
                if (outputStream == null) {
                    filename = chunk.getFilename();
                    outputStream = new FileOutputStream("/uploads/" + filename);
                }

                outputStream.write(chunk.getData().toByteArray());
                totalBytes += chunk.getData().size();

            } catch (IOException e) {
                onError(e);
            }
        }

        @Override
        public void onError(Throwable t) {
            closeStream();
            responseObserver.onError(Status.INTERNAL
                .withDescription(t.getMessage())
                .asRuntimeException());
        }

        @Override
        public void onCompleted() {
            closeStream();

            UploadResponse response = UploadResponse.newBuilder()
                .setFilename(filename)
                .setTotalBytes(totalBytes)
                .setStatus("SUCCESS")
                .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }

        private void closeStream() {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    // Log error
                }
            }
        }
    };
}
```

## Alternatives & When to Switch

| Switch To | When | Why |
|-----------|------|-----|
| **REST/JSON** | Public APIs, browser clients | Better tooling, wider support |
| **Thrift** | Need RPC with more control | Similar features, more customizable |
| **Apache Avro** | Need schema evolution only | Simpler than full RPC |
| **GraphQL** | Flexible queries needed | Better for client-driven APIs |
| **WebSockets** | Browser real-time only | Native browser support |

## Testing

### Unit Testing

```java
@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void testGetUser() {
        // Arrange
        User user = createTestUser(1L, "John Doe");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserRequest request = UserRequest.newBuilder()
            .setId(1L)
            .build();

        StreamObserver<UserResponse> responseObserver =
            mock(StreamObserver.class);

        // Act
        userService.getUser(request, responseObserver);

        // Assert
        ArgumentCaptor<UserResponse> captor =
            ArgumentCaptor.forClass(UserResponse.class);
        verify(responseObserver).onNext(captor.capture());
        verify(responseObserver).onCompleted();

        UserResponse response = captor.getValue();
        assertEquals(1L, response.getId());
        assertEquals("John Doe", response.getName());
    }
}
```

### Integration Testing

```java
@SpringBootTest
class GrpcIntegrationTest {

    private ManagedChannel channel;
    private UserServiceGrpc.UserServiceBlockingStub stub;

    @BeforeEach
    void setup() {
        channel = ManagedChannelBuilder
            .forAddress("localhost", 8092)
            .usePlaintext()
            .build();

        stub = UserServiceGrpc.newBlockingStub(channel);
    }

    @AfterEach
    void teardown() {
        channel.shutdown();
    }

    @Test
    void testGetUserEndToEnd() {
        UserRequest request = UserRequest.newBuilder()
            .setId(1L)
            .build();

        UserResponse response = stub.getUser(request);

        assertNotNull(response);
        assertEquals(1L, response.getId());
    }
}
```

### Load Testing with ghz

```bash
# Install ghz
go install github.com/bojand/ghz/cmd/ghz@latest

# Run load test
ghz --insecure \
    --proto ./user_service.proto \
    --call com.example.grpc.UserService/GetUser \
    -d '{"id": 1}' \
    -n 10000 \
    -c 50 \
    localhost:8092
```

## Troubleshooting

### Issue: Context Deadline Exceeded

**Problem**: Calls timing out

**Solution**:
```java
// Increase client timeout
UserResponse response = blockingStub
    .withDeadlineAfter(30, TimeUnit.SECONDS)
    .getUser(request);

// Check server processing time
if (Context.current().getDeadline() != null) {
    long remainingNanos = Context.current().getDeadline().timeRemaining(NANOSECONDS);
    if (remainingNanos < THRESHOLD) {
        // Not enough time, return early
    }
}
```

### Issue: Resource Exhausted

**Problem**: Too many concurrent streams

**Solution**:
```java
Server server = ServerBuilder.forPort(8092)
    .maxConcurrentCallsPerConnection(1000)
    .maxInboundMessageSize(10 * 1024 * 1024)
    .executor(Executors.newFixedThreadPool(100))
    .build();
```

### Issue: Connection Refused

**Problem**: Cannot connect to server

**Solutions**:
1. Verify server is running: `netstat -an | grep 8092`
2. Check firewall rules
3. Verify host/port configuration
4. Check if using TLS correctly

### Issue: Slow Performance

**Problem**: gRPC calls slower than expected

**Solutions**:
1. Enable connection reuse
2. Use streaming for large datasets
3. Enable compression
4. Increase thread pool size
5. Use async stubs for concurrent calls
6. Profile with interceptors

## Monitoring and Observability

### Prometheus Metrics

```java
@Component
public class MetricsInterceptor implements ServerInterceptor {

    private final Counter requestCounter = Counter.build()
        .name("grpc_requests_total")
        .help("Total gRPC requests")
        .labelNames("method", "status")
        .register();

    private final Histogram requestDuration = Histogram.build()
        .name("grpc_request_duration_seconds")
        .help("gRPC request duration")
        .labelNames("method")
        .register();

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {

        String method = call.getMethodDescriptor().getFullMethodName();
        Histogram.Timer timer = requestDuration.labels(method).startTimer();

        ServerCall<ReqT, RespT> monitoringCall =
            new ForwardingServerCall.SimpleForwardingServerCall<>(call) {
                @Override
                public void close(Status status, Metadata trailers) {
                    timer.observeDuration();
                    requestCounter.labels(method, status.getCode().name()).inc();
                    super.close(status, trailers);
                }
            };

        return next.startCall(monitoringCall, headers);
    }
}
```

### Distributed Tracing

```java
@Component
public class TracingInterceptor implements ServerInterceptor {

    private final Tracer tracer;

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {

        String method = call.getMethodDescriptor().getFullMethodName();

        Span span = tracer.buildSpan(method)
            .withTag("grpc.method", method)
            .start();

        Context context = Context.current().withValue(SPAN_CONTEXT_KEY, span);

        ServerCall<ReqT, RespT> tracingCall =
            new ForwardingServerCall.SimpleForwardingServerCall<>(call) {
                @Override
                public void close(Status status, Metadata trailers) {
                    span.setTag("grpc.status", status.getCode().name());
                    span.finish();
                    super.close(status, trailers);
                }
            };

        return Contexts.interceptCall(context, tracingCall, headers, next);
    }
}
```

## Best Practices

1. **Define Clear Service Boundaries**: Keep services focused and cohesive
2. **Version Your APIs**: Use package versioning in .proto files
3. **Use Streaming Wisely**: Don't stream if unary RPC suffices
4. **Implement Proper Error Handling**: Use status codes and metadata
5. **Set Appropriate Timeouts**: Both client and server side
6. **Enable Health Checks**: Use standard health checking protocol
7. **Monitor Performance**: Track latency, throughput, error rates
8. **Use Interceptors**: For cross-cutting concerns
9. **Secure Your Services**: Use TLS in production
10. **Document Your APIs**: Include comments in .proto files
11. **Test Thoroughly**: Unit, integration, and load tests
12. **Handle Backpressure**: Use flow control in streaming

## Additional Resources

- **Official Documentation**: https://grpc.io/docs/
- **Protocol Buffers**: https://protobuf.dev/
- **gRPC Java**: https://github.com/grpc/grpc-java
- **Best Practices**: https://grpc.io/docs/guides/performance/
- **Error Handling**: https://grpc.io/docs/guides/error/
- **Spring Boot gRPC**: https://github.com/LogNet/grpc-spring-boot-starter

## Source Code

Implementation: [`grpc-poc/`](../../grpc-poc/)

Key Files:
- `user_service.proto` - Service and message definitions
- `UserServiceImpl.java` - gRPC service implementation
- `GrpcServerConfiguration.java` - Server configuration
- `UserGrpcClient.java` - Client implementation

---

**Last Updated**: 2025-10-22
**Benchmark Version**: 2.0.0
