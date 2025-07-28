package org.techishthoughts.compression.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipParameters;
import org.springframework.stereotype.Service;

import com.aayushatharva.brotli4j.Brotli4jLoader;
import com.aayushatharva.brotli4j.decoder.Decoder;
import com.aayushatharva.brotli4j.encoder.Encoder;
import org.techishthoughts.compression.service.CompressionService.CompressionBenchmarkResult;
import org.techishthoughts.compression.service.CompressionService.CompressionResult;
import org.techishthoughts.compression.service.CompressionService.CrossFrameworkCompressionResult;
import org.techishthoughts.compression.service.CompressionService.PerformanceAnalysisResult;
import org.techishthoughts.payload.model.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.luben.zstd.Zstd;

@Service
public class CompressionService {

    private final ObjectMapper objectMapper;
    private final Map<String, byte[]> compressionCache = new ConcurrentHashMap<>();

    public CompressionService() {
        this.objectMapper = new ObjectMapper();
        // Load Brotli native library
        try {
            Brotli4jLoader.ensureAvailability();
        } catch (Exception e) {
            System.err.println("Warning: Brotli native library not available: " + e.getMessage());
        }
    }

    public CompressionResult compressWithGzip(byte[] data) throws IOException {
        return compressWithGzip(data, 6); // Default compression level
    }

    public CompressionResult compressWithGzip(byte[] data, int level) throws IOException {
        long startTime = System.nanoTime();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        GzipParameters params = new GzipParameters();
        params.setCompressionLevel(level);
        GzipCompressorOutputStream gzipOut = new GzipCompressorOutputStream(baos, params);

        gzipOut.write(data);
        gzipOut.close();

        byte[] compressed = baos.toByteArray();
        long compressionTime = System.nanoTime() - startTime;

        return new CompressionResult("Gzip", compressed, compressionTime, data.length, compressed.length, level);
    }

    public byte[] decompressGzip(byte[] compressedData) throws IOException {
        long startTime = System.nanoTime();

        ByteArrayInputStream bais = new ByteArrayInputStream(compressedData);
        GzipCompressorInputStream gzipIn = new GzipCompressorInputStream(bais);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int bytesRead;

        while ((bytesRead = gzipIn.read(buffer)) != -1) {
            baos.write(buffer, 0, bytesRead);
        }

        gzipIn.close();
        long decompressionTime = System.nanoTime() - startTime;

        System.out.println("Gzip decompression took: " + (decompressionTime / 1_000_000.0) + " ms");

        return baos.toByteArray();
    }

    public CompressionResult compressWithBrotli(byte[] data) throws IOException {
        return compressWithBrotli(data, 11); // Maximum compression level
    }

    public CompressionResult compressWithBrotli(byte[] data, int quality) throws IOException {
        long startTime = System.nanoTime();

        Encoder.Parameters params = new Encoder.Parameters();
        params.setQuality(quality);
        params.setMode(Encoder.Mode.GENERIC);

        byte[] compressed = Encoder.compress(data, params);
        long compressionTime = System.nanoTime() - startTime;

        return new CompressionResult("Brotli", compressed, compressionTime, data.length, compressed.length, quality);
    }

    public byte[] decompressBrotli(byte[] compressedData) throws IOException {
        long startTime = System.nanoTime();

        byte[] decompressed = Decoder.decompress(compressedData).getDecompressedData();
        long decompressionTime = System.nanoTime() - startTime;

        System.out.println("Brotli decompression took: " + (decompressionTime / 1_000_000.0) + " ms");

        return decompressed;
    }

    public CompressionResult compressWithZstd(byte[] data) {
        return compressWithZstd(data, 3); // Default compression level
    }

    public CompressionResult compressWithZstd(byte[] data, int level) {
        long startTime = System.nanoTime();

        byte[] compressed = Zstd.compress(data, level);
        long compressionTime = System.nanoTime() - startTime;

        return new CompressionResult("Zstandard", compressed, compressionTime, data.length, compressed.length, level);
    }

