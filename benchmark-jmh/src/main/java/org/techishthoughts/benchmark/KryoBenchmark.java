package org.techishthoughts.benchmark;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.openjdk.jmh.annotations.*;
import org.techishthoughts.payload.generator.UnifiedPayloadGenerator;
import org.techishthoughts.payload.model.*;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * JMH Microbenchmark for Kryo Serialization
 *
 * Measures PURE serialization/deserialization performance without HTTP overhead.
 * Uses optimized Kryo configuration for maximum performance.
 *
 * Run with: java -jar target/benchmarks.jar KryoBenchmark
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 10, time = 1)
@Fork(value = 2, jvmArgs = {"-Xms2G", "-Xmx2G"})
public class KryoBenchmark {

    private Kryo kryo;
    private List<User> users;
    private byte[] serializedData;

    @Setup(Level.Trial)
    public void setup() throws Exception {
        // Initialize Kryo with optimized settings
        kryo = new Kryo();
        kryo.setRegistrationRequired(false);
        kryo.setReferences(false);
        kryo.setAutoReset(false);
        kryo.setCopyReferences(false);

        // Register classes for better performance
        registerClasses(kryo);

        // Generate test payload (MEDIUM complexity - 100 users)
        UnifiedPayloadGenerator generator = new UnifiedPayloadGenerator();
        users = generator.generateUsers(
            UnifiedPayloadGenerator.ComplexityLevel.MEDIUM,
            100
        );

        // Pre-serialize for deserialization benchmark
        serializedData = serializeToBytes(users);
    }

    private void registerClasses(Kryo kryo) {
        kryo.register(User.class);
        kryo.register(UserProfile.class);
        kryo.register(Address.class);
        kryo.register(Order.class);
        kryo.register(OrderItem.class);
        kryo.register(Payment.class);
        kryo.register(TrackingEvent.class);
        kryo.register(SocialConnection.class);
        kryo.register(Skill.class);
        kryo.register(Education.class);
        kryo.register(Language.class);
        kryo.register(java.time.LocalDateTime.class);
        kryo.register(java.util.ArrayList.class);
        kryo.register(java.util.HashMap.class);
        kryo.register(java.util.LinkedHashMap.class);
    }

    private byte[] serializeToBytes(List<User> users) {
        Output output = new Output(1024 * 1024); // 1MB buffer
        try {
            output.writeInt(users.size());
            for (User user : users) {
                kryo.writeObject(output, user);
            }
            output.flush();
            return output.toBytes();
        } finally {
            output.close();
        }
    }

    private List<User> deserializeFromBytes(byte[] data) {
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        Input input = new Input(bais);
        try {
            int userCount = input.readInt();
            List<User> result = new ArrayList<>(userCount);
            for (int i = 0; i < userCount; i++) {
                User user = kryo.readObject(input, User.class);
                result.add(user);
            }
            return result;
        } finally {
            input.close();
        }
    }

    @Benchmark
    public byte[] serialize() throws Exception {
        return serializeToBytes(users);
    }

    @Benchmark
    public List<User> deserialize() throws Exception {
        return deserializeFromBytes(serializedData);
    }

    @Benchmark
    public List<User> roundtrip() throws Exception {
        byte[] data = serializeToBytes(users);
        return deserializeFromBytes(data);
    }
}
