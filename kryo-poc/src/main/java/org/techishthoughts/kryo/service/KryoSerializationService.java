package org.techishthoughts.kryo.service;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.techishthoughts.kryo.service.KryoSerializationService.CachingResult;
import org.techishthoughts.kryo.service.KryoSerializationService.PerformanceResult;
import org.techishthoughts.kryo.service.KryoSerializationService.SerializationResult;
import org.techishthoughts.payload.model.Address;
import org.techishthoughts.payload.model.Education;
import org.techishthoughts.payload.model.Language;
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

@Service
public class KryoSerializationService {

    private final Kryo kryo;
    private final Kryo optimizedKryo;
    private final RedisTemplate<String, byte[]> redisTemplate;
    private final Map<String, byte[]> memoryCache = new ConcurrentHashMap<>();
    private final boolean redisAvailable;

        // Thread-local Kryo instances for thread safety
    private final ThreadLocal<Kryo> threadKryo = ThreadLocal.withInitial(() -> {
        Kryo k = new Kryo();
        k.setRegistrationRequired(false);
        k.setReferences(true);
        k.setAutoReset(false);

        // Register classes for better performance
        k.register(User.class);
        k.register(UserProfile.class);
        k.register(Address.class);
        k.register(Order.class);
        k.register(OrderItem.class);
        k.register(Payment.class);
        k.register(TrackingEvent.class);
        k.register(SocialConnection.class);
        k.register(Skill.class);
        k.register(Education.class);
        k.register(Language.class);
        k.register(java.time.LocalDateTime.class);
        k.register(java.util.ArrayList.class);
        k.register(java.util.HashMap.class);
        k.register(java.util.LinkedHashMap.class);

        return k;
    });

    private final ThreadLocal<Kryo> threadOptimizedKryo = ThreadLocal.withInitial(() -> {
        Kryo k = new Kryo();
        k.setRegistrationRequired(false);
        k.setReferences(false);
        k.setAutoReset(false);
        k.setCopyReferences(false);

        // Register classes for better performance
        k.register(User.class);
        k.register(UserProfile.class);
        k.register(Address.class);
        k.register(Order.class);
        k.register(OrderItem.class);
        k.register(Payment.class);
        k.register(TrackingEvent.class);
        k.register(SocialConnection.class);
        k.register(Skill.class);
        k.register(Education.class);
        k.register(Language.class);
        k.register(java.time.LocalDateTime.class);

        return k;
    });

    public KryoSerializationService(@Qualifier("kryo") Kryo kryo,
                                   @Qualifier("optimizedKryo") Kryo optimizedKryo,
                                   @org.springframework.beans.factory.annotation.Autowired(required = false) RedisTemplate<String, byte[]> redisTemplate) {
        this.kryo = kryo;
        this.optimizedKryo = optimizedKryo;
        this.redisTemplate = redisTemplate;
        this.redisAvailable = redisTemplate != null;
    }

            public SerializationResult serializeUsers(List<User> users) {
        return serializeUsers(users, threadKryo.get(), "kryo_standard");
    }

    public SerializationResult serializeUsersOptimized(List<User> users) {
        return serializeUsers(users, threadOptimizedKryo.get(), "kryo_optimized");
    }

