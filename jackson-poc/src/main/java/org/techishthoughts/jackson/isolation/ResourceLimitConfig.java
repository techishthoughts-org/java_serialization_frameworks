package org.techishthoughts.jackson.isolation;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Configuration class for defining resource limits for isolated benchmark processes.
 * Provides comprehensive resource constraint settings for fair benchmarking.
 */
@Component
@ConfigurationProperties(prefix = "benchmark.isolation.resources")
public class ResourceLimitConfig {

    // Memory limits
    private String heapSize = "512m";
    private String maxHeapSize = "1g";
    private String metaspaceSize = "128m";
    private String maxMetaspaceSize = "256m";
    private String directMemorySize = "128m";

    // CPU limits
    private int cpuCores = 2;
    private double cpuQuota = 1.0; // CPU quota as fraction (1.0 = 100% of one core)
    private int cpuShares = 1024; // Relative CPU weight
    private String cpuAffinityMask = null; // CPU affinity mask (e.g., "0-1" for cores 0 and 1)

    // I/O limits
    private long diskReadBytesPerSecond = 100 * 1024 * 1024L; // 100 MB/s
    private long diskWriteBytesPerSecond = 100 * 1024 * 1024L; // 100 MB/s
    private int diskReadIopsLimit = 1000;
    private int diskWriteIopsLimit = 1000;

    // Network limits
    private long networkBandwidthBytesPerSecond = 100 * 1024 * 1024L; // 100 MB/s
    private int maxConnections = 100;

    // Process limits
    private int maxProcesses = 50;
    private int maxThreads = 200;
    private int maxFileDescriptors = 1024;

    // Time limits
    private Duration processTimeout = Duration.ofMinutes(10);
    private Duration warmupTimeout = Duration.ofMinutes(2);
    private Duration benchmarkTimeout = Duration.ofMinutes(5);
    private Duration cooldownTimeout = Duration.ofMinutes(1);

    // Container-specific limits
    private boolean useContainers = false;
    private String containerRuntime = "docker"; // "docker" or "podman"
    private String baseImage = "openjdk:17-jdk-slim";
    private String containerNetwork = "bridge";
    private boolean privileged = false;

    // JVM-specific settings
    private boolean useG1GC = true;
    private boolean useZGC = false;
    private boolean useSerialGC = false;
    private boolean useParallelGC = false;
    private String additionalJvmArgs = "-XX:+UseStringDeduplication -XX:+OptimizeStringConcat";

    // Monitoring and profiling
    private boolean enableJfrProfiling = false;
    private boolean enableGcLogging = true;
    private boolean enableJvmMetrics = true;
    private int metricsCollectionIntervalMs = 1000;

    // Resource enforcement strategy
    private ResourceEnforcementStrategy enforcementStrategy = ResourceEnforcementStrategy.SOFT_LIMITS;

    public enum ResourceEnforcementStrategy {
        SOFT_LIMITS,    // Log warnings when limits exceeded
        HARD_LIMITS,    // Kill process when limits exceeded
        ADAPTIVE        // Dynamically adjust limits based on system load
    }

    // Constructors
    public ResourceLimitConfig() {}

    public ResourceLimitConfig(String profile) {
        switch (profile.toLowerCase()) {
            case "light":
                configureLightProfile();
                break;
            case "standard":
                configureStandardProfile();
                break;
            case "heavy":
                configureHeavyProfile();
                break;
            case "container":
                configureContainerProfile();
                break;
            default:
                configureStandardProfile();
        }
    }

    private void configureLightProfile() {
        this.heapSize = "256m";
        this.maxHeapSize = "512m";
        this.cpuCores = 1;
        this.cpuQuota = 0.5;
        this.processTimeout = Duration.ofMinutes(5);
    }

    private void configureStandardProfile() {
        // Use default values (already set above)
    }

    private void configureHeavyProfile() {
        this.heapSize = "1g";
        this.maxHeapSize = "2g";
        this.cpuCores = 4;
        this.cpuQuota = 2.0;
        this.processTimeout = Duration.ofMinutes(20);
    }

    private void configureContainerProfile() {
        this.useContainers = true;
        this.heapSize = "512m";
        this.maxHeapSize = "1g";
        this.cpuQuota = 1.0;
        this.enforcementStrategy = ResourceEnforcementStrategy.HARD_LIMITS;
    }

