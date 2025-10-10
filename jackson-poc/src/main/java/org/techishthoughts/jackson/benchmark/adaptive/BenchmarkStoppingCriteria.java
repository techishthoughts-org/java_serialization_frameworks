package org.techishthoughts.jackson.benchmark.adaptive;

import org.techishthoughts.jackson.benchmark.statistical.ConfidenceIntervalCalculator;
import org.techishthoughts.jackson.benchmark.statistical.StatisticalSignificanceDetector;
import org.techishthoughts.jackson.benchmark.statistical.MeasurementStabilityAnalyzer;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Comprehensive stopping criteria for adaptive benchmarking based on multiple
 * statistical and practical considerations including confidence intervals,
 * statistical significance, measurement stability, and resource constraints.
 */
public class BenchmarkStoppingCriteria {

    private final AdaptiveBenchmarkConfig config;
    private final ConfidenceIntervalCalculator confidenceCalculator;
    private final StatisticalSignificanceDetector significanceDetector;
    private final MeasurementStabilityAnalyzer stabilityAnalyzer;

    // Benchmark tracking
    private final Instant startTime;
    private final Map<String, Object> benchmarkMetrics;

    public enum StoppingReason {
        CONFIDENCE_ACHIEVED("Desired confidence interval width achieved"),
        STATISTICAL_SIGNIFICANCE("Statistical significance detected"),
        MEASUREMENT_STABILITY("Measurements have stabilized"),
        MINIMUM_SAMPLES_REACHED("Minimum sample size reached with acceptable quality"),
        MAXIMUM_SAMPLES_REACHED("Maximum sample size limit reached"),
        TIME_LIMIT_REACHED("Maximum benchmark duration exceeded"),
        CONVERGENCE_DETECTED("Measurements have converged to stable value"),
        INSUFFICIENT_PROGRESS("Insufficient improvement in recent measurements"),
        USER_REQUESTED("Benchmark stopped by user request"),
        ERROR_THRESHOLD_EXCEEDED("Error rate or quality threshold exceeded");

        private final String description;
        StoppingReason(String description) { this.description = description; }
        public String getDescription() { return description; }
    }

    public BenchmarkStoppingCriteria(AdaptiveBenchmarkConfig config) {
        this.config = config;
        this.confidenceCalculator = new ConfidenceIntervalCalculator(config.getConfidenceLevel());
        this.significanceDetector = new StatisticalSignificanceDetector(config);
        this.stabilityAnalyzer = new MeasurementStabilityAnalyzer(config);
        this.startTime = Instant.now();
        this.benchmarkMetrics = new HashMap<>();
    }

    /**
     * Comprehensive stopping decision result.
     */
    public static class StoppingDecision {
        private final boolean shouldStop;
        private final StoppingReason primaryReason;
        private final List<StoppingReason> contributingReasons;
        private final double confidenceScore;
        private final double stabilityScore;
        private final double progressScore;
        private final int recommendedAdditionalSamples;
        private final Duration estimatedRemainingTime;
        private final Map<String, Object> decisionMetrics;
        private final String detailedExplanation;

        public StoppingDecision(boolean shouldStop, StoppingReason primaryReason,
                              List<StoppingReason> contributingReasons, double confidenceScore,
                              double stabilityScore, double progressScore, int recommendedAdditionalSamples,
                              Duration estimatedRemainingTime, Map<String, Object> decisionMetrics,
                              String detailedExplanation) {
            this.shouldStop = shouldStop;
            this.primaryReason = primaryReason;
            this.contributingReasons = new ArrayList<>(contributingReasons);
            this.confidenceScore = confidenceScore;
            this.stabilityScore = stabilityScore;
            this.progressScore = progressScore;
            this.recommendedAdditionalSamples = recommendedAdditionalSamples;
            this.estimatedRemainingTime = estimatedRemainingTime;
            this.decisionMetrics = new HashMap<>(decisionMetrics);
            this.detailedExplanation = detailedExplanation;
        }

        public boolean shouldStop() { return shouldStop; }
        public StoppingReason getPrimaryReason() { return primaryReason; }
        public List<StoppingReason> getContributingReasons() { return contributingReasons; }
        public double getConfidenceScore() { return confidenceScore; }
        public double getStabilityScore() { return stabilityScore; }
        public double getProgressScore() { return progressScore; }
        public int getRecommendedAdditionalSamples() { return recommendedAdditionalSamples; }
        public Duration getEstimatedRemainingTime() { return estimatedRemainingTime; }
        public Map<String, Object> getDecisionMetrics() { return decisionMetrics; }
        public String getDetailedExplanation() { return detailedExplanation; }

