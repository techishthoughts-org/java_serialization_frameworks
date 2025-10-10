package org.techishthoughts.jackson.isolation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Main service for coordinating isolated benchmark executions across different serialization frameworks.
 * Provides high-level orchestration of the process isolation system.
 */
@Service
public class IsolatedBenchmarkExecutor {

    private static final Logger logger = LoggerFactory.getLogger(IsolatedBenchmarkExecutor.class);

    private final ProcessIsolationManager processManager;
    private final CrossProcessResultCollector resultCollector;
    private final ContainerOrchestrator containerOrchestrator;
    private final ResourceLimitConfig resourceConfig;
    
    @Autowired
    public IsolatedBenchmarkExecutor(ProcessIsolationManager processManager,
                                   CrossProcessResultCollector resultCollector,
                                   ContainerOrchestrator containerOrchestrator,
                                   ResourceLimitConfig resourceConfig) {
        this.processManager = processManager;
        this.resultCollector = resultCollector;
        this.containerOrchestrator = containerOrchestrator;
        this.resourceConfig = resourceConfig;
    }

    /**
     * Executes benchmarks for a single framework in isolation
     */
    public CompletableFuture<BenchmarkExecutionResult> executeSingleFrameworkBenchmark(
            String frameworkName, 
            BenchmarkConfiguration config) {
        
        return CompletableFuture.supplyAsync(() -> {
            logger.info("Starting isolated benchmark for framework: {}", frameworkName);
            Instant startTime = Instant.now();
            
            try {
                // Prepare benchmark parameters
                Map<String, Object> params = buildBenchmarkParameters(config);
                
                // Choose execution strategy based on configuration
                ProcessIsolationManager.ProcessResult result;
                if (resourceConfig.isUseContainers()) {
                    result = executeInContainer(frameworkName, params);
                } else {
                    result = executeInSeparateJVM(frameworkName, params);
                }
                
                // Collect and process results
                return processBenchmarkResult(frameworkName, result, startTime);
                
            } catch (Exception e) {
                logger.error("Failed to execute benchmark for framework: {}", frameworkName, e);
                return BenchmarkExecutionResult.failure(frameworkName, e.getMessage(), 
                    Duration.between(startTime, Instant.now()));
            }
        });
    }

    /**
     * Executes benchmarks for multiple frameworks concurrently with fair resource allocation
     */
    public CompletableFuture<List<BenchmarkExecutionResult>> executeConcurrentBenchmarks(
            List<String> frameworks, 
            BenchmarkConfiguration config) {
        
        return CompletableFuture.supplyAsync(() -> {
            logger.info("Starting concurrent benchmarks for {} frameworks", frameworks.size());
            Instant overallStartTime = Instant.now();
            
            try {
                // Create isolated processes for each framework
                List<ProcessIsolationManager.IsolatedProcess> processes = new ArrayList<>();
                Map<String, Object> baseParams = buildBenchmarkParameters(config);
                
                for (String framework : frameworks) {
                    Map<String, Object> frameworkParams = new HashMap<>(baseParams);
                    frameworkParams.put("concurrentExecution", true);
                    frameworkParams.put("totalConcurrentFrameworks", frameworks.size());
                    
                    ProcessIsolationManager.IsolatedProcess process = 
                        processManager.createIsolatedProcess(framework, frameworkParams);
                    processes.add(process);
                }
                
                // Execute all processes concurrently
                CompletableFuture<List<ProcessIsolationManager.ProcessResult>> processFuture = 
                    processManager.startProcessesConcurrently(processes);
                
                List<ProcessIsolationManager.ProcessResult> results = processFuture.get(
                    resourceConfig.getProcessTimeout().plusMinutes(5).getSeconds(), 
                    TimeUnit.SECONDS);
                
                // Process results
                List<BenchmarkExecutionResult> executionResults = new ArrayList<>();
                for (int i = 0; i < frameworks.size(); i++) {
                    String framework = frameworks.get(i);
                    ProcessIsolationManager.ProcessResult result = results.get(i);
                    
                    BenchmarkExecutionResult execResult = processBenchmarkResult(
                        framework, result, overallStartTime);
                    executionResults.add(execResult);
                }
                
                // Aggregate results for cross-framework analysis
                resultCollector.aggregateResults(executionResults);
                
                return executionResults;
                
            } catch (Exception e) {
                logger.error("Failed to execute concurrent benchmarks", e);
                return frameworks.stream()
                    .map(fw -> BenchmarkExecutionResult.failure(fw, e.getMessage(),
                        Duration.between(overallStartTime, Instant.now())))
                    .collect(Collectors.toList());
            }
        });
    }

