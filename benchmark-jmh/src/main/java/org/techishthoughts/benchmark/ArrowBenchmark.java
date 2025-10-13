package org.techishthoughts.benchmark;

import org.apache.arrow.memory.BufferAllocator;
import org.apache.arrow.memory.RootAllocator;
import org.apache.arrow.vector.*;
import org.apache.arrow.vector.complex.ListVector;
import org.apache.arrow.vector.complex.MapVector;
import org.apache.arrow.vector.complex.StructVector;
import org.apache.arrow.vector.types.pojo.ArrowType;
import org.apache.arrow.vector.types.pojo.Field;
import org.apache.arrow.vector.types.pojo.FieldType;
import org.apache.arrow.vector.types.pojo.Schema;
import org.openjdk.jmh.annotations.*;
import org.techishthoughts.payload.generator.UnifiedPayloadGenerator;
import org.techishthoughts.payload.model.User;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.channels.Channels;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.arrow.vector.ipc.ArrowStreamReader;
import org.apache.arrow.vector.ipc.ArrowStreamWriter;

/**
 * JMH Microbenchmark for Apache Arrow Serialization
 *
 * Measures PURE serialization/deserialization performance without HTTP overhead.
 * Apache Arrow is a columnar in-memory data format optimized for analytics.
 *
 * Run with: java -jar target/benchmarks.jar ArrowBenchmark
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 10, time = 1)
@Fork(value = 2, jvmArgs = {"-Xms2G", "-Xmx2G"})
public class ArrowBenchmark {

    private BufferAllocator allocator;
    private List<User> users;
    private byte[] serializedData;
    private Schema schema;

    @Setup(Level.Trial)
    public void setup() throws Exception {
        // Initialize Arrow memory allocator
        allocator = new RootAllocator(Long.MAX_VALUE);

        // Generate test payload (MEDIUM complexity - 100 users)
        UnifiedPayloadGenerator generator = new UnifiedPayloadGenerator();
        users = generator.generateUsers(
            UnifiedPayloadGenerator.ComplexityLevel.MEDIUM,
            100
        );

        // Define Arrow schema for User
        schema = new Schema(Arrays.asList(
            Field.nullable("id", new ArrowType.Int(64, true)),
            Field.nullable("username", new ArrowType.Utf8()),
            Field.nullable("email", new ArrowType.Utf8()),
            Field.nullable("firstName", new ArrowType.Utf8()),
            Field.nullable("lastName", new ArrowType.Utf8()),
            Field.nullable("isActive", new ArrowType.Bool()),
            Field.nullable("loyaltyPoints", new ArrowType.FloatingPoint(org.apache.arrow.vector.types.FloatingPointPrecision.DOUBLE)),
            Field.nullable("createdAt", new ArrowType.Utf8()),
            Field.nullable("lastLoginAt", new ArrowType.Utf8())
        ));

        // Pre-serialize for deserialization benchmark
        serializedData = serializeUsers(users);
    }

    @TearDown(Level.Trial)
    public void tearDown() {
        allocator.close();
    }

    @Benchmark
    public byte[] serialize() throws Exception {
        return serializeUsers(users);
    }

    @Benchmark
    public List<User> deserialize() throws Exception {
        return deserializeUsers(serializedData);
    }

    @Benchmark
    public List<User> roundtrip() throws Exception {
        byte[] data = serializeUsers(users);
        return deserializeUsers(data);
    }

    private byte[] serializeUsers(List<User> userList) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try (VectorSchemaRoot root = VectorSchemaRoot.create(schema, allocator);
             ArrowStreamWriter writer = new ArrowStreamWriter(root, null, Channels.newChannel(out))) {

            writer.start();

            // Get vectors
            BigIntVector idVector = (BigIntVector) root.getVector("id");
            VarCharVector usernameVector = (VarCharVector) root.getVector("username");
            VarCharVector emailVector = (VarCharVector) root.getVector("email");
            VarCharVector firstNameVector = (VarCharVector) root.getVector("firstName");
            VarCharVector lastNameVector = (VarCharVector) root.getVector("lastName");
            BitVector isActiveVector = (BitVector) root.getVector("isActive");
            Float8Vector loyaltyPointsVector = (Float8Vector) root.getVector("loyaltyPoints");
            VarCharVector createdAtVector = (VarCharVector) root.getVector("createdAt");
            VarCharVector lastLoginAtVector = (VarCharVector) root.getVector("lastLoginAt");

            // Set row count
            root.setRowCount(userList.size());

            // Populate vectors
            for (int i = 0; i < userList.size(); i++) {
                User user = userList.get(i);

                if (user.getId() != null) {
                    idVector.setSafe(i, user.getId());
                }
                if (user.getUsername() != null) {
                    usernameVector.setSafe(i, user.getUsername().getBytes());
                }
                if (user.getEmail() != null) {
                    emailVector.setSafe(i, user.getEmail().getBytes());
                }
                if (user.getFirstName() != null) {
                    firstNameVector.setSafe(i, user.getFirstName().getBytes());
                }
                if (user.getLastName() != null) {
                    lastNameVector.setSafe(i, user.getLastName().getBytes());
                }
                if (user.getIsActive() != null) {
                    isActiveVector.setSafe(i, user.getIsActive() ? 1 : 0);
                }
                if (user.getLoyaltyPoints() != null) {
                    loyaltyPointsVector.setSafe(i, user.getLoyaltyPoints());
                }
                if (user.getCreatedAt() != null) {
                    createdAtVector.setSafe(i, user.getCreatedAt().toString().getBytes());
                }
                if (user.getLastLoginAt() != null) {
                    lastLoginAtVector.setSafe(i, user.getLastLoginAt().toString().getBytes());
                }
            }

            writer.writeBatch();
            writer.end();
        }

        return out.toByteArray();
    }

    private List<User> deserializeUsers(byte[] data) throws Exception {
        List<User> result = new ArrayList<>();
        ByteArrayInputStream in = new ByteArrayInputStream(data);

        try (ArrowStreamReader reader = new ArrowStreamReader(in, allocator)) {
            while (reader.loadNextBatch()) {
                VectorSchemaRoot root = reader.getVectorSchemaRoot();

                BigIntVector idVector = (BigIntVector) root.getVector("id");
                VarCharVector usernameVector = (VarCharVector) root.getVector("username");
                VarCharVector emailVector = (VarCharVector) root.getVector("email");
                VarCharVector firstNameVector = (VarCharVector) root.getVector("firstName");
                VarCharVector lastNameVector = (VarCharVector) root.getVector("lastName");
                BitVector isActiveVector = (BitVector) root.getVector("isActive");
                Float8Vector loyaltyPointsVector = (Float8Vector) root.getVector("loyaltyPoints");
                VarCharVector createdAtVector = (VarCharVector) root.getVector("createdAt");
                VarCharVector lastLoginAtVector = (VarCharVector) root.getVector("lastLoginAt");

                for (int i = 0; i < root.getRowCount(); i++) {
                    User user = new User();

                    if (!idVector.isNull(i)) {
                        user.setId(idVector.get(i));
                    }
                    if (!usernameVector.isNull(i)) {
                        user.setUsername(new String(usernameVector.get(i)));
                    }
                    if (!emailVector.isNull(i)) {
                        user.setEmail(new String(emailVector.get(i)));
                    }
                    if (!firstNameVector.isNull(i)) {
                        user.setFirstName(new String(firstNameVector.get(i)));
                    }
                    if (!lastNameVector.isNull(i)) {
                        user.setLastName(new String(lastNameVector.get(i)));
                    }
                    if (!isActiveVector.isNull(i)) {
                        user.setIsActive(isActiveVector.get(i) == 1);
                    }
                    if (!loyaltyPointsVector.isNull(i)) {
                        user.setLoyaltyPoints(loyaltyPointsVector.get(i));
                    }

                    result.add(user);
                }
            }
        }

        return result;
    }
}
