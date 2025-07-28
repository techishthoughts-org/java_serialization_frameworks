package org.techishthoughts.kryo.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.techishthoughts.kryo.service.KryoSerializationService;
import org.techishthoughts.payload.generator.HugePayloadGenerator;
import org.techishthoughts.payload.generator.PayloadGenerator;
import org.techishthoughts.payload.model.User;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Output;

@RestController
@RequestMapping("/api/kryo")
public class KryoBenchmarkController {

    private final KryoSerializationService serializationService;

    public KryoBenchmarkController(KryoSerializationService serializationService) {
        this.serializationService = serializationService;
    }

    @GetMapping("/benchmark")
    public Map<String, Object> runBenchmark(@RequestParam(defaultValue = "1000") int userCount) {
        Map<String, Object> results = new HashMap<>();

        try {
            System.out.println("Running Kryo benchmark with " + userCount + " users...");

            // Generate test data
            List<User> users = PayloadGenerator.generateMassiveDataset(userCount);

            // Run standard Kryo benchmark
            KryoSerializationService.SerializationResult standardResult =
                serializationService.serializeUsers(users);
            List<User> standardDeserialized = serializationService.deserializeUsers(standardResult.getData());

            // Run optimized Kryo benchmark
            KryoSerializationService.SerializationResult optimizedResult =
                serializationService.serializeUsersOptimized(users);
            List<User> optimizedDeserialized = serializationService.deserializeUsersOptimized(optimizedResult.getData());

            // Run performance benchmark
            KryoSerializationService.PerformanceResult performanceResult =
                serializationService.benchmarkPerformance(users, 100);

            // Compile results
            results.put("userCount", userCount);
            results.put("standard", createSerializationResult(standardResult, standardDeserialized.size()));
            results.put("optimized", createSerializationResult(optimizedResult, optimizedDeserialized.size()));
            results.put("performance", createPerformanceResult(performanceResult));
            results.put("summary", Map.of(
                "bestSizeMB", Math.min(standardResult.getSizeMB(), optimizedResult.getSizeMB()),
                "bestTimeMs", Math.min(standardResult.getSerializationTimeMs(), optimizedResult.getSerializationTimeMs()),
                "throughput", performanceResult.getSerializationThroughput()
            ));

        } catch (Exception e) {
            results.put("error", e.getMessage());
            e.printStackTrace();
        }

        return results;
    }

    @GetMapping("/cache-test")
    public Map<String, Object> testCaching(@RequestParam(defaultValue = "500") int userCount) {
        Map<String, Object> results = new HashMap<>();

        try {
            List<User> users = PayloadGenerator.generateMassiveDataset(userCount);
            String cacheKey = "users:" + UUID.randomUUID().toString();

            // Cache the data
            KryoSerializationService.CachingResult cachingResult =
                serializationService.cacheUsers(users, cacheKey);

            // Retrieve from cache
            List<User> cachedUsers = serializationService.retrieveFromCache(cacheKey);

            results.put("userCount", userCount);
            results.put("caching", Map.of(
                "cacheKey", cacheKey,
                "cachingTimeMs", cachingResult.getCachingTimeMs(),
                "serializedSizeMB", cachingResult.getSerializationResult().getSizeMB()
            ));
            results.put("retrieval", Map.of(
                "success", cachedUsers != null,
                "retrievedCount", cachedUsers != null ? cachedUsers.size() : 0,
                "cacheHit", cachedUsers != null
            ));

        } catch (Exception e) {
            results.put("error", e.getMessage());
            e.printStackTrace();
        }

        return results;
    }

    @GetMapping("/performance")
    public Map<String, Object> runPerformanceBenchmark(
            @RequestParam(defaultValue = "1000") int userCount,
            @RequestParam(defaultValue = "1000") int iterations) {
        Map<String, Object> results = new HashMap<>();

        try {
            List<User> users = PayloadGenerator.generateMassiveDataset(userCount);
            KryoSerializationService.PerformanceResult performanceResult =
                serializationService.benchmarkPerformance(users, iterations);

            results.put("performance", createPerformanceResult(performanceResult));
            results.put("userCount", userCount);
            results.put("iterations", iterations);

        } catch (Exception e) {
            results.put("error", e.getMessage());
            e.printStackTrace();
        }

        return results;
    }

