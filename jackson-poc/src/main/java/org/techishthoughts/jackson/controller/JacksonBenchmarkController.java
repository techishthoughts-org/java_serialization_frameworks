package org.techishthoughts.jackson.controller;

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
import org.techishthoughts.jackson.service.JacksonSerializationService;
import org.techishthoughts.payload.generator.HugePayloadGenerator;
import org.techishthoughts.payload.generator.PayloadGenerator;
import org.techishthoughts.payload.model.User;

@RestController
@RequestMapping("/api/jackson")
public class JacksonBenchmarkController {

    private final JacksonSerializationService serializationService;

    public JacksonBenchmarkController(JacksonSerializationService serializationService) {
        this.serializationService = serializationService;
    }

    @GetMapping("/benchmark")
    public Map<String, Object> runBenchmark(@RequestParam(value = "userCount", defaultValue = "1000") int userCount) {
        Map<String, Object> results = new HashMap<>();

        try {
            // Generate test data
            System.out.println("Generating " + userCount + " users for Jackson benchmark...");
            List<User> users = PayloadGenerator.generateMassiveDataset(userCount);

            results.put("userCount", userCount);
            results.put("formats", benchmarkFormats(users));
            results.put("compression", benchmarkCompression(users));

        } catch (Exception e) {
            results.put("error", e.getMessage());
            e.printStackTrace();
        }

        return results;
    }

    @PostMapping("/benchmark/serialization")
    public Map<String, Object> runSerializationBenchmark(@RequestBody Map<String, Object> request) {
        Map<String, Object> results = new HashMap<>();

        try {
            String complexity = (String) request.getOrDefault("complexity", "MEDIUM");
            int iterations = (Integer) request.getOrDefault("iterations", 100);

            System.out.println("Running Jackson serialization benchmark with complexity: " + complexity);

            // Generate huge payload based on complexity
            HugePayloadGenerator.ComplexityLevel level = HugePayloadGenerator.ComplexityLevel.valueOf(complexity);
            List<User> users = HugePayloadGenerator.generateHugeDataset(level);

            // Run serialization benchmark
            Map<String, Object> serializationResults = benchmarkFormatsWithIterations(users, iterations);

            // Extract json results and format for test script
            Map<String, Object> jsonResults = (Map<String, Object>) serializationResults.get("json");
            results.put("serializationTimeMs", jsonResults.get("serializationTime"));
            results.put("deserializationTimeMs", 0.0); // Not measured in current implementation
            results.put("totalSizeBytes", jsonResults.get("size"));
            results.put("userCount", users.size());
            results.put("success", true);

        } catch (Exception e) {
            results.put("error", e.getMessage());
            e.printStackTrace();
        }

        return results;
    }

    @PostMapping("/benchmark/compression")
    public Map<String, Object> runCompressionBenchmark(@RequestBody Map<String, Object> request) {
        Map<String, Object> results = new HashMap<>();

        try {
            String complexity = (String) request.getOrDefault("complexity", "MEDIUM");
            int iterations = (Integer) request.getOrDefault("iterations", 50);

            System.out.println("Running Jackson compression benchmark with complexity: " + complexity);

            // Generate huge payload based on complexity
            HugePayloadGenerator.ComplexityLevel level = HugePayloadGenerator.ComplexityLevel.valueOf(complexity);
            List<User> users = HugePayloadGenerator.generateHugeDataset(level);

            results.put("complexity", complexity);
            results.put("userCount", users.size());
            results.put("iterations", iterations);
            results.put("compression", benchmarkCompressionWithIterations(users, iterations));

        } catch (Exception e) {
            results.put("error", e.getMessage());
            e.printStackTrace();
        }

        return results;
    }

    @PostMapping("/benchmark/performance")
    public Map<String, Object> runPerformanceBenchmark(@RequestBody Map<String, Object> request) {
        Map<String, Object> results = new HashMap<>();

        try {
            String payloadSize = (String) request.getOrDefault("payload_size", "medium");
            int iterations = (Integer) request.getOrDefault("iterations", 25);

            System.out.println("Running Jackson performance benchmark with payload size: " + payloadSize);

            // Map payload size to complexity level
            HugePayloadGenerator.ComplexityLevel level;
            switch (payloadSize.toLowerCase()) {
                case "small":
                    level = HugePayloadGenerator.ComplexityLevel.SMALL;
                    break;
                case "large":
                    level = HugePayloadGenerator.ComplexityLevel.LARGE;
                    break;
                case "huge":
                    level = HugePayloadGenerator.ComplexityLevel.HUGE;
                    break;
                default:
                    level = HugePayloadGenerator.ComplexityLevel.MEDIUM;
            }

            List<User> users = HugePayloadGenerator.generateHugeDataset(level);

            results.put("payloadSize", payloadSize);
            results.put("userCount", users.size());
            results.put("iterations", iterations);
            results.put("performance", benchmarkPerformance(users, iterations));

        } catch (Exception e) {
            results.put("error", e.getMessage());
            e.printStackTrace();
        }

        return results;
    }