                private SerializationResult serializeUsers(List<User> users, Kryo kryoInstance, String format) {
        long startTime = System.nanoTime();

        // Use direct buffer instead of ByteArrayOutputStream
        Output output = new Output(1024 * 1024); // 1MB buffer

        try {
            // Use the Kryo instance directly (it should be thread-safe with proper configuration)
            Kryo threadKryo = kryoInstance;

            System.out.println("=== KRYO DEBUG START ===");
            System.out.println("Kryo serialization: Starting with " + users.size() + " users");
            System.out.println("Kryo instance: " + threadKryo.getClass().getName());
            System.out.println("Kryo registration required: " + threadKryo.isRegistrationRequired());
            System.out.println("Kryo references: " + threadKryo.getReferences());

            // Write user count first
            output.writeInt(users.size());
            System.out.println("Kryo serialization: Wrote user count: " + users.size());
            System.out.println("Output position after writing count: " + output.position());

            // Serialize each user
            int successfulUsers = 0;
            for (int i = 0; i < users.size(); i++) {
                User user = users.get(i);
                try {
                    System.out.println("Attempting to serialize user " + i + ": " + user.getUsername());
                    System.out.println("Output position before serializing user: " + output.position());

                    threadKryo.writeObject(output, user);
                    successfulUsers++;

                    System.out.println("Successfully serialized user " + i + ". Output position: " + output.position());
                } catch (Exception e) {
                    System.out.println("ERROR: Failed to serialize user " + i + ": " + e.getMessage());
                    e.printStackTrace();
                    // Continue with remaining users
                }
            }

            System.out.println("Kryo serialization: Successfully serialized " + successfulUsers + " out of " + users.size() + " users");

            output.flush();
            byte[] data = output.toBytes();
            long serializationTime = System.nanoTime() - startTime;

            System.out.println("Kryo serialization: Produced " + data.length + " bytes");
            System.out.println("Output buffer size: " + output.getBuffer().length);
            System.out.println("Output position: " + output.position());
            System.out.println("=== KRYO DEBUG END ===");

            return new SerializationResult(format, data, serializationTime, data.length);

        } catch (Exception e) {
            System.out.println("ERROR during Kryo serialization: " + e.getMessage());
            e.printStackTrace();
            return new SerializationResult(format, new byte[0], 0, 0);
        } finally {
            output.close();
        }
    }

    public List<User> deserializeUsers(byte[] data) {
        return deserializeUsers(data, threadKryo.get());
    }

    public List<User> deserializeUsersOptimized(byte[] data) {
        return deserializeUsers(data, threadOptimizedKryo.get());
    }

    private List<User> deserializeUsers(byte[] data, Kryo kryoInstance) {
        long startTime = System.nanoTime();

        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        Input input = new Input(bais);

                try {
            // Use the Kryo instance directly (it should be thread-safe with proper configuration)
            Kryo threadKryo = kryoInstance;

            // Read user count
            int userCount = input.readInt();
            List<User> users = new java.util.ArrayList<>(userCount);

            // Deserialize each user
            for (int i = 0; i < userCount; i++) {
                try {
                    User user = threadKryo.readObject(input, User.class);
                    users.add(user);
                } catch (Exception e) {
                    System.out.println("Warning: Failed to deserialize user " + i + ": " + e.getMessage());
                    // Continue with remaining users
                }
            }

            long deserializationTime = System.nanoTime() - startTime;
            System.out.println("Kryo deserialization took: " + (deserializationTime / 1_000_000.0) + " ms");

            return users;

        } catch (Exception e) {
            System.out.println("Error during Kryo deserialization: " + e.getMessage());
            return new java.util.ArrayList<>();
        } finally {
            input.close();
        }
    }

    public CachingResult cacheUsers(List<User> users, String cacheKey) {
        long startTime = System.nanoTime();

        SerializationResult serializationResult = serializeUsersOptimized(users);

        // Cache in memory
        memoryCache.put(cacheKey, serializationResult.getData());

        // Cache in Redis (if available)
        if (redisAvailable) {
            try {
                redisTemplate.opsForValue().set(cacheKey, serializationResult.getData());
            } catch (Exception e) {
                System.out.println("Warning: Redis caching failed, using memory cache only: " + e.getMessage());
            }
        }

        long cachingTime = System.nanoTime() - startTime;

        return new CachingResult(cacheKey, serializationResult, cachingTime);
    }

    public List<User> retrieveFromCache(String cacheKey) {
        long startTime = System.nanoTime();

        // Try memory cache first
        byte[] data = memoryCache.get(cacheKey);

        if (data == null && redisAvailable) {
            // Try Redis cache
            try {
                data = redisTemplate.opsForValue().get(cacheKey);
            } catch (Exception e) {
                System.out.println("Warning: Redis retrieval failed: " + e.getMessage());
            }
        }

        if (data == null) {
            return null;
        }

        List<User> users = deserializeUsersOptimized(data);
        long retrievalTime = System.nanoTime() - startTime;

        System.out.println("Cache retrieval took: " + (retrievalTime / 1_000_000.0) + " ms");

        return users;
    }

