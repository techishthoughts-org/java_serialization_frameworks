package org.techishthoughts.jackson.benchmark.statistical;

import org.techishthoughts.jackson.benchmark.adaptive.AdaptiveBenchmarkConfig;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Analyzes measurement stability using multiple statistical techniques including
 * coefficient of variation, trend analysis, change point detection, and stationarity tests.
 */
public class MeasurementStabilityAnalyzer {

    private final AdaptiveBenchmarkConfig config;
    private final ConfidenceIntervalCalculator confidenceCalculator;

    public MeasurementStabilityAnalyzer(AdaptiveBenchmarkConfig config) {
        this.config = config;
        this.confidenceCalculator = new ConfidenceIntervalCalculator(config.getConfidenceLevel());
    }

    /**
     * Comprehensive stability analysis result.
     */
    public static class StabilityAnalysis {
        private final boolean isStable;
        private final double coefficientOfVariation;
        private final double trendSlope;
        private final boolean hasChangePoint;
        private final int changePointIndex;
        private final boolean isStationary;
        private final double stabilityScore;
        private final int windowSize;
        private final String stabilityReason;
        private final Map<String, Double> metrics;

        public StabilityAnalysis(boolean isStable, double coefficientOfVariation, double trendSlope,
                               boolean hasChangePoint, int changePointIndex, boolean isStationary,
                               double stabilityScore, int windowSize, String stabilityReason,
                               Map<String, Double> metrics) {
            this.isStable = isStable;
            this.coefficientOfVariation = coefficientOfVariation;
            this.trendSlope = trendSlope;
            this.hasChangePoint = hasChangePoint;
            this.changePointIndex = changePointIndex;
            this.isStationary = isStationary;
            this.stabilityScore = stabilityScore;
            this.windowSize = windowSize;
            this.stabilityReason = stabilityReason;
            this.metrics = new HashMap<>(metrics);
        }

        public boolean isStable() { return isStable; }
        public double getCoefficientOfVariation() { return coefficientOfVariation; }
        public double getTrendSlope() { return trendSlope; }
        public boolean hasChangePoint() { return hasChangePoint; }
        public int getChangePointIndex() { return changePointIndex; }
        public boolean isStationary() { return isStationary; }
        public double getStabilityScore() { return stabilityScore; }
        public int getWindowSize() { return windowSize; }
        public String getStabilityReason() { return stabilityReason; }
        public Map<String, Double> getMetrics() { return metrics; }

        @Override
        public String toString() {
            return String.format("StabilityAnalysis{stable=%s, cv=%.4f, score=%.3f, reason='%s'}",
                    isStable, coefficientOfVariation, stabilityScore, stabilityReason);
        }
    }

