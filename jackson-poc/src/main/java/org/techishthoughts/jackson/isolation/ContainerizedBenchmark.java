package org.techishthoughts.jackson.isolation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Handles Docker-based containerized benchmark execution for maximum isolation.
 * Provides sophisticated container management and resource control.
 */
public class ContainerizedBenchmark {

    private static final Logger logger = LoggerFactory.getLogger(ContainerizedBenchmark.class);
    
    private final ResourceLimitConfig resourceConfig;
    private final String containerRuntime;
    private final Path workspaceRoot;
    private final Map<String, ContainerInfo> activeContainers;

    public ContainerizedBenchmark(ResourceLimitConfig resourceConfig) {
        this.resourceConfig = resourceConfig;
        this.containerRuntime = resourceConfig.getContainerRuntime();
        this.activeContainers = new HashMap<>();
        
        try {
            this.workspaceRoot = Files.createTempDirectory("container-benchmarks-");
            logger.info("Container workspace created at: {}", workspaceRoot);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create container workspace", e);
        }
        
        validateContainerRuntime();
    }

    /**
     * Executes a benchmark in a containerized environment
     */
    public CompletableFuture<ContainerizedBenchmarkResult> executeBenchmark(
            String frameworkName, 
            Map<String, Object> benchmarkParams) {
        
        return CompletableFuture.supplyAsync(() -> {
            String containerId = generateContainerId(frameworkName);
            logger.info("Starting containerized benchmark for framework: {} in container: {}", 
                frameworkName, containerId);
            
            Instant startTime = Instant.now();
            
            try {
                // Prepare container workspace
                Path containerWorkspace = prepareContainerWorkspace(containerId, frameworkName, benchmarkParams);
                
                // Build container image if needed
                String imageName = buildOrGetImage(frameworkName);
                
                // Create and configure container
                ContainerConfiguration config = createContainerConfiguration(
                    containerId, imageName, containerWorkspace, benchmarkParams);
                
                // Start container
                ContainerInfo containerInfo = startContainer(config);
                activeContainers.put(containerId, containerInfo);
                
                // Monitor and wait for completion
                ContainerExecutionResult result = monitorContainerExecution(containerInfo);
                
                // Collect results
                Object benchmarkResults = collectContainerResults(containerInfo);
                
                // Cleanup
                cleanupContainer(containerInfo);
                
                return new ContainerizedBenchmarkResult(
                    containerId, frameworkName, result.isSuccess(), 
                    result.getExitCode(), result.getOutput(), benchmarkResults,
                    Duration.between(startTime, Instant.now())
                );
                
            } catch (Exception e) {
                logger.error("Containerized benchmark failed for framework: {}", frameworkName, e);
                return new ContainerizedBenchmarkResult(
                    containerId, frameworkName, false, -1, e.getMessage(), null,
                    Duration.between(startTime, Instant.now())
                );
            } finally {
                activeContainers.remove(containerId);
            }
        });
    }