    /**
     * Creates JVM arguments string from the current configuration
     */
    public String toJvmArgsString() {
        StringBuilder args = new StringBuilder();
        
        // Memory settings
        args.append(" -Xms").append(heapSize);
        args.append(" -Xmx").append(maxHeapSize);
        args.append(" -XX:MetaspaceSize=").append(metaspaceSize);
        args.append(" -XX:MaxMetaspaceSize=").append(maxMetaspaceSize);
        args.append(" -XX:MaxDirectMemorySize=").append(directMemorySize);

        // GC settings
        if (useG1GC) {
            args.append(" -XX:+UseG1GC");
        } else if (useZGC) {
            args.append(" -XX:+UseZGC");
        } else if (useSerialGC) {
            args.append(" -XX:+UseSerialGC");
        } else if (useParallelGC) {
            args.append(" -XX:+UseParallelGC");
        }

        // Monitoring settings
        if (enableGcLogging) {
            args.append(" -Xlog:gc*:gc.log:time");
        }
        
        if (enableJfrProfiling) {
            args.append(" -XX:+FlightRecorder");
            args.append(" -XX:StartFlightRecording=duration=")
                .append(benchmarkTimeout.getSeconds())
                .append("s,filename=benchmark.jfr");
        }

        // Additional JVM arguments
        if (additionalJvmArgs != null && !additionalJvmArgs.trim().isEmpty()) {
            args.append(" ").append(additionalJvmArgs);
        }

        return args.toString().trim();
    }

    /**
     * Validates the resource configuration
     */
    public void validate() throws IllegalArgumentException {
        if (cpuCores <= 0) {
            throw new IllegalArgumentException("CPU cores must be positive");
        }
        if (cpuQuota <= 0) {
            throw new IllegalArgumentException("CPU quota must be positive");
        }
        if (processTimeout.isNegative() || processTimeout.isZero()) {
            throw new IllegalArgumentException("Process timeout must be positive");
        }
        if (parseMemorySize(heapSize) > parseMemorySize(maxHeapSize)) {
            throw new IllegalArgumentException("Initial heap size cannot be larger than max heap size");
        }
        if (maxThreads <= 0) {
            throw new IllegalArgumentException("Max threads must be positive");
        }
    }

    private long parseMemorySize(String size) {
        if (size.endsWith("g") || size.endsWith("G")) {
            return Long.parseLong(size.substring(0, size.length() - 1)) * 1024 * 1024 * 1024;
        } else if (size.endsWith("m") || size.endsWith("M")) {
            return Long.parseLong(size.substring(0, size.length() - 1)) * 1024 * 1024;
        } else if (size.endsWith("k") || size.endsWith("K")) {
            return Long.parseLong(size.substring(0, size.length() - 1)) * 1024;
        } else {
            return Long.parseLong(size);
        }
    }

    // Getters and Setters
    public String getHeapSize() { return heapSize; }
    public void setHeapSize(String heapSize) { this.heapSize = heapSize; }

    public String getMaxHeapSize() { return maxHeapSize; }
    public void setMaxHeapSize(String maxHeapSize) { this.maxHeapSize = maxHeapSize; }

    public String getMetaspaceSize() { return metaspaceSize; }
    public void setMetaspaceSize(String metaspaceSize) { this.metaspaceSize = metaspaceSize; }

    public String getMaxMetaspaceSize() { return maxMetaspaceSize; }
    public void setMaxMetaspaceSize(String maxMetaspaceSize) { this.maxMetaspaceSize = maxMetaspaceSize; }

    public String getDirectMemorySize() { return directMemorySize; }
    public void setDirectMemorySize(String directMemorySize) { this.directMemorySize = directMemorySize; }

    public int getCpuCores() { return cpuCores; }
    public void setCpuCores(int cpuCores) { this.cpuCores = cpuCores; }

    public double getCpuQuota() { return cpuQuota; }
    public void setCpuQuota(double cpuQuota) { this.cpuQuota = cpuQuota; }

    public int getCpuShares() { return cpuShares; }
    public void setCpuShares(int cpuShares) { this.cpuShares = cpuShares; }

    public String getCpuAffinityMask() { return cpuAffinityMask; }
    public void setCpuAffinityMask(String cpuAffinityMask) { this.cpuAffinityMask = cpuAffinityMask; }

    public long getDiskReadBytesPerSecond() { return diskReadBytesPerSecond; }
    public void setDiskReadBytesPerSecond(long diskReadBytesPerSecond) { this.diskReadBytesPerSecond = diskReadBytesPerSecond; }

    public long getDiskWriteBytesPerSecond() { return diskWriteBytesPerSecond; }
    public void setDiskWriteBytesPerSecond(long diskWriteBytesPerSecond) { this.diskWriteBytesPerSecond = diskWriteBytesPerSecond; }

    public int getDiskReadIopsLimit() { return diskReadIopsLimit; }
    public void setDiskReadIopsLimit(int diskReadIopsLimit) { this.diskReadIopsLimit = diskReadIopsLimit; }

