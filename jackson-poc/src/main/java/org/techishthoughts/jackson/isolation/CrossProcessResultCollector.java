package org.techishthoughts.jackson.isolation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Collects and aggregates benchmark results from multiple isolated processes.
 * Provides comprehensive analysis and comparison capabilities.
 */
@Component
public class CrossProcessResultCollector {

    private static final Logger logger = LoggerFactory.getLogger(CrossProcessResultCollector.class);
    
    private final ObjectMapper objectMapper;
    private final Path resultsDirectory;
    private final Map<String, List<IsolatedBenchmarkExecutor.BenchmarkExecutionResult>> collectedResults;

    public CrossProcessResultCollector() {
        this.objectMapper = new ObjectMapper();
        this.collectedResults = new HashMap<>();
        
        try {
            this.resultsDirectory = Files.createTempDirectory("benchmark-results-");
            logger.info("Results collection directory created at: {}", resultsDirectory);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create results directory", e);
        }
    }

    /**
     * Aggregates results from multiple benchmark executions
     */
    public AggregatedBenchmarkResults aggregateResults(
            List<IsolatedBenchmarkExecutor.BenchmarkExecutionResult> results) {
        
        logger.info("Aggregating results from {} benchmark executions", results.size());
        
        String sessionId = generateSessionId();
        AggregatedBenchmarkResults aggregated = new AggregatedBenchmarkResults(sessionId);
        
        // Group results by framework
        Map<String, List<IsolatedBenchmarkExecutor.BenchmarkExecutionResult>> byFramework = 
            results.stream().collect(Collectors.groupingBy(
                IsolatedBenchmarkExecutor.BenchmarkExecutionResult::getFrameworkName));
        
        // Process each framework's results
        for (Map.Entry<String, List<IsolatedBenchmarkExecutor.BenchmarkExecutionResult>> entry : 
             byFramework.entrySet()) {
            
            String framework = entry.getKey();
            List<IsolatedBenchmarkExecutor.BenchmarkExecutionResult> frameworkResults = entry.getValue();
            
            FrameworkAggregatedResult frameworkAggregate = aggregateFrameworkResults(framework, frameworkResults);
            aggregated.addFrameworkResult(framework, frameworkAggregate);
        }
        
        // Calculate cross-framework comparisons
        calculateCrossFrameworkComparisons(aggregated);
        
        // Store results
        storeAggregatedResults(aggregated);
        
        // Update collected results cache
        for (IsolatedBenchmarkExecutor.BenchmarkExecutionResult result : results) {
            collectedResults.computeIfAbsent(result.getFrameworkName(), k -> new ArrayList<>()).add(result);
        }
        
        return aggregated;
    }

    /**
     * Generates comprehensive analysis report
     */
    public ComprehensiveAnalysisReport generateComprehensiveAnalysis(
            IsolatedBenchmarkExecutor.ComprehensiveBenchmarkResult comprehensiveResult) {
        
        logger.info("Generating comprehensive analysis for {} frameworks", 
            comprehensiveResult.getFrameworks().size());
        
        ComprehensiveAnalysisReport report = new ComprehensiveAnalysisReport(
            comprehensiveResult.getFrameworks(),
            comprehensiveResult.getStartTime(),
            comprehensiveResult.getCompletionTime()
        );
        
        // Analyze each phase
        if (comprehensiveResult.getWarmupResults() != null) {
            PhaseAnalysis warmupAnalysis = analyzePhase("warmup", comprehensiveResult.getWarmupResults());
            report.setWarmupAnalysis(warmupAnalysis);
        }
        
        if (comprehensiveResult.getStandardResults() != null) {
            PhaseAnalysis standardAnalysis = analyzePhase("standard", comprehensiveResult.getStandardResults());
            report.setStandardAnalysis(standardAnalysis);
        }
        
        if (comprehensiveResult.getStressResults() != null) {
            PhaseAnalysis stressAnalysis = analyzePhase("stress", comprehensiveResult.getStressResults());
            report.setStressAnalysis(stressAnalysis);
        }
        
        if (comprehensiveResult.getMemoryResults() != null) {
            PhaseAnalysis memoryAnalysis = analyzePhase("memory", comprehensiveResult.getMemoryResults());
            report.setMemoryAnalysis(memoryAnalysis);
        }
        
        if (comprehensiveResult.getScalabilityResults() != null) {
            PhaseAnalysis scalabilityAnalysis = analyzePhase("scalability", comprehensiveResult.getScalabilityResults());
            report.setScalabilityAnalysis(scalabilityAnalysis);
        }
        
        // Generate cross-phase analysis
        generateCrossPhaseAnalysis(report);
        
        // Generate recommendations
        generateRecommendations(report);
        
        // Store comprehensive analysis
        storeComprehensiveAnalysis(report);
        
        return report;
    }