    /**
     * Executes multiple benchmarks concurrently in separate containers
     */
    public CompletableFuture<List<ContainerizedBenchmarkResult>> executeConcurrentBenchmarks(
            List<String> frameworks, 
            Map<String, Object> benchmarkParams) {
        
        List<CompletableFuture<ContainerizedBenchmarkResult>> futures = frameworks.stream()
            .map(framework -> executeBenchmark(framework, benchmarkParams))
            .collect(java.util.stream.Collectors.toList());
        
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> futures.stream()
                .map(CompletableFuture::join)
                .collect(java.util.stream.Collectors.toList()));
    }

    /**
     * Creates a container with resource limits and network isolation
     */
    private ContainerInfo startContainer(ContainerConfiguration config) throws IOException, InterruptedException {
        List<String> command = buildContainerCommand(config);
        
        logger.info("Starting container with command: {}", String.join(" ", command));
        
        ProcessBuilder pb = new ProcessBuilder(command);
        Process process = pb.start();
        
        // Wait for container to start
        boolean started = process.waitFor(30, TimeUnit.SECONDS);
        if (!started || process.exitValue() != 0) {
            String error = readProcessOutput(process.getErrorStream());
            throw new RuntimeException("Failed to start container: " + error);
        }
        
        String containerId = readProcessOutput(process.getInputStream()).trim();
        
        ContainerInfo containerInfo = new ContainerInfo(
            containerId, config.getImageName(), config.getWorkspace(), 
            Instant.now(), config.getFrameworkName()
        );
        
        // Wait for container to be ready
        waitForContainerReady(containerInfo);
        
        logger.info("Container started successfully: {}", containerId);
        return containerInfo;
    }

    /**
     * Monitors container execution with timeout and resource monitoring
     */
    private ContainerExecutionResult monitorContainerExecution(ContainerInfo containerInfo) 
            throws IOException, InterruptedException {
        
        String containerId = containerInfo.getContainerId();
        logger.info("Monitoring container execution: {}", containerId);
        
        // Start resource monitoring in background
        CompletableFuture<Void> monitoringFuture = startResourceMonitoring(containerInfo);
        
        try {
            // Wait for container to complete
            List<String> waitCommand = Arrays.asList(
                containerRuntime, "wait", containerId
            );
            
            ProcessBuilder pb = new ProcessBuilder(waitCommand);
            Process waitProcess = pb.start();
            
            boolean completed = waitProcess.waitFor(
                resourceConfig.getProcessTimeout().getSeconds(), 
                TimeUnit.SECONDS
            );
            
            if (!completed) {
                logger.warn("Container {} timed out, stopping it", containerId);
                stopContainer(containerId);
                return new ContainerExecutionResult(false, 124, "Container timed out");
            }
            
            int exitCode = Integer.parseInt(readProcessOutput(waitProcess.getInputStream()).trim());
            
            // Get container logs
            String output = getContainerLogs(containerId);
            
            return new ContainerExecutionResult(exitCode == 0, exitCode, output);
            
        } finally {
            monitoringFuture.cancel(true);
        }
    }

    /**
     * Starts resource monitoring for a container
     */
    private CompletableFuture<Void> startResourceMonitoring(ContainerInfo containerInfo) {
        return CompletableFuture.runAsync(() -> {
            String containerId = containerInfo.getContainerId();
            List<ContainerResourceMetrics> metrics = new ArrayList<>();
            
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    ContainerResourceMetrics metric = collectContainerMetrics(containerId);
                    if (metric != null) {
                        metrics.add(metric);
                        containerInfo.addResourceMetrics(metric);
                        
                        // Check resource limits
                        checkResourceLimits(containerInfo, metric);
                    }
                    
                    Thread.sleep(resourceConfig.getMetricsCollectionIntervalMs());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                logger.warn("Error collecting container metrics for {}: {}", containerId, e.getMessage());
            }
            
            containerInfo.setResourceMetrics(metrics);
        });
    }

    /**
     * Collects resource metrics from a running container
     */
    private ContainerResourceMetrics collectContainerMetrics(String containerId) {
        try {
            List<String> statsCommand = Arrays.asList(
                containerRuntime, "stats", containerId, "--no-stream", "--format", 
                "table {{.CPUPerc}},{{.MemUsage}},{{.MemPerc}},{{.NetIO}},{{.BlockIO}}"
            );
            
            ProcessBuilder pb = new ProcessBuilder(statsCommand);
            Process process = pb.start();
            
            boolean completed = process.waitFor(5, TimeUnit.SECONDS);
            if (!completed || process.exitValue() != 0) {
                return null;
            }
            
            String output = readProcessOutput(process.getInputStream());
            return parseContainerStats(output);
            
        } catch (Exception e) {
            logger.debug("Failed to collect container metrics: {}", e.getMessage());
            return null;
        }
    }

    private ContainerResourceMetrics parseContainerStats(String statsOutput) {
        // Parse Docker/Podman stats output
        String[] lines = statsOutput.split("\n");
        if (lines.length < 2) {
            return null;
        }
        
        // Skip header line, parse data line
        String dataLine = lines[1];
        String[] parts = dataLine.split(",");
        
        if (parts.length >= 5) {
            try {
                String cpuPercStr = parts[0].trim().replace("%", "");
                double cpuPercentage = Double.parseDouble(cpuPercStr);
                
                String memUsage = parts[1].trim(); // e.g., "123MB / 512MB"
                String memPercStr = parts[2].trim().replace("%", "");
                double memoryPercentage = Double.parseDouble(memPercStr);
                
                String networkIO = parts[3].trim();
                String blockIO = parts[4].trim();
                
                return new ContainerResourceMetrics(
                    Instant.now(), cpuPercentage, memUsage, memoryPercentage, 
                    networkIO, blockIO
                );
            } catch (NumberFormatException e) {
                logger.debug("Failed to parse container stats: {}", e.getMessage());
            }
        }
        
        return null;
    }

    private void checkResourceLimits(ContainerInfo containerInfo, ContainerResourceMetrics metrics) {
        // Check CPU limits
        if (metrics.getCpuPercentage() > resourceConfig.getCpuQuota() * 100) {
            logger.warn("Container {} exceeded CPU limit: {}%", 
                containerInfo.getContainerId(), metrics.getCpuPercentage());
        }
        
        // Check memory limits  
        if (metrics.getMemoryPercentage() > 90.0) {
            logger.warn("Container {} approaching memory limit: {}%", 
                containerInfo.getContainerId(), metrics.getMemoryPercentage());
        }
        
        // Apply enforcement strategy
        switch (resourceConfig.getEnforcementStrategy()) {
            case HARD_LIMITS:
                if (metrics.getCpuPercentage() > resourceConfig.getCpuQuota() * 150 ||
                    metrics.getMemoryPercentage() > 95.0) {
                    logger.warn("Container {} exceeded hard limits, stopping", containerInfo.getContainerId());
                    try {
                        stopContainer(containerInfo.getContainerId());
                    } catch (Exception e) {
                        logger.error("Failed to stop container", e);
                    }
                }
                break;
            case SOFT_LIMITS:
                // Just log warnings
                break;
            case ADAPTIVE:
                // Could implement dynamic resource adjustment
                break;
        }
    }

    private Path prepareContainerWorkspace(String containerId, String frameworkName, 
                                         Map<String, Object> benchmarkParams) throws IOException {
        Path containerWorkspace = workspaceRoot.resolve(containerId);
        Files.createDirectories(containerWorkspace);
        Files.createDirectories(containerWorkspace.resolve("input"));
        Files.createDirectories(containerWorkspace.resolve("output"));
        Files.createDirectories(containerWorkspace.resolve("logs"));
        
        // Create benchmark parameters file
        Path paramsFile = containerWorkspace.resolve("input").resolve("benchmark-params.json");
        String paramsJson = serializeParameters(benchmarkParams);
        Files.write(paramsFile, paramsJson.getBytes());
        
        // Create framework-specific configuration
        createFrameworkConfiguration(containerWorkspace, frameworkName, benchmarkParams);
        
        return containerWorkspace;
    }

    private void createFrameworkConfiguration(Path workspace, String frameworkName, 
                                            Map<String, Object> params) throws IOException {
        // Create framework-specific configuration files
        Path configFile = workspace.resolve("input").resolve("framework-config.properties");
        
        Properties config = new Properties();
        config.setProperty("framework.name", frameworkName);
        config.setProperty("benchmark.iterations", params.getOrDefault("benchmarkIterations", "1000").toString());
        config.setProperty("payload.size", params.getOrDefault("payloadSize", "1000").toString());
        config.setProperty("warmup.iterations", params.getOrDefault("warmupIterations", "100").toString());
        
        try (var writer = Files.newBufferedWriter(configFile)) {
            config.store(writer, "Framework configuration for " + frameworkName);
        }
    }

    private String buildOrGetImage(String frameworkName) throws IOException, InterruptedException {
        String imageName = "benchmark-" + frameworkName.toLowerCase();
        
        // Check if image exists
        if (imageExists(imageName)) {
            logger.info("Using existing image: {}", imageName);
            return imageName;
        }
        
        // Build image
        logger.info("Building container image: {}", imageName);
        buildImage(imageName, frameworkName);
        
        return imageName;
    }

    private boolean imageExists(String imageName) throws IOException, InterruptedException {
        List<String> command = Arrays.asList(
            containerRuntime, "image", "inspect", imageName
        );
        
        ProcessBuilder pb = new ProcessBuilder(command);
        Process process = pb.start();
        
        return process.waitFor(10, TimeUnit.SECONDS) && process.exitValue() == 0;
    }

    private void buildImage(String imageName, String frameworkName) throws IOException, InterruptedException {
        // Create Dockerfile
        Path dockerfilePath = createDockerfile(frameworkName);
        
        List<String> buildCommand = Arrays.asList(
            containerRuntime, "build", 
            "-t", imageName,
            "-f", dockerfilePath.toString(),
            "."
        );
        
        ProcessBuilder pb = new ProcessBuilder(buildCommand);
        pb.directory(workspaceRoot.toFile());
        Process process = pb.start();
        
        boolean completed = process.waitFor(5, TimeUnit.MINUTES);
        if (!completed || process.exitValue() != 0) {
            String error = readProcessOutput(process.getErrorStream());
            throw new RuntimeException("Failed to build image: " + error);
        }
        
        logger.info("Successfully built image: {}", imageName);
    }

    private Path createDockerfile(String frameworkName) throws IOException {
        Path dockerfilePath = workspaceRoot.resolve("Dockerfile." + frameworkName);
        
        StringBuilder dockerfile = new StringBuilder();
        dockerfile.append("FROM ").append(resourceConfig.getBaseImage()).append("\n");
        dockerfile.append("LABEL framework=").append(frameworkName).append("\n");
        dockerfile.append("LABEL purpose=benchmark\n");
        dockerfile.append("\n");
        dockerfile.append("# Install required packages\n");
        dockerfile.append("RUN apt-get update && apt-get install -y \\\n");
        dockerfile.append("    curl \\\n");
        dockerfile.append("    && rm -rf /var/lib/apt/lists/*\n");
        dockerfile.append("\n");
        dockerfile.append("# Copy application JAR\n");
        dockerfile.append("COPY *.jar /app/benchmark.jar\n");
        dockerfile.append("\n");
        dockerfile.append("# Create workspace\n");
        dockerfile.append("RUN mkdir -p /workspace/input /workspace/output /workspace/logs\n");
        dockerfile.append("WORKDIR /workspace\n");
        dockerfile.append("\n");
        dockerfile.append("# Set JVM options\n");
        dockerfile.append("ENV JAVA_OPTS=\"").append(resourceConfig.toJvmArgsString()).append("\"\n");
        dockerfile.append("\n");
        dockerfile.append("# Entry point\n");
        dockerfile.append("ENTRYPOINT [\"java\", \"-cp\", \"/app/benchmark.jar\", \\\n");
        dockerfile.append("    \"org.techishthoughts.jackson.isolation.ProcessIsolatedRunner\", \\\n");
        dockerfile.append("    \"--framework=").append(frameworkName).append("\", \\\n");
        dockerfile.append("    \"--workspace=/workspace\"]\n");
        
        Files.write(dockerfilePath, dockerfile.toString().getBytes());
        return dockerfilePath;
    }

    private ContainerConfiguration createContainerConfiguration(
            String containerId, String imageName, Path workspace, Map<String, Object> params) {
        
        return new ContainerConfiguration(containerId, imageName, workspace, params);
    }

    private List<String> buildContainerCommand(ContainerConfiguration config) {
        List<String> command = new ArrayList<>();
        
        // Base command
        command.add(containerRuntime);
        command.add("run");
        command.add("--detach");
        command.add("--name");
        command.add(config.getContainerId());
        
        // Resource limits
        addResourceLimits(command);
        
        // Network configuration
        addNetworkConfiguration(command);
        
        // Volume mounts
        command.add("--volume");
        command.add(config.getWorkspace().toString() + ":/workspace");
        
        // Security settings
        if (!resourceConfig.isPrivileged()) {
            command.add("--security-opt");
            command.add("no-new-privileges");
            command.add("--cap-drop");
            command.add("ALL");
        }
        
        // Environment variables
        addEnvironmentVariables(command, config);
        
        // Image name
        command.add(config.getImageName());
        
        return command;
    }

    private void addResourceLimits(List<String> command) {
        // Memory limits
        command.add("--memory");
        command.add(resourceConfig.getMaxHeapSize());
        command.add("--memory-swap");
        command.add(resourceConfig.getMaxHeapSize()); // No swap
        
        // CPU limits
        if (resourceConfig.getCpuQuota() < 1.0) {
            command.add("--cpus");
            command.add(String.valueOf(resourceConfig.getCpuQuota()));
        } else {
            command.add("--cpu-shares");
            command.add(String.valueOf(resourceConfig.getCpuShares()));
        }
        
        // I/O limits (if supported by runtime)
        if ("docker".equals(containerRuntime)) {
            command.add("--device-read-bps");
            command.add("/dev/sda:" + resourceConfig.getDiskReadBytesPerSecond());
            command.add("--device-write-bps");
            command.add("/dev/sda:" + resourceConfig.getDiskWriteBytesPerSecond());
        }
        
        // Process limits
        command.add("--pids-limit");
        command.add(String.valueOf(resourceConfig.getMaxProcesses()));
        
        // File descriptor limits
        command.add("--ulimit");
        command.add("nofile=" + resourceConfig.getMaxFileDescriptors());
    }

    private void addNetworkConfiguration(List<String> command) {
        if ("none".equals(resourceConfig.getContainerNetwork())) {
            command.add("--network");
            command.add("none");
        } else {
            command.add("--network");
            command.add(resourceConfig.getContainerNetwork());
        }
    }

    private void addEnvironmentVariables(List<String> command, ContainerConfiguration config) {
        command.add("--env");
        command.add("FRAMEWORK_NAME=" + config.getFrameworkName());
        command.add("--env");
        command.add("CONTAINER_ID=" + config.getContainerId());
        command.add("--env");
        command.add("JAVA_OPTS=" + resourceConfig.toJvmArgsString());
    }

    private void waitForContainerReady(ContainerInfo containerInfo) throws IOException, InterruptedException {
        String containerId = containerInfo.getContainerId();
        int maxAttempts = 30;
        
        for (int i = 0; i < maxAttempts; i++) {
            if (isContainerRunning(containerId)) {
                return;
            }
            Thread.sleep(1000);
        }
        
        throw new RuntimeException("Container did not become ready within timeout: " + containerId);
    }

    private boolean isContainerRunning(String containerId) throws IOException, InterruptedException {
        List<String> command = Arrays.asList(
            containerRuntime, "inspect", containerId, "--format", "{{.State.Running}}"
        );
        
        ProcessBuilder pb = new ProcessBuilder(command);
        Process process = pb.start();
        
        boolean completed = process.waitFor(5, TimeUnit.SECONDS);
        if (!completed || process.exitValue() != 0) {
            return false;
        }
        
        String output = readProcessOutput(process.getInputStream()).trim();
        return "true".equals(output);
    }

    private String getContainerLogs(String containerId) throws IOException, InterruptedException {
        List<String> command = Arrays.asList(
            containerRuntime, "logs", containerId
        );
        
        ProcessBuilder pb = new ProcessBuilder(command);
        Process process = pb.start();
        
        boolean completed = process.waitFor(30, TimeUnit.SECONDS);
        if (!completed) {
            process.destroyForcibly();
            return "Log collection timed out";
        }
        
        if (process.exitValue() == 0) {
            return readProcessOutput(process.getInputStream());
        } else {
            return readProcessOutput(process.getErrorStream());
        }
    }

    private Object collectContainerResults(ContainerInfo containerInfo) {
        try {
            Path resultsFile = containerInfo.getWorkspace().resolve("output").resolve("results.json");
            if (Files.exists(resultsFile)) {
                return Files.readString(resultsFile);
            }
        } catch (IOException e) {
            logger.warn("Failed to collect results from container: {}", containerInfo.getContainerId(), e);
        }
        return null;
    }

    private void stopContainer(String containerId) throws IOException, InterruptedException {
        List<String> command = Arrays.asList(containerRuntime, "stop", containerId);
        
        ProcessBuilder pb = new ProcessBuilder(command);
        Process process = pb.start();
        
        process.waitFor(30, TimeUnit.SECONDS);
    }

    private void cleanupContainer(ContainerInfo containerInfo) {
        String containerId = containerInfo.getContainerId();
        
        try {
            // Stop container if still running
            stopContainer(containerId);
            
            // Remove container
            List<String> removeCommand = Arrays.asList(containerRuntime, "rm", containerId);
            ProcessBuilder pb = new ProcessBuilder(removeCommand);
            Process process = pb.start();
            process.waitFor(10, TimeUnit.SECONDS);
            
            logger.info("Cleaned up container: {}", containerId);
            
        } catch (Exception e) {
            logger.warn("Failed to cleanup container: {}", containerId, e);
        }
    }

    private void validateContainerRuntime() {
        try {
            List<String> command = Arrays.asList(containerRuntime, "--version");
            ProcessBuilder pb = new ProcessBuilder(command);
            Process process = pb.start();
            
            boolean completed = process.waitFor(10, TimeUnit.SECONDS);
            if (!completed || process.exitValue() != 0) {
                throw new RuntimeException("Container runtime not available: " + containerRuntime);
            }
            
            String version = readProcessOutput(process.getInputStream());
            logger.info("Using container runtime: {}", version.split("\n")[0]);
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to validate container runtime: " + containerRuntime, e);
        }
    }

    private String generateContainerId(String frameworkName) {
        return String.format("benchmark-%s-%d", 
            frameworkName.toLowerCase().replace("_", "-"), 
            System.currentTimeMillis());
    }

    private String serializeParameters(Map<String, Object> params) {
        // Simple JSON serialization
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

    private String readProcessOutput(java.io.InputStream inputStream) throws IOException {
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }
        return output.toString();
    }

    /**
     * Terminates all active containers
     */
    public void terminateAllContainers() {
        logger.info("Terminating all active containers: {}", activeContainers.size());
        
        activeContainers.values().parallelStream().forEach(containerInfo -> {
            try {
                cleanupContainer(containerInfo);
            } catch (Exception e) {
                logger.warn("Failed to cleanup container: {}", containerInfo.getContainerId(), e);
            }
        });
        
        activeContainers.clear();
    }

    // Data classes
    public static class ContainerizedBenchmarkResult {
        private final String containerId;
        private final String frameworkName;
        private final boolean success;
        private final int exitCode;
        private final String output;
        private final Object benchmarkResults;
        private final Duration executionTime;
        private final Instant timestamp;

        public ContainerizedBenchmarkResult(String containerId, String frameworkName, boolean success, 
                                          int exitCode, String output, Object benchmarkResults, 
                                          Duration executionTime) {
            this.containerId = containerId;
            this.frameworkName = frameworkName;
            this.success = success;
            this.exitCode = exitCode;
            this.output = output;
            this.benchmarkResults = benchmarkResults;
            this.executionTime = executionTime;
            this.timestamp = Instant.now();
        }

        // Getters
        public String getContainerId() { return containerId; }
        public String getFrameworkName() { return frameworkName; }
        public boolean isSuccess() { return success; }
        public int getExitCode() { return exitCode; }
        public String getOutput() { return output; }
        public Object getBenchmarkResults() { return benchmarkResults; }
        public Duration getExecutionTime() { return executionTime; }
        public Instant getTimestamp() { return timestamp; }
    }

    private static class ContainerConfiguration {
        private final String containerId;
        private final String imageName;
        private final Path workspace;
        private final Map<String, Object> parameters;
        private String frameworkName;

        public ContainerConfiguration(String containerId, String imageName, Path workspace, 
                                    Map<String, Object> parameters) {
            this.containerId = containerId;
            this.imageName = imageName;
            this.workspace = workspace;
            this.parameters = parameters;
        }

        // Getters
        public String getContainerId() { return containerId; }
        public String getImageName() { return imageName; }
        public Path getWorkspace() { return workspace; }
        public Map<String, Object> getParameters() { return parameters; }
        public String getFrameworkName() { return frameworkName; }
        public void setFrameworkName(String frameworkName) { this.frameworkName = frameworkName; }
    }

    public static class ContainerInfo {
        private final String containerId;
        private final String imageName;
        private final Path workspace;
        private final Instant startTime;
        private final String frameworkName;
        private List<ContainerResourceMetrics> resourceMetrics = new ArrayList<>();

        public ContainerInfo(String containerId, String imageName, Path workspace, 
                           Instant startTime, String frameworkName) {
            this.containerId = containerId;
            this.imageName = imageName;
            this.workspace = workspace;
            this.startTime = startTime;
            this.frameworkName = frameworkName;
        }

        public void addResourceMetrics(ContainerResourceMetrics metrics) {
            this.resourceMetrics.add(metrics);
        }

        // Getters and setters
        public String getContainerId() { return containerId; }
        public String getImageName() { return imageName; }
        public Path getWorkspace() { return workspace; }
        public Instant getStartTime() { return startTime; }
        public String getFrameworkName() { return frameworkName; }
        public List<ContainerResourceMetrics> getResourceMetrics() { return resourceMetrics; }
        public void setResourceMetrics(List<ContainerResourceMetrics> resourceMetrics) { 
            this.resourceMetrics = resourceMetrics; 
        }
    }

    private static class ContainerExecutionResult {
        private final boolean success;
        private final int exitCode;
        private final String output;

        public ContainerExecutionResult(boolean success, int exitCode, String output) {
            this.success = success;
            this.exitCode = exitCode;
            this.output = output;
        }

        public boolean isSuccess() { return success; }
        public int getExitCode() { return exitCode; }
        public String getOutput() { return output; }
    }

    public static class ContainerResourceMetrics {
        private final Instant timestamp;
        private final double cpuPercentage;
        private final String memoryUsage;
        private final double memoryPercentage;
        private final String networkIO;
        private final String blockIO;

        public ContainerResourceMetrics(Instant timestamp, double cpuPercentage, String memoryUsage, 
                                      double memoryPercentage, String networkIO, String blockIO) {
            this.timestamp = timestamp;
            this.cpuPercentage = cpuPercentage;
            this.memoryUsage = memoryUsage;
            this.memoryPercentage = memoryPercentage;
            this.networkIO = networkIO;
            this.blockIO = blockIO;
        }

        // Getters
        public Instant getTimestamp() { return timestamp; }
        public double getCpuPercentage() { return cpuPercentage; }
        public String getMemoryUsage() { return memoryUsage; }
        public double getMemoryPercentage() { return memoryPercentage; }
        public String getNetworkIO() { return networkIO; }
        public String getBlockIO() { return blockIO; }
    }
}