        @Override
        public String toString() {
            return String.format("StoppingDecision{stop=%s, reason=%s, confidence=%.3f, stability=%.3f, progress=%.3f}",
                    shouldStop, primaryReason, confidenceScore, stabilityScore, progressScore);
        }
    }

    /**
     * Evaluates whether the benchmark should stop based on comprehensive criteria.
     */
    public StoppingDecision evaluateStoppingCriteria(List<Double> measurements) {
        Map<String, Object> metrics = new HashMap<>();
        List<StoppingReason> reasons = new ArrayList<>();

        // Basic validation
        if (measurements.isEmpty()) {
            return new StoppingDecision(false, null, Collections.emptyList(), 0.0, 0.0, 0.0,
                                      config.getMinimumSampleSize(), config.getMaxBenchmarkDuration(),
                                      metrics, "No measurements available for evaluation");
        }

        Duration elapsed = Duration.between(startTime, Instant.now());
        int sampleCount = measurements.size();

        // Hard limits check
        StoppingReason hardLimit = checkHardLimits(sampleCount, elapsed);
        if (hardLimit != null) {
            return createStoppingDecision(true, hardLimit, Collections.singletonList(hardLimit),
                                        measurements, metrics);
        }

        // Statistical quality assessment
        double confidenceScore = assessConfidenceQuality(measurements, metrics);
        double stabilityScore = assessStabilityQuality(measurements, metrics);
        double progressScore = assessProgressQuality(measurements, metrics);

        // Individual stopping criteria evaluation
        List<StoppingReason> satisfiedCriteria = evaluateIndividualCriteria(measurements, metrics);

        // Overall stopping decision
        boolean shouldStop = shouldStopBasedOnCriteria(satisfiedCriteria, confidenceScore,
                                                      stabilityScore, progressScore, sampleCount);

        StoppingReason primaryReason = determinePrimaryReason(satisfiedCriteria, sampleCount, elapsed);

        // Additional samples recommendation
        int additionalSamples = 0;
        Duration estimatedTime = Duration.ZERO;
        if (!shouldStop) {
            additionalSamples = estimateAdditionalSamples(measurements, confidenceScore, stabilityScore);
            estimatedTime = estimateRemainingTime(measurements, additionalSamples, elapsed);
        }

        String explanation = buildDetailedExplanation(shouldStop, satisfiedCriteria, confidenceScore,
                                                    stabilityScore, progressScore, additionalSamples);

        return new StoppingDecision(shouldStop, primaryReason, satisfiedCriteria, confidenceScore,
                                  stabilityScore, progressScore, additionalSamples, estimatedTime,
                                  metrics, explanation);
    }

    /**
     * Evaluates confidence interval-based stopping criteria.
     */
    public boolean isConfidenceIntervalSufficient(List<Double> measurements) {
        if (measurements.size() < config.getMinimumSampleSize()) {
            return false;
        }

        ConfidenceIntervalCalculator.ConfidenceInterval ci =
            confidenceCalculator.calculateTConfidenceInterval(measurements);

        return ci.getRelativeWidth() <= config.getMarginOfError();
    }

    /**
     * Evaluates statistical significance-based stopping criteria.
     */
    public boolean hasStatisticalSignificance(List<Double> measurements) {
        if (measurements.size() < config.getMinimumSampleSize()) {
            return false;
        }

        StatisticalSignificanceDetector.SignificanceResult result =
            significanceDetector.performOneSampleTTest(measurements);

        return result.isSignificant() && result.getPower() >= 0.8;
    }

    /**
     * Evaluates measurement stability-based stopping criteria.
     */
    public boolean isMeasurementStable(List<Double> measurements) {
        if (measurements.size() < config.getStabilityWindowSize()) {
            return false;
        }

        MeasurementStabilityAnalyzer.StabilityAnalysis analysis =
            stabilityAnalyzer.analyzeStability(measurements);

        return analysis.isStable() && analysis.getStabilityScore() >= 0.8;
    }

    /**
     * Evaluates convergence-based stopping criteria.
     */
    public boolean hasConverged(List<Double> measurements) {
        if (measurements.size() < config.getConvergenceWindowSize() * 2) {
            return false;
        }

        return stabilityAnalyzer.isConverging(measurements) &&
               stabilityAnalyzer.calculateConvergenceRate(measurements) > config.getConvergenceThreshold();
    }

    // Private implementation methods

