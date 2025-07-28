package org.techishthoughts.benchmark;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.io.JsonDecoder;
import org.apache.avro.io.JsonEncoder;
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
import org.techishthoughts.payload.model.Address;
import org.techishthoughts.payload.model.Order;
import org.techishthoughts.payload.model.OrderItem;
import org.techishthoughts.payload.model.Payment;
import org.techishthoughts.payload.model.Skill;
import org.techishthoughts.payload.model.SocialConnection;
import org.techishthoughts.payload.model.TrackingEvent;
import org.techishthoughts.payload.model.User;
import org.techishthoughts.payload.model.UserProfile;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.DefaultSerializers;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@Fork(value = 1, jvmArgs = {"-Xms2G", "-Xmx2G"})
@Warmup(iterations = 3, time = 2)
@Measurement(iterations = 5, time = 3)
public class SerializationBenchmark {

    private User testUser;
    private ObjectMapper jsonMapper;
    private ObjectMapper cborMapper;
    private Kryo kryo;
    private Fury fury;
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
        fury.register(TrackingEvent.TrackingEventType.class);

        // Register collections
        fury.register(java.util.ArrayList.class);
        fury.register(java.util.HashMap.class);
        fury.register(java.util.LinkedHashMap.class);

        // Initialize Avro
        String schemaJson = """
            {
                "type": "record",
                "name": "User",
                "fields": [
                    {"name": "id", "type": "long"},
                    {"name": "username", "type": "string"},
                    {"name": "email", "type": "string"},
                    {"name": "firstName", "type": "string"},
                    {"name": "lastName", "type": "string"},
                    {"name": "isActive", "type": "boolean"}
                ]
            }
            """;
        avroSchema = new org.apache.avro.Schema.Parser().parse(schemaJson);
        avroWriter = new GenericDatumWriter<>(avroSchema);
        avroReader = new GenericDatumReader<>(avroSchema);
    }

    @Benchmark
    public void jacksonJsonSerialize(Blackhole bh) throws IOException {
        byte[] bytes = jsonMapper.writeValueAsBytes(testUser);
        bh.consume(bytes);
    }

    @Benchmark
    public void jacksonJsonDeserialize(Blackhole bh) throws IOException {
        byte[] bytes = jsonMapper.writeValueAsBytes(testUser);
        User user = jsonMapper.readValue(bytes, User.class);
        bh.consume(user);
    }

    @Benchmark
    public void jacksonCborSerialize(Blackhole bh) throws IOException {
        byte[] bytes = cborMapper.writeValueAsBytes(testUser);
        bh.consume(bytes);
    }

    @Benchmark
    public void jacksonCborDeserialize(Blackhole bh) throws IOException {
        byte[] bytes = cborMapper.writeValueAsBytes(testUser);
        User user = cborMapper.readValue(bytes, User.class);
        bh.consume(user);
    }

    @Benchmark
    public void kryoSerialize(Blackhole bh) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Output output = new Output(baos);
        kryo.writeObject(output, testUser);
        output.close();
        byte[] bytes = baos.toByteArray();
        bh.consume(bytes);
    }

    @Benchmark
    public void kryoDeserialize(Blackhole bh) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Output output = new Output(baos);
        kryo.writeObject(output, testUser);
        output.close();
        byte[] bytes = baos.toByteArray();

        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        Input input = new Input(bais);
        User user = kryo.readObject(input, User.class);
        input.close();
        bh.consume(user);
    }

    @Benchmark
    public void furySerialize(Blackhole bh) {
        byte[] bytes = fury.serialize(testUser);
        bh.consume(bytes);
    }

    @Benchmark
    public void furyDeserialize(Blackhole bh) {
        byte[] bytes = fury.serialize(testUser);
        User user = (User) fury.deserialize(bytes);
        bh.consume(user);
    }

    @Benchmark
    public void avroSerialize(Blackhole bh) throws IOException {
        GenericRecord record = new org.apache.avro.generic.GenericData.Record(avroSchema);
        record.put("id", testUser.getId());
        record.put("username", testUser.getUsername());
        record.put("email", testUser.getEmail());
        record.put("firstName", testUser.getFirstName());
        record.put("lastName", testUser.getLastName());
        record.put("isActive", testUser.getIsActive());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        JsonEncoder encoder = EncoderFactory.get().jsonEncoder(avroSchema, baos);
        avroWriter.write(record, encoder);
        encoder.flush();
        byte[] bytes = baos.toByteArray();
        bh.consume(bytes);
    }

    @Benchmark
    public void avroDeserialize(Blackhole bh) throws IOException {
        GenericRecord record = new org.apache.avro.generic.GenericData.Record(avroSchema);
        record.put("id", testUser.getId());
        record.put("username", testUser.getUsername());
        record.put("email", testUser.getEmail());
        record.put("firstName", testUser.getFirstName());
        record.put("lastName", testUser.getLastName());
        record.put("isActive", testUser.getIsActive());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        JsonEncoder encoder = EncoderFactory.get().jsonEncoder(avroSchema, baos);
        avroWriter.write(record, encoder);
        encoder.flush();
        byte[] bytes = baos.toByteArray();

        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        JsonDecoder decoder = DecoderFactory.get().jsonDecoder(avroSchema, bais);
        GenericRecord readRecord = avroReader.read(null, decoder);
        bh.consume(readRecord);
    }
}