    public int getDiskWriteIopsLimit() { return diskWriteIopsLimit; }
    public void setDiskWriteIopsLimit(int diskWriteIopsLimit) { this.diskWriteIopsLimit = diskWriteIopsLimit; }

    public long getNetworkBandwidthBytesPerSecond() { return networkBandwidthBytesPerSecond; }
    public void setNetworkBandwidthBytesPerSecond(long networkBandwidthBytesPerSecond) { this.networkBandwidthBytesPerSecond = networkBandwidthBytesPerSecond; }

    public int getMaxConnections() { return maxConnections; }
    public void setMaxConnections(int maxConnections) { this.maxConnections = maxConnections; }

    public int getMaxProcesses() { return maxProcesses; }
    public void setMaxProcesses(int maxProcesses) { this.maxProcesses = maxProcesses; }

    public int getMaxThreads() { return maxThreads; }
    public void setMaxThreads(int maxThreads) { this.maxThreads = maxThreads; }

    public int getMaxFileDescriptors() { return maxFileDescriptors; }
    public void setMaxFileDescriptors(int maxFileDescriptors) { this.maxFileDescriptors = maxFileDescriptors; }

    public Duration getProcessTimeout() { return processTimeout; }
    public void setProcessTimeout(Duration processTimeout) { this.processTimeout = processTimeout; }

    public Duration getWarmupTimeout() { return warmupTimeout; }
    public void setWarmupTimeout(Duration warmupTimeout) { this.warmupTimeout = warmupTimeout; }

    public Duration getBenchmarkTimeout() { return benchmarkTimeout; }
    public void setBenchmarkTimeout(Duration benchmarkTimeout) { this.benchmarkTimeout = benchmarkTimeout; }

    public Duration getCooldownTimeout() { return cooldownTimeout; }
    public void setCooldownTimeout(Duration cooldownTimeout) { this.cooldownTimeout = cooldownTimeout; }

    public boolean isUseContainers() { return useContainers; }
    public void setUseContainers(boolean useContainers) { this.useContainers = useContainers; }

    public String getContainerRuntime() { return containerRuntime; }
    public void setContainerRuntime(String containerRuntime) { this.containerRuntime = containerRuntime; }

    public String getBaseImage() { return baseImage; }
    public void setBaseImage(String baseImage) { this.baseImage = baseImage; }

    public String getContainerNetwork() { return containerNetwork; }
    public void setContainerNetwork(String containerNetwork) { this.containerNetwork = containerNetwork; }

    public boolean isPrivileged() { return privileged; }
    public void setPrivileged(boolean privileged) { this.privileged = privileged; }

    public boolean isUseG1GC() { return useG1GC; }
    public void setUseG1GC(boolean useG1GC) { this.useG1GC = useG1GC; }

    public boolean isUseZGC() { return useZGC; }
    public void setUseZGC(boolean useZGC) { this.useZGC = useZGC; }

    public boolean isUseSerialGC() { return useSerialGC; }
    public void setUseSerialGC(boolean useSerialGC) { this.useSerialGC = useSerialGC; }

    public boolean isUseParallelGC() { return useParallelGC; }
    public void setUseParallelGC(boolean useParallelGC) { this.useParallelGC = useParallelGC; }

    public String getAdditionalJvmArgs() { return additionalJvmArgs; }
    public void setAdditionalJvmArgs(String additionalJvmArgs) { this.additionalJvmArgs = additionalJvmArgs; }

    public boolean isEnableJfrProfiling() { return enableJfrProfiling; }
    public void setEnableJfrProfiling(boolean enableJfrProfiling) { this.enableJfrProfiling = enableJfrProfiling; }

    public boolean isEnableGcLogging() { return enableGcLogging; }
    public void setEnableGcLogging(boolean enableGcLogging) { this.enableGcLogging = enableGcLogging; }

    public boolean isEnableJvmMetrics() { return enableJvmMetrics; }
    public void setEnableJvmMetrics(boolean enableJvmMetrics) { this.enableJvmMetrics = enableJvmMetrics; }

    public int getMetricsCollectionIntervalMs() { return metricsCollectionIntervalMs; }
    public void setMetricsCollectionIntervalMs(int metricsCollectionIntervalMs) { this.metricsCollectionIntervalMs = metricsCollectionIntervalMs; }

    public ResourceEnforcementStrategy getEnforcementStrategy() { return enforcementStrategy; }
    public void setEnforcementStrategy(ResourceEnforcementStrategy enforcementStrategy) { this.enforcementStrategy = enforcementStrategy; }
}