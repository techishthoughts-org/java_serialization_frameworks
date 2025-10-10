package org.techishthoughts.jackson.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.techishthoughts.jackson.JacksonPocApplication;
import org.techishthoughts.payload.config.BenchmarkProperties;
import org.techishthoughts.payload.generator.HugePayloadGenerator.ComplexityLevel;
import org.techishthoughts.payload.generator.UnifiedPayloadGenerator;
import org.techishthoughts.payload.model.User;
import org.techishthoughts.payload.service.BenchmarkConfig;
import org.techishthoughts.payload.service.SerializationException;
import org.techishthoughts.payload.service.result.BenchmarkResult;
import org.techishthoughts.payload.service.result.CompressionResult;
import org.techishthoughts.payload.service.result.SerializationResult;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Unit tests for JacksonSerializationServiceImpl.
 * Tests serialization, compression, and benchmarking functionality.
 */
@SpringBootTest(classes = JacksonPocApplication.class)
class JacksonSerializationServiceImplTest {

    private JacksonSerializationServiceImpl service;
    private UnifiedPayloadGenerator payloadGenerator;

    @BeforeEach
    void setUp() {
        BenchmarkProperties properties = new BenchmarkProperties();
        payloadGenerator = new UnifiedPayloadGenerator(properties);

        ObjectMapper standardMapper = new ObjectMapper();
        ObjectMapper compactMapper = new ObjectMapper();

        service = new JacksonSerializationServiceImpl(standardMapper, compactMapper, payloadGenerator);
    }

    @Test
    void testGetFrameworkName() {
        // When
        String frameworkName = service.getFrameworkName();

        // Then
        assertEquals("Jackson JSON", frameworkName);
    }

    @Test
    void testSerializeUsers() throws SerializationException {
        // Given
        List<User> users = payloadGenerator.generateDataset(ComplexityLevel.SMALL);

        // When
        SerializationResult result = service.serialize(users);

        // Then
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals("Jackson JSON", result.getFramework());
        assertEquals("JSON", result.getFormat());
        assertNotNull(result.getData());
        assertTrue(result.getData().length > 0);
        assertEquals(users.size(), result.getInputObjectCount());
        assertTrue(result.getSerializationTimeMs() >= 0);
    }

    @Test
    void testDeserializeUsers() throws SerializationException {
        // Given
        List<User> originalUsers = payloadGenerator.generateDataset(ComplexityLevel.SMALL);
        SerializationResult serializationResult = service.serialize(originalUsers);

        // When
        List<User> deserializedUsers = service.deserialize(serializationResult.getData());

        // Then
        assertNotNull(deserializedUsers);
        assertEquals(originalUsers.size(), deserializedUsers.size());

        // Verify basic properties of first user
        if (!deserializedUsers.isEmpty() && !originalUsers.isEmpty()) {
            User original = originalUsers.get(0);
            User deserialized = deserializedUsers.get(0);

            assertEquals(original.getId(), deserialized.getId());
            assertEquals(original.getUsername(), deserialized.getUsername());
            assertEquals(original.getEmail(), deserialized.getEmail());
        }
    }

    @Test
    void testRoundtripSerialization() throws SerializationException {
        // Given
        List<User> originalUsers = payloadGenerator.generateDataset(ComplexityLevel.SMALL);

        // When
        boolean roundtripSuccess = service.testRoundtrip(originalUsers);

        // Then
        assertTrue(roundtripSuccess);
    }

    @Test
    void testCompressWithGzip() throws SerializationException {
        // Given
        List<User> users = payloadGenerator.generateDataset(ComplexityLevel.SMALL);
        SerializationResult serializationResult = service.serialize(users);
        byte[] originalData = serializationResult.getData();

        // When
        CompressionResult compressionResult = service.compressWithGzip(originalData);

        // Then
        assertNotNull(compressionResult);
        assertTrue(compressionResult.isSuccess());
        assertEquals("GZIP", compressionResult.getAlgorithm());
        assertNotNull(compressionResult.getCompressedData());
        assertEquals(originalData.length, compressionResult.getOriginalSize());
        assertTrue(compressionResult.getCompressedSize() > 0);
        assertTrue(compressionResult.getCompressionRatio() > 0);
        assertTrue(compressionResult.getCompressionRatio() < 1.0); // Should compress
        assertTrue(compressionResult.getCompressionTimeMs() >= 0);
    }

    @Test
    void testCompressWithZstd() throws SerializationException {
        // Given
        List<User> users = payloadGenerator.generateDataset(ComplexityLevel.SMALL);
        SerializationResult serializationResult = service.serialize(users);
        byte[] originalData = serializationResult.getData();

        // When
        CompressionResult compressionResult = service.compressWithZstd(originalData);

        // Then
        assertNotNull(compressionResult);
        assertTrue(compressionResult.isSuccess());
        assertEquals("Zstandard", compressionResult.getAlgorithm());
        assertTrue(compressionResult.getCompressedSize() > 0);
        assertTrue(compressionResult.getCompressionRatio() < 1.0); // Should compress
    }

