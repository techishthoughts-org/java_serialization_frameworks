package org.techishthoughts.benchmark;

import com.aayushatharva.brotli4j.Brotli4jLoader;
import com.aayushatharva.brotli4j.Brotli4jLoader;
import com.aayushatharva.brotli4j.encoder.Encoder;
import com.github.luben.zstd.Zstd;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPOutputStream;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@Fork(value = 1, jvmArgs = {"-Xms2G", "-Xmx2G"})
@Warmup(iterations = 3, time = 2)
@Measurement(iterations = 5, time = 3)
public class CompressionBenchmark {

    private byte[] testData;
    private byte[] jsonData;
    private byte[] largeData;

    @Setup
    public void setup() {
        // Initialize Brotli
        Brotli4jLoader.ensureAvailability();

        // Create test data
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            sb.append("This is test data for compression benchmarking. ");
            sb.append("It contains repeated patterns to test compression efficiency. ");
            sb.append("Line ").append(i).append(" of test data.\n");
        }
        testData = sb.toString().getBytes(StandardCharsets.UTF_8);

        // JSON-like data
        StringBuilder jsonSb = new StringBuilder();
        jsonSb.append("[");
        for (int i = 0; i < 100; i++) {
            if (i > 0) jsonSb.append(",");
            jsonSb.append("{\"id\":").append(i).append(",\"name\":\"User").append(i).append("\",\"data\":\"");
            jsonSb.append("Some repeated data pattern for compression testing");
            jsonSb.append("\"}");
        }
        jsonSb.append("]");
        jsonData = jsonSb.toString().getBytes(StandardCharsets.UTF_8);

        // Large data for compression ratio testing
        StringBuilder largeSb = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            largeSb.append("Large dataset for compression benchmarking. ");
            largeSb.append("This contains many repeated patterns and structures. ");
            largeSb.append("Line ").append(i).append(" of large test data.\n");
        }
        largeData = largeSb.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Benchmark
    public void gzipCompress(Blackhole bh) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (GZIPOutputStream gzipOut = new GZIPOutputStream(baos)) {
            gzipOut.write(testData);
        }
        byte[] compressed = baos.toByteArray();
        bh.consume(compressed);
    }

    @Benchmark
    public void gzipDecompress(Blackhole bh) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (GZIPOutputStream gzipOut = new GZIPOutputStream(baos)) {
            gzipOut.write(testData);
        }
        byte[] compressed = baos.toByteArray();

        ByteArrayInputStream bais = new ByteArrayInputStream(compressed);
        try (java.util.zip.GZIPInputStream gzipIn = new java.util.zip.GZIPInputStream(bais)) {
            byte[] decompressed = gzipIn.readAllBytes();
            bh.consume(decompressed);
        }
    }

    // Brotli benchmarks disabled due to API issues
    /*
    @Benchmark
    public void brotliCompress(Blackhole bh) throws IOException {
        Encoder.Parameters params = new Encoder.Parameters().setQuality(11);
        byte[] compressed = com.aayushatharva.brotli4j.encoder.BrotliEncoder.compress(testData, params);
        bh.consume(compressed);
    }

    @Benchmark
    public void brotliDecompress(Blackhole bh) throws IOException {
        Encoder.Parameters params = new Encoder.Parameters().setQuality(11);
        byte[] compressed = com.aayushatharva.brotli4j.encoder.BrotliEncoder.compress(testData, params);

        byte[] decompressed = com.aayushatharva.brotli4j.decoder.BrotliDecoder.decompress(compressed);
        bh.consume(decompressed);
    }
    */

    @Benchmark
    public void zstdCompress(Blackhole bh) {
        byte[] compressed = Zstd.compress(testData);
        bh.consume(compressed);
    }

    @Benchmark
    public void zstdDecompress(Blackhole bh) {
        byte[] compressed = Zstd.compress(testData);
        byte[] decompressed = Zstd.decompress(compressed, testData.length);
        bh.consume(decompressed);
    }

    @Benchmark
    public void gzipJsonCompress(Blackhole bh) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (GZIPOutputStream gzipOut = new GZIPOutputStream(baos)) {
            gzipOut.write(jsonData);
        }
        byte[] compressed = baos.toByteArray();
        bh.consume(compressed);
    }

    // Brotli JSON compression disabled due to API issues
    /*
    @Benchmark
    public void brotliJsonCompress(Blackhole bh) throws IOException {
        Encoder.Parameters params = new Encoder.Parameters().setQuality(11);
        byte[] compressed = com.aayushatharva.brotli4j.encoder.BrotliEncoder.compress(jsonData, params);
        bh.consume(compressed);
    }
    */

    @Benchmark
    public void zstdJsonCompress(Blackhole bh) {
        byte[] compressed = Zstd.compress(jsonData);
        bh.consume(compressed);
    }

    @Benchmark
    public void gzipLargeCompress(Blackhole bh) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (GZIPOutputStream gzipOut = new GZIPOutputStream(baos)) {
            gzipOut.write(largeData);
        }
        byte[] compressed = baos.toByteArray();
        bh.consume(compressed);
    }

    // Brotli large compression disabled due to API issues
    /*
    @Benchmark
    public void brotliLargeCompress(Blackhole bh) throws IOException {
        Encoder.Parameters params = new Encoder.Parameters().setQuality(11);
        byte[] compressed = com.aayushatharva.brotli4j.encoder.BrotliEncoder.compress(largeData, params);
        bh.consume(compressed);
    }
    */

    @Benchmark
    public void zstdLargeCompress(Blackhole bh) {
        byte[] compressed = Zstd.compress(largeData);
        bh.consume(compressed);
    }
}