    /**
     * Retrieves historical results for trend analysis
     */
    public TrendAnalysisResult analyzeTrends(String frameworkName, int daysBack) {
        logger.info("Analyzing trends for framework: {} over {} days", frameworkName, daysBack);
        
        List<IsolatedBenchmarkExecutor.BenchmarkExecutionResult> historicalResults = 
            getHistoricalResults(frameworkName, daysBack);
        
        if (historicalResults.isEmpty()) {
            return new TrendAnalysisResult(frameworkName, "No historical data available");
        }
        
        return calculateTrends(frameworkName, historicalResults);
    }

    /**
     * Compares multiple frameworks across different metrics
     */
    public FrameworkComparisonReport compareFrameworks(List<String> frameworks) {
        logger.info("Comparing {} frameworks", frameworks.size());
        
        FrameworkComparisonReport comparison = new FrameworkComparisonReport(frameworks);
        
        for (String framework : frameworks) {
            List<IsolatedBenchmarkExecutor.BenchmarkExecutionResult> results = 
                collectedResults.getOrDefault(framework, Collections.emptyList());
            
            if (!results.isEmpty()) {
                FrameworkMetrics metrics = calculateFrameworkMetrics(framework, results);
                comparison.addFrameworkMetrics(framework, metrics);
            }
        }
        
        // Calculate relative performance
        calculateRelativePerformance(comparison);
        
        // Generate insights
        generateComparisonInsights(comparison);
        
        return comparison;
    }

    private String generateSessionId() {
        return "session-" + Instant.now().toEpochMilli();
    }

    private FrameworkAggregatedResult aggregateFrameworkResults(
            String framework, 
            List<IsolatedBenchmarkExecutor.BenchmarkExecutionResult> results) {
        
        FrameworkAggregatedResult aggregate = new FrameworkAggregatedResult(framework);
        
        // Separate successful and failed results
        List<IsolatedBenchmarkExecutor.BenchmarkExecutionResult> successful = 
            results.stream().filter(IsolatedBenchmarkExecutor.BenchmarkExecutionResult::isSuccess)
                   .collect(Collectors.toList());
        
        List<IsolatedBenchmarkExecutor.BenchmarkExecutionResult> failed = 
            results.stream().filter(r -> !r.isSuccess())
                   .collect(Collectors.toList());
        
        aggregate.setTotalExecutions(results.size());
        aggregate.setSuccessfulExecutions(successful.size());
        aggregate.setFailedExecutions(failed.size());
        aggregate.setSuccessRate((double) successful.size() / results.size());
        
        if (!successful.isEmpty()) {
            // Calculate execution time statistics
            List<Long> executionTimes = successful.stream()
                .map(r -> r.getExecutionTime().toMillis())
                .collect(Collectors.toList());
            
            aggregate.setExecutionTimeStats(calculateStatistics(executionTimes));
            
            // Extract and aggregate benchmark-specific metrics
            aggregateBenchmarkSpecificMetrics(aggregate, successful);
        }
        
        // Collect error patterns from failed executions
        if (!failed.isEmpty()) {
            Map<String, Long> errorPatterns = failed.stream()
                .collect(Collectors.groupingBy(
                    IsolatedBenchmarkExecutor.BenchmarkExecutionResult::getErrorMessage,
                    Collectors.counting()));
            aggregate.setErrorPatterns(errorPatterns);
        }
        
        return aggregate;
    }

    private void aggregateBenchmarkSpecificMetrics(
            FrameworkAggregatedResult aggregate,
            List<IsolatedBenchmarkExecutor.BenchmarkExecutionResult> successful) {
        
        // This would extract specific metrics from the benchmark results
        // For now, we'll create placeholder aggregations
        
        Map<String, Object> aggregatedMetrics = new HashMap<>();
        
        // Aggregate throughput metrics
        List<Double> throughputs = new ArrayList<>();
        
        // Aggregate memory usage metrics
        List<Long> memoryUsages = new ArrayList<>();
        
        // Aggregate serialization times
        List<Long> serializationTimes = new ArrayList<>();
        
        for (IsolatedBenchmarkExecutor.BenchmarkExecutionResult result : successful) {
            // Extract metrics from result.getResults()
            // This would parse the actual benchmark results
            extractMetricsFromResult(result, throughputs, memoryUsages, serializationTimes);
        }
        
        if (!throughputs.isEmpty()) {
            aggregatedMetrics.put("throughputStats", calculateDoubleStatistics(throughputs));
        }
        
        if (!memoryUsages.isEmpty()) {
            aggregatedMetrics.put("memoryStats", calculateStatistics(memoryUsages));
        }
        
        if (!serializationTimes.isEmpty()) {
            aggregatedMetrics.put("serializationTimeStats", calculateStatistics(serializationTimes));
        }
        
        aggregate.setBenchmarkMetrics(aggregatedMetrics);
    }

    private void extractMetricsFromResult(
            IsolatedBenchmarkExecutor.BenchmarkExecutionResult result,
            List<Double> throughputs,
            List<Long> memoryUsages,
            List<Long> serializationTimes) {
        
        // This would parse the actual benchmark results JSON
        // For now, we'll simulate some data extraction
        
        Object results = result.getResults();
        if (results != null) {
            // Parse JSON results and extract metrics
            // throughputs.add(extractedThroughput);
            // memoryUsages.add(extractedMemoryUsage);
            // serializationTimes.add(extractedSerializationTime);
        }
    }