    private StoppingReason checkHardLimits(int sampleCount, Duration elapsed) {
        if (sampleCount >= config.getMaximumSampleSize()) {
            return StoppingReason.MAXIMUM_SAMPLES_REACHED;
        }

        if (elapsed.compareTo(config.getMaxBenchmarkDuration()) >= 0) {
            return StoppingReason.TIME_LIMIT_REACHED;
        }

        return null;
    }

    private double assessConfidenceQuality(List<Double> measurements, Map<String, Object> metrics) {
        if (measurements.size() < 2) {
            metrics.put("confidenceQuality", 0.0);
            return 0.0;
        }

        ConfidenceIntervalCalculator.ConfidenceInterval ci =
            confidenceCalculator.calculateTConfidenceInterval(measurements);

        double relativeWidth = ci.getRelativeWidth();
        double targetWidth = config.getMarginOfError();

        // Quality score based on how close we are to target
        double quality = Math.max(0.0, 1.0 - relativeWidth / targetWidth);
        quality = Math.min(1.0, quality);

        metrics.put("confidenceInterval", ci);
        metrics.put("confidenceQuality", quality);
        metrics.put("targetMarginOfError", targetWidth);

        return quality;
    }

    private double assessStabilityQuality(List<Double> measurements, Map<String, Object> metrics) {
        if (measurements.size() < config.getStabilityWindowSize()) {
            metrics.put("stabilityQuality", 0.0);
            return 0.0;
        }

        MeasurementStabilityAnalyzer.StabilityAnalysis analysis =
            stabilityAnalyzer.analyzeStability(measurements);

        metrics.put("stabilityAnalysis", analysis);
        metrics.put("stabilityQuality", analysis.getStabilityScore());

        return analysis.getStabilityScore();
    }

    private double assessProgressQuality(List<Double> measurements, Map<String, Object> metrics) {
        if (measurements.size() < config.getConvergenceWindowSize()) {
            metrics.put("progressQuality", 0.0);
            return 0.0;
        }

        boolean isConverging = stabilityAnalyzer.isConverging(measurements);
        double convergenceRate = stabilityAnalyzer.calculateConvergenceRate(measurements);

        // Progress quality based on convergence
        double quality = isConverging ? Math.min(1.0, convergenceRate / config.getConvergenceThreshold()) : 0.0;

        metrics.put("isConverging", isConverging);
        metrics.put("convergenceRate", convergenceRate);
        metrics.put("progressQuality", quality);

        return quality;
    }

    private List<StoppingReason> evaluateIndividualCriteria(List<Double> measurements, Map<String, Object> metrics) {
        List<StoppingReason> satisfied = new ArrayList<>();

        // Confidence interval criterion
        if (isConfidenceIntervalSufficient(measurements)) {
            satisfied.add(StoppingReason.CONFIDENCE_ACHIEVED);
        }

        // Statistical significance criterion
        if (hasStatisticalSignificance(measurements)) {
            satisfied.add(StoppingReason.STATISTICAL_SIGNIFICANCE);
        }

        // Stability criterion
        if (isMeasurementStable(measurements)) {
            satisfied.add(StoppingReason.MEASUREMENT_STABILITY);
        }

        // Convergence criterion
        if (hasConverged(measurements)) {
            satisfied.add(StoppingReason.CONVERGENCE_DETECTED);
        }

        // Minimum samples with quality
        if (measurements.size() >= config.getMinimumSampleSize()) {
            double avgQuality = (assessConfidenceQuality(measurements, new HashMap<>()) +
                               assessStabilityQuality(measurements, new HashMap<>())) / 2.0;
            if (avgQuality >= 0.7) { // 70% quality threshold
                satisfied.add(StoppingReason.MINIMUM_SAMPLES_REACHED);
            }
        }

        // Progress criterion
        double progressQuality = assessProgressQuality(measurements, new HashMap<>());
        if (progressQuality < 0.1 && measurements.size() > config.getMinimumSampleSize() * 2) {
            satisfied.add(StoppingReason.INSUFFICIENT_PROGRESS);
        }

        metrics.put("satisfiedCriteria", satisfied);
        return satisfied;
    }

    private boolean shouldStopBasedOnCriteria(List<StoppingReason> satisfiedCriteria,
                                            double confidenceScore, double stabilityScore,
                                            double progressScore, int sampleCount) {
        // Must have minimum samples
        if (sampleCount < config.getMinimumSampleSize()) {
            return false;
        }

        // Stop if multiple high-quality criteria are met
        if (satisfiedCriteria.size() >= 2) {
            return true;
        }

        // Stop if any critical criterion is met with high quality
        boolean hasHighQuality = confidenceScore >= 0.9 || stabilityScore >= 0.9;
        if (hasHighQuality && !satisfiedCriteria.isEmpty()) {
            return true;
        }

        // Stop if insufficient progress is detected
        if (satisfiedCriteria.contains(StoppingReason.INSUFFICIENT_PROGRESS)) {
            return true;
        }

        return false;
    }

