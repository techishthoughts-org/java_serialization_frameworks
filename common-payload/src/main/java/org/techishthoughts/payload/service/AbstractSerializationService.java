package org.techishthoughts.payload.service;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.techishthoughts.payload.generator.UnifiedPayloadGenerator;
import org.techishthoughts.payload.model.User;
import org.techishthoughts.payload.service.result.BenchmarkResult;
import org.techishthoughts.payload.service.result.CompressionResult;
import org.techishthoughts.payload.service.result.SerializationResult;

/**
 * Abstract base class for serialization services.
 * Provides common functionality and benchmark orchestration.
 * Framework-specific implementations should extend this class.
 */
public abstract class AbstractSerializationService implements SerializationService {

    protected final Logger logger = LoggerFactory.getLogger(getClass());
    protected final UnifiedPayloadGenerator payloadGenerator;

    protected AbstractSerializationService(UnifiedPayloadGenerator payloadGenerator) {
        this.payloadGenerator = payloadGenerator;
    }

    @Override
    public BenchmarkResult runBenchmark(BenchmarkConfig config) throws SerializationException {
        logger.info("Starting {} benchmark with config: {}", getFrameworkName(), config);

        BenchmarkResult.Builder resultBuilder = BenchmarkResult.builder(getFrameworkName(), config)
                .startTime(LocalDateTime.now());

        try {
            // Generate test data
            List<User> users = payloadGenerator.generateDataset(config.getComplexity());
            logger.info("Generated {} users for benchmarking", users.size());

            // Memory monitoring setup
            Runtime runtime = Runtime.getRuntime();
            long initialMemory = 0;
            long peakMemory = 0;

            if (config.isEnableMemoryMonitoring()) {
                runtime.gc(); // Clean slate
                Thread.sleep(100);
                initialMemory = runtime.totalMemory() - runtime.freeMemory();
                peakMemory = initialMemory;
            }

            // Warmup if enabled
            if (config.isEnableWarmup()) {
                logger.info("Running {} warmup iterations", config.getWarmupIterations());
                runWarmup(users, config.getWarmupIterations());

                // Clean up after warmup
                runtime.gc();
                Thread.sleep(100);
            }

            // Run main benchmark iterations
            for (int i = 0; i < config.getIterations(); i++) {
                if (i % 10 == 0) {
                    logger.debug("Benchmark iteration {}/{}", i + 1, config.getIterations());
                }

                // Check timeout
                if (System.currentTimeMillis() - resultBuilder.build().getStartTime().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli() > config.getTimeoutMs()) {
                    logger.warn("Benchmark timeout reached, stopping at iteration {}", i);
                    break;
                }

                // Serialize
                SerializationResult serializationResult = serialize(users);
                resultBuilder.addSerializationResult(serializationResult);

                // Compression if enabled
                if (config.isEnableCompression()) {
                    CompressionResult compressionResult = compress(serializationResult.getData());
                    resultBuilder.addCompressionResult(compressionResult);
                }

                // Memory monitoring
                if (config.isEnableMemoryMonitoring()) {
                    long currentMemory = runtime.totalMemory() - runtime.freeMemory();
                    if (currentMemory > peakMemory) {
                        peakMemory = currentMemory;
                    }

                    // Check memory pressure
                    double memoryUsagePercent = ((double) currentMemory / runtime.maxMemory()) * 100;
                    if (memoryUsagePercent > 85.0) {
                        logger.warn("High memory usage detected: {:.1f}%, running GC", memoryUsagePercent);
                        runtime.gc();
                    }
                }
            }

            // Roundtrip test if enabled
            if (config.isEnableRoundtripTest()) {
                boolean roundtripSuccess = testRoundtrip(users);
                resultBuilder.roundtripSuccess(roundtripSuccess);
                logger.info("Roundtrip test: {}", roundtripSuccess ? "PASSED" : "FAILED");
            }

            // Collect final memory metrics
            if (config.isEnableMemoryMonitoring()) {
                runtime.gc();
                Thread.sleep(100);
                long finalMemory = runtime.totalMemory() - runtime.freeMemory();

                BenchmarkResult.MemoryMetrics memoryMetrics = new BenchmarkResult.MemoryMetrics(
                        initialMemory / (1024 * 1024), // MB
                        peakMemory / (1024 * 1024),    // MB
                        finalMemory / (1024 * 1024),   // MB
                        0, // GC count (would need JMX monitoring)
                        0  // GC time (would need JMX monitoring)
                );
                resultBuilder.memoryMetrics(memoryMetrics);
            }

            BenchmarkResult result = resultBuilder.endTime(LocalDateTime.now()).build();
            logger.info("{} benchmark completed successfully: {}", getFrameworkName(), result);
            return result;

        } catch (Exception e) {
            logger.error("{} benchmark failed", getFrameworkName(), e);
            return resultBuilder
                    .endTime(LocalDateTime.now())
                    .error("Benchmark execution failed: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Run warmup iterations to prepare JVM for benchmarking
     */
    protected void runWarmup(List<User> users, int warmupIterations) throws SerializationException {
        for (int i = 0; i < warmupIterations; i++) {
            try {
                SerializationResult result = serialize(users);
                if (supportsDeserialization()) {
                    deserialize(result.getData());
                }
            } catch (Exception e) {
                logger.warn("Warmup iteration {} failed: {}", i + 1, e.getMessage());
            }
        }
    }

    @Override
    public boolean testRoundtrip(List<User> users) {
        try {
            SerializationResult serialized = serialize(users);
            if (!supportsDeserialization()) {
                // If deserialization is not supported, just check if serialization worked
                return serialized.isSuccess() && serialized.getData().length > 0;
            }

            List<User> deserialized = deserialize(serialized.getData());
            return users.size() == deserialized.size();
        } catch (Exception e) {
            logger.warn("Roundtrip test failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Indicates whether this framework supports deserialization.
     * Some frameworks might be write-only in certain configurations.
     */
    protected boolean supportsDeserialization() {
        return true; // Most frameworks support both serialization and deserialization
    }

    /**
     * Get performance characteristics of this framework
     */
    public PerformanceCharacteristics getPerformanceCharacteristics() {
        return new PerformanceCharacteristics(
                getTypicalUseCase(),
                supportsSchemaEvolution(),
                getSupportedCompressionAlgorithms(),
                getExpectedPerformanceTier(),
                getMemoryFootprint()
        );
    }

    /**
     * Get expected performance tier for this framework
     */
    protected abstract PerformanceTier getExpectedPerformanceTier();

    /**
     * Get expected memory footprint category
     */
    protected abstract MemoryFootprint getMemoryFootprint();

    /**
     * Performance tier classification
     */
    public enum PerformanceTier {
        ULTRA_HIGH("Ultra High - Sub-millisecond serialization"),
        VERY_HIGH("Very High - 1-5ms typical serialization"),
        HIGH("High - 5-20ms typical serialization"),
        MEDIUM("Medium - 20-100ms typical serialization"),
        LOW("Low - 100ms+ typical serialization");

        private final String description;

        PerformanceTier(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * Memory footprint classification
     */
    public enum MemoryFootprint {
        VERY_LOW("Very Low - <10MB typical usage"),
        LOW("Low - 10-50MB typical usage"),
        MEDIUM("Medium - 50-200MB typical usage"),
        HIGH("High - 200-500MB typical usage"),
        VERY_HIGH("Very High - 500MB+ typical usage");

        private final String description;

        MemoryFootprint(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * Comprehensive performance characteristics
     */
    public static class PerformanceCharacteristics {
        private final String useCase;
        private final boolean schemaEvolution;
        private final List<String> compressionAlgorithms;
        private final PerformanceTier performanceTier;
        private final MemoryFootprint memoryFootprint;

        public PerformanceCharacteristics(String useCase, boolean schemaEvolution,
                                        List<String> compressionAlgorithms,
                                        PerformanceTier performanceTier,
                                        MemoryFootprint memoryFootprint) {
            this.useCase = useCase;
            this.schemaEvolution = schemaEvolution;
            this.compressionAlgorithms = compressionAlgorithms;
            this.performanceTier = performanceTier;
            this.memoryFootprint = memoryFootprint;
        }

        // Getters
        public String getUseCase() { return useCase; }
        public boolean isSchemaEvolution() { return schemaEvolution; }
        public List<String> getCompressionAlgorithms() { return compressionAlgorithms; }
        public PerformanceTier getPerformanceTier() { return performanceTier; }
        public MemoryFootprint getMemoryFootprint() { return memoryFootprint; }
    }
}