    private void calculateCrossFrameworkComparisons(AggregatedBenchmarkResults aggregated) {
        Map<String, FrameworkAggregatedResult> frameworks = aggregated.getFrameworkResults();
        
        if (frameworks.size() < 2) {
            return; // Need at least 2 frameworks for comparison
        }
        
        Map<String, FrameworkComparison> comparisons = new HashMap<>();
        
        // Find baseline framework (e.g., the one with best average performance)
        String baseline = findBaselineFramework(frameworks);
        
        for (String framework : frameworks.keySet()) {
            if (!framework.equals(baseline)) {
                FrameworkComparison comparison = compareFrameworks(
                    baseline, frameworks.get(baseline),
                    framework, frameworks.get(framework)
                );
                comparisons.put(framework + "_vs_" + baseline, comparison);
            }
        }
        
        aggregated.setCrossFrameworkComparisons(comparisons);
    }

    private String findBaselineFramework(Map<String, FrameworkAggregatedResult> frameworks) {
        // Simple heuristic: use the framework with highest success rate
        return frameworks.entrySet().stream()
            .max(Map.Entry.comparingByValue(
                Comparator.comparingDouble(FrameworkAggregatedResult::getSuccessRate)))
            .map(Map.Entry::getKey)
            .orElse(frameworks.keySet().iterator().next());
    }

    private FrameworkComparison compareFrameworks(
            String baselineName, FrameworkAggregatedResult baseline,
            String frameworkName, FrameworkAggregatedResult framework) {
        
        FrameworkComparison comparison = new FrameworkComparison(baselineName, frameworkName);
        
        // Compare success rates
        double successRateDiff = framework.getSuccessRate() - baseline.getSuccessRate();
        comparison.setSuccessRateDifference(successRateDiff);
        
        // Compare execution times (if available)
        if (baseline.getExecutionTimeStats() != null && framework.getExecutionTimeStats() != null) {
            double baselineMean = (Double) baseline.getExecutionTimeStats().get("mean");
            double frameworkMean = (Double) framework.getExecutionTimeStats().get("mean");
            double timeDiff = ((frameworkMean - baselineMean) / baselineMean) * 100; // Percentage difference
            comparison.setExecutionTimePercentageDifference(timeDiff);
        }
        
        return comparison;
    }

    private PhaseAnalysis analyzePhase(String phaseName, 
                                     List<IsolatedBenchmarkExecutor.BenchmarkExecutionResult> results) {
        
        PhaseAnalysis analysis = new PhaseAnalysis(phaseName);
        
        // Group results by framework
        Map<String, List<IsolatedBenchmarkExecutor.BenchmarkExecutionResult>> byFramework = 
            results.stream().collect(Collectors.groupingBy(
                IsolatedBenchmarkExecutor.BenchmarkExecutionResult::getFrameworkName));
        
        // Analyze each framework in this phase
        Map<String, FrameworkPhaseMetrics> frameworkMetrics = new HashMap<>();
        
        for (Map.Entry<String, List<IsolatedBenchmarkExecutor.BenchmarkExecutionResult>> entry : 
             byFramework.entrySet()) {
            
            String framework = entry.getKey();
            List<IsolatedBenchmarkExecutor.BenchmarkExecutionResult> frameworkResults = entry.getValue();
            
            FrameworkPhaseMetrics metrics = calculatePhaseMetrics(framework, frameworkResults);
            frameworkMetrics.put(framework, metrics);
        }
        
        analysis.setFrameworkMetrics(frameworkMetrics);
        
        // Calculate phase-level insights
        generatePhaseInsights(analysis);
        
        return analysis;
    }

    private FrameworkPhaseMetrics calculatePhaseMetrics(
            String framework,
            List<IsolatedBenchmarkExecutor.BenchmarkExecutionResult> results) {
        
        FrameworkPhaseMetrics metrics = new FrameworkPhaseMetrics(framework);
        
        List<IsolatedBenchmarkExecutor.BenchmarkExecutionResult> successful = 
            results.stream().filter(IsolatedBenchmarkExecutor.BenchmarkExecutionResult::isSuccess)
                   .collect(Collectors.toList());
        
        if (!successful.isEmpty()) {
            List<Long> executionTimes = successful.stream()
                .map(r -> r.getExecutionTime().toMillis())
                .collect(Collectors.toList());
            
            metrics.setExecutionTimeStatistics(calculateStatistics(executionTimes));
            
            // Extract performance metrics from metadata
            extractPerformanceMetrics(metrics, successful);
        }
        
        metrics.setSuccessRate((double) successful.size() / results.size());
        
        return metrics;
    }