    /**
     * Executes a comprehensive benchmark suite with multiple phases
     */
    public CompletableFuture<ComprehensiveBenchmarkResult> executeComprehensiveBenchmark(
            List<String> frameworks, 
            ComprehensiveBenchmarkConfiguration config) {
        
        return CompletableFuture.supplyAsync(() -> {
            logger.info("Starting comprehensive benchmark suite for {} frameworks", frameworks.size());
            Instant startTime = Instant.now();
            
            ComprehensiveBenchmarkResult result = new ComprehensiveBenchmarkResult(frameworks, startTime);
            
            try {
                // Phase 1: Warmup
                if (config.isIncludeWarmup()) {
                    logger.info("Phase 1: Warmup benchmarks");
                    BenchmarkConfiguration warmupConfig = config.getWarmupConfig();
                    List<BenchmarkExecutionResult> warmupResults = 
                        executeConcurrentBenchmarks(frameworks, warmupConfig).get();
                    result.setWarmupResults(warmupResults);
                }
                
                // Phase 2: Standard benchmarks
                logger.info("Phase 2: Standard benchmarks");
                List<BenchmarkExecutionResult> standardResults = 
                    executeConcurrentBenchmarks(frameworks, config.getStandardConfig()).get();
                result.setStandardResults(standardResults);
                
                // Phase 3: Stress testing (if enabled)
                if (config.isIncludeStressTesting()) {
                    logger.info("Phase 3: Stress testing");
                    List<BenchmarkExecutionResult> stressResults = 
                        executeStressTesting(frameworks, config.getStressConfig()).get();
                    result.setStressResults(stressResults);
                }
                
                // Phase 4: Memory pressure testing (if enabled)
                if (config.isIncludeMemoryPressure()) {
                    logger.info("Phase 4: Memory pressure testing");
                    List<BenchmarkExecutionResult> memoryResults = 
                        executeMemoryPressureTesting(frameworks, config.getMemoryConfig()).get();
                    result.setMemoryResults(memoryResults);
                }
                
                // Phase 5: Scalability testing (if enabled)
                if (config.isIncludeScalability()) {
                    logger.info("Phase 5: Scalability testing");
                    List<BenchmarkExecutionResult> scalabilityResults = 
                        executeScalabilityTesting(frameworks, config.getScalabilityConfig()).get();
                    result.setScalabilityResults(scalabilityResults);
                }
                
                result.setSuccess(true);
                result.setCompletionTime(Instant.now());
                
                // Generate comprehensive analysis
                resultCollector.generateComprehensiveAnalysis(result);
                
                logger.info("Comprehensive benchmark completed successfully in {}", 
                    Duration.between(startTime, Instant.now()));
                
                return result;
                
            } catch (Exception e) {
                logger.error("Comprehensive benchmark failed", e);
                result.setSuccess(false);
                result.setError(e.getMessage());
                result.setCompletionTime(Instant.now());
                return result;
            }
        });
    }

    /**
     * Executes stress testing with gradually increasing load
     */
    private CompletableFuture<List<BenchmarkExecutionResult>> executeStressTesting(
            List<String> frameworks, 
            BenchmarkConfiguration config) {
        
        return CompletableFuture.supplyAsync(() -> {
            logger.info("Executing stress testing for {} frameworks", frameworks.size());
            
            List<BenchmarkExecutionResult> results = new ArrayList<>();
            
            // Test with increasing thread counts
            int[] threadCounts = {1, 2, 4, 8, 16};
            
            for (int threads : threadCounts) {
                logger.info("Stress testing with {} threads", threads);
                
                BenchmarkConfiguration stressConfig = config.copy();
                stressConfig.setParameter("stressThreads", threads);
                stressConfig.setParameter("stressDurationSeconds", 30);
                
                try {
                    List<BenchmarkExecutionResult> threadResults = 
                        executeConcurrentBenchmarks(frameworks, stressConfig).get();
                    
                    // Mark results with thread count
                    threadResults.forEach(result -> 
                        result.addMetadata("stressThreads", threads));
                    
                    results.addAll(threadResults);
                    
                    // Brief cooldown between stress levels
                    Thread.sleep(5000);
                    
                } catch (Exception e) {
                    logger.warn("Stress test with {} threads failed: {}", threads, e.getMessage());
                }
            }
            
            return results;
        });
    }

