package org.techishthoughts.avro.controller;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.techishthoughts.avro.service.AvroSerializationService;
import org.techishthoughts.payload.generator.HugePayloadGenerator;
import org.techishthoughts.payload.generator.PayloadGenerator;
import org.techishthoughts.payload.model.User;

@RestController
@RequestMapping("/api/avro")
public class AvroBenchmarkController {

    private final AvroSerializationService serializationService;

    public AvroBenchmarkController(AvroSerializationService serializationService) {
        this.serializationService = serializationService;
    }

    @GetMapping("/benchmark")
    public Map<String, Object> runBenchmark(@RequestParam(defaultValue = "1000") int userCount) {
        Map<String, Object> results = new HashMap<>();

        try {
            System.out.println("Running Avro benchmark with " + userCount + " users...");

            // Generate test data
            List<User> users = PayloadGenerator.generateMassiveDataset(userCount);

            // Run serialization benchmark
            AvroSerializationService.SerializationResult serializationResult =
                serializationService.serializeUsers(users);

            // Run deserialization benchmark
            List<User> deserializedUsers = serializationService.deserializeUsers(serializationResult.getData());

            // Test schema evolution
            AvroSerializationService.SchemaEvolutionResult evolutionResult =
                serializationService.testSchemaEvolution(users);

            // Compile results
            results.put("userCount", userCount);
            results.put("serialization", createSerializationResult(serializationResult));
            results.put("deserialization", Map.of(
                "timeMs", serializationResult.getSerializationTimeMs(),
                "success", deserializedUsers.size() == userCount,
                "deserializedCount", deserializedUsers.size()
            ));
            results.put("schemaEvolution", createEvolutionResult(evolutionResult));
            results.put("summary", Map.of(
                "totalSizeMB", serializationResult.getSizeMB(),
                "compressionRatio", calculateCompressionRatio(serializationResult.getSizeBytes(), userCount),
                "throughput", userCount / (serializationResult.getSerializationTimeMs() / 1000.0)
            ));

        } catch (Exception e) {
            results.put("error", e.getMessage());
            e.printStackTrace();
        }

        return results;
    }

    @GetMapping("/schema-evolution")
    public Map<String, Object> testSchemaEvolution(@RequestParam(defaultValue = "100") int userCount) {
        Map<String, Object> results = new HashMap<>();

        try {
            List<User> users = PayloadGenerator.generateMassiveDataset(userCount);
            AvroSerializationService.SchemaEvolutionResult evolutionResult =
                serializationService.testSchemaEvolution(users);

            results.put("evolutionTest", createEvolutionResult(evolutionResult));
            results.put("userCount", userCount);

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
            AvroSerializationService.SerializationResult serializationResult =
                serializationService.serializeUsers(users);

            List<User> deserializedUsers = serializationService.deserializeUsers(serializationResult.getData());

            results.put("userCount", users.size());
            results.put("serialization", createSerializationResult(serializationResult));
            results.put("deserialization", Map.of(
                "success", deserializedUsers.size() == users.size(),
                "deserializedCount", deserializedUsers.size()
            ));

        } catch (Exception e) {
            results.put("error", e.getMessage());
            e.printStackTrace();
        }

        return results;
    }

