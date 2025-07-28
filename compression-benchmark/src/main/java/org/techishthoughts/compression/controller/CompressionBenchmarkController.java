package org.techishthoughts.compression.controller;

import org.techishthoughts.compression.service.CompressionService;
import org.techishthoughts.payload.generator.PayloadGenerator;
import org.techishthoughts.payload.model.User;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/compression")
public class CompressionBenchmarkController {

    private final CompressionService compressionService;

    public CompressionBenchmarkController(CompressionService compressionService) {
        this.compressionService = compressionService;
    }

    @GetMapping("/benchmark")
    public Map<String, Object> runCompressionBenchmark(@RequestParam(defaultValue = "1000") int userCount) {
        Map<String, Object> results = new HashMap<>();

        try {
            System.out.println("Running compression benchmark with " + userCount + " users...");

            // Generate test data
            List<User> users = PayloadGenerator.generateMassiveDataset(userCount);

            // Serialize to JSON for compression testing
            byte[] jsonData = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsBytes(users);

            // Run comprehensive compression benchmark
            CompressionService.CompressionBenchmarkResult benchmarkResult =
                compressionService.benchmarkAllAlgorithms(jsonData);

            // Run cross-framework compression test
            CompressionService.CrossFrameworkCompressionResult crossFrameworkResult =
                compressionService.benchmarkAcrossFrameworks(users);

            // Compile results
            results.put("userCount", userCount);
            results.put("originalSizeMB", jsonData.length / (1024.0 * 1024.0));
            results.put("compressionResults", createCompressionResults(benchmarkResult));
            results.put("crossFrameworkResults", createCrossFrameworkResults(crossFrameworkResult));
            results.put("summary", createSummary(benchmarkResult, crossFrameworkResult));

        } catch (Exception e) {
            results.put("error", e.getMessage());
            e.printStackTrace();
        }

        return results;
    }

    @GetMapping("/algorithm-comparison")
    public Map<String, Object> compareAlgorithms(@RequestParam(defaultValue = "500") int userCount) {
        Map<String, Object> results = new HashMap<>();

                try {
            List<User> users = PayloadGenerator.generateMassiveDataset(userCount);
            byte[] jsonData = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsBytes(users);

            // Test each algorithm at optimal settings
            CompressionService.CompressionResult gzipResult = compressionService.compressWithGzip(jsonData, 6);
            CompressionService.CompressionResult brotliResult = compressionService.compressWithBrotli(jsonData, 11);
            CompressionService.CompressionResult zstdResult = compressionService.compressWithZstd(jsonData, 3);

            results.put("userCount", userCount);
            results.put("originalSizeMB", jsonData.length / (1024.0 * 1024.0));
            results.put("algorithms", Map.of(
                "gzip", createAlgorithmResult(gzipResult, "Universal compatibility, fallback"),
                "brotli", createAlgorithmResult(brotliResult, "Static, pre-compressed assets"),
                "zstd", createAlgorithmResult(zstdResult, "Dynamic, on-the-fly content")
            ));

        } catch (Exception e) {
            results.put("error", e.getMessage());
            e.printStackTrace();
        }

        return results;
    }

    @GetMapping("/performance-analysis")
    public Map<String, Object> analyzePerformance(
            @RequestParam(defaultValue = "1000") int userCount,
            @RequestParam(defaultValue = "1000") int iterations) {
        Map<String, Object> results = new HashMap<>();

                try {
            List<User> users = PayloadGenerator.generateMassiveDataset(userCount);
            byte[] jsonData = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsBytes(users);

            CompressionService.PerformanceAnalysisResult performanceResult =
                compressionService.analyzePerformance(jsonData, iterations);

            results.put("userCount", userCount);
            results.put("iterations", iterations);
            results.put("performance", Map.of(
                "gzipAvgTimeMs", performanceResult.getGzipAvgTime() / 1_000_000.0,
                "brotliAvgTimeMs", performanceResult.getBrotliAvgTime() / 1_000_000.0,
                "zstdAvgTimeMs", performanceResult.getZstdAvgTime() / 1_000_000.0,
                "gzipThroughput", iterations / (performanceResult.getGzipAvgTime() / 1_000_000_000.0),
                "brotliThroughput", iterations / (performanceResult.getBrotliAvgTime() / 1_000_000_000.0),
                "zstdThroughput", iterations / (performanceResult.getZstdAvgTime() / 1_000_000_000.0)
            ));

        } catch (Exception e) {
            results.put("error", e.getMessage());
            e.printStackTrace();
        }

        return results;
    }