    /**
     * Performs comprehensive stability analysis on measurement data.
     */
    public StabilityAnalysis analyzeStability(List<Double> measurements) {
        if (measurements.size() < config.getStabilityWindowSize()) {
            return new StabilityAnalysis(false, Double.MAX_VALUE, 0.0, false, -1, false,
                                       0.0, measurements.size(), "Insufficient data for analysis",
                                       Collections.emptyMap());
        }

        // Use the most recent window for analysis
        int windowSize = Math.min(config.getStabilityWindowSize(), measurements.size());
        List<Double> window = measurements.subList(measurements.size() - windowSize, measurements.size());

        Map<String, Double> metrics = new HashMap<>();

        // Calculate basic statistics
        double mean = calculateMean(window);
        double stdDev = calculateStandardDeviation(window, mean);
        double coefficientOfVariation = mean > 0 ? stdDev / mean : Double.MAX_VALUE;

        metrics.put("mean", mean);
        metrics.put("stdDev", stdDev);
        metrics.put("cv", coefficientOfVariation);

        // Trend analysis
        double trendSlope = calculateTrendSlope(window);
        double trendSignificance = calculateTrendSignificance(window, trendSlope);

        metrics.put("trendSlope", trendSlope);
        metrics.put("trendSignificance", trendSignificance);

        // Change point detection
        int changePointIndex = detectChangePoint(window);
        boolean hasChangePoint = changePointIndex >= 0;

        metrics.put("changePointIndex", (double) changePointIndex);

        // Stationarity test
        boolean isStationary = testStationarity(window);
        metrics.put("stationary", isStationary ? 1.0 : 0.0);

        // Stability score calculation
        double stabilityScore = calculateStabilityScore(coefficientOfVariation, Math.abs(trendSlope),
                                                      hasChangePoint, isStationary, trendSignificance);
        metrics.put("stabilityScore", stabilityScore);

        // Determine overall stability
        boolean cvStable = coefficientOfVariation <= config.getCoefficientOfVariationThreshold();
        boolean trendStable = Math.abs(trendSlope) < mean * 0.01; // Less than 1% trend per measurement
        boolean noSignificantChanges = !hasChangePoint;
        boolean sufficientlyStationary = isStationary;

        boolean isStable = cvStable && trendStable && noSignificantChanges && sufficientlyStationary;

        String reason = buildStabilityReason(cvStable, trendStable, noSignificantChanges, sufficientlyStationary);

        return new StabilityAnalysis(isStable, coefficientOfVariation, trendSlope, hasChangePoint,
                                   changePointIndex, isStationary, stabilityScore, windowSize, reason, metrics);
    }

    /**
     * Determines if measurements are converging to a stable value.
     */
    public boolean isConverging(List<Double> measurements) {
        if (measurements.size() < config.getConvergenceWindowSize() * 2) {
            return false;
        }

        int windowSize = config.getConvergenceWindowSize();

        // Compare two consecutive windows
        List<Double> earlierWindow = measurements.subList(measurements.size() - windowSize * 2,
                                                         measurements.size() - windowSize);
        List<Double> laterWindow = measurements.subList(measurements.size() - windowSize,
                                                       measurements.size());

        double earlierCV = calculateCoefficientOfVariation(earlierWindow);
        double laterCV = calculateCoefficientOfVariation(laterWindow);

        // Convergence if CV is decreasing and below threshold
        return laterCV < earlierCV && laterCV <= config.getCoefficientOfVariationThreshold();
    }

    /**
     * Calculates the rate of convergence.
     */
    public double calculateConvergenceRate(List<Double> measurements) {
        if (measurements.size() < config.getConvergenceWindowSize() * 3) {
            return 0.0;
        }

        int windowSize = config.getConvergenceWindowSize();
        List<Double> cvValues = new ArrayList<>();

        // Calculate CV for sliding windows
        for (int i = windowSize; i <= measurements.size(); i++) {
            List<Double> window = measurements.subList(i - windowSize, i);
            cvValues.add(calculateCoefficientOfVariation(window));
        }

        // Calculate the slope of CV over time (negative slope indicates convergence)
        return -calculateTrendSlope(cvValues); // Negative because we want decreasing CV
    }

    /**
     * Estimates how many more measurements are needed for stability.
     */
    public int estimateAdditionalMeasurements(List<Double> measurements) {
        if (measurements.isEmpty()) {
            return config.getMinimumSampleSize();
        }

        StabilityAnalysis analysis = analyzeStability(measurements);

        if (analysis.isStable()) {
            return 0;
        }

        // Estimate based on current CV and target CV
        double currentCV = analysis.getCoefficientOfVariation();
        double targetCV = config.getCoefficientOfVariationThreshold();

        if (currentCV <= targetCV) {
            // CV is good, might need more samples for other criteria
            return Math.max(10, config.getMinimumSampleSize() - measurements.size());
        }

        // Estimate additional samples needed to reduce CV
        double ratio = currentCV / targetCV;
        int currentSamples = measurements.size();
        int estimatedTotal = (int) (currentSamples * ratio * ratio); // CV decreases roughly with sqrt(n)

        int additional = Math.max(0, estimatedTotal - currentSamples);
        return Math.min(additional, config.getMaximumSampleSize() - currentSamples);
    }