    /**
     * Executes memory pressure testing with varying heap sizes
     */
    private CompletableFuture<List<BenchmarkExecutionResult>> executeMemoryPressureTesting(
            List<String> frameworks, 
            BenchmarkConfiguration config) {
        
        return CompletableFuture.supplyAsync(() -> {
            logger.info("Executing memory pressure testing for {} frameworks", frameworks.size());
            
            List<BenchmarkExecutionResult> results = new ArrayList<>();
            
            // Test with different heap sizes
            String[] heapSizes = {"256m", "512m", "1g", "2g"};
            
            for (String heapSize : heapSizes) {
                logger.info("Memory pressure testing with heap size: {}", heapSize);
                
                BenchmarkConfiguration memoryConfig = config.copy();
                memoryConfig.setParameter("maxHeapSize", heapSize);
                memoryConfig.setParameter("memoryIterations", 100);
                
                try {
                    // Create temporary resource config with specific heap size
                    ResourceLimitConfig tempConfig = new ResourceLimitConfig();
                    tempConfig.setMaxHeapSize(heapSize);
                    
                    List<BenchmarkExecutionResult> memoryResults = 
                        executeConcurrentBenchmarks(frameworks, memoryConfig).get();
                    
                    memoryResults.forEach(result -> 
                        result.addMetadata("heapSize", heapSize));
                    
                    results.addAll(memoryResults);
                    
                } catch (Exception e) {
                    logger.warn("Memory pressure test with heap {} failed: {}", heapSize, e.getMessage());
                }
            }
            
            return results;
        });
    }

    /**
     * Executes scalability testing with varying payload sizes
     */
    private CompletableFuture<List<BenchmarkExecutionResult>> executeScalabilityTesting(
            List<String> frameworks, 
            BenchmarkConfiguration config) {
        
        return CompletableFuture.supplyAsync(() -> {
            logger.info("Executing scalability testing for {} frameworks", frameworks.size());
            
            List<BenchmarkExecutionResult> results = new ArrayList<>();
            
            // Test with increasing payload sizes
            int[] payloadSizes = {1000, 10000, 100000, 1000000, 10000000};
            
            for (int payloadSize : payloadSizes) {
                logger.info("Scalability testing with payload size: {}", payloadSize);
                
                BenchmarkConfiguration scalabilityConfig = config.copy();
                scalabilityConfig.setParameter("payloadSize", payloadSize);
                scalabilityConfig.setParameter("benchmarkIterations", Math.max(100, 10000000 / payloadSize));
                
                try {
                    List<BenchmarkExecutionResult> scalabilityResults = 
                        executeConcurrentBenchmarks(frameworks, scalabilityConfig).get();
                    
                    scalabilityResults.forEach(result -> 
                        result.addMetadata("payloadSize", payloadSize));
                    
                    results.addAll(scalabilityResults);
                    
                } catch (Exception e) {
                    logger.warn("Scalability test with payload size {} failed: {}", payloadSize, e.getMessage());
                }
            }
            
            return results;
        });
    }

    private ProcessIsolationManager.ProcessResult executeInSeparateJVM(String frameworkName, 
                                                                      Map<String, Object> params) {
        ProcessIsolationManager.IsolatedProcess process = 
            processManager.createIsolatedProcess(frameworkName, params);
        
        try {
            return processManager.startProcess(process).get(
                resourceConfig.getProcessTimeout().getSeconds(), 
                TimeUnit.SECONDS);
        } catch (Exception e) {
            logger.error("Failed to execute in separate JVM for framework: {}", frameworkName, e);
            return new ProcessIsolationManager.ProcessResult(process.getProcessId(), false, 
                e.getMessage(), null);
        }
    }

    private ProcessIsolationManager.ProcessResult executeInContainer(String frameworkName, 
                                                                   Map<String, Object> params) {
        try {
            return containerOrchestrator.executeInContainer(frameworkName, params).get(
                resourceConfig.getProcessTimeout().getSeconds(), 
                TimeUnit.SECONDS);
        } catch (Exception e) {
            logger.error("Failed to execute in container for framework: {}", frameworkName, e);
            return new ProcessIsolationManager.ProcessResult("container-" + frameworkName, false, 
                e.getMessage(), null);
        }
    }

    private Map<String, Object> buildBenchmarkParameters(BenchmarkConfiguration config) {
        Map<String, Object> params = new HashMap<>();
        
        // Standard benchmark parameters
        params.put("benchmarkIterations", config.getIterations());
        params.put("payloadSize", config.getPayloadSize());
        params.put("warmupIterations", config.getWarmupIterations());
        
        // Phase control
        params.put("runWarmup", config.isIncludeWarmup());
        params.put("runBenchmark", true);
        params.put("runStress", config.isIncludeStressTesting());
        params.put("runMemory", config.isIncludeMemoryPressure());
        
        // Add any custom parameters
        params.putAll(config.getCustomParameters());
        
        return params;
    }

