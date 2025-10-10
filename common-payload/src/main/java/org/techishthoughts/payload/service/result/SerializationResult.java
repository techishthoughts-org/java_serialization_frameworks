package org.techishthoughts.payload.service.result;

import java.time.LocalDateTime;

/**
 * Standardized result object for serialization operations.
 * Provides consistent metrics across all frameworks.
 */
public class SerializationResult {

    private final String framework;
    private final String format;
    private final byte[] data;
    private final long serializationTimeNs;
    private final int inputObjectCount;
    private final LocalDateTime timestamp;
    private final boolean success;
    private final String errorMessage;

    private SerializationResult(Builder builder) {
        this.framework = builder.framework;
        this.format = builder.format;
        this.data = builder.data;
        this.serializationTimeNs = builder.serializationTimeNs;
        this.inputObjectCount = builder.inputObjectCount;
        this.timestamp = builder.timestamp;
        this.success = builder.success;
        this.errorMessage = builder.errorMessage;
    }

    // Getters
    public String getFramework() { return framework; }
    public String getFormat() { return format; }
    public byte[] getData() { return data; }
    public long getSerializationTimeNs() { return serializationTimeNs; }
    public double getSerializationTimeMs() { return serializationTimeNs / 1_000_000.0; }
    public int getInputObjectCount() { return inputObjectCount; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public boolean isSuccess() { return success; }
    public String getErrorMessage() { return errorMessage; }

    // Size calculations
    public int getSizeBytes() { return data != null ? data.length : 0; }
    public double getSizeKB() { return getSizeBytes() / 1024.0; }
    public double getSizeMB() { return getSizeBytes() / (1024.0 * 1024.0); }

    // Performance calculations
    public double getBytesPerSecond() {
        if (serializationTimeNs <= 0) return 0;
        return (getSizeBytes() * 1_000_000_000.0) / serializationTimeNs;
    }

    public double getObjectsPerSecond() {
        if (serializationTimeNs <= 0) return 0;
        return (inputObjectCount * 1_000_000_000.0) / serializationTimeNs;
    }

    // Builder pattern
    public static Builder builder(String framework) {
        return new Builder(framework);
    }

    public static class Builder {
        private final String framework;
        private String format;
        private byte[] data;
        private long serializationTimeNs;
        private int inputObjectCount;
        private LocalDateTime timestamp = LocalDateTime.now();
        private boolean success = true;
        private String errorMessage;

        public Builder(String framework) {
            this.framework = framework;
        }

        public Builder format(String format) {
            this.format = format;
            return this;
        }

        public Builder data(byte[] data) {
            this.data = data;
            return this;
        }

        public Builder serializationTime(long nanoseconds) {
            this.serializationTimeNs = nanoseconds;
            return this;
        }

        public Builder inputObjectCount(int count) {
            this.inputObjectCount = count;
            return this;
        }

        public Builder timestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
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

        public SerializationResult build() {
            return new SerializationResult(this);
        }
    }

    @Override
    public String toString() {
        return String.format("SerializationResult{framework='%s', format='%s', success=%b, size=%d bytes, time=%.2f ms}",
                framework, format, success, getSizeBytes(), getSerializationTimeMs());
    }
}