    /**
     * Detects outliers using IQR method and statistical tests.
     */
    public List<Integer> detectOutliers(List<Double> measurements) {
        if (measurements.size() < 4) {
            return Collections.emptyList();
        }

        List<Integer> outlierIndices = new ArrayList<>();

        // IQR method
        double[] sortedValues = measurements.stream().mapToDouble(Double::doubleValue).sorted().toArray();
        int n = sortedValues.length;

        double q1 = calculatePercentile(sortedValues, 0.25);
        double q3 = calculatePercentile(sortedValues, 0.75);
        double iqr = q3 - q1;

        double lowerBound = q1 - 1.5 * iqr;
        double upperBound = q3 + 1.5 * iqr;

        // Find outliers
        for (int i = 0; i < measurements.size(); i++) {
            double value = measurements.get(i);
            if (value < lowerBound || value > upperBound) {
                outlierIndices.add(i);
            }
        }

        // Additional z-score based outlier detection
        double mean = calculateMean(measurements);
        double stdDev = calculateStandardDeviation(measurements, mean);

        if (stdDev > 0) {
            for (int i = 0; i < measurements.size(); i++) {
                double zScore = Math.abs((measurements.get(i) - mean) / stdDev);
                if (zScore > config.getOutlierThreshold() && !outlierIndices.contains(i)) {
                    outlierIndices.add(i);
                }
            }
        }

        return outlierIndices;
    }

    // Private helper methods

    private double calculateMean(List<Double> values) {
        return values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }

    private double calculateStandardDeviation(List<Double> values, double mean) {
        if (values.size() <= 1) return 0.0;

        double sumSquaredDiffs = values.stream()
                .mapToDouble(value -> Math.pow(value - mean, 2))
                .sum();
        return Math.sqrt(sumSquaredDiffs / (values.size() - 1));
    }

    private double calculateCoefficientOfVariation(List<Double> values) {
        double mean = calculateMean(values);
        if (mean == 0) return Double.MAX_VALUE;

        double stdDev = calculateStandardDeviation(values, mean);
        return stdDev / Math.abs(mean);
    }

    private double calculateTrendSlope(List<Double> values) {
        if (values.size() < 2) return 0.0;

        double n = values.size();
        double sumX = n * (n - 1) / 2; // Sum of indices
        double sumY = values.stream().mapToDouble(Double::doubleValue).sum();
        double sumXY = 0.0;
        double sumXX = (n - 1) * n * (2 * n - 1) / 6;

        for (int i = 0; i < values.size(); i++) {
            sumXY += i * values.get(i);
        }

        double numerator = n * sumXY - sumX * sumY;
        double denominator = n * sumXX - sumX * sumX;

        return denominator != 0 ? numerator / denominator : 0.0;
    }

    private double calculateTrendSignificance(List<Double> values, double slope) {
        if (values.size() < 3) return 0.0;

        double mean = calculateMean(values);
        double stdError = calculateStandardDeviation(values, mean) / Math.sqrt(values.size());

        if (stdError == 0) return 0.0;

        // t-statistic for slope significance
        return Math.abs(slope / stdError);
    }

    private int detectChangePoint(List<Double> measurements) {
        if (measurements.size() < 6) return -1;

        int bestChangePoint = -1;
        double maxTestStatistic = 0.0;

        // CUSUM-based change point detection
        for (int k = 2; k < measurements.size() - 2; k++) {
            double testStatistic = calculateCUSUMStatistic(measurements, k);
            if (testStatistic > maxTestStatistic) {
                maxTestStatistic = testStatistic;
                bestChangePoint = k;
            }
        }

        // Threshold for significant change point
        double threshold = 2.0; // Conservative threshold
        return maxTestStatistic > threshold ? bestChangePoint : -1;
    }

