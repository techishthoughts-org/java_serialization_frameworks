package org.techishthoughts.jackson.monitoring;

/**
 * Configuration class for resource monitoring settings.
 */
public class MonitoringConfiguration {
    
    private final long samplingIntervalMs;
    private final boolean enableCpuMonitoring;
    private final boolean enableMemoryMonitoring;
    private final boolean enableIoMonitoring;
    private final boolean enableGcProfiling;
    private final boolean enableJfrProfiling;
    private final boolean enableCustomMetrics;
    private final int metricsHistorySize;
    private final double cpuThreshold;
    private final double memoryThreshold;
    
    private MonitoringConfiguration(Builder builder) {
        this.samplingIntervalMs = builder.samplingIntervalMs;
        this.enableCpuMonitoring = builder.enableCpuMonitoring;
        this.enableMemoryMonitoring = builder.enableMemoryMonitoring;
        this.enableIoMonitoring = builder.enableIoMonitoring;
        this.enableGcProfiling = builder.enableGcProfiling;
        this.enableJfrProfiling = builder.enableJfrProfiling;
        this.enableCustomMetrics = builder.enableCustomMetrics;
        this.metricsHistorySize = builder.metricsHistorySize;
        this.cpuThreshold = builder.cpuThreshold;
        this.memoryThreshold = builder.memoryThreshold;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static MonitoringConfiguration defaultConfig() {
        return builder().build();
    }
    
    // Getters
    public long getSamplingIntervalMs() { return samplingIntervalMs; }
    public boolean isEnableCpuMonitoring() { return enableCpuMonitoring; }
    public boolean isEnableMemoryMonitoring() { return enableMemoryMonitoring; }
    public boolean isEnableIoMonitoring() { return enableIoMonitoring; }
    public boolean isEnableGcProfiling() { return enableGcProfiling; }
    public boolean isEnableJfrProfiling() { return enableJfrProfiling; }
    public boolean isEnableCustomMetrics() { return enableCustomMetrics; }
    public int getMetricsHistorySize() { return metricsHistorySize; }
    public double getCpuThreshold() { return cpuThreshold; }
    public double getMemoryThreshold() { return memoryThreshold; }
    
    public static class Builder {
        private long samplingIntervalMs = 1000; // 1 second default
        private boolean enableCpuMonitoring = true;
        private boolean enableMemoryMonitoring = true;
        private boolean enableIoMonitoring = true;
        private boolean enableGcProfiling = true;
        private boolean enableJfrProfiling = false; // Disabled by default (requires JFR)
        private boolean enableCustomMetrics = true;
        private int metricsHistorySize = 1000;
        private double cpuThreshold = 0.8; // 80% CPU threshold
        private double memoryThreshold = 0.85; // 85% memory threshold
        
        public Builder samplingInterval(long intervalMs) {
            this.samplingIntervalMs = intervalMs;
            return this;
        }
        
        public Builder enableCpuMonitoring(boolean enable) {
            this.enableCpuMonitoring = enable;
            return this;
        }
        
        public Builder enableMemoryMonitoring(boolean enable) {
            this.enableMemoryMonitoring = enable;
            return this;
        }
        
        public Builder enableIoMonitoring(boolean enable) {
            this.enableIoMonitoring = enable;
            return this;
        }
        
        public Builder enableGcProfiling(boolean enable) {
            this.enableGcProfiling = enable;
            return this;
        }
        
        public Builder enableJfrProfiling(boolean enable) {
            this.enableJfrProfiling = enable;
            return this;
        }
        
        public Builder enableCustomMetrics(boolean enable) {
            this.enableCustomMetrics = enable;
            return this;
        }
        
        public Builder metricsHistorySize(int size) {
            this.metricsHistorySize = size;
            return this;
        }
        
        public Builder cpuThreshold(double threshold) {
            this.cpuThreshold = threshold;
            return this;
        }
        
        public Builder memoryThreshold(double threshold) {
            this.memoryThreshold = threshold;
            return this;
        }
        
        public MonitoringConfiguration build() {
            return new MonitoringConfiguration(this);
        }
    }
}