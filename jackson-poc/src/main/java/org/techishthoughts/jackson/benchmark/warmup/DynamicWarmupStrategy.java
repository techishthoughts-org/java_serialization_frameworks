package org.techishthoughts.jackson.benchmark.warmup;

import org.techishthoughts.jackson.benchmark.adaptive.AdaptiveBenchmarkConfig;
import org.techishthoughts.jackson.benchmark.statistical.MeasurementStabilityAnalyzer;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.function.Supplier;

/**
 * Dynamic warmup strategy that adapts the warmup process based on JIT compilation state,
 * measurement stability, and performance characteristics. Integrates multiple warmup
 * techniques for optimal benchmark preparation.
 */
public class DynamicWarmupStrategy {

    private final AdaptiveBenchmarkConfig config;
    private final JITCompilationDetector jitDetector;
    private final MeasurementStabilityAnalyzer stabilityAnalyzer;

    // Warmup state tracking
    private final List<Double> warmupTimes = new ArrayList<>();
    private final Map<String, Object> warmupMetrics = new HashMap<>();
    private Instant warmupStartTime;
    private int currentIteration = 0;
    private WarmupPhase currentPhase = WarmupPhase.INITIALIZATION;

    public enum WarmupPhase {
        INITIALIZATION,
        JIT_COMPILATION,
        STABILIZATION,
        VALIDATION,
        COMPLETED
    }

    public DynamicWarmupStrategy(AdaptiveBenchmarkConfig config) {
        this.config = config;
        this.jitDetector = new JITCompilationDetector(config);
        this.stabilityAnalyzer = new MeasurementStabilityAnalyzer(config);
    }

    /**
     * Comprehensive warmup result containing all relevant information.
     */
    public static class WarmupResult {
        private final boolean isComplete;
        private final int totalIterations;
        private final Duration totalDuration;
        private final WarmupPhase finalPhase;
        private final double finalStabilityScore;
        private final double coefficientOfVariation;
        private final boolean jitStable;
        private final Map<String, Object> metrics;
        private final String completionReason;

        public WarmupResult(boolean isComplete, int totalIterations, Duration totalDuration,
                          WarmupPhase finalPhase, double finalStabilityScore, double coefficientOfVariation,
                          boolean jitStable, Map<String, Object> metrics, String completionReason) {
            this.isComplete = isComplete;
            this.totalIterations = totalIterations;
            this.totalDuration = totalDuration;
            this.finalPhase = finalPhase;
            this.finalStabilityScore = finalStabilityScore;
            this.coefficientOfVariation = coefficientOfVariation;
            this.jitStable = jitStable;
            this.metrics = new HashMap<>(metrics);
            this.completionReason = completionReason;
        }

        public boolean isComplete() { return isComplete; }
        public int getTotalIterations() { return totalIterations; }
        public Duration getTotalDuration() { return totalDuration; }
        public WarmupPhase getFinalPhase() { return finalPhase; }
        public double getFinalStabilityScore() { return finalStabilityScore; }
        public double getCoefficientOfVariation() { return coefficientOfVariation; }
        public boolean isJitStable() { return jitStable; }
        public Map<String, Object> getMetrics() { return metrics; }
        public String getCompletionReason() { return completionReason; }

        @Override
        public String toString() {
            return String.format("WarmupResult{complete=%s, iterations=%d, duration=%s, cv=%.4f, phase=%s, reason='%s'}",
                    isComplete, totalIterations, totalDuration, coefficientOfVariation, finalPhase, completionReason);
        }
    }

    /**
     * Executes dynamic warmup using the provided benchmark function.
     */
    public WarmupResult executeWarmup(Supplier<Double> benchmarkFunction) {
        reset();
        warmupStartTime = Instant.now();
        warmupMetrics.put("startTime", warmupStartTime);

        try {
            while (!isWarmupComplete() && currentIteration < config.getMaximumWarmupIterations()) {
                // Execute one iteration of the benchmark
                double executionTime = benchmarkFunction.get();
                processWarmupIteration(executionTime);
                currentIteration++;

                // Check for timeout
                if (Duration.between(warmupStartTime, Instant.now()).compareTo(config.getWarmupTimeout()) > 0) {
                    break;
                }
            }

            return buildWarmupResult();

        } catch (Exception e) {
            warmupMetrics.put("error", e.getMessage());
            return new WarmupResult(false, currentIteration, Duration.between(warmupStartTime, Instant.now()),
                                  currentPhase, 0.0, Double.MAX_VALUE, false,
                                  warmupMetrics, "Error during warmup: " + e.getMessage());
        }
    }