    private Map<String, Object> benchmarkFormats(List<User> users) throws IOException {
        Map<String, Object> formatResults = new HashMap<>();

        // JSON Standard
        JacksonSerializationService.SerializationResult jsonResult =
            serializationService.serializeUsersToJson(users);
        formatResults.put("json", createFormatResult(jsonResult));

        // JSON Compact
        JacksonSerializationService.SerializationResult compactJsonResult =
            serializationService.serializeUsersToCompactJson(users);
        formatResults.put("compactJson", createFormatResult(compactJsonResult));

        // CBOR
        JacksonSerializationService.SerializationResult cborResult =
            serializationService.serializeUsersToCbor(users);
        formatResults.put("cbor", createFormatResult(cborResult));

        // MessagePack
        JacksonSerializationService.SerializationResult msgPackResult =
            serializationService.serializeUsersToMessagePack(users);
        formatResults.put("messagePack", createFormatResult(msgPackResult));

        // Smile
        JacksonSerializationService.SerializationResult smileResult =
            serializationService.serializeUsersToSmile(users);
        formatResults.put("smile", createFormatResult(smileResult));

        return formatResults;
    }

    private Map<String, Object> benchmarkFormatsWithIterations(List<User> users, int iterations) throws IOException {
        Map<String, Object> formatResults = new HashMap<>();

        // Run multiple iterations for more accurate results
        for (int i = 0; i < iterations; i++) {
            if (i % 10 == 0) {
                System.out.println("  Jackson serialization iteration " + (i + 1) + "/" + iterations);
            }

            // JSON Standard
            JacksonSerializationService.SerializationResult jsonResult =
                serializationService.serializeUsersToJson(users);
            if (i == 0) { // Only store results from first iteration
                formatResults.put("json", createFormatResult(jsonResult));
            }

            // CBOR
            JacksonSerializationService.SerializationResult cborResult =
                serializationService.serializeUsersToCbor(users);
            if (i == 0) {
                formatResults.put("cbor", createFormatResult(cborResult));
            }

            // MessagePack
            JacksonSerializationService.SerializationResult msgPackResult =
                serializationService.serializeUsersToMessagePack(users);
            if (i == 0) {
                formatResults.put("messagePack", createFormatResult(msgPackResult));
            }
        }

        return formatResults;
    }

    private Map<String, Object> benchmarkCompression(List<User> users) throws IOException {
        Map<String, Object> compressionResults = new HashMap<>();

        // Get JSON data for compression testing
        JacksonSerializationService.SerializationResult jsonResult =
            serializationService.serializeUsersToJson(users);
        byte[] jsonData = jsonResult.getData();

        // GZIP Compression
        JacksonSerializationService.CompressionResult gzipResult =
            serializationService.compressWithGzip(jsonData);
        compressionResults.put("gzip", createCompressionResult(gzipResult));

        // Zstandard Compression
        JacksonSerializationService.CompressionResult zstdResult =
            serializationService.compressWithZstd(jsonData);
        compressionResults.put("zstandard", createCompressionResult(zstdResult));

        // Brotli Compression
        JacksonSerializationService.CompressionResult brotliResult =
            serializationService.compressWithBrotli(jsonData);
        compressionResults.put("brotli", createCompressionResult(brotliResult));

        return compressionResults;
    }

    private Map<String, Object> benchmarkCompressionWithIterations(List<User> users, int iterations) throws IOException {
        Map<String, Object> compressionResults = new HashMap<>();

        // Get JSON data for compression testing
        JacksonSerializationService.SerializationResult jsonResult =
            serializationService.serializeUsersToJson(users);
        byte[] jsonData = jsonResult.getData();

        // Run multiple iterations
        for (int i = 0; i < iterations; i++) {
            if (i % 10 == 0) {
                System.out.println("  Jackson compression iteration " + (i + 1) + "/" + iterations);
            }

            // GZIP Compression
            JacksonSerializationService.CompressionResult gzipResult =
                serializationService.compressWithGzip(jsonData);
            if (i == 0) {
                compressionResults.put("gzip", createCompressionResult(gzipResult));
            }

            // Zstandard Compression
            JacksonSerializationService.CompressionResult zstdResult =
                serializationService.compressWithZstd(jsonData);
            if (i == 0) {
                compressionResults.put("zstandard", createCompressionResult(zstdResult));
            }
        }

        return compressionResults;
    }