    private StoppingReason determinePrimaryReason(List<StoppingReason> satisfiedCriteria,
                                                int sampleCount, Duration elapsed) {
        if (satisfiedCriteria.isEmpty()) {
            return null;
        }

        // Priority order for primary reason
        StoppingReason[] priorityOrder = {
            StoppingReason.CONFIDENCE_ACHIEVED,
            StoppingReason.STATISTICAL_SIGNIFICANCE,
            StoppingReason.MEASUREMENT_STABILITY,
            StoppingReason.CONVERGENCE_DETECTED,
            StoppingReason.MINIMUM_SAMPLES_REACHED,
            StoppingReason.INSUFFICIENT_PROGRESS
        };

        for (StoppingReason reason : priorityOrder) {
            if (satisfiedCriteria.contains(reason)) {
                return reason;
            }
        }

        return satisfiedCriteria.get(0);
    }

    private int estimateAdditionalSamples(List<Double> measurements, double confidenceScore, double stabilityScore) {
        int baseEstimate = config.getMinimumSampleSize();

        // Estimate based on confidence interval
        if (confidenceScore < 0.5) {
            ConfidenceIntervalCalculator.ConfidenceInterval ci =
                confidenceCalculator.calculateTConfidenceInterval(measurements);
            int ciEstimate = confidenceCalculator.estimateRequiredSampleSize(measurements,
                                                                            config.getMarginOfError() *
                                                                            Math.abs(ci.getMean()));
            baseEstimate = Math.max(baseEstimate, ciEstimate - measurements.size());
        }

        // Estimate based on stability
        if (stabilityScore < 0.5) {
            int stabilityEstimate = stabilityAnalyzer.estimateAdditionalMeasurements(measurements);
            baseEstimate = Math.max(baseEstimate, stabilityEstimate);
        }

        // Cap at remaining capacity
        int maxAdditional = config.getMaximumSampleSize() - measurements.size();
        return Math.max(0, Math.min(baseEstimate, maxAdditional));
    }

    private Duration estimateRemainingTime(List<Double> measurements, int additionalSamples, Duration elapsed) {
        if (additionalSamples <= 0 || measurements.isEmpty()) {
            return Duration.ZERO;
        }

        // Estimate time per measurement based on elapsed time
        double avgTimePerMeasurement = (double) elapsed.toMillis() / measurements.size();
        long estimatedMillis = (long) (additionalSamples * avgTimePerMeasurement * 1.2); // 20% buffer

        return Duration.ofMillis(estimatedMillis);
    }

    private String buildDetailedExplanation(boolean shouldStop, List<StoppingReason> satisfiedCriteria,
                                          double confidenceScore, double stabilityScore, double progressScore,
                                          int additionalSamples) {
        StringBuilder explanation = new StringBuilder();

        if (shouldStop) {
            explanation.append("Benchmark stopping recommended. ");
            if (!satisfiedCriteria.isEmpty()) {
                explanation.append("Satisfied criteria: ")
                          .append(satisfiedCriteria.stream()
                                 .map(r -> r.getDescription())
                                 .reduce((a, b) -> a + "; " + b)
                                 .orElse(""))
                          .append(". ");
            }
        } else {
            explanation.append("Benchmark should continue. ");
            if (additionalSamples > 0) {
                explanation.append("Estimated ").append(additionalSamples)
                          .append(" additional samples needed. ");
            }
        }

        explanation.append(String.format("Quality scores - Confidence: %.1f%%, Stability: %.1f%%, Progress: %.1f%%",
                                        confidenceScore * 100, stabilityScore * 100, progressScore * 100));

        return explanation.toString();
    }

    private StoppingDecision createStoppingDecision(boolean shouldStop, StoppingReason primaryReason,
                                                   List<StoppingReason> reasons, List<Double> measurements,
                                                   Map<String, Object> metrics) {
        Duration elapsed = Duration.between(startTime, Instant.now());
        return new StoppingDecision(shouldStop, primaryReason, reasons, 0.0, 0.0, 0.0, 0,
                                  Duration.ZERO, metrics,
                                  primaryReason != null ? primaryReason.getDescription() : "Unknown reason");
    }
}