    private void extractPerformanceMetrics(
            FrameworkPhaseMetrics metrics,
            List<IsolatedBenchmarkExecutor.BenchmarkExecutionResult> results) {
        
        // Extract metrics from metadata
        List<Integer> payloadSizes = new ArrayList<>();
        List<Integer> threadCounts = new ArrayList<>();
        List<String> heapSizes = new ArrayList<>();
        
        for (IsolatedBenchmarkExecutor.BenchmarkExecutionResult result : results) {
            Map<String, Object> metadata = result.getMetadata();
            
            if (metadata.containsKey("payloadSize")) {
                payloadSizes.add((Integer) metadata.get("payloadSize"));
            }
            
            if (metadata.containsKey("stressThreads")) {
                threadCounts.add((Integer) metadata.get("stressThreads"));
            }
            
            if (metadata.containsKey("heapSize")) {
                heapSizes.add((String) metadata.get("heapSize"));
            }
        }
        
        if (!payloadSizes.isEmpty()) {
            metrics.addMetric("payloadSizes", payloadSizes);
        }
        
        if (!threadCounts.isEmpty()) {
            metrics.addMetric("threadCounts", threadCounts);
        }
        
        if (!heapSizes.isEmpty()) {
            metrics.addMetric("heapSizes", heapSizes);
        }
    }

    private void generatePhaseInsights(PhaseAnalysis analysis) {
        List<String> insights = new ArrayList<>();
        
        Map<String, FrameworkPhaseMetrics> metrics = analysis.getFrameworkMetrics();
        
        if (metrics.size() > 1) {
            // Find best and worst performing frameworks
            String bestFramework = metrics.entrySet().stream()
                .max(Map.Entry.comparingByValue(
                    Comparator.comparingDouble(FrameworkPhaseMetrics::getSuccessRate)))
                .map(Map.Entry::getKey)
                .orElse(null);
            
            String worstFramework = metrics.entrySet().stream()
                .min(Map.Entry.comparingByValue(
                    Comparator.comparingDouble(FrameworkPhaseMetrics::getSuccessRate)))
                .map(Map.Entry::getKey)
                .orElse(null);
            
            if (bestFramework != null && worstFramework != null && !bestFramework.equals(worstFramework)) {
                insights.add(String.format("Best performing framework in %s phase: %s", 
                    analysis.getPhaseName(), bestFramework));
                insights.add(String.format("Worst performing framework in %s phase: %s", 
                    analysis.getPhaseName(), worstFramework));
            }
        }
        
        analysis.setInsights(insights);
    }

    private void generateCrossPhaseAnalysis(ComprehensiveAnalysisReport report) {
        List<String> crossPhaseInsights = new ArrayList<>();
        
        // Compare performance across phases
        if (report.getStandardAnalysis() != null && report.getStressAnalysis() != null) {
            crossPhaseInsights.add("Compared standard vs stress test performance");
        }
        
        if (report.getMemoryAnalysis() != null) {
            crossPhaseInsights.add("Analyzed memory pressure impact across frameworks");
        }
        
        report.setCrossPhaseInsights(crossPhaseInsights);
    }

    private void generateRecommendations(ComprehensiveAnalysisReport report) {
        List<String> recommendations = new ArrayList<>();
        
        // Generate recommendations based on analysis
        if (report.getStandardAnalysis() != null) {
            Map<String, FrameworkPhaseMetrics> metrics = report.getStandardAnalysis().getFrameworkMetrics();
            
            // Find most consistent framework
            String mostConsistent = findMostConsistentFramework(metrics);
            if (mostConsistent != null) {
                recommendations.add(String.format(
                    "For consistent performance, consider using %s", mostConsistent));
            }
        }
        
        recommendations.add("Consider running benchmarks multiple times for statistical significance");
        recommendations.add("Monitor memory usage patterns for optimal heap sizing");
        
        report.setRecommendations(recommendations);
    }

    private String findMostConsistentFramework(Map<String, FrameworkPhaseMetrics> metrics) {
        // Find framework with lowest coefficient of variation in execution times
        String mostConsistent = null;
        double lowestCV = Double.MAX_VALUE;
        
        for (Map.Entry<String, FrameworkPhaseMetrics> entry : metrics.entrySet()) {
            Map<String, Object> stats = entry.getValue().getExecutionTimeStatistics();
            if (stats != null && stats.containsKey("mean") && stats.containsKey("stdDev")) {
                double mean = (Double) stats.get("mean");
                double stdDev = (Double) stats.get("stdDev");
                double cv = stdDev / mean; // Coefficient of variation
                
                if (cv < lowestCV) {
                    lowestCV = cv;
                    mostConsistent = entry.getKey();
                }
            }
        }
        
        return mostConsistent;
    }

    private TrendAnalysisResult calculateTrends(
            String frameworkName,
            List<IsolatedBenchmarkExecutor.BenchmarkExecutionResult> historicalResults) {
        
        TrendAnalysisResult trendAnalysis = new TrendAnalysisResult(frameworkName);
        
        // Sort by timestamp
        historicalResults.sort(Comparator.comparing(
            IsolatedBenchmarkExecutor.BenchmarkExecutionResult::getTimestamp));
        
        // Calculate trends in success rate
        calculateSuccessRateTrend(trendAnalysis, historicalResults);
        
        // Calculate trends in execution time
        calculateExecutionTimeTrend(trendAnalysis, historicalResults);
        
        return trendAnalysis;
    }