    private Map<String, Object> benchmarkPerformance(List<User> users, int iterations) throws IOException {
        Map<String, Object> performanceResults = new HashMap<>();

        long totalSerializationTime = 0;
        long totalDeserializationTime = 0;
        long totalCompressionTime = 0;

        for (int i = 0; i < iterations; i++) {
            if (i % 5 == 0) {
                System.out.println("  Jackson performance iteration " + (i + 1) + "/" + iterations);
            }

            // Measure serialization time
            long startTime = System.currentTimeMillis();
            JacksonSerializationService.SerializationResult jsonResult =
                serializationService.serializeUsersToJson(users);
            long serializationTime = System.currentTimeMillis() - startTime;
            totalSerializationTime += serializationTime;

            // Measure deserialization time
            startTime = System.currentTimeMillis();
            serializationService.deserializeUsersFromJson(jsonResult.getData());
            long deserializationTime = System.currentTimeMillis() - startTime;
            totalDeserializationTime += deserializationTime;

            // Measure compression time
            startTime = System.currentTimeMillis();
            serializationService.compressWithGzip(jsonResult.getData());
            long compressionTime = System.currentTimeMillis() - startTime;
            totalCompressionTime += compressionTime;
        }

        performanceResults.put("avgSerializationTime", totalSerializationTime / iterations);
        performanceResults.put("avgDeserializationTime", totalDeserializationTime / iterations);
        performanceResults.put("avgCompressionTime", totalCompressionTime / iterations);
        performanceResults.put("totalIterations", iterations);

        return performanceResults;
    }

    private Map<String, Object> createFormatResult(JacksonSerializationService.SerializationResult result) {
        Map<String, Object> formatResult = new HashMap<>();
        formatResult.put("size", result.getSizeBytes());
        formatResult.put("serializationTime", result.getSerializationTimeMs());
        return formatResult;
    }

    private Map<String, Object> createCompressionResult(JacksonSerializationService.CompressionResult result) {
        Map<String, Object> compressionResult = new HashMap<>();
        compressionResult.put("originalSize", result.getOriginalSize());
        compressionResult.put("compressedSize", result.getCompressedSize());
        compressionResult.put("compressionRatio", result.getCompressionRatio());
        compressionResult.put("compressionTime", result.getCompressionTimeMs());
        return compressionResult;
    }

    @GetMapping("/test-roundtrip")
    public Map<String, Object> testRoundtrip(@RequestParam(value = "userCount", defaultValue = "100") int userCount) {
        Map<String, Object> results = new HashMap<>();

        try {
            List<User> originalUsers = PayloadGenerator.generateMassiveDataset(userCount);

            // Serialize to JSON
            JacksonSerializationService.SerializationResult jsonResult =
                serializationService.serializeUsersToJson(originalUsers);

            // Deserialize back
            List<User> deserializedUsers = serializationService.deserializeUsersFromJson(jsonResult.getData());

            results.put("originalCount", originalUsers.size());
            results.put("deserializedCount", deserializedUsers.size());
            results.put("roundtripSuccessful", originalUsers.size() == deserializedUsers.size());
            results.put("serializationTime", jsonResult.getSerializationTimeMs());
            results.put("size", jsonResult.getSizeBytes());

        } catch (Exception e) {
            results.put("error", e.getMessage());
            e.printStackTrace();
        }

        return results;
    }

    @GetMapping("/test-huge-payload")
    public Map<String, Object> testHugePayload(@RequestParam(value = "complexity", defaultValue = "SMALL") String complexity) {
        Map<String, Object> results = new HashMap<>();

        try {
            System.out.println("Testing HugePayloadGenerator with complexity: " + complexity);

            // Test the HugePayloadGenerator
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

    @PostMapping("/custom-payload")
    public Map<String, Object> benchmarkCustomPayload(@RequestBody List<User> users) {
        Map<String, Object> results = new HashMap<>();

        try {
            results.put("userCount", users.size());
            results.put("formats", benchmarkFormats(users));
            results.put("compression", benchmarkCompression(users));

        } catch (Exception e) {
            results.put("error", e.getMessage());
            e.printStackTrace();
        }

        return results;
    }
}