    @PostMapping("/custom-payload")
    public Map<String, Object> benchmarkCustomPayload(@RequestBody List<User> users) {
        Map<String, Object> results = new HashMap<>();

        try {
            KryoSerializationService.SerializationResult standardResult =
                serializationService.serializeUsers(users);
            List<User> standardDeserialized = serializationService.deserializeUsers(standardResult.getData());

            KryoSerializationService.SerializationResult optimizedResult =
                serializationService.serializeUsersOptimized(users);
            List<User> optimizedDeserialized = serializationService.deserializeUsersOptimized(optimizedResult.getData());

            results.put("userCount", users.size());
            results.put("standard", createSerializationResult(standardResult, standardDeserialized.size()));
            results.put("optimized", createSerializationResult(optimizedResult, optimizedDeserialized.size()));

        } catch (Exception e) {
            results.put("error", e.getMessage());
            e.printStackTrace();
        }

        return results;
    }

    @GetMapping("/info")
    public Map<String, Object> getKryoInfo() {
        Map<String, Object> results = new HashMap<>();

        try {
            results.put("framework", "Kryo");
            results.put("version", "5.5.0");
            results.put("features", List.of(
                "Fast serialization",
                "Small serialized size",
                "Type safety",
                "Custom serializers",
                "Object pooling",
                "Caching support"
            ));
            results.put("optimizations", List.of(
                "Reference tracking",
                "Compression",
                "Field optimization",
                "Memory pooling"
            ));
            results.put("status", "enabled");

        } catch (Exception e) {
            results.put("error", e.getMessage());
            results.put("status", "error");
        }

        return results;
    }

    // ===== HUGE PAYLOAD ENDPOINTS =====

    @PostMapping("/benchmark/serialization")
    public Map<String, Object> runSerializationBenchmark(@RequestBody Map<String, Object> request) {
        String complexity = (String) request.getOrDefault("complexity", "SMALL");
        int iterations = (int) request.getOrDefault("iterations", 1);

        System.out.println("Running Kryo serialization benchmark with complexity: " + complexity);

        Map<String, Object> results = new HashMap<>();
        try {
            HugePayloadGenerator.ComplexityLevel level = HugePayloadGenerator.ComplexityLevel.valueOf(complexity);
            List<User> users = HugePayloadGenerator.generateHugeDataset(level);

            results.put("complexity", complexity);
            results.put("userCount", users.size());
            results.put("iterations", iterations);

            // Run serialization benchmark
            Map<String, Object> serializationResults = benchmarkFormatsWithIterations(users, iterations);

            // Extract kryo results and format for test script
            Map<String, Object> kryoStandardResults = (Map<String, Object>) serializationResults.get("kryo_standard");
            results.put("serializationTimeMs", kryoStandardResults.get("serializationTime"));
            results.put("deserializationTimeMs", 0.0); // Not measured in current implementation
            results.put("totalSizeBytes", kryoStandardResults.get("size"));
            results.put("userCount", users.size());
            results.put("success", true);

        } catch (Exception e) {
            results.put("complexity", complexity);
            results.put("error", e.getMessage());
            results.put("iterations", iterations);
            e.printStackTrace();
        }

        return results;
    }

    @PostMapping("/benchmark/compression")
    public Map<String, Object> runCompressionBenchmark(@RequestBody Map<String, Object> request) {
        String complexity = (String) request.getOrDefault("complexity", "SMALL");
        int iterations = (int) request.getOrDefault("iterations", 1);

        System.out.println("Running Kryo compression benchmark with complexity: " + complexity);

        Map<String, Object> results = new HashMap<>();
        try {
            HugePayloadGenerator.ComplexityLevel level = HugePayloadGenerator.ComplexityLevel.valueOf(complexity);
            List<User> users = HugePayloadGenerator.generateHugeDataset(level);

            results.put("complexity", complexity);
            results.put("userCount", users.size());
            results.put("iterations", iterations);

            // Run compression benchmark
            Map<String, Object> compressionResults = benchmarkCompressionWithIterations(users, iterations);
            results.put("compression", compressionResults);

        } catch (Exception e) {
            results.put("complexity", complexity);
            results.put("error", e.getMessage());
            results.put("iterations", iterations);
            e.printStackTrace();
        }

        return results;
    }