    public byte[] decompressZstd(byte[] compressedData) {
        long startTime = System.nanoTime();

        byte[] decompressed = Zstd.decompress(compressedData, (int) Zstd.decompressedSize(compressedData));
        long decompressionTime = System.nanoTime() - startTime;

        System.out.println("Zstandard decompression took: " + (decompressionTime / 1_000_000.0) + " ms");

        return decompressed;
    }

    public CompressionBenchmarkResult benchmarkAllAlgorithms(byte[] data) throws IOException {
        CompressionBenchmarkResult result = new CompressionBenchmarkResult();

        // Test Gzip at different levels
        result.addResult(compressWithGzip(data, 1)); // Fast
        result.addResult(compressWithGzip(data, 6)); // Balanced
        result.addResult(compressWithGzip(data, 9)); // Maximum

        // Test Brotli at different quality levels
        result.addResult(compressWithBrotli(data, 1));  // Fast
        result.addResult(compressWithBrotli(data, 6));  // Balanced
        result.addResult(compressWithBrotli(data, 11)); // Maximum

        // Test Zstandard at different levels
        result.addResult(compressWithZstd(data, 1));  // Fast
        result.addResult(compressWithZstd(data, 3));  // Balanced
        result.addResult(compressWithZstd(data, 19)); // Maximum

        return result;
    }

    public CrossFrameworkCompressionResult benchmarkAcrossFrameworks(List<User> users) throws IOException {
        CrossFrameworkCompressionResult result = new CrossFrameworkCompressionResult();

        // Serialize with different frameworks
        byte[] jsonData = objectMapper.writeValueAsBytes(users);
        result.addSerializationResult("JSON", jsonData);

        // Test compression on JSON
        result.addCompressionResult("JSON + Gzip", compressWithGzip(jsonData));
        result.addCompressionResult("JSON + Brotli", compressWithBrotli(jsonData));
        result.addCompressionResult("JSON + Zstd", compressWithZstd(jsonData));

        // Test compression on other formats (simulated)
        // In a real implementation, you'd serialize with each framework
        byte[] protobufData = simulateProtobufSerialization(users);
        result.addSerializationResult("Protobuf", protobufData);
        result.addCompressionResult("Protobuf + Gzip", compressWithGzip(protobufData));
        result.addCompressionResult("Protobuf + Zstd", compressWithZstd(protobufData));

        byte[] avroData = simulateAvroSerialization(users);
        result.addSerializationResult("Avro", avroData);
        result.addCompressionResult("Avro + Gzip", compressWithGzip(avroData));
        result.addCompressionResult("Avro + Zstd", compressWithZstd(avroData));

        return result;
    }

    public PerformanceAnalysisResult analyzePerformance(byte[] data, int iterations) throws IOException {
        PerformanceAnalysisResult result = new PerformanceAnalysisResult();

        // Warm up
        for (int i = 0; i < 10; i++) {
            compressWithGzip(data);
            compressWithBrotli(data);
            compressWithZstd(data);
        }

        // Benchmark compression
        long gzipTotalTime = 0;
        long brotliTotalTime = 0;
        long zstdTotalTime = 0;

        for (int i = 0; i < iterations; i++) {
            long start = System.nanoTime();
            compressWithGzip(data);
            gzipTotalTime += System.nanoTime() - start;

            start = System.nanoTime();
            compressWithBrotli(data);
            brotliTotalTime += System.nanoTime() - start;

            start = System.nanoTime();
            compressWithZstd(data);
            zstdTotalTime += System.nanoTime() - start;
        }

        result.setGzipAvgTime(gzipTotalTime / (double) iterations);
        result.setBrotliAvgTime(brotliTotalTime / (double) iterations);
        result.setZstdAvgTime(zstdTotalTime / (double) iterations);
        result.setIterations(iterations);

        return result;
    }