    private BenchmarkExecutionResult processBenchmarkResult(String frameworkName, 
                                                          ProcessIsolationManager.ProcessResult result, 
                                                          Instant startTime) {
        Duration executionTime = Duration.between(startTime, Instant.now());
        
        if (result.isSuccess()) {
            logger.info("Benchmark completed successfully for framework: {} in {}", 
                frameworkName, executionTime);
            return BenchmarkExecutionResult.success(frameworkName, result.getResults(), executionTime);
        } else {
            logger.warn("Benchmark failed for framework: {} - {}", frameworkName, result.getMessage());
            return BenchmarkExecutionResult.failure(frameworkName, result.getMessage(), executionTime);
        }
    }

    /**
     * Gets current status of all active benchmark processes
     */
    public Map<String, ProcessIsolationManager.ProcessStatus> getActiveProcessStatuses() {
        return processManager.getProcessStatuses();
    }

    /**
     * Terminates all active benchmark processes
     */
    public void terminateAllBenchmarks() {
        logger.info("Terminating all active benchmarks");
        processManager.terminateAllProcesses();
    }

    // Configuration classes
    public static class BenchmarkConfiguration {
        private int iterations = 1000;
        private int payloadSize = 1000;
        private int warmupIterations = 100;
        private boolean includeWarmup = true;
        private boolean includeStressTesting = false;
        private boolean includeMemoryPressure = false;
        private Map<String, Object> customParameters = new HashMap<>();

        public BenchmarkConfiguration copy() {
            BenchmarkConfiguration copy = new BenchmarkConfiguration();
            copy.iterations = this.iterations;
            copy.payloadSize = this.payloadSize;
            copy.warmupIterations = this.warmupIterations;
            copy.includeWarmup = this.includeWarmup;
            copy.includeStressTesting = this.includeStressTesting;
            copy.includeMemoryPressure = this.includeMemoryPressure;
            copy.customParameters = new HashMap<>(this.customParameters);
            return copy;
        }

        public void setParameter(String key, Object value) {
            customParameters.put(key, value);
        }

        // Getters and setters
        public int getIterations() { return iterations; }
        public void setIterations(int iterations) { this.iterations = iterations; }
        public int getPayloadSize() { return payloadSize; }
        public void setPayloadSize(int payloadSize) { this.payloadSize = payloadSize; }
        public int getWarmupIterations() { return warmupIterations; }
        public void setWarmupIterations(int warmupIterations) { this.warmupIterations = warmupIterations; }
        public boolean isIncludeWarmup() { return includeWarmup; }
        public void setIncludeWarmup(boolean includeWarmup) { this.includeWarmup = includeWarmup; }
        public boolean isIncludeStressTesting() { return includeStressTesting; }
        public void setIncludeStressTesting(boolean includeStressTesting) { this.includeStressTesting = includeStressTesting; }
        public boolean isIncludeMemoryPressure() { return includeMemoryPressure; }
        public void setIncludeMemoryPressure(boolean includeMemoryPressure) { this.includeMemoryPressure = includeMemoryPressure; }
        public Map<String, Object> getCustomParameters() { return customParameters; }
        public void setCustomParameters(Map<String, Object> customParameters) { this.customParameters = customParameters; }
    }

    public static class ComprehensiveBenchmarkConfiguration {
        private boolean includeWarmup = true;
        private boolean includeStressTesting = true;
        private boolean includeMemoryPressure = true;
        private boolean includeScalability = true;
        private BenchmarkConfiguration warmupConfig = new BenchmarkConfiguration();
        private BenchmarkConfiguration standardConfig = new BenchmarkConfiguration();
        private BenchmarkConfiguration stressConfig = new BenchmarkConfiguration();
        private BenchmarkConfiguration memoryConfig = new BenchmarkConfiguration();
        private BenchmarkConfiguration scalabilityConfig = new BenchmarkConfiguration();