    @Test
    void testCompressWithBrotli() throws SerializationException {
        // Given
        List<User> users = payloadGenerator.generateDataset(ComplexityLevel.SMALL);
        SerializationResult serializationResult = service.serialize(users);
        byte[] originalData = serializationResult.getData();

        // When
        CompressionResult compressionResult = service.compressWithBrotli(originalData);

        // Then
        assertNotNull(compressionResult);
        assertTrue(compressionResult.isSuccess());
        assertEquals("Brotli", compressionResult.getAlgorithm());
        assertTrue(compressionResult.getCompressedSize() > 0);
        assertTrue(compressionResult.getCompressionRatio() < 1.0); // Should compress
    }

    @Test
    void testRunBenchmarkSmall() throws SerializationException {
        // Given
        BenchmarkConfig config = BenchmarkConfig.builder()
                .withComplexity(ComplexityLevel.SMALL)
                .withIterations(5)
                .withWarmup(false, 0)
                .withCompression(true)
                .withRoundtripTest(true)
                .withMemoryMonitoring(true);

        // When
        BenchmarkResult result = service.runBenchmark(config);

        // Then
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals("Jackson JSON", result.getFramework());
        assertEquals(config, result.getConfig());
        assertTrue(result.isRoundtripSuccess());

        // Verify we have serialization results
        assertFalse(result.getSerializationResults().isEmpty());
        assertEquals(5, result.getSerializationResults().size());

        // Verify we have compression results
        assertFalse(result.getCompressionResults().isEmpty());
        assertEquals(5, result.getCompressionResults().size());

        // Verify aggregate metrics
        assertTrue(result.getAverageSerializationTimeMs().isPresent());
        assertTrue(result.getAverageCompressionRatio().isPresent());
        assertTrue(result.getAverageSerializedSizeBytes().isPresent());

        assertEquals(5, result.getSuccessfulSerializations());
        assertEquals(5, result.getSuccessfulCompressions());
        assertEquals(100.0, result.getSuccessRatePercent());

        // Verify memory metrics are present
        assertNotNull(result.getMemoryMetrics());
    }

    @Test
    void testRunBenchmarkWithoutCompression() throws SerializationException {
        // Given
        BenchmarkConfig config = BenchmarkConfig.builder()
                .withComplexity(ComplexityLevel.SMALL)
                .withIterations(3)
                .withCompression(false)
                .withRoundtripTest(false);

        // When
        BenchmarkResult result = service.runBenchmark(config);

        // Then
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals(3, result.getSerializationResults().size());
        assertTrue(result.getCompressionResults().isEmpty()); // No compression
    }

    @Test
    void testSerializeCompact() throws SerializationException {
        // Given
        List<User> users = payloadGenerator.generateDataset(ComplexityLevel.SMALL);

        // When
        SerializationResult result = service.serializeCompact(users);

        // Then
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals("Jackson JSON", result.getFramework());
        assertEquals("Compact JSON", result.getFormat());
        assertNotNull(result.getData());
        assertTrue(result.getData().length > 0);
    }

    @Test
    void testGetSupportedCompressionAlgorithms() {
        // When
        List<String> algorithms = service.getSupportedCompressionAlgorithms();

        // Then
        assertNotNull(algorithms);
        assertTrue(algorithms.contains("GZIP"));
        assertTrue(algorithms.contains("Zstandard"));
        assertTrue(algorithms.contains("Brotli"));
    }

    @Test
    void testSupportsSchemaEvolution() {
        // When
        boolean supportsSchemaEvolution = service.supportsSchemaEvolution();

        // Then
        assertTrue(supportsSchemaEvolution); // JSON is schema-flexible
    }

    @Test
    void testGetTypicalUseCase() {
        // When
        String useCase = service.getTypicalUseCase();

        // Then
        assertNotNull(useCase);
        assertFalse(useCase.isEmpty());
        assertTrue(useCase.contains("Web APIs") || useCase.contains("REST"));
    }

    @Test
    void testPerformanceCharacteristics() throws SerializationException {
        // Given
        List<User> users = payloadGenerator.generateDataset(ComplexityLevel.MEDIUM);

        // When
        long startTime = System.nanoTime();
        SerializationResult result = service.serialize(users);
        long endTime = System.nanoTime();

        // Then
        double actualTimeMs = (endTime - startTime) / 1_000_000.0;
        double reportedTimeMs = result.getSerializationTimeMs();

        // The reported time should be close to the actual time (within 10ms tolerance)
        assertTrue(Math.abs(actualTimeMs - reportedTimeMs) < 10.0,
                String.format("Time mismatch: actual=%.2f ms, reported=%.2f ms", actualTimeMs, reportedTimeMs));
    }
}