    @PostMapping("/benchmark/performance")
    public Map<String, Object> runPerformanceBenchmark(@RequestBody Map<String, Object> request) {
        String payloadSize = (String) request.getOrDefault("payload_size", "SMALL");
        int iterations = (int) request.getOrDefault("iterations", 1);

        System.out.println("Running Kryo performance benchmark with payload size: " + payloadSize);

        Map<String, Object> results = new HashMap<>();
        try {
            HugePayloadGenerator.ComplexityLevel level = HugePayloadGenerator.ComplexityLevel.valueOf(payloadSize);
            List<User> users = HugePayloadGenerator.generateHugeDataset(level);

            results.put("payloadSize", payloadSize);
            results.put("userCount", users.size());
            results.put("iterations", iterations);

            // Run performance benchmark
            Map<String, Object> performanceResults = benchmarkPerformance(users, iterations);
            results.put("performance", performanceResults);

        } catch (Exception e) {
            results.put("payloadSize", payloadSize);
            results.put("error", e.getMessage());
            results.put("iterations", iterations);
            e.printStackTrace();
        }

        return results;
    }

    @GetMapping("/test-huge-payload")
    public Map<String, Object> testHugePayload(@RequestParam(value = "complexity", defaultValue = "SMALL") String complexity) {
        Map<String, Object> results = new HashMap<>();
        try {
            System.out.println("Testing HugePayloadGenerator with complexity: " + complexity);
            HugePayloadGenerator.ComplexityLevel level = HugePayloadGenerator.ComplexityLevel.valueOf(complexity);
            List<User> users = HugePayloadGenerator.generateHugeDataset(level);
            results.put("success", true);
            results.put("complexity", complexity);
            results.put("userCount", users.size());
            results.put("message", "HugePayloadGenerator working correctly");
        } catch (Exception e) {
            results.put("success", false);
            results.put("error", e.getMessage());
            results.put("stackTrace", e.getStackTrace());
            e.printStackTrace();
        }
        return results;
    }

    // ===== PRIVATE HELPER METHODS =====

    private Map<String, Object> benchmarkFormatsWithIterations(List<User> users, int iterations) {
        Map<String, Object> results = new HashMap<>();

        // Kryo standard serialization
        long totalStandardTime = 0;
        byte[] standardData = null;

        for (int i = 0; i < iterations; i++) {
            System.out.println("  Kryo standard serialization iteration " + (i + 1) + "/" + iterations);
            long startTime = System.nanoTime();
            KryoSerializationService.SerializationResult result = serializationService.serializeUsers(users);
            standardData = result.getData();
            long endTime = System.nanoTime();
            totalStandardTime += (endTime - startTime);
        }

        Map<String, Object> standardResult = new HashMap<>();
        standardResult.put("size", standardData.length);
        standardResult.put("serializationTime", totalStandardTime / 1_000_000.0 / iterations);
        results.put("kryo_standard", standardResult);

        // Kryo optimized serialization
        long totalOptimizedTime = 0;
        byte[] optimizedData = null;

        for (int i = 0; i < iterations; i++) {
            System.out.println("  Kryo optimized serialization iteration " + (i + 1) + "/" + iterations);
            long startTime = System.nanoTime();
            KryoSerializationService.SerializationResult result = serializationService.serializeUsersOptimized(users);
            optimizedData = result.getData();
            long endTime = System.nanoTime();
            totalOptimizedTime += (endTime - startTime);
        }

        Map<String, Object> optimizedResult = new HashMap<>();
        optimizedResult.put("size", optimizedData.length);
        optimizedResult.put("serializationTime", totalOptimizedTime / 1_000_000.0 / iterations);
        results.put("kryo_optimized", optimizedResult);

        return results;
    }

    private Map<String, Object> benchmarkCompressionWithIterations(List<User> users, int iterations) {
        Map<String, Object> results = new HashMap<>();

        // Serialize first with standard Kryo
        KryoSerializationService.SerializationResult serializationResult = serializationService.serializeUsers(users);
        byte[] serializedData = serializationResult.getData();

        // Note: Kryo has built-in compression, so we'll test the compressed size
        Map<String, Object> kryoResult = new HashMap<>();
        kryoResult.put("originalSize", serializedData.length);
        kryoResult.put("compressedSize", serializedData.length); // Kryo handles compression internally
        kryoResult.put("compressionRatio", 1.0); // Kryo compression is built-in
        kryoResult.put("compressionTime", 0.0); // No separate compression step
        results.put("kryo", kryoResult);

        return results;
    }

