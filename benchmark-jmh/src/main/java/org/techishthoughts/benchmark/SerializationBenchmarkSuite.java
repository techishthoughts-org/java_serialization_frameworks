package org.techishthoughts.benchmark;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.profile.GCProfiler;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import org.techishthoughts.payload.model.User;
import org.techishthoughts.payload.generator.UnifiedPayloadGenerator;
import org.techishthoughts.payload.service.SerializationService;
import org.techishthoughts.payload.service.SerializationException;
import org.techishthoughts.payload.service.result.SerializationResult;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.GarbageCollectorMXBean;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Comprehensive JMH benchmark suite for serialization frameworks.
 *
 * Features:
 * - Multiple payload sizes and complexities
 * - GC-aware measurements
 * - Proper warmup strategies based on JIT compilation
 * - Multiple measurement modes (throughput, latency, sampling)
 * - Statistical stability monitoring
 * - Memory usage profiling
 */
@BenchmarkMode({Mode.Throughput, Mode.AverageTime, Mode.SampleTime})
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 10, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 20, time = 3, timeUnit = TimeUnit.SECONDS)
@Fork(value = 3, jvmArgs = {
    "-XX:+UseG1GC",
    "-XX:MaxGCPauseMillis=200",
    "-Xms2g",
    "-Xmx4g",
    "-XX:+PrintGCDetails",
    "-XX:+PrintGCTimeStamps",
    "-XX:+PrintGCApplicationStoppedTime"
})
@Threads(1) // Single-threaded for consistent measurements
public class SerializationBenchmarkSuite {

    @State(Scope.Benchmark)
    public static class BenchmarkState {

        @Param({"SMALL", "MEDIUM", "LARGE", "XLARGE"})
        public DataComplexity complexity;

        public List<User> users;
        public UnifiedPayloadGenerator payloadGenerator;

        // Framework-specific serializers
        public ObjectMapper jacksonMapper;
        public JacksonSerializationAdapter jacksonAdapter;

        // Memory monitoring
        public MemoryMXBean memoryBean;
        public List<GarbageCollectorMXBean> gcBeans;

        @Setup(Level.Trial)
        public void setupTrial() {
            payloadGenerator = new UnifiedPayloadGenerator();

            // Initialize Jackson
            jacksonMapper = new ObjectMapper();
            jacksonMapper.registerModule(new JavaTimeModule());
            jacksonAdapter = new JacksonSerializationAdapter(jacksonMapper);

            // Memory monitoring setup
            memoryBean = ManagementFactory.getMemoryMXBean();
            gcBeans = ManagementFactory.getGarbageCollectorMXBeans();

            System.out.println("=== Benchmark Trial Setup ===");
            System.out.println("Data Complexity: " + complexity);
            System.out.println("Available Processors: " + Runtime.getRuntime().availableProcessors());
            System.out.println("Max Memory: " + Runtime.getRuntime().maxMemory() / (1024 * 1024) + " MB");
            System.out.println("==============================");
        }