    private double calculateCUSUMStatistic(List<Double> measurements, int changePoint) {
        List<Double> before = measurements.subList(0, changePoint);
        List<Double> after = measurements.subList(changePoint, measurements.size());

        if (before.isEmpty() || after.isEmpty()) return 0.0;

        double meanBefore = calculateMean(before);
        double meanAfter = calculateMean(after);
        double overallMean = calculateMean(measurements);

        double varBefore = calculateVariance(before);
        double varAfter = calculateVariance(after);
        double pooledVar = ((before.size() - 1) * varBefore + (after.size() - 1) * varAfter) /
                          (measurements.size() - 2);

        if (pooledVar == 0) return 0.0;

        // CUSUM test statistic
        double statistic = (before.size() * Math.pow(meanBefore - overallMean, 2) +
                           after.size() * Math.pow(meanAfter - overallMean, 2)) / pooledVar;

        return statistic;
    }

    private double calculateVariance(List<Double> values) {
        if (values.size() <= 1) return 0.0;

        double mean = calculateMean(values);
        return values.stream()
                .mapToDouble(value -> Math.pow(value - mean, 2))
                .sum() / (values.size() - 1);
    }

    private boolean testStationarity(List<Double> measurements) {
        if (measurements.size() < 10) return true; // Assume stationary for small samples

        // Simple stationarity test: compare means and variances of different segments
        int segmentSize = measurements.size() / 3;
        if (segmentSize < 2) return true;

        List<Double> firstSegment = measurements.subList(0, segmentSize);
        List<Double> lastSegment = measurements.subList(measurements.size() - segmentSize, measurements.size());

        double meanDiff = Math.abs(calculateMean(firstSegment) - calculateMean(lastSegment));
        double varDiff = Math.abs(calculateVariance(firstSegment) - calculateVariance(lastSegment));

        double overallMean = calculateMean(measurements);
        double overallVar = calculateVariance(measurements);

        // Stationarity criteria: means and variances should be similar
        boolean meanStationary = overallMean == 0 || meanDiff / Math.abs(overallMean) < 0.2;
        boolean varStationary = overallVar == 0 || varDiff / overallVar < 0.5;

        return meanStationary && varStationary;
    }

    private double calculateStabilityScore(double cv, double trendMagnitude, boolean hasChangePoint,
                                         boolean isStationary, double trendSignificance) {
        double score = 1.0;

        // Penalize high coefficient of variation
        score *= Math.exp(-cv / config.getCoefficientOfVariationThreshold());

        // Penalize significant trends
        score *= Math.exp(-trendSignificance / 2.0);

        // Penalize change points
        if (hasChangePoint) {
            score *= 0.5;
        }

        // Penalize non-stationary behavior
        if (!isStationary) {
            score *= 0.7;
        }

        return Math.max(0.0, Math.min(1.0, score));
    }

    private String buildStabilityReason(boolean cvStable, boolean trendStable,
                                      boolean noSignificantChanges, boolean sufficientlyStationary) {
        List<String> issues = new ArrayList<>();

        if (!cvStable) issues.add("high coefficient of variation");
        if (!trendStable) issues.add("significant trend detected");
        if (!noSignificantChanges) issues.add("change point detected");
        if (!sufficientlyStationary) issues.add("non-stationary behavior");

        if (issues.isEmpty()) {
            return "Measurements are stable";
        } else {
            return "Issues: " + String.join(", ", issues);
        }
    }

    private double calculatePercentile(double[] sortedValues, double percentile) {
        if (sortedValues.length == 0) return 0.0;
        if (sortedValues.length == 1) return sortedValues[0];

        double index = percentile * (sortedValues.length - 1);
        int lowerIndex = (int) Math.floor(index);
        int upperIndex = (int) Math.ceil(index);

        if (lowerIndex == upperIndex) {
            return sortedValues[lowerIndex];
        } else {
            double weight = index - lowerIndex;
            return sortedValues[lowerIndex] * (1 - weight) + sortedValues[upperIndex] * weight;
        }
    }
}