    private byte[] simulateProtobufSerialization(List<User> users) throws IOException {
        // Simulate Protobuf size (typically 40-60% of JSON size)
        byte[] jsonData = objectMapper.writeValueAsBytes(users);
        return java.util.Arrays.copyOf(jsonData, (int) (jsonData.length * 0.5));
    }

    private byte[] simulateAvroSerialization(List<User> users) throws IOException {
        // Simulate Avro size (typically 50-70% of JSON size)
        byte[] jsonData = objectMapper.writeValueAsBytes(users);
        return java.util.Arrays.copyOf(jsonData, (int) (jsonData.length * 0.6));
    }

    public static class CompressionResult {
        private final String algorithm;
        private final byte[] compressedData;
        private final long compressionTimeNs;
        private final int originalSize;
        private final int compressedSize;
        private final int level;

        public CompressionResult(String algorithm, byte[] compressedData, long compressionTimeNs,
                               int originalSize, int compressedSize, int level) {
            this.algorithm = algorithm;
            this.compressedData = compressedData;
            this.compressionTimeNs = compressionTimeNs;
            this.originalSize = originalSize;
            this.compressedSize = compressedSize;
            this.level = level;
        }

        public String getAlgorithm() { return algorithm; }
        public byte[] getCompressedData() { return compressedData; }
        public long getCompressionTimeNs() { return compressionTimeNs; }
        public double getCompressionTimeMs() { return compressionTimeNs / 1_000_000.0; }
        public int getOriginalSize() { return originalSize; }
        public int getCompressedSize() { return compressedSize; }
        public int getLevel() { return level; }
        public double getCompressionRatio() { return (double) compressedSize / originalSize; }
        public double getSpaceSavings() { return (1.0 - getCompressionRatio()) * 100.0; }
        public double getOriginalSizeMB() { return originalSize / (1024.0 * 1024.0); }
        public double getCompressedSizeMB() { return compressedSize / (1024.0 * 1024.0); }
    }

    public static class CompressionBenchmarkResult {
        private final java.util.List<CompressionResult> results = new java.util.ArrayList<>();

        public void addResult(CompressionResult result) {
            results.add(result);
        }

        public java.util.List<CompressionResult> getResults() {
            return results;
        }

        public CompressionResult getBestCompression() {
            return results.stream()
                .min((a, b) -> Double.compare(a.getCompressionRatio(), b.getCompressionRatio()))
                .orElse(null);
        }

        public CompressionResult getFastestCompression() {
            return results.stream()
                .min((a, b) -> Long.compare(a.getCompressionTimeNs(), b.getCompressionTimeNs()))
                .orElse(null);
        }
    }

    public static class CrossFrameworkCompressionResult {
        private final Map<String, byte[]> serializationResults = new java.util.HashMap<>();
        private final Map<String, CompressionResult> compressionResults = new java.util.HashMap<>();

        public void addSerializationResult(String format, byte[] data) {
            serializationResults.put(format, data);
        }

        public void addCompressionResult(String format, CompressionResult result) {
            compressionResults.put(format, result);
        }

        public Map<String, byte[]> getSerializationResults() { return serializationResults; }
        public Map<String, CompressionResult> getCompressionResults() { return compressionResults; }
    }

    public static class PerformanceAnalysisResult {
        private double gzipAvgTime;
        private double brotliAvgTime;
        private double zstdAvgTime;
        private int iterations;

        public double getGzipAvgTime() { return gzipAvgTime; }
        public void setGzipAvgTime(double gzipAvgTime) { this.gzipAvgTime = gzipAvgTime; }
        public double getBrotliAvgTime() { return brotliAvgTime; }
        public void setBrotliAvgTime(double brotliAvgTime) { this.brotliAvgTime = brotliAvgTime; }
        public double getZstdAvgTime() { return zstdAvgTime; }
        public void setZstdAvgTime(double zstdAvgTime) { this.zstdAvgTime = zstdAvgTime; }
        public int getIterations() { return iterations; }
        public void setIterations(int iterations) { this.iterations = iterations; }
    }
}