    @GetMapping("/compression-levels")
    public Map<String, Object> testCompressionLevels(@RequestParam(defaultValue = "500") int userCount) {
        Map<String, Object> results = new HashMap<>();

                try {
            List<User> users = PayloadGenerator.generateMassiveDataset(userCount);
            byte[] jsonData = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsBytes(users);

            // Test different compression levels
            Map<String, Object> gzipLevels = new HashMap<>();
            gzipLevels.put("fast", createCompressionResult(compressionService.compressWithGzip(jsonData, 1)));
            gzipLevels.put("balanced", createCompressionResult(compressionService.compressWithGzip(jsonData, 6)));
            gzipLevels.put("maximum", createCompressionResult(compressionService.compressWithGzip(jsonData, 9)));

            Map<String, Object> brotliLevels = new HashMap<>();
            brotliLevels.put("fast", createCompressionResult(compressionService.compressWithBrotli(jsonData, 1)));
            brotliLevels.put("balanced", createCompressionResult(compressionService.compressWithBrotli(jsonData, 6)));
            brotliLevels.put("maximum", createCompressionResult(compressionService.compressWithBrotli(jsonData, 11)));

            Map<String, Object> zstdLevels = new HashMap<>();
            zstdLevels.put("fast", createCompressionResult(compressionService.compressWithZstd(jsonData, 1)));
            zstdLevels.put("balanced", createCompressionResult(compressionService.compressWithZstd(jsonData, 3)));
            zstdLevels.put("maximum", createCompressionResult(compressionService.compressWithZstd(jsonData, 19)));

            results.put("userCount", userCount);
            results.put("originalSizeMB", jsonData.length / (1024.0 * 1024.0));
            results.put("gzipLevels", gzipLevels);
            results.put("brotliLevels", brotliLevels);
            results.put("zstdLevels", zstdLevels);

        } catch (Exception e) {
            results.put("error", e.getMessage());
            e.printStackTrace();
        }

        return results;
    }

    @PostMapping("/custom-data")
    public Map<String, Object> benchmarkCustomData(@RequestBody List<User> users) {
        Map<String, Object> results = new HashMap<>();

        try {
            byte[] jsonData = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsBytes(users);
            CompressionService.CompressionBenchmarkResult benchmarkResult =
                compressionService.benchmarkAllAlgorithms(jsonData);

            results.put("userCount", users.size());
            results.put("originalSizeMB", jsonData.length / (1024.0 * 1024.0));
            results.put("compressionResults", createCompressionResults(benchmarkResult));

        } catch (Exception e) {
            results.put("error", e.getMessage());
            e.printStackTrace();
        }

        return results;
    }

