package org.techishthoughts.benchmark;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.fury.Fury;
import org.apache.fury.config.Language;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.DefaultSerializers;
import org.techishthoughts.payload.model.Address;
import org.techishthoughts.payload.model.Order;
import org.techishthoughts.payload.model.OrderItem;
import org.techishthoughts.payload.model.Payment;
import org.techishthoughts.payload.model.Skill;
import org.techishthoughts.payload.model.SocialConnection;
import org.techishthoughts.payload.model.TrackingEvent;
import org.techishthoughts.payload.model.User;
import org.techishthoughts.payload.model.UserProfile;
import com.fasterxml.jackson.databind.ObjectMapper;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@Fork(value = 1, jvmArgs = {"-Xms2G", "-Xmx2G"})
@Warmup(iterations = 3, time = 2)
@Measurement(iterations = 5, time = 3)
public class CombinedBenchmark {

    private User testUser;
    private ObjectMapper jsonMapper;
    private Kryo kryo;
    private Fury fury;

    @Setup
    public void setup() {
        // Initialize test data
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("johndoe");
        testUser.setEmail("john.doe@example.com");
        testUser.setFirstName("John");
        testUser.setLastName("Doe");
        testUser.setIsActive(true);
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setLastLoginAt(LocalDateTime.now());

        // Initialize Jackson JSON with JSR310 support
        jsonMapper = new ObjectMapper();
        jsonMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        jsonMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // Initialize Kryo with all required class registrations
        kryo = new Kryo();
        kryo.register(User.class);
        kryo.register(Address.class);
        kryo.register(UserProfile.class);
        kryo.register(Order.class);
        kryo.register(SocialConnection.class);
        kryo.register(LocalDateTime.class);
        kryo.register(Long.class, new DefaultSerializers.LongSerializer());
        kryo.register(String.class, new DefaultSerializers.StringSerializer());
        kryo.register(Integer.class, new DefaultSerializers.IntSerializer());
        kryo.register(Boolean.class, new DefaultSerializers.BooleanSerializer());
        kryo.register(Double.class, new DefaultSerializers.DoubleSerializer());
        kryo.register(java.util.ArrayList.class);
        kryo.register(java.util.HashMap.class);
        kryo.register(java.util.LinkedHashMap.class);

        // Initialize Fury with all required class registrations
        fury = Fury.builder()
                .withLanguage(Language.JAVA)
                .requireClassRegistration(true) // Enable class registration for security
                .build();
        fury.register(User.class);
        fury.register(Address.class);
        fury.register(UserProfile.class);
        fury.register(Order.class);
        fury.register(OrderItem.class);
        fury.register(TrackingEvent.class);
        fury.register(SocialConnection.class);
        fury.register(LocalDateTime.class);

        // Register enums
        fury.register(Address.AddressType.class);
        fury.register(Order.OrderStatus.class);
        fury.register(Payment.PaymentMethod.class);
        fury.register(Payment.PaymentStatus.class);
        fury.register(SocialConnection.SocialPlatform.class);
        fury.register(Skill.SkillLevel.class);
        fury.register(org.techishthoughts.payload.model.Language.LanguageProficiency.class);

        // Register collections
        fury.register(java.util.ArrayList.class);
        fury.register(java.util.HashMap.class);
        fury.register(java.util.LinkedHashMap.class);
    }

    @Benchmark
    public void jacksonJsonGzipSerialize(Blackhole bh) throws IOException {
        byte[] jsonBytes = jsonMapper.writeValueAsBytes(testUser);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (GZIPOutputStream gzipOut = new GZIPOutputStream(baos)) {
            gzipOut.write(jsonBytes);
        }
        byte[] compressed = baos.toByteArray();
        bh.consume(compressed);
    }

    @Benchmark
    public void jacksonJsonGzipDeserialize(Blackhole bh) throws IOException {
        byte[] jsonBytes = jsonMapper.writeValueAsBytes(testUser);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (GZIPOutputStream gzipOut = new GZIPOutputStream(baos)) {
            gzipOut.write(jsonBytes);
        }
        byte[] compressed = baos.toByteArray();

        ByteArrayInputStream bais = new ByteArrayInputStream(compressed);
        try (GZIPInputStream gzipIn = new GZIPInputStream(bais)) {
            byte[] decompressed = gzipIn.readAllBytes();
            User user = jsonMapper.readValue(decompressed, User.class);
            bh.consume(user);
        }
    }

