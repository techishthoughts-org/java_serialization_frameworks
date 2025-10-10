package org.techishthoughts.jackson.isolation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Manages the lifecycle of isolated benchmark processes.
 * Handles process creation, monitoring, resource enforcement, and cleanup.
 */
@Component
public class ProcessIsolationManager {

    private static final Logger logger = LoggerFactory.getLogger(ProcessIsolationManager.class);
    
    private final ResourceLimitConfig resourceConfig;
    private final ExecutorService processExecutor;
    private final ScheduledExecutorService monitoringExecutor;
    private final Map<String, IsolatedProcess> activeProcesses;
    private final AtomicInteger processIdCounter;
    private final Path workspaceRoot;

    @Autowired
    public ProcessIsolationManager(ResourceLimitConfig resourceConfig) {
        this.resourceConfig = resourceConfig;
        this.processExecutor = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "isolated-process-executor");
            t.setDaemon(true);
            return t;
        });
        this.monitoringExecutor = Executors.newScheduledThreadPool(4, r -> {
            Thread t = new Thread(r, "process-monitor");
            t.setDaemon(true);
            return t;
        });
        this.activeProcesses = new ConcurrentHashMap<>();
        this.processIdCounter = new AtomicInteger(0);
        
        try {
            this.workspaceRoot = Files.createTempDirectory("benchmark-isolation-");
            logger.info("Created workspace at: {}", workspaceRoot);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create workspace directory", e);
        }
    }

    /**
     * Creates an isolated process for running benchmarks
     */
    public IsolatedProcess createIsolatedProcess(String frameworkName, Map<String, Object> benchmarkParams) {
        String processId = generateProcessId(frameworkName);
        Path processWorkspace = createProcessWorkspace(processId);
        
        IsolatedProcess process = new IsolatedProcess(
            processId,
            frameworkName,
            processWorkspace,
            benchmarkParams,
            resourceConfig
        );
        
        activeProcesses.put(processId, process);
        logger.info("Created isolated process: {} for framework: {}", processId, frameworkName);
        
        return process;
    }

    /**
     * Starts an isolated process with resource monitoring
     */
    public CompletableFuture<ProcessResult> startProcess(IsolatedProcess isolatedProcess) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.info("Starting isolated process: {}", isolatedProcess.getProcessId());
                
                // Build process command
                List<String> command = buildProcessCommand(isolatedProcess);
                
                // Create process builder
                ProcessBuilder pb = new ProcessBuilder(command);
                pb.directory(isolatedProcess.getWorkspace().toFile());
                pb.environment().putAll(buildEnvironmentVariables(isolatedProcess));
                
                // Start process
                Process process = pb.start();
                isolatedProcess.setSystemProcess(process);
                
                // Start monitoring
                startProcessMonitoring(isolatedProcess);
                
                // Wait for completion or timeout
                boolean completed = process.waitFor(
                    resourceConfig.getProcessTimeout().getSeconds(),
                    TimeUnit.SECONDS
                );
                
                if (!completed) {
                    logger.warn("Process {} timed out, terminating", isolatedProcess.getProcessId());
                    terminateProcess(isolatedProcess);
                    return new ProcessResult(isolatedProcess.getProcessId(), false, "Process timed out", null);
                }
                
                int exitCode = process.exitValue();
                boolean success = exitCode == 0;
                
                logger.info("Process {} completed with exit code: {}", isolatedProcess.getProcessId(), exitCode);
                
                // Collect results
                Object results = collectProcessResults(isolatedProcess);
                
                return new ProcessResult(isolatedProcess.getProcessId(), success, 
                    success ? "Process completed successfully" : "Process failed with exit code: " + exitCode,
                    results);
                    
            } catch (Exception e) {
                logger.error("Error running isolated process: {}", isolatedProcess.getProcessId(), e);
                return new ProcessResult(isolatedProcess.getProcessId(), false, 
                    "Process failed with exception: " + e.getMessage(), null);
            } finally {
                cleanupProcess(isolatedProcess);
            }
        }, processExecutor);
    }

    /**
     * Starts multiple processes concurrently
     */
    public CompletableFuture<List<ProcessResult>> startProcessesConcurrently(List<IsolatedProcess> processes) {
        List<CompletableFuture<ProcessResult>> futures = processes.stream()
            .map(this::startProcess)
            .collect(java.util.stream.Collectors.toList());
        
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> futures.stream()
                .map(CompletableFuture::join)
                .collect(java.util.stream.Collectors.toList()));
    }

    /**
     * Terminates a running process
     */
    public void terminateProcess(IsolatedProcess isolatedProcess) {
        Process systemProcess = isolatedProcess.getSystemProcess();
        if (systemProcess != null && systemProcess.isAlive()) {
            logger.info("Terminating process: {}", isolatedProcess.getProcessId());
            
            // Try graceful shutdown first
            systemProcess.destroy();
            
            try {
                boolean terminated = systemProcess.waitFor(10, TimeUnit.SECONDS);
                if (!terminated) {
                    logger.warn("Process {} did not terminate gracefully, force killing", 
                        isolatedProcess.getProcessId());
                    systemProcess.destroyForcibly();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                systemProcess.destroyForcibly();
            }
        }
    }

    /**
     * Terminates all active processes
     */
    public void terminateAllProcesses() {
        logger.info("Terminating all {} active processes", activeProcesses.size());
        
        activeProcesses.values().parallelStream()
            .forEach(this::terminateProcess);
        
        activeProcesses.clear();
    }

    /**
     * Gets status of all active processes
     */
    public Map<String, ProcessStatus> getProcessStatuses() {
        Map<String, ProcessStatus> statuses = new HashMap<>();
        
        for (IsolatedProcess process : activeProcesses.values()) {
            statuses.put(process.getProcessId(), getProcessStatus(process));
        }
        
        return statuses;
    }

    /**
     * Gets status of a specific process
     */
    public ProcessStatus getProcessStatus(IsolatedProcess isolatedProcess) {
        Process systemProcess = isolatedProcess.getSystemProcess();
        
        if (systemProcess == null) {
            return ProcessStatus.NOT_STARTED;
        } else if (systemProcess.isAlive()) {
            return ProcessStatus.RUNNING;
        } else {
            int exitCode = systemProcess.exitValue();
            return exitCode == 0 ? ProcessStatus.COMPLETED_SUCCESS : ProcessStatus.COMPLETED_FAILURE;
        }
    }

    private String generateProcessId(String frameworkName) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        int counter = processIdCounter.incrementAndGet();
        return String.format("%s-%s-%03d", frameworkName, timestamp, counter);
    }

    private Path createProcessWorkspace(String processId) {
        try {
            Path workspace = workspaceRoot.resolve(processId);
            Files.createDirectories(workspace);
            Files.createDirectories(workspace.resolve("logs"));
            Files.createDirectories(workspace.resolve("output"));
            Files.createDirectories(workspace.resolve("tmp"));
            return workspace;
        } catch (IOException e) {
            throw new RuntimeException("Failed to create process workspace for: " + processId, e);
        }
    }

    private List<String> buildProcessCommand(IsolatedProcess isolatedProcess) {
        List<String> command = new ArrayList<>();
        
        // Java executable
        String javaHome = System.getProperty("java.home");
        command.add(Paths.get(javaHome, "bin", "java").toString());
        
        // JVM arguments from resource config
        String jvmArgs = resourceConfig.toJvmArgsString();
        if (!jvmArgs.isEmpty()) {
            command.addAll(Arrays.asList(jvmArgs.split("\\s+")));
        }
        
        // Set CPU affinity if specified
        if (resourceConfig.getCpuAffinityMask() != null) {
            // This would need platform-specific implementation
            // For now, we'll log the requirement
            logger.info("CPU affinity mask specified: {} (platform-specific implementation needed)", 
                resourceConfig.getCpuAffinityMask());
        }
        
        // Classpath
        command.add("-cp");
        command.add(System.getProperty("java.class.path"));
        
        // Main class (our process runner)
        command.add("org.techishthoughts.jackson.isolation.ProcessIsolatedRunner");
        
        // Process arguments
        command.add("--process-id=" + isolatedProcess.getProcessId());
        command.add("--framework=" + isolatedProcess.getFrameworkName());
        command.add("--workspace=" + isolatedProcess.getWorkspace().toString());
        
        // Benchmark parameters as JSON
        command.add("--params=" + serializeBenchmarkParams(isolatedProcess.getBenchmarkParams()));
        
        return command;
    }

    private Map<String, String> buildEnvironmentVariables(IsolatedProcess isolatedProcess) {
        Map<String, String> env = new HashMap<>(System.getenv());
        
        // Set resource limits as environment variables for the child process
        env.put("BENCHMARK_MAX_MEMORY", resourceConfig.getMaxHeapSize());
        env.put("BENCHMARK_MAX_THREADS", String.valueOf(resourceConfig.getMaxThreads()));
        env.put("BENCHMARK_TIMEOUT_SECONDS", String.valueOf(resourceConfig.getBenchmarkTimeout().getSeconds()));
        env.put("BENCHMARK_WORKSPACE", isolatedProcess.getWorkspace().toString());
        
        // Set JVM options
        env.put("JAVA_OPTS", resourceConfig.toJvmArgsString());
        
        return env;
    }

    private void startProcessMonitoring(IsolatedProcess isolatedProcess) {
        if (resourceConfig.isEnableJvmMetrics()) {
            monitoringExecutor.scheduleAtFixedRate(
                () -> collectProcessMetrics(isolatedProcess),
                1,
                resourceConfig.getMetricsCollectionIntervalMs() / 1000,
                TimeUnit.SECONDS
            );
        }
    }

    private void collectProcessMetrics(IsolatedProcess isolatedProcess) {
        try {
            Process systemProcess = isolatedProcess.getSystemProcess();
            if (systemProcess == null || !systemProcess.isAlive()) {
                return;
            }

            ProcessHandle processHandle = systemProcess.toHandle();
            ProcessHandle.Info info = processHandle.info();
            
            // Collect basic metrics
            Map<String, Object> metrics = new HashMap<>();
            metrics.put("timestamp", System.currentTimeMillis());
            metrics.put("pid", processHandle.pid());
            metrics.put("cpuTime", info.totalCpuDuration().orElse(null));
            
            // Store metrics for later analysis
            isolatedProcess.addMetrics(metrics);
            
            // Check resource limits
            checkResourceLimits(isolatedProcess, metrics);
            
        } catch (Exception e) {
            logger.warn("Error collecting metrics for process: {}", isolatedProcess.getProcessId(), e);
        }
    }

    private void checkResourceLimits(IsolatedProcess isolatedProcess, Map<String, Object> metrics) {
        switch (resourceConfig.getEnforcementStrategy()) {
            case SOFT_LIMITS:
                // Log warnings for limit violations
                logResourceLimitViolations(isolatedProcess, metrics);
                break;
            case HARD_LIMITS:
                // Terminate process if limits exceeded
                if (isResourceLimitExceeded(metrics)) {
                    logger.warn("Process {} exceeded resource limits, terminating", 
                        isolatedProcess.getProcessId());
                    terminateProcess(isolatedProcess);
                }
                break;
            case ADAPTIVE:
                // Implement adaptive resource management
                adjustResourceLimits(isolatedProcess, metrics);
                break;
        }
    }

    private void logResourceLimitViolations(IsolatedProcess isolatedProcess, Map<String, Object> metrics) {
        // Implementation for logging resource violations
        // This would check against configured limits and log warnings
    }

    private boolean isResourceLimitExceeded(Map<String, Object> metrics) {
        // Implementation for checking if resource limits are exceeded
        // This would compare current usage against configured limits
        return false; // Placeholder
    }

    private void adjustResourceLimits(IsolatedProcess isolatedProcess, Map<String, Object> metrics) {
        // Implementation for adaptive resource management
        // This could dynamically adjust limits based on system load
    }

    private String serializeBenchmarkParams(Map<String, Object> params) {
        // Simple JSON serialization for benchmark parameters
        StringBuilder json = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            if (!first) json.append(",");
            json.append("\"").append(entry.getKey()).append("\":\"")
                .append(entry.getValue()).append("\"");
            first = false;
        }
        json.append("}");
        return json.toString();
    }

    private Object collectProcessResults(IsolatedProcess isolatedProcess) {
        try {
            // Read results from the process workspace
            Path resultsFile = isolatedProcess.getWorkspace().resolve("output").resolve("results.json");
            if (Files.exists(resultsFile)) {
                return Files.readString(resultsFile);
            }
        } catch (IOException e) {
            logger.warn("Failed to read results from process: {}", isolatedProcess.getProcessId(), e);
        }
        return null;
    }

    private void cleanupProcess(IsolatedProcess isolatedProcess) {
        activeProcesses.remove(isolatedProcess.getProcessId());
        
        // Optional: Clean up workspace directory
        try {
            // Keep workspace for debugging purposes, but could be configured to clean up
            logger.debug("Process workspace retained at: {}", isolatedProcess.getWorkspace());
        } catch (Exception e) {
            logger.warn("Error during process cleanup: {}", isolatedProcess.getProcessId(), e);
        }
    }

    /**
     * Shutdown the isolation manager and cleanup resources
     */
    public void shutdown() {
        logger.info("Shutting down ProcessIsolationManager");
        
        terminateAllProcesses();
        
        processExecutor.shutdown();
        monitoringExecutor.shutdown();
        
        try {
            if (!processExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                processExecutor.shutdownNow();
            }
            if (!monitoringExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                monitoringExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            processExecutor.shutdownNow();
            monitoringExecutor.shutdownNow();
        }
    }

    // Inner classes for process management
    public enum ProcessStatus {
        NOT_STARTED,
        RUNNING,
        COMPLETED_SUCCESS,
        COMPLETED_FAILURE,
        TERMINATED
    }

    public static class ProcessResult {
        private final String processId;
        private final boolean success;
        private final String message;
        private final Object results;

        public ProcessResult(String processId, boolean success, String message, Object results) {
            this.processId = processId;
            this.success = success;
            this.message = message;
            this.results = results;
        }

        public String getProcessId() { return processId; }
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public Object getResults() { return results; }

        @Override
        public String toString() {
            return String.format("ProcessResult{id='%s', success=%s, message='%s'}", 
                processId, success, message);
        }
    }

    public static class IsolatedProcess {
        private final String processId;
        private final String frameworkName;
        private final Path workspace;
        private final Map<String, Object> benchmarkParams;
        private final ResourceLimitConfig resourceConfig;
        private final List<Map<String, Object>> metrics;
        private Process systemProcess;

        public IsolatedProcess(String processId, String frameworkName, Path workspace,
                             Map<String, Object> benchmarkParams, ResourceLimitConfig resourceConfig) {
            this.processId = processId;
            this.frameworkName = frameworkName;
            this.workspace = workspace;
            this.benchmarkParams = new HashMap<>(benchmarkParams);
            this.resourceConfig = resourceConfig;
            this.metrics = new ArrayList<>();
        }

        // Getters
        public String getProcessId() { return processId; }
        public String getFrameworkName() { return frameworkName; }
        public Path getWorkspace() { return workspace; }
        public Map<String, Object> getBenchmarkParams() { return benchmarkParams; }
        public ResourceLimitConfig getResourceConfig() { return resourceConfig; }
        public List<Map<String, Object>> getMetrics() { return new ArrayList<>(metrics); }
        
        public Process getSystemProcess() { return systemProcess; }
        public void setSystemProcess(Process systemProcess) { this.systemProcess = systemProcess; }
        
        public void addMetrics(Map<String, Object> metric) {
            this.metrics.add(new HashMap<>(metric));
        }
    }
}