    /**
     * Executes a batch warmup for better efficiency when possible.
     */
    public WarmupResult executeBatchWarmup(Supplier<List<Double>> batchBenchmarkFunction, int batchSize) {
        reset();
        warmupStartTime = Instant.now();
        warmupMetrics.put("startTime", warmupStartTime);
        warmupMetrics.put("batchSize", batchSize);

        try {
            while (!isWarmupComplete() && currentIteration < config.getMaximumWarmupIterations()) {
                // Execute a batch of iterations
                List<Double> batchResults = batchBenchmarkFunction.get();

                for (double executionTime : batchResults) {
                    processWarmupIteration(executionTime);
                    currentIteration++;

                    if (currentIteration >= config.getMaximumWarmupIterations()) break;
                }

                // Check for timeout
                if (Duration.between(warmupStartTime, Instant.now()).compareTo(config.getWarmupTimeout()) > 0) {
                    break;
                }
            }

            return buildWarmupResult();

        } catch (Exception e) {
            warmupMetrics.put("error", e.getMessage());
            return new WarmupResult(false, currentIteration, Duration.between(warmupStartTime, Instant.now()),
                                  currentPhase, 0.0, Double.MAX_VALUE, false,
                                  warmupMetrics, "Error during batch warmup: " + e.getMessage());
        }
    }

    /**
     * Checks if warmup should continue based on current state.
     */
    public boolean shouldContinueWarmup() {
        return !isWarmupComplete() &&
               currentIteration < config.getMaximumWarmupIterations() &&
               Duration.between(warmupStartTime, Instant.now()).compareTo(config.getWarmupTimeout()) <= 0;
    }

    /**
     * Gets current warmup status and recommendations.
     */
    public Map<String, Object> getWarmupStatus() {
        Map<String, Object> status = new HashMap<>(warmupMetrics);

        status.put("currentIteration", currentIteration);
        status.put("currentPhase", currentPhase);
        status.put("isComplete", isWarmupComplete());

        if (!warmupTimes.isEmpty()) {
            status.put("currentCV", calculateCoefficientOfVariation());
            status.put("targetCV", config.getJitStabilityThreshold());
        }

        // JIT compilation status
        JITCompilationDetector.JITState jitState = jitDetector.analyzeJITState();
        status.put("jitState", jitState.toString());
        status.put("jitStable", jitState.isStable());

        // Stability analysis
        if (warmupTimes.size() >= config.getStabilityWindowSize()) {
            MeasurementStabilityAnalyzer.StabilityAnalysis stability =
                stabilityAnalyzer.analyzeStability(warmupTimes);
            status.put("stabilityScore", stability.getStabilityScore());
            status.put("measurementStable", stability.isStable());
        }

        // Estimated completion
        int estimatedAdditionalIterations = estimateRemainingIterations();
        status.put("estimatedRemainingIterations", estimatedAdditionalIterations);

        return status;
    }

    // Private implementation methods

    private void processWarmupIteration(double executionTime) {
        warmupTimes.add(executionTime);
        jitDetector.recordExecution(executionTime);

        // Update phase based on current state
        updateWarmupPhase();

        // Track metrics
        if (currentIteration % 10 == 0 || currentIteration < 20) {
            updateWarmupMetrics();
        }
    }

    private void updateWarmupPhase() {
        if (currentPhase == WarmupPhase.COMPLETED) {
            return;
        }

        if (currentIteration < config.getMinimumWarmupIterations()) {
            if (currentIteration < 5) {
                currentPhase = WarmupPhase.INITIALIZATION;
            } else if (!jitDetector.isJITStable()) {
                currentPhase = WarmupPhase.JIT_COMPILATION;
            } else {
                currentPhase = WarmupPhase.STABILIZATION;
            }
        } else {
            // Minimum iterations met, check for completion criteria
            if (isWarmupComplete()) {
                currentPhase = WarmupPhase.COMPLETED;
            } else if (jitDetector.isJITStable()) {
                currentPhase = WarmupPhase.VALIDATION;
            } else {
                currentPhase = WarmupPhase.STABILIZATION;
            }
        }
    }

    private boolean isWarmupComplete() {
        // Must meet minimum iteration requirement
        if (currentIteration < config.getMinimumWarmupIterations()) {
            return false;
        }

        // JIT must be stable
        if (!jitDetector.isWarmupComplete()) {
            return false;
        }

        // Measurements must be stable
        if (warmupTimes.size() >= config.getStabilityWindowSize()) {
            MeasurementStabilityAnalyzer.StabilityAnalysis stability =
                stabilityAnalyzer.analyzeStability(warmupTimes);

            if (!stability.isStable()) {
                return false;
            }

            // Additional validation for coefficient of variation
            double cv = calculateCoefficientOfVariation();
            if (cv > config.getJitStabilityThreshold()) {
                return false;
            }
        } else {
            return false; // Not enough data for stability analysis
        }

        return true;
    }