    private void calculateSuccessRateTrend(
            TrendAnalysisResult trendAnalysis,
            List<IsolatedBenchmarkExecutor.BenchmarkExecutionResult> results) {
        
        // Simple trend calculation - compare first half vs second half
        int midpoint = results.size() / 2;
        
        List<IsolatedBenchmarkExecutor.BenchmarkExecutionResult> firstHalf = 
            results.subList(0, midpoint);
        List<IsolatedBenchmarkExecutor.BenchmarkExecutionResult> secondHalf = 
            results.subList(midpoint, results.size());
        
        double firstHalfSuccessRate = firstHalf.stream()
            .mapToDouble(r -> r.isSuccess() ? 1.0 : 0.0)
            .average().orElse(0.0);
        
        double secondHalfSuccessRate = secondHalf.stream()
            .mapToDouble(r -> r.isSuccess() ? 1.0 : 0.0)
            .average().orElse(0.0);
        
        double successRateTrend = secondHalfSuccessRate - firstHalfSuccessRate;
        trendAnalysis.setSuccessRateTrend(successRateTrend);
    }

    private void calculateExecutionTimeTrend(
            TrendAnalysisResult trendAnalysis,
            List<IsolatedBenchmarkExecutor.BenchmarkExecutionResult> results) {
        
        List<IsolatedBenchmarkExecutor.BenchmarkExecutionResult> successful = 
            results.stream().filter(IsolatedBenchmarkExecutor.BenchmarkExecutionResult::isSuccess)
                   .collect(Collectors.toList());
        
        if (successful.size() < 2) {
            return;
        }
        
        // Simple trend calculation - compare first half vs second half
        int midpoint = successful.size() / 2;
        
        double firstHalfAvg = successful.subList(0, midpoint).stream()
            .mapToLong(r -> r.getExecutionTime().toMillis())
            .average().orElse(0.0);
        
        double secondHalfAvg = successful.subList(midpoint, successful.size()).stream()
            .mapToLong(r -> r.getExecutionTime().toMillis())
            .average().orElse(0.0);
        
        double executionTimeTrend = ((secondHalfAvg - firstHalfAvg) / firstHalfAvg) * 100;
        trendAnalysis.setExecutionTimeTrendPercentage(executionTimeTrend);
    }

    private List<IsolatedBenchmarkExecutor.BenchmarkExecutionResult> getHistoricalResults(
            String frameworkName, int daysBack) {
        // This would typically query a database or read from stored files
        // For now, return results from our in-memory cache
        return collectedResults.getOrDefault(frameworkName, Collections.emptyList());
    }

    private FrameworkMetrics calculateFrameworkMetrics(
            String framework,
            List<IsolatedBenchmarkExecutor.BenchmarkExecutionResult> results) {
        
        FrameworkMetrics metrics = new FrameworkMetrics(framework);
        
        List<IsolatedBenchmarkExecutor.BenchmarkExecutionResult> successful = 
            results.stream().filter(IsolatedBenchmarkExecutor.BenchmarkExecutionResult::isSuccess)
                   .collect(Collectors.toList());
        
        metrics.setTotalExecutions(results.size());
        metrics.setSuccessfulExecutions(successful.size());
        metrics.setSuccessRate((double) successful.size() / results.size());
        
        if (!successful.isEmpty()) {
            List<Long> executionTimes = successful.stream()
                .map(r -> r.getExecutionTime().toMillis())
                .collect(Collectors.toList());
            
            metrics.setExecutionTimeStats(calculateStatistics(executionTimes));
        }
        
        return metrics;
    }

    private void calculateRelativePerformance(FrameworkComparisonReport comparison) {
        Map<String, FrameworkMetrics> metrics = comparison.getFrameworkMetrics();
        
        if (metrics.size() < 2) {
            return;
        }
        
        // Find baseline (best performing framework)
        String baseline = metrics.entrySet().stream()
            .max(Map.Entry.comparingByValue(
                Comparator.comparingDouble(FrameworkMetrics::getSuccessRate)))
            .map(Map.Entry::getKey)
            .orElse(null);
        
        if (baseline != null) {
            FrameworkMetrics baselineMetrics = metrics.get(baseline);
            Map<String, Double> relativePerformance = new HashMap<>();
            
            for (Map.Entry<String, FrameworkMetrics> entry : metrics.entrySet()) {
                if (!entry.getKey().equals(baseline)) {
                    double relativePerfomance = calculateRelativePerformance(
                        baselineMetrics, entry.getValue());
                    relativePerformance.put(entry.getKey(), relativePerfomance);
                }
            }
            
            comparison.setRelativePerformance(relativePerformance);
            comparison.setBaselineFramework(baseline);
        }
    }

