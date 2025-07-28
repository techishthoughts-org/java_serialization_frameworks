package org.techishthoughts.benchmark;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.DatumWriter;
import org.apache.fury.Fury;
import org.apache.fury.config.Language;
import org.nustaq.serialization.FSTConfiguration;
import org.nustaq.serialization.FSTObjectInput;
import org.nustaq.serialization.FSTObjectOutput;
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
import org.techishthoughts.payload.model.Address;
import org.techishthoughts.payload.model.Order;
import org.techishthoughts.payload.model.OrderItem;
import org.techishthoughts.payload.model.Payment;
import org.techishthoughts.payload.model.Skill;
import org.techishthoughts.payload.model.SocialConnection;
import org.techishthoughts.payload.model.TrackingEvent;
import org.techishthoughts.payload.model.User;
import org.techishthoughts.payload.model.UserProfile;
import org.xerial.snappy.Snappy;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.DefaultSerializers;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import com.github.luben.zstd.Zstd;
import com.google.flatbuffers.FlatBufferBuilder;

import net.jpountz.lz4.LZ4Compressor;
import net.jpountz.lz4.LZ4Factory;
import net.jpountz.lz4.LZ4FastDecompressor;

/**
 * Advanced Serialization Benchmark (2025)
 *
 * Comprehensive benchmark for all modern serialization frameworks and compression algorithms
 * including the latest 2025 technologies.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@Fork(value = 1, jvmArgs = {"-Xms2G", "-Xmx2G"})
@Warmup(iterations = 3, time = 2)
@Measurement(iterations = 5, time = 3)
public class AdvancedSerializationBenchmark {

    private User testUser;
    private ObjectMapper jsonMapper;
    private ObjectMapper cborMapper;
    private ObjectMapper messagePackMapper;
    private Kryo kryo;
    private Fury fury;
    private FSTConfiguration fstConfig;
    private LZ4Compressor lz4Compressor;
    private LZ4FastDecompressor lz4Decompressor;
    private DatumWriter<GenericRecord> avroWriter;
    private DatumReader<GenericRecord> avroReader;
    private org.apache.avro.Schema avroSchema;

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

        // Initialize Jackson CBOR
        cborMapper = new ObjectMapper(new CBORFactory());

        // Initialize Jackson MessagePack (using CBOR as alternative)
        messagePackMapper = new ObjectMapper(new CBORFactory());

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

        // Initialize Apache Fury
        fury = Fury.builder()
                .withLanguage(Language.JAVA)
                .requireClassRegistration(true)
                .build();
        registerFuryClasses();

        // Initialize FST
        fstConfig = FSTConfiguration.createDefaultConfiguration();
        fstConfig.setShareReferences(true);
        fstConfig.setForceSerializable(true);
        registerFstClasses();

        // Initialize LZ4
        LZ4Factory factory = LZ4Factory.fastestInstance();
        lz4Compressor = factory.fastCompressor();
        lz4Decompressor = factory.fastDecompressor();

        // Initialize Avro
        this.avroSchema = org.apache.avro.reflect.ReflectData.get().getSchema(User.class);
        this.avroWriter = new org.apache.avro.reflect.ReflectDatumWriter<>(avroSchema);
        this.avroReader = new org.apache.avro.reflect.ReflectDatumReader<>(avroSchema);
    }

    private void registerFuryClasses() {
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
        fury.register(TrackingEvent.TrackingEventType.class);

        fury.register(java.util.ArrayList.class);
        fury.register(java.util.HashMap.class);
    }

    private void registerFstClasses() {
        fstConfig.registerClass(User.class);
        fstConfig.registerClass(Address.class);
        fstConfig.registerClass(UserProfile.class);
        fstConfig.registerClass(Order.class);
        fstConfig.registerClass(SocialConnection.class);
        fstConfig.registerClass(LocalDateTime.class);
        fstConfig.registerClass(java.util.ArrayList.class);
        fstConfig.registerClass(java.util.HashMap.class);
    }

    // ========== SERIALIZATION BENCHMARKS ==========

    @Benchmark
    public void jacksonJsonSerialize(Blackhole bh) throws IOException {
        byte[] data = jsonMapper.writeValueAsBytes(testUser);
        bh.consume(data);
    }

    @Benchmark
    public void jacksonJsonDeserialize(Blackhole bh) throws IOException {
        byte[] data = jsonMapper.writeValueAsBytes(testUser);
        User user = jsonMapper.readValue(data, User.class);
        bh.consume(user);
    }

    @Benchmark
    public void jacksonCborSerialize(Blackhole bh) throws IOException {
        byte[] data = cborMapper.writeValueAsBytes(testUser);
        bh.consume(data);
    }

    @Benchmark
    public void jacksonCborDeserialize(Blackhole bh) throws IOException {
        byte[] data = cborMapper.writeValueAsBytes(testUser);
        User user = cborMapper.readValue(data, User.class);
        bh.consume(user);
    }

    @Benchmark
    public void jacksonMessagePackSerialize(Blackhole bh) throws IOException {
        byte[] data = messagePackMapper.writeValueAsBytes(testUser);
        bh.consume(data);
    }

    @Benchmark
    public void jacksonMessagePackDeserialize(Blackhole bh) throws IOException {
        byte[] data = messagePackMapper.writeValueAsBytes(testUser);
        User user = messagePackMapper.readValue(data, User.class);
        bh.consume(user);
    }

    @Benchmark
    public void kryoSerialize(Blackhole bh) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Output output = new Output(baos);
        kryo.writeObject(output, testUser);
        output.flush();
        byte[] data = output.toBytes();
        bh.consume(data);
        output.close();
    }

    @Benchmark
    public void kryoDeserialize(Blackhole bh) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Output output = new Output(baos);
        kryo.writeObject(output, testUser);
        output.flush();
        byte[] data = output.toBytes();
        output.close();

        Input input = new Input(data);
        User user = kryo.readObject(input, User.class);
        bh.consume(user);
        input.close();
    }

    @Benchmark
    public void furySerialize(Blackhole bh) {
        byte[] data = fury.serialize(testUser);
        bh.consume(data);
    }

    @Benchmark
    public void furyDeserialize(Blackhole bh) {
        byte[] data = fury.serialize(testUser);
        User user = (User) fury.deserialize(data);
        bh.consume(user);
    }

    @Benchmark
    public void fstSerialize(Blackhole bh) throws Exception {
        FSTObjectOutput output = fstConfig.getObjectOutput();
        output.writeObject(testUser);
        byte[] data = output.getCopyOfWrittenBuffer();
        output.close();
        bh.consume(data);
    }

    @Benchmark
    public void fstDeserialize(Blackhole bh) throws Exception {
        FSTObjectOutput output = fstConfig.getObjectOutput();
        output.writeObject(testUser);
        byte[] data = output.getCopyOfWrittenBuffer();
        output.close();

        FSTObjectInput input = fstConfig.getObjectInput(data);
        User user = (User) input.readObject();
        input.close();
        bh.consume(user);
    }

    @Benchmark
    public void flatbuffersSerialize(Blackhole bh) {
        FlatBufferBuilder builder = new FlatBufferBuilder(1024);

        // Simplified FlatBuffers serialization
        int username = builder.createString(testUser.getUsername());
        int email = builder.createString(testUser.getEmail());
        int firstName = builder.createString(testUser.getFirstName());
        int lastName = builder.createString(testUser.getLastName());

        org.techishthoughts.flatbuffers.User.startUser(builder);
        org.techishthoughts.flatbuffers.User.addId(builder, testUser.getId());
        org.techishthoughts.flatbuffers.User.addUsername(builder, username);
        org.techishthoughts.flatbuffers.User.addEmail(builder, email);
        org.techishthoughts.flatbuffers.User.addFirstName(builder, firstName);
        org.techishthoughts.flatbuffers.User.addLastName(builder, lastName);
        org.techishthoughts.flatbuffers.User.addIsActive(builder, testUser.getIsActive());

        int root = org.techishthoughts.flatbuffers.User.endUser(builder);
        org.techishthoughts.flatbuffers.User.finishUserBuffer(builder, root);

        byte[] data = builder.sizedByteArray();
        bh.consume(data);
    }

    @Benchmark
    public void flatbuffersDeserialize(Blackhole bh) {
        FlatBufferBuilder builder = new FlatBufferBuilder(1024);

        // Serialize first
        int username = builder.createString(testUser.getUsername());
        int email = builder.createString(testUser.getEmail());
        int firstName = builder.createString(testUser.getFirstName());
        int lastName = builder.createString(testUser.getLastName());

        org.techishthoughts.flatbuffers.User.startUser(builder);
        org.techishthoughts.flatbuffers.User.addId(builder, testUser.getId());
        org.techishthoughts.flatbuffers.User.addUsername(builder, username);
        org.techishthoughts.flatbuffers.User.addEmail(builder, email);
        org.techishthoughts.flatbuffers.User.addFirstName(builder, firstName);
        org.techishthoughts.flatbuffers.User.addLastName(builder, lastName);
        org.techishthoughts.flatbuffers.User.addIsActive(builder, testUser.getIsActive());

        int root = org.techishthoughts.flatbuffers.User.endUser(builder);
        org.techishthoughts.flatbuffers.User.finishUserBuffer(builder, root);

        byte[] data = builder.sizedByteArray();

        // Deserialize
        java.nio.ByteBuffer buffer = java.nio.ByteBuffer.wrap(data);
        org.techishthoughts.flatbuffers.User fbUser = org.techishthoughts.flatbuffers.User.getRootAsUser(buffer);
        bh.consume(fbUser);
    }



    /*
    @Benchmark
    public void messagePackSerialize(Blackhole bh) throws IOException {
        // Standalone MessagePack serialization - disabled for now
        bh.consume("disabled");
    }
    */

    /*
    @Benchmark
    public void messagePackDeserialize(Blackhole bh) throws IOException {
        // Standalone MessagePack deserialization - disabled for now
        bh.consume("disabled");
    }
    */

    /*
    @Benchmark
    public void bsonSerialize(Blackhole bh) {
        // BSON serialization - disabled for now
        bh.consume("disabled");
    }
    */

    /*
    @Benchmark
    public void bsonDeserialize(Blackhole bh) {
        // BSON deserialization - disabled for now
        bh.consume("disabled");
    }
    */

    // ========== COMPRESSION BENCHMARKS ==========

    @Benchmark
    public void lz4Compress(Blackhole bh) throws IOException {
        byte[] data = jsonMapper.writeValueAsBytes(testUser);
        byte[] compressed = lz4Compressor.compress(data);
        bh.consume(compressed);
    }

    @Benchmark
    public void lz4Decompress(Blackhole bh) throws IOException {
        byte[] data = jsonMapper.writeValueAsBytes(testUser);
        byte[] compressed = lz4Compressor.compress(data);
        byte[] decompressed = lz4Decompressor.decompress(compressed, data.length);
        bh.consume(decompressed);
    }

    @Benchmark
    public void snappyCompress(Blackhole bh) throws IOException {
        byte[] data = jsonMapper.writeValueAsBytes(testUser);
        byte[] compressed = Snappy.compress(data);
        bh.consume(compressed);
    }

    @Benchmark
    public void snappyDecompress(Blackhole bh) throws IOException {
        byte[] data = jsonMapper.writeValueAsBytes(testUser);
        byte[] compressed = Snappy.compress(data);
        byte[] decompressed = Snappy.uncompress(compressed);
        bh.consume(decompressed);
    }

    @Benchmark
    public void zstdCompress(Blackhole bh) throws IOException {
        byte[] data = jsonMapper.writeValueAsBytes(testUser);
        byte[] compressed = Zstd.compress(data);
        bh.consume(compressed);
    }

    @Benchmark
    public void zstdDecompress(Blackhole bh) throws IOException {
        byte[] data = jsonMapper.writeValueAsBytes(testUser);
        byte[] compressed = Zstd.compress(data);
        byte[] decompressed = Zstd.decompress(compressed, data.length);
        bh.consume(decompressed);
    }

    // ========== COMBINED SERIALIZATION + COMPRESSION BENCHMARKS ==========

    @Benchmark
    public void kryoLz4Serialize(Blackhole bh) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Output output = new Output(baos);
        kryo.writeObject(output, testUser);
        output.flush();
        byte[] data = output.toBytes();
        output.close();

        byte[] compressed = lz4Compressor.compress(data);
        bh.consume(compressed);
    }

    @Benchmark
    public void kryoLz4Deserialize(Blackhole bh) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Output output = new Output(baos);
        kryo.writeObject(output, testUser);
        output.flush();
        byte[] data = output.toBytes();
        output.close();

        byte[] compressed = lz4Compressor.compress(data);
        byte[] decompressed = lz4Decompressor.decompress(compressed, data.length);

        Input input = new Input(decompressed);
        User user = kryo.readObject(input, User.class);
        input.close();
        bh.consume(user);
    }

    @Benchmark
    public void furySnappySerialize(Blackhole bh) throws IOException {
        byte[] data = fury.serialize(testUser);
        byte[] compressed = Snappy.compress(data);
        bh.consume(compressed);
    }

    @Benchmark
    public void furySnappyDeserialize(Blackhole bh) throws IOException {
        byte[] data = fury.serialize(testUser);
        byte[] compressed = Snappy.compress(data);
        byte[] decompressed = Snappy.uncompress(compressed);
        User user = (User) fury.deserialize(decompressed);
        bh.consume(user);
    }

    @Benchmark
    public void fstZstdSerialize(Blackhole bh) throws Exception {
        FSTObjectOutput output = fstConfig.getObjectOutput();
        output.writeObject(testUser);
        byte[] data = output.getCopyOfWrittenBuffer();
        output.close();

        byte[] compressed = Zstd.compress(data);
        bh.consume(compressed);
    }

    @Benchmark
    public void fstZstdDeserialize(Blackhole bh) throws Exception {
        FSTObjectOutput output = fstConfig.getObjectOutput();
        output.writeObject(testUser);
        byte[] data = output.getCopyOfWrittenBuffer();
        output.close();

        byte[] compressed = Zstd.compress(data);
        byte[] decompressed = Zstd.decompress(compressed, data.length);

        FSTObjectInput input = fstConfig.getObjectInput(decompressed);
        User user = (User) input.readObject();
        input.close();
        bh.consume(user);
    }
}
