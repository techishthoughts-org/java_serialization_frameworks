package org.techishthoughts.payload.service.result;

import java.time.LocalDateTime;

/**
 * Standardized result object for compression operations.
 */
public class CompressionResult {

    private final String algorithm;
    private final byte[] compressedData;
    private final long compressionTimeNs;
    private final int originalSize;
    private final LocalDateTime timestamp;
    private final boolean success;
    private final String errorMessage;

    private CompressionResult(Builder builder) {
        this.algorithm = builder.algorithm;
        this.compressedData = builder.compressedData;
        this.compressionTimeNs = builder.compressionTimeNs;
        this.originalSize = builder.originalSize;
        this.timestamp = builder.timestamp;
        this.success = builder.success;
        this.errorMessage = builder.errorMessage;
    }

    // Getters
    public String getAlgorithm() { return algorithm; }
    public byte[] getCompressedData() { return compressedData; }
    public long getCompressionTimeNs() { return compressionTimeNs; }
    public double getCompressionTimeMs() { return compressionTimeNs / 1_000_000.0; }
    public int getOriginalSize() { return originalSize; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public boolean isSuccess() { return success; }
    public String getErrorMessage() { return errorMessage; }

    // Compression calculations
    public int getCompressedSize() { return compressedData != null ? compressedData.length : 0; }
    public double getCompressionRatio() {
        return originalSize > 0 ? (double) getCompressedSize() / originalSize : 0.0;
    }
    public double getSpaceSavings() { return 1.0 - getCompressionRatio(); }
    public double getSpaceSavingsPercent() { return getSpaceSavings() * 100.0; }

    // Size helpers
    public double getOriginalSizeMB() { return originalSize / (1024.0 * 1024.0); }
    public double getCompressedSizeMB() { return getCompressedSize() / (1024.0 * 1024.0); }

    // Performance calculations
    public double getBytesPerSecond() {
        if (compressionTimeNs <= 0) return 0;
        return (originalSize * 1_000_000_000.0) / compressionTimeNs;
    }

    // Builder pattern
    public static Builder builder(String algorithm) {
        return new Builder(algorithm);
    }

    public static class Builder {
        private final String algorithm;
        private byte[] compressedData;
        private long compressionTimeNs;
        private int originalSize;
        private LocalDateTime timestamp = LocalDateTime.now();
        private boolean success = true;
        private String errorMessage;

        public Builder(String algorithm) {
            this.algorithm = algorithm;
        }

        public Builder compressedData(byte[] data) {
            this.compressedData = data;
            return this;
        }

        public Builder compressionTime(long nanoseconds) {
            this.compressionTimeNs = nanoseconds;
            return this;
        }

        public Builder originalSize(int size) {
            this.originalSize = size;
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

        public CompressionResult build() {
            return new CompressionResult(this);
        }
    }

    @Override
    public String toString() {
        return String.format("CompressionResult{algorithm='%s', success=%b, ratio=%.2f%%, originalSize=%d, compressedSize=%d, time=%.2f ms}",
                algorithm, success, getSpaceSavingsPercent(), originalSize, getCompressedSize(), getCompressionTimeMs());
    }
}