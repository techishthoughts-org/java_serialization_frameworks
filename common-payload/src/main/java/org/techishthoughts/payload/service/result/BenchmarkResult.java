package org.techishthoughts.payload.service.result;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;

import org.techishthoughts.payload.service.BenchmarkConfig;

/**
 * Comprehensive benchmark result containing all test metrics.
 */
public class BenchmarkResult {

    private final String framework;
    private final BenchmarkConfig config;
    private final List<SerializationResult> serializationResults;
    private final List<CompressionResult> compressionResults;
    private final boolean roundtripSuccess;
    private final MemoryMetrics memoryMetrics;
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;
    private final boolean success;
    private final String errorMessage;

    private BenchmarkResult(Builder builder) {
        this.framework = builder.framework;
        this.config = builder.config;
        this.serializationResults = new ArrayList<>(builder.serializationResults);
        this.compressionResults = new ArrayList<>(builder.compressionResults);
        this.roundtripSuccess = builder.roundtripSuccess;
        this.memoryMetrics = builder.memoryMetrics;
        this.startTime = builder.startTime;
        this.endTime = builder.endTime;
        this.success = builder.success;
        this.errorMessage = builder.errorMessage;
    }

    // Getters
    public String getFramework() { return framework; }
    public BenchmarkConfig getConfig() { return config; }
    public List<SerializationResult> getSerializationResults() { return new ArrayList<>(serializationResults); }
    public List<CompressionResult> getCompressionResults() { return new ArrayList<>(compressionResults); }
    public boolean isRoundtripSuccess() { return roundtripSuccess; }
    public MemoryMetrics getMemoryMetrics() { return memoryMetrics; }
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public boolean isSuccess() { return success; }
    public String getErrorMessage() { return errorMessage; }

    // Aggregate calculations
    public OptionalDouble getAverageSerializationTimeMs() {
        return serializationResults.stream()
                .filter(SerializationResult::isSuccess)
                .mapToDouble(SerializationResult::getSerializationTimeMs)
                .average();
    }

    public OptionalDouble getAverageCompressionRatio() {
        return compressionResults.stream()
                .filter(CompressionResult::isSuccess)
                .mapToDouble(CompressionResult::getCompressionRatio)
                .average();
    }

    public OptionalDouble getAverageSerializedSizeBytes() {
        return serializationResults.stream()
                .filter(SerializationResult::isSuccess)
                .mapToDouble(SerializationResult::getSizeBytes)
                .average();
    }

    public int getSuccessfulSerializations() {
        return (int) serializationResults.stream().filter(SerializationResult::isSuccess).count();
    }

    public int getSuccessfulCompressions() {
        return (int) compressionResults.stream().filter(CompressionResult::isSuccess).count();
    }

    public double getSuccessRatePercent() {
        if (serializationResults.isEmpty()) return 0.0;
        return (getSuccessfulSerializations() * 100.0) / serializationResults.size();
    }

    public long getTotalDurationMs() {
        if (startTime == null || endTime == null) return 0;
        return java.time.Duration.between(startTime, endTime).toMillis();
    }

    // Builder pattern
    public static Builder builder(String framework, BenchmarkConfig config) {
        return new Builder(framework, config);
    }

    public static class Builder {
        private final String framework;
        private final BenchmarkConfig config;
        private final List<SerializationResult> serializationResults = new ArrayList<>();
        private final List<CompressionResult> compressionResults = new ArrayList<>();
        private boolean roundtripSuccess = false;
        private MemoryMetrics memoryMetrics;
        private LocalDateTime startTime = LocalDateTime.now();
        private LocalDateTime endTime;
        private boolean success = true;
        private String errorMessage;

        public Builder(String framework, BenchmarkConfig config) {
            this.framework = framework;
            this.config = config;
        }

        public Builder addSerializationResult(SerializationResult result) {
            this.serializationResults.add(result);
            return this;
        }

        public Builder addCompressionResult(CompressionResult result) {
            this.compressionResults.add(result);
            return this;
        }

        public Builder roundtripSuccess(boolean success) {
            this.roundtripSuccess = success;
            return this;
        }

        public Builder memoryMetrics(MemoryMetrics metrics) {
            this.memoryMetrics = metrics;
            return this;
        }

        public Builder startTime(LocalDateTime startTime) {
            this.startTime = startTime;
            return this;
        }

        public Builder endTime(LocalDateTime endTime) {
            this.endTime = endTime;
            return this;
        }

        public Builder success(boolean success) {
            this.success = success;
            return this;
        }

        public Builder error(String errorMessage) {
            this.success = false;
            this.errorMessage = errorMessage;
            return this;
        }

        public BenchmarkResult build() {
            if (endTime == null) {
                endTime = LocalDateTime.now();
            }
            return new BenchmarkResult(this);
        }
    }

    /**
     * Memory usage metrics during benchmark execution
     */
    public static class MemoryMetrics {
        private final long initialMemoryMb;
        private final long peakMemoryMb;
        private final long finalMemoryMb;
        private final long gcCount;
        private final long gcTimeMs;

        public MemoryMetrics(long initialMemoryMb, long peakMemoryMb, long finalMemoryMb, long gcCount, long gcTimeMs) {
            this.initialMemoryMb = initialMemoryMb;
            this.peakMemoryMb = peakMemoryMb;
            this.finalMemoryMb = finalMemoryMb;
            this.gcCount = gcCount;
            this.gcTimeMs = gcTimeMs;
        }

        public long getInitialMemoryMb() { return initialMemoryMb; }
        public long getPeakMemoryMb() { return peakMemoryMb; }
        public long getFinalMemoryMb() { return finalMemoryMb; }
        public long getGcCount() { return gcCount; }
        public long getGcTimeMs() { return gcTimeMs; }
        public long getMemoryDeltaMb() { return finalMemoryMb - initialMemoryMb; }
    }

    @Override
    public String toString() {
        return String.format("BenchmarkResult{framework='%s', success=%b, successRate=%.1f%%, " +
                        "avgSerializationTime=%.2f ms, duration=%d ms}",
                framework, success, getSuccessRatePercent(),
                getAverageSerializationTimeMs().orElse(0.0), getTotalDurationMs());
    }
}