    public PerformanceResult benchmarkPerformance(List<User> users, int iterations) {
        long totalSerializationTime = 0;
        long totalDeserializationTime = 0;
        byte[] serializedData = null;

        // Warm up
        for (int i = 0; i < 10; i++) {
            SerializationResult result = serializeUsersOptimized(users);
            deserializeUsersOptimized(result.getData());
        }

        // Benchmark serialization
        for (int i = 0; i < iterations; i++) {
            long startTime = System.nanoTime();
            SerializationResult result = serializeUsersOptimized(users);
            totalSerializationTime += System.nanoTime() - startTime;

            if (serializedData == null) {
                serializedData = result.getData();
            }
        }

        // Benchmark deserialization
        for (int i = 0; i < iterations; i++) {
            long startTime = System.nanoTime();
            deserializeUsersOptimized(serializedData);
            totalDeserializationTime += System.nanoTime() - startTime;
        }

        double avgSerializationTime = totalSerializationTime / (double) iterations;
        double avgDeserializationTime = totalDeserializationTime / (double) iterations;

        return new PerformanceResult(
            iterations,
            avgSerializationTime / 1_000_000.0, // Convert to ms
            avgDeserializationTime / 1_000_000.0, // Convert to ms
            serializedData != null ? serializedData.length : 0,
            users.size() / (avgSerializationTime / 1_000_000_000.0), // Throughput
            users.size() / (avgDeserializationTime / 1_000_000_000.0) // Throughput
        );
    }

    public static class SerializationResult {
        private final String format;
        private final byte[] data;
        private final long serializationTimeNs;
        private final int sizeBytes;

        public SerializationResult(String format, byte[] data, long serializationTimeNs, int sizeBytes) {
            this.format = format;
            this.data = data;
            this.serializationTimeNs = serializationTimeNs;
            this.sizeBytes = sizeBytes;
        }

        public String getFormat() { return format; }
        public byte[] getData() { return data; }
        public long getSerializationTimeNs() { return serializationTimeNs; }
        public double getSerializationTimeMs() { return serializationTimeNs / 1_000_000.0; }
        public int getSizeBytes() { return sizeBytes; }
        public double getSizeKB() { return sizeBytes / 1024.0; }
        public double getSizeMB() { return sizeBytes / (1024.0 * 1024.0); }
    }

    public static class CachingResult {
        private final String cacheKey;
        private final SerializationResult serializationResult;
        private final long cachingTimeNs;

        public CachingResult(String cacheKey, SerializationResult serializationResult, long cachingTimeNs) {
            this.cacheKey = cacheKey;
            this.serializationResult = serializationResult;
            this.cachingTimeNs = cachingTimeNs;
        }

        public String getCacheKey() { return cacheKey; }
        public SerializationResult getSerializationResult() { return serializationResult; }
        public long getCachingTimeNs() { return cachingTimeNs; }
        public double getCachingTimeMs() { return cachingTimeNs / 1_000_000.0; }
    }

    public static class PerformanceResult {
        private final int iterations;
        private final double avgSerializationTimeMs;
        private final double avgDeserializationTimeMs;
        private final int payloadSizeBytes;
        private final double serializationThroughput;
        private final double deserializationThroughput;

        public PerformanceResult(int iterations, double avgSerializationTimeMs, double avgDeserializationTimeMs,
                               int payloadSizeBytes, double serializationThroughput, double deserializationThroughput) {
            this.iterations = iterations;
            this.avgSerializationTimeMs = avgSerializationTimeMs;
            this.avgDeserializationTimeMs = avgDeserializationTimeMs;
            this.payloadSizeBytes = payloadSizeBytes;
            this.serializationThroughput = serializationThroughput;
            this.deserializationThroughput = deserializationThroughput;
        }

        public int getIterations() { return iterations; }
        public double getAvgSerializationTimeMs() { return avgSerializationTimeMs; }
        public double getAvgDeserializationTimeMs() { return avgDeserializationTimeMs; }
        public int getPayloadSizeBytes() { return payloadSizeBytes; }
        public double getSerializationThroughput() { return serializationThroughput; }
        public double getDeserializationThroughput() { return deserializationThroughput; }
    }
}