    @GetMapping("/recommendations")
    public Map<String, Object> getCompressionRecommendations() {
        Map<String, Object> results = new HashMap<>();

        results.put("recommendations", Map.of(
            "dynamicContent", Map.of(
                "primary", "Zstandard (level 3)",
                "reason", "Best speed/ratio balance for on-the-fly compression",
                "fallback", "Gzip (level 6)",
                "fallbackReason", "Universal compatibility"
            ),
            "staticContent", Map.of(
                "primary", "Brotli (level 11)",
                "reason", "Maximum compression for pre-compressed assets",
                "fallback", "Zstandard (level 19)",
                "fallbackReason", "Excellent compression with good speed"
            ),
            "apiResponses", Map.of(
                "primary", "Zstandard (level 1-3)",
                "reason", "Fast compression for real-time responses",
                "fallback", "Gzip (level 6)",
                "fallbackReason", "Wide browser support"
            ),
            "dataStorage", Map.of(
                "primary", "Zstandard (level 3-6)",
                "reason", "Good balance for stored data",
                "fallback", "Gzip (level 9)",
                "fallbackReason", "Maximum compression when speed not critical"
            )
        ));

        results.put("algorithmComparison", Map.of(
            "gzip", Map.of(
                "compressionRatio", "Good",
                "compressionSpeed", "Fast",
                "decompressionSpeed", "Fast",
                "cpuUsage", "Low",
                "bestUseCase", "Universal compatibility, fallback"
            ),
            "brotli", Map.of(
                "compressionRatio", "Excellent",
                "compressionSpeed", "Slow (at high levels)",
                "decompressionSpeed", "Very Fast",
                "cpuUsage", "High (at high levels)",
                "bestUseCase", "Static, pre-compressed assets"
            ),
            "zstd", Map.of(
                "compressionRatio", "Very Good",
                "compressionSpeed", "Very Fast",
                "decompressionSpeed", "Very Fast",
                "cpuUsage", "Low-Medium",
                "bestUseCase", "Dynamic, on-the-fly content"
            )
        ));

        return results;
    }

    private Map<String, Object> createCompressionResults(CompressionService.CompressionBenchmarkResult benchmarkResult) {
        return Map.of(
            "allResults", benchmarkResult.getResults().stream()
                .map(this::createCompressionResult)
                .collect(Collectors.toList()),
            "bestCompression", createCompressionResult(benchmarkResult.getBestCompression()),
            "fastestCompression", createCompressionResult(benchmarkResult.getFastestCompression())
        );
    }

    private Map<String, Object> createCrossFrameworkResults(CompressionService.CrossFrameworkCompressionResult result) {
        Map<String, Object> serializationResults = new HashMap<>();
        result.getSerializationResults().forEach((format, data) ->
            serializationResults.put(format, Map.of(
                "sizeBytes", data.length,
                "sizeMB", data.length / (1024.0 * 1024.0)
            ))
        );

        Map<String, Object> compressionResults = new HashMap<>();
        result.getCompressionResults().forEach((format, compResult) ->
            compressionResults.put(format, createCompressionResult(compResult))
        );

        return Map.of(
            "serialization", serializationResults,
            "compression", compressionResults
        );
    }

    private Map<String, Object> createSummary(CompressionService.CompressionBenchmarkResult benchmarkResult,
                                             CompressionService.CrossFrameworkCompressionResult crossFrameworkResult) {
        return Map.of(
            "bestCompressionRatio", benchmarkResult.getBestCompression().getCompressionRatio(),
            "fastestCompressionTime", benchmarkResult.getFastestCompression().getCompressionTimeMs(),
            "averageCompressionRatio", benchmarkResult.getResults().stream()
                .mapToDouble(CompressionService.CompressionResult::getCompressionRatio)
                .average()
                .orElse(0.0),
            "totalSpaceSavings", benchmarkResult.getResults().stream()
                .mapToDouble(CompressionService.CompressionResult::getSpaceSavings)
                .average()
                .orElse(0.0)
        );
    }

    private Map<String, Object> createCompressionResult(CompressionService.CompressionResult result) {
        return Map.of(
            "algorithm", result.getAlgorithm(),
            "level", result.getLevel(),
            "compressionTimeMs", result.getCompressionTimeMs(),
            "originalSizeBytes", result.getOriginalSize(),
            "originalSizeMB", result.getOriginalSizeMB(),
            "compressedSizeBytes", result.getCompressedSize(),
            "compressedSizeMB", result.getCompressedSizeMB(),
            "compressionRatio", result.getCompressionRatio(),
            "spaceSavings", result.getSpaceSavings()
        );
    }

    private Map<String, Object> createAlgorithmResult(CompressionService.CompressionResult result, String bestUseCase) {
        Map<String, Object> baseResult = createCompressionResult(result);
        baseResult.put("bestUseCase", bestUseCase);
        return baseResult;
    }
}