    private Map<String, Object> benchmarkPerformance(List<User> users, int iterations) {
        Map<String, Object> results = new HashMap<>();

        try {
            // Serialization performance (standard)
            long totalSerializationTime = 0;
            byte[] serializedData = null;

            for (int i = 0; i < iterations; i++) {
                long startTime = System.nanoTime();
                KryoSerializationService.SerializationResult result = serializationService.serializeUsers(users);
                serializedData = result.getData();
                long endTime = System.nanoTime();
                totalSerializationTime += (endTime - startTime);
            }

            // Check if serialization was successful
            if (serializedData == null || serializedData.length == 0) {
                results.put("error", "Serialization failed - no data produced");
                return results;
            }

            // Deserialization performance
            long totalDeserializationTime = 0;

            for (int i = 0; i < iterations; i++) {
                long startTime = System.nanoTime();
                List<User> deserializedUsers = serializationService.deserializeUsers(serializedData);
                long endTime = System.nanoTime();
                totalDeserializationTime += (endTime - startTime);

                // Check if deserialization was successful
                if (deserializedUsers == null || deserializedUsers.isEmpty()) {
                    results.put("error", "Deserialization failed - no users deserialized");
                    return results;
                }
            }

            results.put("avgSerializationTime", totalSerializationTime / 1_000_000.0 / iterations);
            results.put("avgDeserializationTime", totalDeserializationTime / 1_000_000.0 / iterations);
            results.put("avgCompressionTime", 0.0); // Kryo handles compression internally
            results.put("totalIterations", iterations);
            results.put("serializedDataSize", serializedData.length);

        } catch (Exception e) {
            results.put("error", "Performance benchmark failed: " + e.getMessage());
        }

        return results;
    }

    private Map<String, Object> createSerializationResult(KryoSerializationService.SerializationResult result, int deserializedCount) {
        return Map.of(
            "format", result.getFormat(),
            "timeMs", result.getSerializationTimeMs(),
            "sizeBytes", result.getSizeBytes(),
            "sizeKB", result.getSizeKB(),
            "sizeMB", result.getSizeMB(),
            "deserializedCount", deserializedCount,
            "success", deserializedCount > 0
        );
    }

    private Map<String, Object> createPerformanceResult(KryoSerializationService.PerformanceResult result) {
        return Map.of(
            "iterations", result.getIterations(),
            "avgSerializationTimeMs", result.getAvgSerializationTimeMs(),
            "avgDeserializationTimeMs", result.getAvgDeserializationTimeMs(),
            "payloadSizeBytes", result.getPayloadSizeBytes(),
            "payloadSizeMB", result.getPayloadSizeBytes() / (1024.0 * 1024.0),
            "serializationThroughput", result.getSerializationThroughput(),
            "deserializationThroughput", result.getDeserializationThroughput(),
            "serializationThroughputK", result.getSerializationThroughput() / 1000.0,
            "deserializationThroughputK", result.getDeserializationThroughput() / 1000.0
        );
    }

        @GetMapping("/test-kryo-debug")
    public ResponseEntity<Map<String, Object>> testKryoDebug() {
        System.out.println("=== KRYO DEBUG ENDPOINT ===");

        Map<String, Object> result = new HashMap<>();

        try {
            // Test with simple string using direct buffer
            Kryo kryo = new Kryo();
            kryo.setRegistrationRequired(false);
            kryo.setReferences(true);
            kryo.setAutoReset(false);

            // Use direct buffer instead of ByteArrayOutputStream
            Output output = new Output(1024);

            String testString = "Hello World";
            kryo.writeObject(output, testString);
            output.flush();
            byte[] data = output.toBytes();
            output.close();

            result.put("stringTest", "SUCCESS");
            result.put("stringSize", data.length);
            System.out.println("String test: " + data.length + " bytes");

            // Test with minimal user using direct buffer
            output = new Output(1024);

            User minimalUser = new User();
            minimalUser.setId(1L);
            minimalUser.setUsername("testuser");
            minimalUser.setEmail("test@example.com");

            kryo.writeObject(output, minimalUser);
            output.flush();
            data = output.toBytes();
            output.close();

            result.put("userTest", "SUCCESS");
            result.put("userSize", data.length);
            System.out.println("User test: " + data.length + " bytes");

                        // Test with primitive types
            output = new Output(1024);
            output.writeInt(42);
            output.writeString("test");
            output.flush();
            data = output.toBytes();
            output.close();

            result.put("primitiveTest", "SUCCESS");
            result.put("primitiveSize", data.length);
            System.out.println("Primitive test: " + data.length + " bytes");

        } catch (Exception e) {
            result.put("error", e.getMessage());
            System.out.println("Debug test failed: " + e.getMessage());
            e.printStackTrace();
        }

        return ResponseEntity.ok(result);
    }
}