        // Getters and setters
        public boolean isIncludeWarmup() { return includeWarmup; }
        public void setIncludeWarmup(boolean includeWarmup) { this.includeWarmup = includeWarmup; }
        public boolean isIncludeStressTesting() { return includeStressTesting; }
        public void setIncludeStressTesting(boolean includeStressTesting) { this.includeStressTesting = includeStressTesting; }
        public boolean isIncludeMemoryPressure() { return includeMemoryPressure; }
        public void setIncludeMemoryPressure(boolean includeMemoryPressure) { this.includeMemoryPressure = includeMemoryPressure; }
        public boolean isIncludeScalability() { return includeScalability; }
        public void setIncludeScalability(boolean includeScalability) { this.includeScalability = includeScalability; }
        public BenchmarkConfiguration getWarmupConfig() { return warmupConfig; }
        public void setWarmupConfig(BenchmarkConfiguration warmupConfig) { this.warmupConfig = warmupConfig; }
        public BenchmarkConfiguration getStandardConfig() { return standardConfig; }
        public void setStandardConfig(BenchmarkConfiguration standardConfig) { this.standardConfig = standardConfig; }
        public BenchmarkConfiguration getStressConfig() { return stressConfig; }
        public void setStressConfig(BenchmarkConfiguration stressConfig) { this.stressConfig = stressConfig; }
        public BenchmarkConfiguration getMemoryConfig() { return memoryConfig; }
        public void setMemoryConfig(BenchmarkConfiguration memoryConfig) { this.memoryConfig = memoryConfig; }
        public BenchmarkConfiguration getScalabilityConfig() { return scalabilityConfig; }
        public void setScalabilityConfig(BenchmarkConfiguration scalabilityConfig) { this.scalabilityConfig = scalabilityConfig; }
    }

    public static class BenchmarkExecutionResult {
        private final String frameworkName;
        private final boolean success;
        private final String errorMessage;
        private final Object results;
        private final Duration executionTime;
        private final Map<String, Object> metadata;
        private final Instant timestamp;

        private BenchmarkExecutionResult(String frameworkName, boolean success, String errorMessage,
                                       Object results, Duration executionTime) {
            this.frameworkName = frameworkName;
            this.success = success;
            this.errorMessage = errorMessage;
            this.results = results;
            this.executionTime = executionTime;
            this.metadata = new HashMap<>();
            this.timestamp = Instant.now();
        }

        public static BenchmarkExecutionResult success(String frameworkName, Object results, Duration executionTime) {
            return new BenchmarkExecutionResult(frameworkName, true, null, results, executionTime);
        }

        public static BenchmarkExecutionResult failure(String frameworkName, String errorMessage, Duration executionTime) {
            return new BenchmarkExecutionResult(frameworkName, false, errorMessage, null, executionTime);
        }

        public void addMetadata(String key, Object value) {
            this.metadata.put(key, value);
        }

        // Getters
        public String getFrameworkName() { return frameworkName; }
        public boolean isSuccess() { return success; }
        public String getErrorMessage() { return errorMessage; }
        public Object getResults() { return results; }
        public Duration getExecutionTime() { return executionTime; }
        public Map<String, Object> getMetadata() { return metadata; }
        public Instant getTimestamp() { return timestamp; }
    }

    public static class ComprehensiveBenchmarkResult {
        private final List<String> frameworks;
        private final Instant startTime;
        private Instant completionTime;
        private boolean success;
        private String error;
        private List<BenchmarkExecutionResult> warmupResults;
        private List<BenchmarkExecutionResult> standardResults;
        private List<BenchmarkExecutionResult> stressResults;
        private List<BenchmarkExecutionResult> memoryResults;
        private List<BenchmarkExecutionResult> scalabilityResults;

        public ComprehensiveBenchmarkResult(List<String> frameworks, Instant startTime) {
            this.frameworks = new ArrayList<>(frameworks);
            this.startTime = startTime;
        }

        // Getters and setters
        public List<String> getFrameworks() { return frameworks; }
        public Instant getStartTime() { return startTime; }
        public Instant getCompletionTime() { return completionTime; }
        public void setCompletionTime(Instant completionTime) { this.completionTime = completionTime; }
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
        public List<BenchmarkExecutionResult> getWarmupResults() { return warmupResults; }
        public void setWarmupResults(List<BenchmarkExecutionResult> warmupResults) { this.warmupResults = warmupResults; }
        public List<BenchmarkExecutionResult> getStandardResults() { return standardResults; }
        public void setStandardResults(List<BenchmarkExecutionResult> standardResults) { this.standardResults = standardResults; }
        public List<BenchmarkExecutionResult> getStressResults() { return stressResults; }
        public void setStressResults(List<BenchmarkExecutionResult> stressResults) { this.stressResults = stressResults; }
        public List<BenchmarkExecutionResult> getMemoryResults() { return memoryResults; }
        public void setMemoryResults(List<BenchmarkExecutionResult> memoryResults) { this.memoryResults = memoryResults; }
        public List<BenchmarkExecutionResult> getScalabilityResults() { return scalabilityResults; }
        public void setScalabilityResults(List<BenchmarkExecutionResult> scalabilityResults) { this.scalabilityResults = scalabilityResults; }

        public Duration getTotalDuration() {
            return completionTime != null ? Duration.between(startTime, completionTime) : Duration.ZERO;
        }
    }
}