    private int estimateRemainingIterations() {
        if (isWarmupComplete()) {
            return 0;
        }

        int minimumRemaining = Math.max(0, config.getMinimumWarmupIterations() - currentIteration);

        // Estimate based on JIT state
        int jitRecommended = jitDetector.getRecommendedWarmupIterations();

        // Estimate based on stability
        int stabilityRecommended = 0;
        if (warmupTimes.size() >= config.getStabilityWindowSize()) {
            stabilityRecommended = stabilityAnalyzer.estimateAdditionalMeasurements(warmupTimes);
        }

        int estimated = Math.max(minimumRemaining, Math.max(jitRecommended, stabilityRecommended));
        return Math.min(estimated, config.getMaximumWarmupIterations() - currentIteration);
    }

    private double calculateCoefficientOfVariation() {
        if (warmupTimes.size() < 2) {
            return Double.MAX_VALUE;
        }

        double mean = warmupTimes.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        if (mean == 0) return Double.MAX_VALUE;

        double variance = warmupTimes.stream()
                .mapToDouble(time -> Math.pow(time - mean, 2))
                .sum() / (warmupTimes.size() - 1);
        double stdDev = Math.sqrt(variance);

        return stdDev / mean;
    }

    private void updateWarmupMetrics() {
        warmupMetrics.put("iteration", currentIteration);
        warmupMetrics.put("phase", currentPhase);
        warmupMetrics.put("elapsedTime", Duration.between(warmupStartTime, Instant.now()));

        if (!warmupTimes.isEmpty()) {
            warmupMetrics.put("meanExecutionTime", warmupTimes.stream().mapToDouble(Double::doubleValue).average().orElse(0.0));
            warmupMetrics.put("coefficientOfVariation", calculateCoefficientOfVariation());
        }

        // JIT compilation metrics
        Map<String, Object> jitMetrics = jitDetector.getCompilationStatistics();
        warmupMetrics.put("jitMetrics", jitMetrics);
    }

    private WarmupResult buildWarmupResult() {
        Duration totalDuration = Duration.between(warmupStartTime, Instant.now());
        boolean isComplete = isWarmupComplete();

        double cv = calculateCoefficientOfVariation();
        double stabilityScore = 0.0;
        boolean jitStable = jitDetector.isJITStable();

        if (warmupTimes.size() >= config.getStabilityWindowSize()) {
            MeasurementStabilityAnalyzer.StabilityAnalysis stability =
                stabilityAnalyzer.analyzeStability(warmupTimes);
            stabilityScore = stability.getStabilityScore();
        }

        // Build completion reason
        String reason = buildCompletionReason(isComplete);

        // Final metrics update
        updateWarmupMetrics();
        warmupMetrics.put("finalPhase", currentPhase);
        warmupMetrics.put("finalStabilityScore", stabilityScore);
        warmupMetrics.put("totalDuration", totalDuration);
        warmupMetrics.put("completionReason", reason);

        return new WarmupResult(isComplete, currentIteration, totalDuration, currentPhase,
                              stabilityScore, cv, jitStable, warmupMetrics, reason);
    }

    private String buildCompletionReason(boolean isComplete) {
        if (!isComplete) {
            List<String> reasons = new ArrayList<>();

            if (currentIteration >= config.getMaximumWarmupIterations()) {
                reasons.add("maximum iterations reached");
            }

            if (Duration.between(warmupStartTime, Instant.now()).compareTo(config.getWarmupTimeout()) > 0) {
                reasons.add("timeout exceeded");
            }

            if (currentIteration < config.getMinimumWarmupIterations()) {
                reasons.add("minimum iterations not met");
            }

            if (!jitDetector.isWarmupComplete()) {
                reasons.add("JIT not stable");
            }

            if (warmupTimes.size() >= config.getStabilityWindowSize()) {
                MeasurementStabilityAnalyzer.StabilityAnalysis stability =
                    stabilityAnalyzer.analyzeStability(warmupTimes);
                if (!stability.isStable()) {
                    reasons.add("measurements not stable");
                }
            }

            if (calculateCoefficientOfVariation() > config.getJitStabilityThreshold()) {
                reasons.add("high coefficient of variation");
            }

            return reasons.isEmpty() ? "warmup incomplete" : String.join(", ", reasons);
        } else {
            return "warmup completed successfully";
        }
    }

    private void reset() {
        warmupTimes.clear();
        warmupMetrics.clear();
        currentIteration = 0;
        currentPhase = WarmupPhase.INITIALIZATION;
        jitDetector.reset();
    }
}