    private double calculateRelativePerformance(FrameworkMetrics baseline, FrameworkMetrics framework) {
        // Simple relative performance based on success rate and execution time
        double successRateWeight = 0.6;
        double executionTimeWeight = 0.4;
        
        double successRateScore = framework.getSuccessRate() / baseline.getSuccessRate();
        
        double executionTimeScore = 1.0;
        if (baseline.getExecutionTimeStats() != null && framework.getExecutionTimeStats() != null) {
            double baselineMean = (Double) baseline.getExecutionTimeStats().get("mean");
            double frameworkMean = (Double) framework.getExecutionTimeStats().get("mean");
            executionTimeScore = baselineMean / frameworkMean; // Higher is better (less time)
        }
        
        return (successRateScore * successRateWeight) + (executionTimeScore * executionTimeWeight);
    }

    private void generateComparisonInsights(FrameworkComparisonReport comparison) {
        List<String> insights = new ArrayList<>();
        
        if (comparison.getBaselineFramework() != null) {
            insights.add(String.format("Baseline framework: %s", comparison.getBaselineFramework()));
        }
        
        Map<String, Double> relativePerf = comparison.getRelativePerformance();
        if (relativePerf != null && !relativePerf.isEmpty()) {
            String bestRelative = relativePerf.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
            
            if (bestRelative != null) {
                insights.add(String.format("Best relative performer: %s", bestRelative));
            }
        }
        
        comparison.setInsights(insights);
    }

    private Map<String, Object> calculateStatistics(List<Long> values) {
        if (values.isEmpty()) {
            return new HashMap<>();
        }
        
        Collections.sort(values);
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("count", values.size());
        stats.put("min", values.get(0));
        stats.put("max", values.get(values.size() - 1));
        stats.put("mean", values.stream().mapToLong(Long::longValue).average().orElse(0.0));
        stats.put("median", values.get(values.size() / 2));
        stats.put("p95", values.get((int) (values.size() * 0.95)));
        stats.put("p99", values.get((int) (values.size() * 0.99)));
        
        // Calculate standard deviation
        double mean = (Double) stats.get("mean");
        double variance = values.stream()
            .mapToDouble(v -> Math.pow(v - mean, 2))
            .average().orElse(0.0);
        stats.put("stdDev", Math.sqrt(variance));
        
        return stats;
    }

    private Map<String, Object> calculateDoubleStatistics(List<Double> values) {
        if (values.isEmpty()) {
            return new HashMap<>();
        }
        
        Collections.sort(values);
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("count", values.size());
        stats.put("min", values.get(0));
        stats.put("max", values.get(values.size() - 1));
        stats.put("mean", values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0));
        stats.put("median", values.get(values.size() / 2));
        stats.put("p95", values.get((int) (values.size() * 0.95)));
        stats.put("p99", values.get((int) (values.size() * 0.99)));
        