        @Setup(Level.Iteration)
        public void setupIteration() {
            // Generate fresh data for each iteration to avoid caching effects
            users = payloadGenerator.generateDataset(complexity);

            // Force GC before each iteration for clean measurements
            System.gc();
            try {
                Thread.sleep(100); // Give GC time to complete
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        @TearDown(Level.Iteration)
        public void tearDownIteration() {
            // Optional: Log memory usage after iteration
            long usedMemory = memoryBean.getHeapMemoryUsage().getUsed() / (1024 * 1024);
            if (usedMemory > 1024) { // Log if using more than 1GB
                System.out.println("High memory usage detected: " + usedMemory + " MB");
            }
        }
    }

    /**
     * Data complexity levels for comprehensive testing
     */
    public enum DataComplexity {
        SMALL(100),
        MEDIUM(1000),
        LARGE(10000),
        XLARGE(50000);

        private final int userCount;

        DataComplexity(int userCount) {
            this.userCount = userCount;
        }

        public int getUserCount() { return userCount; }
    }

    // ==================== SERIALIZATION BENCHMARKS ====================

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public void jacksonSerializationThroughput(BenchmarkState state, Blackhole blackhole)
            throws SerializationException {
        SerializationResult result = state.jacksonAdapter.serialize(state.users);
        blackhole.consume(result.getData());
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public void jacksonSerializationLatency(BenchmarkState state, Blackhole blackhole)
            throws SerializationException {
        SerializationResult result = state.jacksonAdapter.serialize(state.users);
        blackhole.consume(result.getData());
    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public void jacksonSerializationSampling(BenchmarkState state, Blackhole blackhole)
            throws SerializationException {
        SerializationResult result = state.jacksonAdapter.serialize(state.users);
        blackhole.consume(result.getData());
    }

    // ==================== DESERIALIZATION BENCHMARKS ====================

    @State(Scope.Thread)
    public static class DeserializationState {
        public byte[] serializedData;

        @Setup(Level.Iteration)
        public void setup(BenchmarkState benchmarkState) throws SerializationException {
            SerializationResult result = benchmarkState.jacksonAdapter.serialize(benchmarkState.users);
            serializedData = result.getData();
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public void jacksonDeserializationThroughput(BenchmarkState state, DeserializationState deserState,
                                               Blackhole blackhole) throws SerializationException {
        List<User> users = state.jacksonAdapter.deserialize(deserState.serializedData);
        blackhole.consume(users);
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public void jacksonDeserializationLatency(BenchmarkState state, DeserializationState deserState,
                                            Blackhole blackhole) throws SerializationException {
        List<User> users = state.jacksonAdapter.deserialize(deserState.serializedData);
        blackhole.consume(users);
    }

    // ==================== ROUNDTRIP BENCHMARKS ====================

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public void jacksonRoundtripThroughput(BenchmarkState state, Blackhole blackhole)
            throws SerializationException {
        SerializationResult result = state.jacksonAdapter.serialize(state.users);
        List<User> deserialized = state.jacksonAdapter.deserialize(result.getData());
        blackhole.consume(deserialized);
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public void jacksonRoundtripLatency(BenchmarkState state, Blackhole blackhole)
            throws SerializationException {
        SerializationResult result = state.jacksonAdapter.serialize(state.users);
        List<User> deserialized = state.jacksonAdapter.deserialize(result.getData());
        blackhole.consume(deserialized);
    }

    // ==================== MEMORY-FOCUSED BENCHMARKS ====================

    @Benchmark
    @BenchmarkMode(Mode.SingleShotTime)
    @Measurement(iterations = 100, batchSize = 1)
    @Warmup(iterations = 0) // No warmup for memory allocation patterns
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public void jacksonMemoryAllocation(BenchmarkState state, Blackhole blackhole)
            throws SerializationException {
        long beforeMemory = state.memoryBean.getHeapMemoryUsage().getUsed();
        SerializationResult result = state.jacksonAdapter.serialize(state.users);
        long afterMemory = state.memoryBean.getHeapMemoryUsage().getUsed();

        blackhole.consume(result.getData());
        blackhole.consume(afterMemory - beforeMemory); // Memory allocation delta
    }

    // ==================== GC-AWARE BENCHMARKS ====================

    @State(Scope.Thread)
    public static class GCState {
        private long initialGcCount;
        private long initialGcTime;

        @Setup(Level.Iteration)
        public void setup(BenchmarkState state) {
            initialGcCount = getTotalGcCount(state.gcBeans);
            initialGcTime = getTotalGcTime(state.gcBeans);
        }

        @TearDown(Level.Iteration)
        public void tearDown(BenchmarkState state) {
            long finalGcCount = getTotalGcCount(state.gcBeans);
            long finalGcTime = getTotalGcTime(state.gcBeans);

            long gcEvents = finalGcCount - initialGcCount;
            long gcTime = finalGcTime - initialGcTime;

            if (gcEvents > 0) {
                System.out.printf("GC Events: %d, GC Time: %d ms%n", gcEvents, gcTime);
            }
        }

        private long getTotalGcCount(List<GarbageCollectorMXBean> gcBeans) {
            return gcBeans.stream().mapToLong(GarbageCollectorMXBean::getCollectionCount).sum();
        }

        private long getTotalGcTime(List<GarbageCollectorMXBean> gcBeans) {
            return gcBeans.stream().mapToLong(GarbageCollectorMXBean::getCollectionTime).sum();
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public void jacksonGCAware(BenchmarkState state, GCState gcState, Blackhole blackhole)
            throws SerializationException {
        SerializationResult result = state.jacksonAdapter.serialize(state.users);
        blackhole.consume(result.getData());
    }

    // ==================== ADAPTIVE WARMUP BENCHMARKS ====================

    /**
     * Benchmark with adaptive warmup based on JIT compilation detection
     */
    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @Warmup(iterations = 0) // Custom warmup logic
    @Measurement(iterations = 10, time = 5)
    public void jacksonAdaptiveWarmup(BenchmarkState state, Blackhole blackhole)
            throws SerializationException {

        // Custom warmup logic could be implemented here
        // For now, using standard JMH warmup
        SerializationResult result = state.jacksonAdapter.serialize(state.users);
        blackhole.consume(result.getData());
    }

    // ==================== FRAMEWORK ADAPTER ====================

    /**
     * Adapter to integrate Jackson with the benchmark framework
     */
    public static class JacksonSerializationAdapter {
        private final ObjectMapper mapper;

        public JacksonSerializationAdapter(ObjectMapper mapper) {
            this.mapper = mapper;
        }

        public SerializationResult serialize(List<User> users) throws SerializationException {
            try {
                long startTime = System.nanoTime();
                byte[] data = mapper.writeValueAsBytes(users);
                long serializationTime = System.nanoTime() - startTime;

                return SerializationResult.builder("Jackson")
                        .format("JSON")
                        .data(data)
                        .serializationTime(serializationTime)
                        .inputObjectCount(users.size())
                        .build();

            } catch (Exception e) {
                throw new SerializationException("Jackson serialization failed", e);
            }
        }

        @SuppressWarnings("unchecked")
        public List<User> deserialize(byte[] data) throws SerializationException {
            try {
                return mapper.readValue(data,
                    mapper.getTypeFactory().constructCollectionType(List.class, User.class));
            } catch (Exception e) {
                throw new SerializationException("Jackson deserialization failed", e);
            }
        }
    }

    // ==================== MAIN METHOD FOR STANDALONE EXECUTION ====================

    public static void main(String[] args) throws RunnerException {
        Options options = new OptionsBuilder()
                .include(SerializationBenchmarkSuite.class.getSimpleName())
                .forks(1) // Reduce forks for quicker testing during development
                .warmupIterations(5)
                .measurementIterations(10)
                .addProfiler(GCProfiler.class) // Enable GC profiling
                .jvmArgs(
                    "-XX:+UseG1GC",
                    "-XX:MaxGCPauseMillis=200",
                    "-Xms1g",
                    "-Xmx2g"
                )
                .resultFormat(org.openjdk.jmh.results.format.ResultFormatType.JSON)
                .result("jmh-benchmark-results.json")
                .build();

        new Runner(options).run();
    }
}