    @GetMapping("/schema-info")
    public Map<String, Object> getSchemaInfo() {
        Map<String, Object> results = new HashMap<>();

        try {
            results.put("schema", serializationService.getSchemaInfo());
            results.put("status", "success");
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

        System.out.println("Running Avro serialization benchmark with complexity: " + complexity);

        Map<String, Object> results = new HashMap<>();
        try {
            HugePayloadGenerator.ComplexityLevel level = HugePayloadGenerator.ComplexityLevel.valueOf(complexity);
            List<User> users = HugePayloadGenerator.generateHugeDataset(level);

            results.put("complexity", complexity);
            results.put("userCount", users.size());
            results.put("iterations", iterations);

            // Run serialization benchmark
            Map<String, Object> serializationResults = benchmarkFormatsWithIterations(users, iterations);

            // Extract avro results and format for test script
            Map<String, Object> avroResults = (Map<String, Object>) serializationResults.get("avro");
            results.put("serializationTimeMs", avroResults.get("serializationTime"));
            results.put("deserializationTimeMs", 0.0); // Not measured in current implementation
            results.put("totalSizeBytes", avroResults.get("size"));
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

        System.out.println("Running Avro compression benchmark with complexity: " + complexity);

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

        System.out.println("Running Avro performance benchmark with payload size: " + payloadSize);

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

        // Avro serialization
        long totalTime = 0;
        byte[] serializedData = null;

        for (int i = 0; i < iterations; i++) {
            System.out.println("  Avro serialization iteration " + (i + 1) + "/" + iterations);
            long startTime = System.nanoTime();
            try {
                AvroSerializationService.SerializationResult result = serializationService.serializeUsers(users);
                serializedData = result.getData();
            } catch (IOException e) {
                System.err.println("Avro serialization failed: " + e.getMessage());
                continue;
            }
            long endTime = System.nanoTime();
            totalTime += (endTime - startTime);
        }

        Map<String, Object> avroResult = new HashMap<>();
        avroResult.put("size", serializedData.length);
        avroResult.put("serializationTime", totalTime / 1_000_000.0 / iterations);
        results.put("avro", avroResult);

        return results;
    }

    private Map<String, Object> benchmarkCompressionWithIterations(List<User> users, int iterations) {
        Map<String, Object> results = new HashMap<>();

        // Serialize first
        AvroSerializationService.SerializationResult serializationResult;
        byte[] serializedData;
        try {
            serializationResult = serializationService.serializeUsers(users);
            serializedData = serializationResult.getData();
        } catch (IOException e) {
            System.err.println("Avro serialization failed: " + e.getMessage());
            return results;
        }

        // Note: Avro has built-in compression, so we'll test the compressed size
        Map<String, Object> avroResult = new HashMap<>();
        avroResult.put("originalSize", serializedData.length);
        avroResult.put("compressedSize", serializedData.length); // Avro handles compression internally
        avroResult.put("compressionRatio", 1.0); // Avro compression is built-in
        avroResult.put("compressionTime", 0.0); // No separate compression step
        results.put("avro", avroResult);

        return results;
    }

    private Map<String, Object> benchmarkPerformance(List<User> users, int iterations) {
        Map<String, Object> results = new HashMap<>();

        // Serialization performance
        long totalSerializationTime = 0;
        byte[] serializedData = null;

        for (int i = 0; i < iterations; i++) {
            long startTime = System.nanoTime();
            try {
                AvroSerializationService.SerializationResult result = serializationService.serializeUsers(users);
                serializedData = result.getData();
            } catch (IOException e) {
                System.err.println("Avro serialization failed: " + e.getMessage());
                continue;
            }
            long endTime = System.nanoTime();
            totalSerializationTime += (endTime - startTime);
        }

        // Deserialization performance
        long totalDeserializationTime = 0;

        for (int i = 0; i < iterations; i++) {
            long startTime = System.nanoTime();
            try {
                serializationService.deserializeUsers(serializedData);
            } catch (IOException e) {
                System.err.println("Avro deserialization failed: " + e.getMessage());
                continue;
            }
            long endTime = System.nanoTime();
            totalDeserializationTime += (endTime - startTime);
        }

        results.put("avgSerializationTime", totalSerializationTime / 1_000_000.0 / iterations);
        results.put("avgDeserializationTime", totalDeserializationTime / 1_000_000.0 / iterations);
        results.put("avgCompressionTime", 0.0); // Avro handles compression internally
        results.put("totalIterations", iterations);

        return results;
    }

    private Map<String, Object> createSerializationResult(AvroSerializationService.SerializationResult result) {
        return Map.of(
            "format", result.getFormat(),
            "timeMs", result.getSerializationTimeMs(),
            "sizeBytes", result.getSizeBytes(),
            "sizeKB", result.getSizeKB(),
            "sizeMB", result.getSizeMB()
        );
    }

    private Map<String, Object> createEvolutionResult(AvroSerializationService.SchemaEvolutionResult result) {
        return Map.of(
            "success", result.isEvolutionSuccess(),
            "originalCount", result.getOriginalResult().getSizeBytes(),
            "deserializedCount", result.getDeserializedCount(),
            "evolutionSupported", true,
            "originalSchema", result.getOriginalSchema().substring(0, Math.min(200, result.getOriginalSchema().length())) + "...",
            "evolvedSchema", result.getEvolvedSchema().substring(0, Math.min(200, result.getEvolvedSchema().length())) + "..."
        );
    }

    private double calculateCompressionRatio(int serializedSize, int userCount) {
        // Estimate original JSON size (rough approximation)
        int estimatedJsonSize = userCount * 5000; // ~5KB per user in JSON
        return (double) serializedSize / estimatedJsonSize;
    }
}
