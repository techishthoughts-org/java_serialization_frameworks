package org.techishthoughts.payload.service;

import org.techishthoughts.payload.generator.HugePayloadGenerator.ComplexityLevel;

/**
 * Configuration for benchmark execution.
 * Encapsulates all parameters needed for running performance tests.
 */
public class BenchmarkConfig {

    private ComplexityLevel complexity = ComplexityLevel.MEDIUM;
    private int iterations = 100;
    private boolean enableWarmup = true;
    private int warmupIterations = 10;
    private boolean enableCompression = true;
    private boolean enableRoundtripTest = true;
    private boolean enableMemoryMonitoring = true;
    private long timeoutMs = 60000; // 1 minute

    public BenchmarkConfig() {}

    public BenchmarkConfig(ComplexityLevel complexity, int iterations) {
        this.complexity = complexity;
        this.iterations = iterations;
    }

    // Getters and setters
    public ComplexityLevel getComplexity() { return complexity; }
    public void setComplexity(ComplexityLevel complexity) { this.complexity = complexity; }

    public int getIterations() { return iterations; }
    public void setIterations(int iterations) { this.iterations = iterations; }

    public boolean isEnableWarmup() { return enableWarmup; }
    public void setEnableWarmup(boolean enableWarmup) { this.enableWarmup = enableWarmup; }

    public int getWarmupIterations() { return warmupIterations; }
    public void setWarmupIterations(int warmupIterations) { this.warmupIterations = warmupIterations; }

    public boolean isEnableCompression() { return enableCompression; }
    public void setEnableCompression(boolean enableCompression) { this.enableCompression = enableCompression; }

    public boolean isEnableRoundtripTest() { return enableRoundtripTest; }
    public void setEnableRoundtripTest(boolean enableRoundtripTest) { this.enableRoundtripTest = enableRoundtripTest; }

    public boolean isEnableMemoryMonitoring() { return enableMemoryMonitoring; }
    public void setEnableMemoryMonitoring(boolean enableMemoryMonitoring) { this.enableMemoryMonitoring = enableMemoryMonitoring; }

    public long getTimeoutMs() { return timeoutMs; }
    public void setTimeoutMs(long timeoutMs) { this.timeoutMs = timeoutMs; }

    // Builder pattern for easier configuration
    public static BenchmarkConfig builder() {
        return new BenchmarkConfig();
    }

    public BenchmarkConfig withComplexity(ComplexityLevel complexity) {
        this.complexity = complexity;
        return this;
    }

    public BenchmarkConfig withIterations(int iterations) {
        this.iterations = iterations;
        return this;
    }

    public BenchmarkConfig withWarmup(boolean enable, int iterations) {
        this.enableWarmup = enable;
        this.warmupIterations = iterations;
        return this;
    }

    public BenchmarkConfig withCompression(boolean enable) {
        this.enableCompression = enable;
        return this;
    }

    public BenchmarkConfig withRoundtripTest(boolean enable) {
        this.enableRoundtripTest = enable;
        return this;
    }

    public BenchmarkConfig withMemoryMonitoring(boolean enable) {
        this.enableMemoryMonitoring = enable;
        return this;
    }

    public BenchmarkConfig withTimeout(long timeoutMs) {
        this.timeoutMs = timeoutMs;
        return this;
    }

    @Override
    public String toString() {
        return "BenchmarkConfig{" +
                "complexity=" + complexity +
                ", iterations=" + iterations +
                ", enableWarmup=" + enableWarmup +
                ", warmupIterations=" + warmupIterations +
                ", enableCompression=" + enableCompression +
                ", enableRoundtripTest=" + enableRoundtripTest +
                ", enableMemoryMonitoring=" + enableMemoryMonitoring +
                ", timeoutMs=" + timeoutMs +
                '}';
    }
}