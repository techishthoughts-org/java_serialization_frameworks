package org.techishthoughts.jackson.isolation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Main class for running benchmarks in isolated JVM processes.
 * This class is executed as a separate process and performs the actual benchmarking work.
 */
public class ProcessIsolatedRunner {

    private static final Logger logger = LoggerFactory.getLogger(ProcessIsolatedRunner.class);
    
    private final String processId;
    private final String frameworkName;
    private final Path workspace;
    private final Map<String, Object> benchmarkParams;
    private final ObjectMapper objectMapper;
    private final AtomicBoolean shutdownRequested;
    private final ProcessMetricsCollector metricsCollector;

    public ProcessIsolatedRunner(String processId, String frameworkName, Path workspace, 
                               Map<String, Object> benchmarkParams) {
        this.processId = processId;
        this.frameworkName = frameworkName;
        this.workspace = workspace;
        this.benchmarkParams = benchmarkParams;
        this.objectMapper = new ObjectMapper();
        this.shutdownRequested = new AtomicBoolean(false);
        this.metricsCollector = new ProcessMetricsCollector();
        
        // Setup shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutdown hook triggered for process: {}", processId);
            shutdownRequested.set(true);
            cleanup();
        }));
    }

    /**
     * Main entry point for isolated process execution
     */
    public static void main(String[] args) {
        try {
            // Parse command line arguments
            ProcessArgs processArgs = parseArguments(args);
            
            // Create and run the isolated runner
            ProcessIsolatedRunner runner = new ProcessIsolatedRunner(
                processArgs.processId,
                processArgs.framework,
                processArgs.workspace,
                processArgs.params
            );
            
            int exitCode = runner.run();
            System.exit(exitCode);
            
        } catch (Exception e) {
            System.err.println("Fatal error in isolated process: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Main execution method
     */
    public int run() {
        try {
            logger.info("Starting isolated benchmark process: {} for framework: {}", processId, frameworkName);
            
            // Initialize workspace
            initializeWorkspace();
            
            // Setup logging
            setupLogging();
            
            // Start metrics collection
            metricsCollector.start();
            
            // Run benchmark phases
            BenchmarkResults results = runBenchmarkPhases();
            
            // Save results
            saveResults(results);
            
            logger.info("Benchmark process completed successfully: {}", processId);
            return 0;
            
        } catch (Exception e) {
            logger.error("Error in benchmark process: {}", processId, e);
            try {
                saveErrorResults(e);
            } catch (IOException ioException) {
                logger.error("Failed to save error results", ioException);
            }
            return 1;
        } finally {
            metricsCollector.stop();
            cleanup();
        }
    }

    private void initializeWorkspace() throws IOException {
        logger.info("Initializing workspace: {}", workspace);
        
        // Ensure workspace directories exist
        Files.createDirectories(workspace.resolve("logs"));
        Files.createDirectories(workspace.resolve("output"));
        Files.createDirectories(workspace.resolve("tmp"));
        Files.createDirectories(workspace.resolve("metrics"));
        
        // Write process info
        Map<String, Object> processInfo = new HashMap<>();
        processInfo.put("processId", processId);
        processInfo.put("frameworkName", frameworkName);
        processInfo.put("startTime", Instant.now().toString());
        processInfo.put("jvmInfo", getJvmInfo());
        processInfo.put("systemInfo", getSystemInfo());
        processInfo.put("benchmarkParams", benchmarkParams);
        
        Files.write(workspace.resolve("process-info.json"), 
            objectMapper.writeValueAsBytes(processInfo));
    }

    private void setupLogging() {
        // Configure logging to write to workspace
        System.setProperty("LOG_PATH", workspace.resolve("logs").toString());
        System.setProperty("LOG_FILENAME", "benchmark-" + processId);
    }

    private BenchmarkResults runBenchmarkPhases() throws Exception {
        BenchmarkResults results = new BenchmarkResults(processId, frameworkName);
        
        // Phase 1: Warmup
        if (shouldRunPhase("warmup")) {
            logger.info("Starting warmup phase");
            results.setWarmupResults(runWarmupPhase());
        }
        
        // Phase 2: Actual benchmark
        if (shouldRunPhase("benchmark")) {
            logger.info("Starting benchmark phase");
            results.setBenchmarkResults(runBenchmarkPhase());
        }
        
        // Phase 3: Stress test (optional)
        if (shouldRunPhase("stress")) {
            logger.info("Starting stress test phase");
            results.setStressResults(runStressPhase());
        }
        
        // Phase 4: Memory pressure test (optional)
        if (shouldRunPhase("memory")) {
            logger.info("Starting memory pressure test phase");
            results.setMemoryResults(runMemoryPressurePhase());
        }
        
        return results;
    }

    private PhaseResults runWarmupPhase() throws Exception {
        logger.info("Running warmup phase for {}", frameworkName);
        
        PhaseResults results = new PhaseResults("warmup");
        long startTime = System.nanoTime();
        
        try {
            // Get warmup iterations from params or use default
            int iterations = (Integer) benchmarkParams.getOrDefault("warmupIterations", 100);
            int payloadSize = (Integer) benchmarkParams.getOrDefault("payloadSize", 1000);
            
            // Perform warmup operations
            for (int i = 0; i < iterations && !shutdownRequested.get(); i++) {
                runSingleBenchmarkIteration(payloadSize, false);
                
                if (i % 10 == 0) {
                    logger.debug("Warmup progress: {}/{}", i, iterations);
                }
            }
            
            results.setSuccess(true);
            results.setIterations(iterations);
            
        } catch (Exception e) {
            logger.error("Warmup phase failed", e);
            results.setSuccess(false);
            results.setError(e.getMessage());
            throw e;
        } finally {
            results.setDurationNanos(System.nanoTime() - startTime);
        }
        
        return results;
    }

    private PhaseResults runBenchmarkPhase() throws Exception {
        logger.info("Running benchmark phase for {}", frameworkName);
        
        PhaseResults results = new PhaseResults("benchmark");
        long startTime = System.nanoTime();
        
        try {
            int iterations = (Integer) benchmarkParams.getOrDefault("benchmarkIterations", 1000);
            int payloadSize = (Integer) benchmarkParams.getOrDefault("payloadSize", 1000);
            
            List<Long> operationTimes = new ArrayList<>();
            List<Integer> memorySizes = new ArrayList<>();
            
            for (int i = 0; i < iterations && !shutdownRequested.get(); i++) {
                BenchmarkIteration iteration = runSingleBenchmarkIteration(payloadSize, true);
                operationTimes.add(iteration.getDurationNanos());
                memorySizes.add(iteration.getResultSize());
                
                if (i % 100 == 0) {
                    logger.debug("Benchmark progress: {}/{}", i, iterations);
                }
            }
            
            // Calculate statistics
            results.setSuccess(true);
            results.setIterations(iterations);
            results.setOperationTimes(operationTimes);
            results.setMemorySizes(memorySizes);
            results.setStatistics(calculateStatistics(operationTimes));
            
        } catch (Exception e) {
            logger.error("Benchmark phase failed", e);
            results.setSuccess(false);
            results.setError(e.getMessage());
            throw e;
        } finally {
            results.setDurationNanos(System.nanoTime() - startTime);
        }
        
        return results;
    }

    private PhaseResults runStressPhase() throws Exception {
        logger.info("Running stress test phase for {}", frameworkName);
        
        PhaseResults results = new PhaseResults("stress");
        long startTime = System.nanoTime();
        
        try {
            int duration = (Integer) benchmarkParams.getOrDefault("stressDurationSeconds", 60);
            int threads = (Integer) benchmarkParams.getOrDefault("stressThreads", 4);
            int payloadSize = (Integer) benchmarkParams.getOrDefault("payloadSize", 1000);
            
            // Run multi-threaded stress test
            StressTestResult stressResult = runMultiThreadedStressTest(duration, threads, payloadSize);
            
            results.setSuccess(true);
            results.setIterations(stressResult.getTotalOperations());
            results.setThroughput(stressResult.getOperationsPerSecond());
            results.setStatistics(stressResult.getStatistics());
            
        } catch (Exception e) {
            logger.error("Stress phase failed", e);
            results.setSuccess(false);
            results.setError(e.getMessage());
            throw e;
        } finally {
            results.setDurationNanos(System.nanoTime() - startTime);
        }
        
        return results;
    }

    private PhaseResults runMemoryPressurePhase() throws Exception {
        logger.info("Running memory pressure test phase for {}", frameworkName);
        
        PhaseResults results = new PhaseResults("memory");
        long startTime = System.nanoTime();
        
        try {
            int iterations = (Integer) benchmarkParams.getOrDefault("memoryIterations", 100);
            List<Integer> payloadSizes = Arrays.asList(1000, 10000, 100000, 1000000);
            
            Map<Integer, List<Long>> sizeToTimes = new HashMap<>();
            
            for (int size : payloadSizes) {
                List<Long> times = new ArrayList<>();
                
                for (int i = 0; i < iterations && !shutdownRequested.get(); i++) {
                    BenchmarkIteration iteration = runSingleBenchmarkIteration(size, true);
                    times.add(iteration.getDurationNanos());
                    
                    // Force GC periodically to test under memory pressure
                    if (i % 10 == 0) {
                        System.gc();
                        Thread.sleep(10);
                    }
                }
                
                sizeToTimes.put(size, times);
            }
            
            results.setSuccess(true);
            results.setMemoryPressureResults(sizeToTimes);
            
        } catch (Exception e) {
            logger.error("Memory pressure phase failed", e);
            results.setSuccess(false);
            results.setError(e.getMessage());
            throw e;
        } finally {
            results.setDurationNanos(System.nanoTime() - startTime);
        }
        
        return results;
    }

    private BenchmarkIteration runSingleBenchmarkIteration(int payloadSize, boolean measureMetrics) throws Exception {
        // This would be implemented based on the specific framework
        // For now, we'll simulate the benchmark operation
        
        long startTime = System.nanoTime();
        long startMemory = measureMetrics ? getCurrentMemoryUsage() : 0;
        
        // Simulate serialization work based on framework
        byte[] result = simulateSerializationWork(payloadSize);
        
        long endTime = System.nanoTime();
        long endMemory = measureMetrics ? getCurrentMemoryUsage() : 0;
        
        return new BenchmarkIteration(
            endTime - startTime,
            result.length,
            measureMetrics ? endMemory - startMemory : 0
        );
    }

    private byte[] simulateSerializationWork(int payloadSize) throws Exception {
        // This is a placeholder - in real implementation this would call
        // the actual Jackson serialization service based on frameworkName
        
        switch (frameworkName.toLowerCase()) {
            case "jackson-json":
                return simulateJacksonJsonSerialization(payloadSize);
            case "jackson-smile":
                return simulateJacksonSmileSerialization(payloadSize);
            case "jackson-cbor":
                return simulateJacksonCborSerialization(payloadSize);
            case "jackson-msgpack":
                return simulateJacksonMessagePackSerialization(payloadSize);
            default:
                return simulateJacksonJsonSerialization(payloadSize);
        }
    }

    private byte[] simulateJacksonJsonSerialization(int payloadSize) throws Exception {
        // Simulate JSON serialization
        StringBuilder json = new StringBuilder("{\"data\":\"");
        for (int i = 0; i < payloadSize; i++) {
            json.append("x");
        }
        json.append("\"}");
        return json.toString().getBytes();
    }

    private byte[] simulateJacksonSmileSerialization(int payloadSize) throws Exception {
        // Simulate binary format - typically smaller than JSON
        return new byte[payloadSize / 2];
    }

    private byte[] simulateJacksonCborSerialization(int payloadSize) throws Exception {
        // Simulate CBOR binary format
        return new byte[(int) (payloadSize * 0.7)];
    }

    private byte[] simulateJacksonMessagePackSerialization(int payloadSize) throws Exception {
        // Simulate MessagePack binary format
        return new byte[(int) (payloadSize * 0.6)];
    }

    private StressTestResult runMultiThreadedStressTest(int durationSeconds, int threads, int payloadSize) 
            throws Exception {
        // Implementation for multi-threaded stress testing
        // This would coordinate multiple threads performing benchmark operations
        
        return new StressTestResult(1000, 100.0, new HashMap<>());
    }

    private boolean shouldRunPhase(String phase) {
        return (Boolean) benchmarkParams.getOrDefault("run" + 
            phase.substring(0, 1).toUpperCase() + phase.substring(1), true);
    }

    private Map<String, Object> calculateStatistics(List<Long> times) {
        if (times.isEmpty()) {
            return new HashMap<>();
        }
        
        List<Long> sorted = new ArrayList<>(times);
        Collections.sort(sorted);
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("count", times.size());
        stats.put("min", sorted.get(0));
        stats.put("max", sorted.get(sorted.size() - 1));
        stats.put("mean", times.stream().mapToLong(Long::longValue).average().orElse(0));
        stats.put("p50", sorted.get(sorted.size() / 2));
        stats.put("p95", sorted.get((int) (sorted.size() * 0.95)));
        stats.put("p99", sorted.get((int) (sorted.size() * 0.99)));
        
        return stats;
    }

    private long getCurrentMemoryUsage() {
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        return memoryMXBean.getHeapMemoryUsage().getUsed();
    }

    private Map<String, Object> getJvmInfo() {
        RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        
        Map<String, Object> jvmInfo = new HashMap<>();
        jvmInfo.put("jvmName", runtimeMXBean.getVmName());
        jvmInfo.put("jvmVersion", runtimeMXBean.getVmVersion());
        jvmInfo.put("jvmVendor", runtimeMXBean.getVmVendor());
        jvmInfo.put("startTime", runtimeMXBean.getStartTime());
        jvmInfo.put("uptime", runtimeMXBean.getUptime());
        jvmInfo.put("heapMemory", memoryMXBean.getHeapMemoryUsage().toString());
        jvmInfo.put("nonHeapMemory", memoryMXBean.getNonHeapMemoryUsage().toString());
        jvmInfo.put("threadCount", threadMXBean.getThreadCount());
        jvmInfo.put("peakThreadCount", threadMXBean.getPeakThreadCount());
        
        return jvmInfo;
    }

    private Map<String, Object> getSystemInfo() {
        Map<String, Object> systemInfo = new HashMap<>();
        systemInfo.put("osName", System.getProperty("os.name"));
        systemInfo.put("osVersion", System.getProperty("os.version"));
        systemInfo.put("osArch", System.getProperty("os.arch"));
        systemInfo.put("javaVersion", System.getProperty("java.version"));
        systemInfo.put("javaVendor", System.getProperty("java.vendor"));
        systemInfo.put("availableProcessors", Runtime.getRuntime().availableProcessors());
        systemInfo.put("maxMemory", Runtime.getRuntime().maxMemory());
        systemInfo.put("totalMemory", Runtime.getRuntime().totalMemory());
        systemInfo.put("freeMemory", Runtime.getRuntime().freeMemory());
        
        return systemInfo;
    }

    private void saveResults(BenchmarkResults results) throws IOException {
        logger.info("Saving benchmark results for process: {}", processId);
        
        Path resultsFile = workspace.resolve("output").resolve("results.json");
        Files.write(resultsFile, objectMapper.writeValueAsBytes(results));
        
        // Also save metrics
        metricsCollector.saveMetrics(workspace.resolve("metrics").resolve("process-metrics.json"));
    }

    private void saveErrorResults(Exception error) throws IOException {
        Map<String, Object> errorResult = new HashMap<>();
        errorResult.put("processId", processId);
        errorResult.put("frameworkName", frameworkName);
        errorResult.put("success", false);
        errorResult.put("error", error.getMessage());
        errorResult.put("stackTrace", getStackTrace(error));
        errorResult.put("timestamp", Instant.now().toString());
        
        Path errorFile = workspace.resolve("output").resolve("error.json");
        Files.write(errorFile, objectMapper.writeValueAsBytes(errorResult));
    }

    private String getStackTrace(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }

    private void cleanup() {
        logger.info("Cleaning up process: {}", processId);
        // Any cleanup operations
    }

    private static ProcessArgs parseArguments(String[] args) {
        Map<String, String> argMap = new HashMap<>();
        
        for (String arg : args) {
            if (arg.startsWith("--")) {
                String[] parts = arg.substring(2).split("=", 2);
                if (parts.length == 2) {
                    argMap.put(parts[0], parts[1]);
                }
            }
        }
        
        String processId = argMap.get("process-id");
        String framework = argMap.get("framework");
        Path workspace = Paths.get(argMap.get("workspace"));
        
        // Parse JSON parameters
        Map<String, Object> params = new HashMap<>();
        String paramsJson = argMap.get("params");
        if (paramsJson != null) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                params = mapper.readValue(paramsJson, Map.class);
            } catch (Exception e) {
                System.err.println("Failed to parse parameters: " + e.getMessage());
            }
        }
        
        return new ProcessArgs(processId, framework, workspace, params);
    }

    // Inner classes for data structures
    private static class ProcessArgs {
        final String processId;
        final String framework;
        final Path workspace;
        final Map<String, Object> params;

        ProcessArgs(String processId, String framework, Path workspace, Map<String, Object> params) {
            this.processId = processId;
            this.framework = framework;
            this.workspace = workspace;
            this.params = params;
        }
    }

    private static class BenchmarkResults {
        private final String processId;
        private final String frameworkName;
        private final String timestamp;
        private PhaseResults warmupResults;
        private PhaseResults benchmarkResults;
        private PhaseResults stressResults;
        private PhaseResults memoryResults;

        public BenchmarkResults(String processId, String frameworkName) {
            this.processId = processId;
            this.frameworkName = frameworkName;
            this.timestamp = Instant.now().toString();
        }

        // Getters and setters
        public String getProcessId() { return processId; }
        public String getFrameworkName() { return frameworkName; }
        public String getTimestamp() { return timestamp; }
        public PhaseResults getWarmupResults() { return warmupResults; }
        public void setWarmupResults(PhaseResults warmupResults) { this.warmupResults = warmupResults; }
        public PhaseResults getBenchmarkResults() { return benchmarkResults; }
        public void setBenchmarkResults(PhaseResults benchmarkResults) { this.benchmarkResults = benchmarkResults; }
        public PhaseResults getStressResults() { return stressResults; }
        public void setStressResults(PhaseResults stressResults) { this.stressResults = stressResults; }
        public PhaseResults getMemoryResults() { return memoryResults; }
        public void setMemoryResults(PhaseResults memoryResults) { this.memoryResults = memoryResults; }
    }

    private static class PhaseResults {
        private final String phaseName;
        private final String startTime;
        private boolean success;
        private String error;
        private long durationNanos;
        private int iterations;
        private double throughput;
        private List<Long> operationTimes;
        private List<Integer> memorySizes;
        private Map<String, Object> statistics;
        private Map<Integer, List<Long>> memoryPressureResults;

        public PhaseResults(String phaseName) {
            this.phaseName = phaseName;
            this.startTime = Instant.now().toString();
        }

        // Getters and setters
        public String getPhaseName() { return phaseName; }
        public String getStartTime() { return startTime; }
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
        public long getDurationNanos() { return durationNanos; }
        public void setDurationNanos(long durationNanos) { this.durationNanos = durationNanos; }
        public int getIterations() { return iterations; }
        public void setIterations(int iterations) { this.iterations = iterations; }
        public double getThroughput() { return throughput; }
        public void setThroughput(double throughput) { this.throughput = throughput; }
        public List<Long> getOperationTimes() { return operationTimes; }
        public void setOperationTimes(List<Long> operationTimes) { this.operationTimes = operationTimes; }
        public List<Integer> getMemorySizes() { return memorySizes; }
        public void setMemorySizes(List<Integer> memorySizes) { this.memorySizes = memorySizes; }
        public Map<String, Object> getStatistics() { return statistics; }
        public void setStatistics(Map<String, Object> statistics) { this.statistics = statistics; }
        public Map<Integer, List<Long>> getMemoryPressureResults() { return memoryPressureResults; }
        public void setMemoryPressureResults(Map<Integer, List<Long>> memoryPressureResults) { 
            this.memoryPressureResults = memoryPressureResults; 
        }
    }

    private static class BenchmarkIteration {
        private final long durationNanos;
        private final int resultSize;
        private final long memoryDelta;

        public BenchmarkIteration(long durationNanos, int resultSize, long memoryDelta) {
            this.durationNanos = durationNanos;
            this.resultSize = resultSize;
            this.memoryDelta = memoryDelta;
        }

        public long getDurationNanos() { return durationNanos; }
        public int getResultSize() { return resultSize; }
        public long getMemoryDelta() { return memoryDelta; }
    }

    private static class StressTestResult {
        private final int totalOperations;
        private final double operationsPerSecond;
        private final Map<String, Object> statistics;

        public StressTestResult(int totalOperations, double operationsPerSecond, Map<String, Object> statistics) {
            this.totalOperations = totalOperations;
            this.operationsPerSecond = operationsPerSecond;
            this.statistics = statistics;
        }

        public int getTotalOperations() { return totalOperations; }
        public double getOperationsPerSecond() { return operationsPerSecond; }
        public Map<String, Object> getStatistics() { return statistics; }
    }

    private static class ProcessMetricsCollector {
        private volatile boolean collecting = false;

        public void start() {
            collecting = true;
            // Implementation for starting metrics collection
        }

        public void stop() {
            collecting = false;
        }

        public void saveMetrics(Path metricsFile) throws IOException {
            // Implementation for saving collected metrics
            Map<String, Object> metrics = new HashMap<>();
            metrics.put("collectionEnd", Instant.now().toString());
            
            Files.write(metricsFile, new ObjectMapper().writeValueAsBytes(metrics));
        }
    }
}