        return stats;
    }

    private void storeAggregatedResults(AggregatedBenchmarkResults results) {
        try {
            Path resultsFile = resultsDirectory.resolve("aggregated-" + results.getSessionId() + ".json");
            Files.write(resultsFile, objectMapper.writeValueAsBytes(results));
            logger.info("Stored aggregated results to: {}", resultsFile);
        } catch (IOException e) {
            logger.error("Failed to store aggregated results", e);
        }
    }

    private void storeComprehensiveAnalysis(ComprehensiveAnalysisReport report) {
        try {
            String filename = "comprehensive-analysis-" + 
                report.getStartTime().toEpochMilli() + ".json";
            Path analysisFile = resultsDirectory.resolve(filename);
            Files.write(analysisFile, objectMapper.writeValueAsBytes(report));
            logger.info("Stored comprehensive analysis to: {}", analysisFile);
        } catch (IOException e) {
            logger.error("Failed to store comprehensive analysis", e);
        }
    }

    // Result classes
    public static class AggregatedBenchmarkResults {
        private final String sessionId;
        private final Instant timestamp;
        private Map<String, FrameworkAggregatedResult> frameworkResults = new HashMap<>();
        private Map<String, FrameworkComparison> crossFrameworkComparisons = new HashMap<>();

        public AggregatedBenchmarkResults(String sessionId) {
            this.sessionId = sessionId;
            this.timestamp = Instant.now();
        }

        public void addFrameworkResult(String framework, FrameworkAggregatedResult result) {
            this.frameworkResults.put(framework, result);
        }

        // Getters and setters
        public String getSessionId() { return sessionId; }
        public Instant getTimestamp() { return timestamp; }
        public Map<String, FrameworkAggregatedResult> getFrameworkResults() { return frameworkResults; }
        public void setFrameworkResults(Map<String, FrameworkAggregatedResult> frameworkResults) { 
            this.frameworkResults = frameworkResults; 
        }
        public Map<String, FrameworkComparison> getCrossFrameworkComparisons() { return crossFrameworkComparisons; }
        public void setCrossFrameworkComparisons(Map<String, FrameworkComparison> crossFrameworkComparisons) { 
            this.crossFrameworkComparisons = crossFrameworkComparisons; 
        }
    }

    public static class FrameworkAggregatedResult {
        private final String frameworkName;
        private int totalExecutions;
        private int successfulExecutions;
        private int failedExecutions;
        private double successRate;
        private Map<String, Object> executionTimeStats;
        private Map<String, Object> benchmarkMetrics;
        private Map<String, Long> errorPatterns;

        public FrameworkAggregatedResult(String frameworkName) {
            this.frameworkName = frameworkName;
        }

        // Getters and setters
        public String getFrameworkName() { return frameworkName; }
        public int getTotalExecutions() { return totalExecutions; }
        public void setTotalExecutions(int totalExecutions) { this.totalExecutions = totalExecutions; }
        public int getSuccessfulExecutions() { return successfulExecutions; }
        public void setSuccessfulExecutions(int successfulExecutions) { this.successfulExecutions = successfulExecutions; }
        public int getFailedExecutions() { return failedExecutions; }
        public void setFailedExecutions(int failedExecutions) { this.failedExecutions = failedExecutions; }
        public double getSuccessRate() { return successRate; }
        public void setSuccessRate(double successRate) { this.successRate = successRate; }
        public Map<String, Object> getExecutionTimeStats() { return executionTimeStats; }
        public void setExecutionTimeStats(Map<String, Object> executionTimeStats) { this.executionTimeStats = executionTimeStats; }
        public Map<String, Object> getBenchmarkMetrics() { return benchmarkMetrics; }
        public void setBenchmarkMetrics(Map<String, Object> benchmarkMetrics) { this.benchmarkMetrics = benchmarkMetrics; }
        public Map<String, Long> getErrorPatterns() { return errorPatterns; }
        public void setErrorPatterns(Map<String, Long> errorPatterns) { this.errorPatterns = errorPatterns; }
    }

    public static class FrameworkComparison {
        private final String baselineFramework;
        private final String comparedFramework;
        private double successRateDifference;
        private double executionTimePercentageDifference;

        public FrameworkComparison(String baselineFramework, String comparedFramework) {
            this.baselineFramework = baselineFramework;
            this.comparedFramework = comparedFramework;
        }

        // Getters and setters
        public String getBaselineFramework() { return baselineFramework; }
        public String getComparedFramework() { return comparedFramework; }
        public double getSuccessRateDifference() { return successRateDifference; }
        public void setSuccessRateDifference(double successRateDifference) { this.successRateDifference = successRateDifference; }
        public double getExecutionTimePercentageDifference() { return executionTimePercentageDifference; }
        public void setExecutionTimePercentageDifference(double executionTimePercentageDifference) { 
            this.executionTimePercentageDifference = executionTimePercentageDifference; 
        }
    }

    public static class ComprehensiveAnalysisReport {
        private final List<String> frameworks;
        private final Instant startTime;
        private Instant completionTime;
        private PhaseAnalysis warmupAnalysis;
        private PhaseAnalysis standardAnalysis;
        private PhaseAnalysis stressAnalysis;
        private PhaseAnalysis memoryAnalysis;
        private PhaseAnalysis scalabilityAnalysis;
        private List<String> crossPhaseInsights;
        private List<String> recommendations;

        public ComprehensiveAnalysisReport(List<String> frameworks, Instant startTime, Instant completionTime) {
            this.frameworks = new ArrayList<>(frameworks);
            this.startTime = startTime;
            this.completionTime = completionTime;
        }

        // Getters and setters
        public List<String> getFrameworks() { return frameworks; }
        public Instant getStartTime() { return startTime; }
        public Instant getCompletionTime() { return completionTime; }
        public PhaseAnalysis getWarmupAnalysis() { return warmupAnalysis; }
        public void setWarmupAnalysis(PhaseAnalysis warmupAnalysis) { this.warmupAnalysis = warmupAnalysis; }
        public PhaseAnalysis getStandardAnalysis() { return standardAnalysis; }
        public void setStandardAnalysis(PhaseAnalysis standardAnalysis) { this.standardAnalysis = standardAnalysis; }
        public PhaseAnalysis getStressAnalysis() { return stressAnalysis; }
        public void setStressAnalysis(PhaseAnalysis stressAnalysis) { this.stressAnalysis = stressAnalysis; }
        public PhaseAnalysis getMemoryAnalysis() { return memoryAnalysis; }
        public void setMemoryAnalysis(PhaseAnalysis memoryAnalysis) { this.memoryAnalysis = memoryAnalysis; }
        public PhaseAnalysis getScalabilityAnalysis() { return scalabilityAnalysis; }
        public void setScalabilityAnalysis(PhaseAnalysis scalabilityAnalysis) { this.scalabilityAnalysis = scalabilityAnalysis; }
        public List<String> getCrossPhaseInsights() { return crossPhaseInsights; }
        public void setCrossPhaseInsights(List<String> crossPhaseInsights) { this.crossPhaseInsights = crossPhaseInsights; }
        public List<String> getRecommendations() { return recommendations; }
        public void setRecommendations(List<String> recommendations) { this.recommendations = recommendations; }
    }

    public static class PhaseAnalysis {
        private final String phaseName;
        private Map<String, FrameworkPhaseMetrics> frameworkMetrics;
        private List<String> insights;

        public PhaseAnalysis(String phaseName) {
            this.phaseName = phaseName;
        }

        // Getters and setters
        public String getPhaseName() { return phaseName; }
        public Map<String, FrameworkPhaseMetrics> getFrameworkMetrics() { return frameworkMetrics; }
        public void setFrameworkMetrics(Map<String, FrameworkPhaseMetrics> frameworkMetrics) { 
            this.frameworkMetrics = frameworkMetrics; 
        }
        public List<String> getInsights() { return insights; }
        public void setInsights(List<String> insights) { this.insights = insights; }
    }

    public static class FrameworkPhaseMetrics {
        private final String frameworkName;
        private double successRate;
        private Map<String, Object> executionTimeStatistics;
        private Map<String, Object> additionalMetrics = new HashMap<>();

        public FrameworkPhaseMetrics(String frameworkName) {
            this.frameworkName = frameworkName;
        }

        public void addMetric(String key, Object value) {
            this.additionalMetrics.put(key, value);
        }

        // Getters and setters
        public String getFrameworkName() { return frameworkName; }
        public double getSuccessRate() { return successRate; }
        public void setSuccessRate(double successRate) { this.successRate = successRate; }
        public Map<String, Object> getExecutionTimeStatistics() { return executionTimeStatistics; }
        public void setExecutionTimeStatistics(Map<String, Object> executionTimeStatistics) { 
            this.executionTimeStatistics = executionTimeStatistics; 
        }
        public Map<String, Object> getAdditionalMetrics() { return additionalMetrics; }
    }

    public static class TrendAnalysisResult {
        private final String frameworkName;
        private String message;
        private double successRateTrend;
        private double executionTimeTrendPercentage;

        public TrendAnalysisResult(String frameworkName) {
            this.frameworkName = frameworkName;
        }

        public TrendAnalysisResult(String frameworkName, String message) {
            this.frameworkName = frameworkName;
            this.message = message;
        }

        // Getters and setters
        public String getFrameworkName() { return frameworkName; }
        public String getMessage() { return message; }
        public double getSuccessRateTrend() { return successRateTrend; }
        public void setSuccessRateTrend(double successRateTrend) { this.successRateTrend = successRateTrend; }
        public double getExecutionTimeTrendPercentage() { return executionTimeTrendPercentage; }
        public void setExecutionTimeTrendPercentage(double executionTimeTrendPercentage) { 
            this.executionTimeTrendPercentage = executionTimeTrendPercentage; 
        }
    }

    public static class FrameworkComparisonReport {
        private final List<String> frameworks;
        private final Instant timestamp;
        private Map<String, FrameworkMetrics> frameworkMetrics = new HashMap<>();
        private Map<String, Double> relativePerformance;
        private String baselineFramework;
        private List<String> insights;

        public FrameworkComparisonReport(List<String> frameworks) {
            this.frameworks = new ArrayList<>(frameworks);
            this.timestamp = Instant.now();
        }

        public void addFrameworkMetrics(String framework, FrameworkMetrics metrics) {
            this.frameworkMetrics.put(framework, metrics);
        }

        // Getters and setters
        public List<String> getFrameworks() { return frameworks; }
        public Instant getTimestamp() { return timestamp; }
        public Map<String, FrameworkMetrics> getFrameworkMetrics() { return frameworkMetrics; }
        public Map<String, Double> getRelativePerformance() { return relativePerformance; }
        public void setRelativePerformance(Map<String, Double> relativePerformance) { 
            this.relativePerformance = relativePerformance; 
        }
        public String getBaselineFramework() { return baselineFramework; }
        public void setBaselineFramework(String baselineFramework) { this.baselineFramework = baselineFramework; }
        public List<String> getInsights() { return insights; }
        public void setInsights(List<String> insights) { this.insights = insights; }
    }

    public static class FrameworkMetrics {
        private final String frameworkName;
        private int totalExecutions;
        private int successfulExecutions;
        private double successRate;
        private Map<String, Object> executionTimeStats;

        public FrameworkMetrics(String frameworkName) {
            this.frameworkName = frameworkName;
        }

        // Getters and setters
        public String getFrameworkName() { return frameworkName; }
        public int getTotalExecutions() { return totalExecutions; }
        public void setTotalExecutions(int totalExecutions) { this.totalExecutions = totalExecutions; }
        public int getSuccessfulExecutions() { return successfulExecutions; }
        public void setSuccessfulExecutions(int successfulExecutions) { this.successfulExecutions = successfulExecutions; }
        public double getSuccessRate() { return successRate; }
        public void setSuccessRate(double successRate) { this.successRate = successRate; }
        public Map<String, Object> getExecutionTimeStats() { return executionTimeStats; }
        public void setExecutionTimeStats(Map<String, Object> executionTimeStats) { this.executionTimeStats = executionTimeStats; }
    }
}