    @Benchmark
    public void kryoGzipSerialize(Blackhole bh) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Output output = new Output(baos);
        kryo.writeObject(output, testUser);
        output.close();
        byte[] kryoBytes = baos.toByteArray();

        ByteArrayOutputStream gzipBaos = new ByteArrayOutputStream();
        try (GZIPOutputStream gzipOut = new GZIPOutputStream(gzipBaos)) {
            gzipOut.write(kryoBytes);
        }
        byte[] compressed = gzipBaos.toByteArray();
        bh.consume(compressed);
    }

    @Benchmark
    public void kryoGzipDeserialize(Blackhole bh) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Output output = new Output(baos);
        kryo.writeObject(output, testUser);
        output.close();
        byte[] kryoBytes = baos.toByteArray();

        ByteArrayOutputStream gzipBaos = new ByteArrayOutputStream();
        try (GZIPOutputStream gzipOut = new GZIPOutputStream(gzipBaos)) {
            gzipOut.write(kryoBytes);
        }
        byte[] compressed = gzipBaos.toByteArray();

        ByteArrayInputStream bais = new ByteArrayInputStream(compressed);
        try (GZIPInputStream gzipIn = new GZIPInputStream(bais)) {
            byte[] decompressed = gzipIn.readAllBytes();
            ByteArrayInputStream kryoBais = new ByteArrayInputStream(decompressed);
            Input input = new Input(kryoBais);
            User user = kryo.readObject(input, User.class);
            input.close();
            bh.consume(user);
        }
    }

    @Benchmark
    public void furyGzipSerialize(Blackhole bh) throws IOException {
        byte[] furyBytes = fury.serialize(testUser);

        ByteArrayOutputStream gzipBaos = new ByteArrayOutputStream();
        try (GZIPOutputStream gzipOut = new GZIPOutputStream(gzipBaos)) {
            gzipOut.write(furyBytes);
        }
        byte[] compressed = gzipBaos.toByteArray();
        bh.consume(compressed);
    }

    @Benchmark
    public void furyGzipDeserialize(Blackhole bh) throws IOException {
        byte[] furyBytes = fury.serialize(testUser);

        ByteArrayOutputStream gzipBaos = new ByteArrayOutputStream();
        try (GZIPOutputStream gzipOut = new GZIPOutputStream(gzipBaos)) {
            gzipOut.write(furyBytes);
        }
        byte[] compressed = gzipBaos.toByteArray();

        ByteArrayInputStream bais = new ByteArrayInputStream(compressed);
        try (GZIPInputStream gzipIn = new GZIPInputStream(bais)) {
            byte[] decompressed = gzipIn.readAllBytes();
            User user = (User) fury.deserialize(decompressed);
            bh.consume(user);
        }
    }

        // Brotli benchmarks disabled due to API issues
    /*
    @Benchmark
    public void jacksonJsonBrotliSerialize(Blackhole bh) throws IOException {
        byte[] jsonBytes = jsonMapper.writeValueAsBytes(testUser);
        byte[] compressed = com.aayushatharva.brotli4j.encoder.BrotliEncoder.compress(
            jsonBytes,
            new com.aayushatharva.brotli4j.encoder.Encoder.Parameters().setQuality(11)
        );
        bh.consume(compressed);
    }

    @Benchmark
    public void jacksonJsonBrotliDeserialize(Blackhole bh) throws IOException {
        byte[] jsonBytes = jsonMapper.writeValueAsBytes(testUser);
        byte[] compressed = com.aayushatharva.brotli4j.encoder.BrotliEncoder.compress(
            jsonBytes,
            new com.aayushatharva.brotli4j.encoder.Encoder.Parameters().setQuality(11)
        );

        byte[] decompressed = com.aayushatharva.brotli4j.decoder.BrotliDecoder.decompress(compressed);
        User user = jsonMapper.readValue(decompressed, User.class);
        bh.consume(user);
    }
    */

        // Brotli benchmarks disabled due to API issues
    /*
    @Benchmark
    public void kryoBrotliSerialize(Blackhole bh) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Output output = new Output(baos);
        kryo.writeObject(output, testUser);
        output.close();
        byte[] kryoBytes = baos.toByteArray();

        byte[] compressed = com.aayushatharva.brotli4j.encoder.BrotliEncoder.compress(
            kryoBytes,
            new com.aayushatharva.brotli4j.encoder.Encoder.Parameters().setQuality(11)
        );
        bh.consume(compressed);
    }

    @Benchmark
    public void kryoBrotliDeserialize(Blackhole bh) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Output output = new Output(baos);
        kryo.writeObject(output, testUser);
        output.close();
        byte[] kryoBytes = baos.toByteArray();

        byte[] compressed = com.aayushatharva.brotli4j.encoder.BrotliEncoder.compress(
            kryoBytes,
            new com.aayushatharva.brotli4j.encoder.Encoder.Parameters().setQuality(11)
        );

        byte[] decompressed = com.aayushatharva.brotli4j.decoder.BrotliDecoder.decompress(compressed);
        ByteArrayInputStream kryoBais = new ByteArrayInputStream(decompressed);
        Input input = new Input(kryoBais);
        User user = kryo.readObject(input, User.class);
        input.close();
        bh.consume(user);
    }

    @Benchmark
    public void furyBrotliSerialize(Blackhole bh) throws IOException {
        byte[] furyBytes = fury.serialize(testUser);
        byte[] compressed = com.aayushatharva.brotli4j.encoder.BrotliEncoder.compress(
            furyBytes,
            new com.aayushatharva.brotli4j.encoder.Encoder.Parameters().setQuality(11)
        );
        bh.consume(compressed);
    }

    @Benchmark
    public void furyBrotliDeserialize(Blackhole bh) throws IOException {
        byte[] furyBytes = fury.serialize(testUser);
        byte[] compressed = com.aayushatharva.brotli4j.encoder.BrotliEncoder.compress(
            furyBytes,
            new com.aayushatharva.brotli4j.encoder.Encoder.Parameters().setQuality(11)
        );

        byte[] decompressed = com.aayushatharva.brotli4j.decoder.BrotliDecoder.decompress(compressed);
        User user = (User) fury.deserialize(decompressed);
        bh.consume(user);
    }
    */

    @Benchmark
    public void jacksonJsonZstdSerialize(Blackhole bh) throws IOException {
        byte[] jsonBytes = jsonMapper.writeValueAsBytes(testUser);
        byte[] compressed = com.github.luben.zstd.Zstd.compress(jsonBytes);
        bh.consume(compressed);
    }

    @Benchmark
    public void jacksonJsonZstdDeserialize(Blackhole bh) throws IOException {
        byte[] jsonBytes = jsonMapper.writeValueAsBytes(testUser);
        byte[] compressed = com.github.luben.zstd.Zstd.compress(jsonBytes);

        byte[] decompressed = com.github.luben.zstd.Zstd.decompress(compressed, jsonBytes.length);
        User user = jsonMapper.readValue(decompressed, User.class);
        bh.consume(user);
    }

    @Benchmark
    public void kryoZstdSerialize(Blackhole bh) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Output output = new Output(baos);
        kryo.writeObject(output, testUser);
        output.close();
        byte[] kryoBytes = baos.toByteArray();

        byte[] compressed = com.github.luben.zstd.Zstd.compress(kryoBytes);
        bh.consume(compressed);
    }

    @Benchmark
    public void kryoZstdDeserialize(Blackhole bh) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Output output = new Output(baos);
        kryo.writeObject(output, testUser);
        output.close();
        byte[] kryoBytes = baos.toByteArray();

        byte[] compressed = com.github.luben.zstd.Zstd.compress(kryoBytes);

        byte[] decompressed = com.github.luben.zstd.Zstd.decompress(compressed, kryoBytes.length);
        ByteArrayInputStream kryoBais = new ByteArrayInputStream(decompressed);
        Input input = new Input(kryoBais);
        User user = kryo.readObject(input, User.class);
        input.close();
        bh.consume(user);
    }

    @Benchmark
    public void furyZstdSerialize(Blackhole bh) throws IOException {
        byte[] furyBytes = fury.serialize(testUser);
        byte[] compressed = com.github.luben.zstd.Zstd.compress(furyBytes);
        bh.consume(compressed);
    }

    @Benchmark
    public void furyZstdDeserialize(Blackhole bh) throws IOException {
        byte[] furyBytes = fury.serialize(testUser);
        byte[] compressed = com.github.luben.zstd.Zstd.compress(furyBytes);

        byte[] decompressed = com.github.luben.zstd.Zstd.decompress(compressed, furyBytes.length);
        User user = (User) fury.deserialize(decompressed);
        bh.consume(user);
    }
}
