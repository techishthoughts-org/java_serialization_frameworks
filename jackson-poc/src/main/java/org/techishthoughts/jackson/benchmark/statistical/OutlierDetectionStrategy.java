package org.techishthoughts.jackson.benchmark.statistical;

import org.techishthoughts.jackson.benchmark.adaptive.AdaptiveBenchmarkConfig;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Advanced outlier detection strategy using multiple statistical methods
 * including IQR, Modified Z-Score, Isolation Forest approximation, and robust estimators.
 * Provides both detection and handling strategies for benchmark measurements.
 */
public class OutlierDetectionStrategy {

    private final AdaptiveBenchmarkConfig config;
    private final Map<DetectionMethod, Double> methodWeights;

    public enum DetectionMethod {
        IQR("Interquartile Range"),
        MODIFIED_Z_SCORE("Modified Z-Score"),
        GRUBBS_TEST("Grubbs Test"),
        DIXON_TEST("Dixon Test"),
        HAMPEL_FILTER("Hampel Filter"),
        ISOLATION_APPROXIMATION("Isolation Forest Approximation");

        private final String description;
        DetectionMethod(String description) { this.description = description; }
        public String getDescription() { return description; }
    }

    public enum HandlingStrategy {
        REMOVE,          // Remove outliers completely
        WINSORIZE,       // Replace with nearest non-outlier value
        TRANSFORM,       // Apply transformation to reduce impact
        ROBUST_STATS,    // Use robust statistical measures
        MARK_ONLY        // Mark but keep in dataset
    }

    public OutlierDetectionStrategy(AdaptiveBenchmarkConfig config) {
        this.config = config;
        this.methodWeights = initializeMethodWeights();
    }

    /**
     * Comprehensive outlier detection result.
     */
    public static class OutlierAnalysis {
        private final List<Integer> outlierIndices;
        private final Map<DetectionMethod, List<Integer>> methodResults;
        private final Map<Integer, Double> outlierScores;
        private final double outlierRate;
        private final HandlingStrategy recommendedStrategy;
        private final String analysisReason;
        private final Map<String, Object> statistics;

        public OutlierAnalysis(List<Integer> outlierIndices, Map<DetectionMethod, List<Integer>> methodResults,
                             Map<Integer, Double> outlierScores, double outlierRate,
                             HandlingStrategy recommendedStrategy, String analysisReason,
                             Map<String, Object> statistics) {
            this.outlierIndices = new ArrayList<>(outlierIndices);
            this.methodResults = new HashMap<>(methodResults);
            this.outlierScores = new HashMap<>(outlierScores);
            this.outlierRate = outlierRate;
            this.recommendedStrategy = recommendedStrategy;
            this.analysisReason = analysisReason;
            this.statistics = new HashMap<>(statistics);
        }

        public List<Integer> getOutlierIndices() { return outlierIndices; }
        public Map<DetectionMethod, List<Integer>> getMethodResults() { return methodResults; }
        public Map<Integer, Double> getOutlierScores() { return outlierScores; }
        public double getOutlierRate() { return outlierRate; }
        public HandlingStrategy getRecommendedStrategy() { return recommendedStrategy; }
        public String getAnalysisReason() { return analysisReason; }
        public Map<String, Object> getStatistics() { return statistics; }

        public boolean hasOutliers() { return !outlierIndices.isEmpty(); }
        public int getOutlierCount() { return outlierIndices.size(); }

        @Override
        public String toString() {
            return String.format("OutlierAnalysis{count=%d, rate=%.3f, strategy=%s, reason='%s'}",
                    outlierIndices.size(), outlierRate, recommendedStrategy, analysisReason);
        }
    }

    /**
     * Performs comprehensive outlier detection using multiple methods.
     */
    public OutlierAnalysis detectOutliers(List<Double> measurements) {
        if (measurements.size() < 4) {
            return new OutlierAnalysis(Collections.emptyList(), Collections.emptyMap(),
                                     Collections.emptyMap(), 0.0, HandlingStrategy.MARK_ONLY,
                                     "Insufficient data for outlier detection",
                                     Collections.emptyMap());
        }

        Map<DetectionMethod, List<Integer>> methodResults = new HashMap<>();
        Map<String, Object> statistics = new HashMap<>();

        // Apply each detection method
        methodResults.put(DetectionMethod.IQR, detectOutliersIQR(measurements));
        methodResults.put(DetectionMethod.MODIFIED_Z_SCORE, detectOutliersModifiedZScore(measurements));
        methodResults.put(DetectionMethod.GRUBBS_TEST, detectOutliersGrubbs(measurements));
        methodResults.put(DetectionMethod.DIXON_TEST, detectOutliersDixon(measurements));
        methodResults.put(DetectionMethod.HAMPEL_FILTER, detectOutliersHampel(measurements));
        methodResults.put(DetectionMethod.ISOLATION_APPROXIMATION, detectOutliersIsolation(measurements));

        // Calculate consensus outliers with scores
        Map<Integer, Double> outlierScores = calculateConsensusScores(methodResults, measurements.size());

        // Determine final outlier list based on threshold
        double outlierThreshold = 0.3; // At least 30% of methods must agree
        List<Integer> finalOutliers = outlierScores.entrySet().stream()
                .filter(entry -> entry.getValue() >= outlierThreshold)
                .map(Map.Entry::getKey)
                .sorted()
                .collect(Collectors.toList());

        double outlierRate = (double) finalOutliers.size() / measurements.size();

        // Determine recommended handling strategy
        HandlingStrategy strategy = recommendHandlingStrategy(outlierRate, measurements.size(), outlierScores);
        String reason = buildAnalysisReason(methodResults, outlierRate, strategy);

        // Calculate statistics
        statistics.put("totalMeasurements", measurements.size());
        statistics.put("outliersFound", finalOutliers.size());
        statistics.put("outlierRate", outlierRate);
        statistics.put("consensusThreshold", outlierThreshold);
        statistics.putAll(calculateOutlierStatistics(measurements, finalOutliers));

        return new OutlierAnalysis(finalOutliers, methodResults, outlierScores, outlierRate,
                                 strategy, reason, statistics);
    }

    /**
     * Applies the recommended handling strategy to the measurements.
     */
    public List<Double> handleOutliers(List<Double> measurements, OutlierAnalysis analysis) {
        if (!analysis.hasOutliers()) {
            return new ArrayList<>(measurements);
        }

        switch (analysis.getRecommendedStrategy()) {
            case REMOVE:
                return removeOutliers(measurements, analysis.getOutlierIndices());
            case WINSORIZE:
                return winsorizeOutliers(measurements, analysis.getOutlierIndices());
            case TRANSFORM:
                return transformMeasurements(measurements, analysis.getOutlierIndices());
            case ROBUST_STATS:
                // For robust stats, return original data but with metadata
                return new ArrayList<>(measurements);
            case MARK_ONLY:
            default:
                return new ArrayList<>(measurements);
        }
    }

    /**
     * Calculates robust statistics that are resistant to outliers.
     */
    public Map<String, Double> calculateRobustStatistics(List<Double> measurements, List<Integer> outlierIndices) {
        Map<String, Double> stats = new HashMap<>();

        if (measurements.isEmpty()) {
            return stats;
        }

        // Clean data (without outliers)
        List<Double> cleanData = removeOutliers(measurements, outlierIndices);

        if (!cleanData.isEmpty()) {
            stats.put("robustMean", calculateMean(cleanData));
            stats.put("robustStdDev", calculateStandardDeviation(cleanData));
        }

        // Median-based statistics (robust to outliers)
        double[] sortedValues = measurements.stream().mapToDouble(Double::doubleValue).sorted().toArray();
        stats.put("median", calculateMedian(sortedValues));
        stats.put("mad", calculateMAD(measurements)); // Median Absolute Deviation
        stats.put("iqr", calculateIQR(sortedValues));

        // Trimmed statistics
        stats.put("trimmedMean", calculateTrimmedMean(measurements, 0.1)); // 10% trimmed mean

        // Robust coefficient of variation
        double median = stats.get("median");
        double mad = stats.get("mad");
        if (median != 0 && mad != 0) {
            stats.put("robustCV", (mad * 1.4826) / Math.abs(median)); // 1.4826 is consistency factor
        }

        return stats;
    }

    // Private detection methods

    private List<Integer> detectOutliersIQR(List<Double> measurements) {
        double[] sortedValues = measurements.stream().mapToDouble(Double::doubleValue).sorted().toArray();

        double q1 = calculatePercentile(sortedValues, 0.25);
        double q3 = calculatePercentile(sortedValues, 0.75);
        double iqr = q3 - q1;

        double lowerBound = q1 - 1.5 * iqr;
        double upperBound = q3 + 1.5 * iqr;

        List<Integer> outliers = new ArrayList<>();
        for (int i = 0; i < measurements.size(); i++) {
            double value = measurements.get(i);
            if (value < lowerBound || value > upperBound) {
                outliers.add(i);
            }
        }
        return outliers;
    }

    private List<Integer> detectOutliersModifiedZScore(List<Double> measurements) {
        double median = calculateMedian(measurements.stream().mapToDouble(Double::doubleValue).sorted().toArray());
        double mad = calculateMAD(measurements);

        if (mad == 0) return Collections.emptyList();

        List<Integer> outliers = new ArrayList<>();
        double threshold = config.getOutlierThreshold(); // typically 3.5

        for (int i = 0; i < measurements.size(); i++) {
            double modifiedZScore = 0.6745 * (measurements.get(i) - median) / mad;
            if (Math.abs(modifiedZScore) > threshold) {
                outliers.add(i);
            }
        }
        return outliers;
    }

    private List<Integer> detectOutliersGrubbs(List<Double> measurements) {
        if (measurements.size() < 3) return Collections.emptyList();

        double mean = calculateMean(measurements);
        double stdDev = calculateStandardDeviation(measurements);

        if (stdDev == 0) return Collections.emptyList();

        // Grubbs critical value approximation for sample size
        double criticalValue = calculateGrubbsCriticalValue(measurements.size());

        List<Integer> outliers = new ArrayList<>();
        for (int i = 0; i < measurements.size(); i++) {
            double grubbsStatistic = Math.abs(measurements.get(i) - mean) / stdDev;
            if (grubbsStatistic > criticalValue) {
                outliers.add(i);
            }
        }
        return outliers;
    }

    private List<Integer> detectOutliersDixon(List<Double> measurements) {
        if (measurements.size() < 3 || measurements.size() > 30) {
            return Collections.emptyList(); // Dixon test works best for small samples
        }

        double[] sortedValues = measurements.stream().mapToDouble(Double::doubleValue).sorted().toArray();
        int n = sortedValues.length;

        // Dixon Q-test critical values (approximation)
        double criticalValue = calculateDixonCriticalValue(n);

        List<Integer> outliers = new ArrayList<>();

        // Test smallest value
        if (n > 3) {
            double q = (sortedValues[1] - sortedValues[0]) / (sortedValues[n-1] - sortedValues[0]);
            if (q > criticalValue) {
                // Find index of smallest value in original array
                for (int i = 0; i < measurements.size(); i++) {
                    if (Math.abs(measurements.get(i) - sortedValues[0]) < 1e-10) {
                        outliers.add(i);
                        break;
                    }
                }
            }
        }

        // Test largest value
        if (n > 3) {
            double q = (sortedValues[n-1] - sortedValues[n-2]) / (sortedValues[n-1] - sortedValues[0]);
            if (q > criticalValue) {
                // Find index of largest value in original array
                for (int i = 0; i < measurements.size(); i++) {
                    if (Math.abs(measurements.get(i) - sortedValues[n-1]) < 1e-10) {
                        outliers.add(i);
                        break;
                    }
                }
            }
        }

        return outliers;
    }

    private List<Integer> detectOutliersHampel(List<Double> measurements) {
        if (measurements.size() < 7) return Collections.emptyList();

        List<Integer> outliers = new ArrayList<>();
        int windowSize = Math.min(7, measurements.size() / 3); // Adaptive window size

        for (int i = 0; i < measurements.size(); i++) {
            // Define window around current point
            int start = Math.max(0, i - windowSize / 2);
            int end = Math.min(measurements.size(), i + windowSize / 2 + 1);

            List<Double> window = measurements.subList(start, end);
            double median = calculateMedian(window.stream().mapToDouble(Double::doubleValue).sorted().toArray());
            double mad = calculateMAD(window);

            if (mad > 0) {
                double hampelScore = 0.6745 * Math.abs(measurements.get(i) - median) / mad;
                if (hampelScore > config.getOutlierThreshold()) {
                    outliers.add(i);
                }
            }
        }

        return outliers;
    }

    private List<Integer> detectOutliersIsolation(List<Double> measurements) {
        // Simplified isolation forest approximation
        // In a real implementation, you'd use a proper isolation forest algorithm

        if (measurements.size() < 8) return Collections.emptyList();

        List<Integer> outliers = new ArrayList<>();
        double mean = calculateMean(measurements);
        double stdDev = calculateStandardDeviation(measurements);

        if (stdDev == 0) return Collections.emptyList();

        // Simple isolation score approximation
        for (int i = 0; i < measurements.size(); i++) {
            double isolationScore = calculateIsolationScore(measurements.get(i), measurements);
            if (isolationScore > 0.7) { // Threshold for isolation
                outliers.add(i);
            }
        }

        return outliers;
    }

    // Private helper methods

    private Map<Integer, Double> calculateConsensusScores(Map<DetectionMethod, List<Integer>> methodResults, int totalSize) {
        Map<Integer, Double> scores = new HashMap<>();

        for (int i = 0; i < totalSize; i++) {
            double score = 0.0;
            double totalWeight = 0.0;

            for (Map.Entry<DetectionMethod, List<Integer>> entry : methodResults.entrySet()) {
                double weight = methodWeights.getOrDefault(entry.getKey(), 1.0);
                totalWeight += weight;

                if (entry.getValue().contains(i)) {
                    score += weight;
                }
            }

            if (totalWeight > 0) {
                scores.put(i, score / totalWeight);
            }
        }

        return scores;
    }

    private HandlingStrategy recommendHandlingStrategy(double outlierRate, int sampleSize,
                                                     Map<Integer, Double> outlierScores) {
        if (outlierRate == 0) {
            return HandlingStrategy.MARK_ONLY;
        }

        if (outlierRate > 0.2) { // More than 20% outliers
            return HandlingStrategy.ROBUST_STATS;
        }

        if (outlierRate > 0.1) { // 10-20% outliers
            return HandlingStrategy.WINSORIZE;
        }

        if (sampleSize < 50) { // Small sample
            return HandlingStrategy.WINSORIZE;
        }

        // Check severity of outliers
        double maxOutlierScore = outlierScores.values().stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
        if (maxOutlierScore > 0.8) { // Very severe outliers
            return HandlingStrategy.REMOVE;
        }

        return HandlingStrategy.WINSORIZE;
    }

    private List<Double> removeOutliers(List<Double> measurements, List<Integer> outlierIndices) {
        Set<Integer> outlierSet = new HashSet<>(outlierIndices);
        List<Double> cleaned = new ArrayList<>();

        for (int i = 0; i < measurements.size(); i++) {
            if (!outlierSet.contains(i)) {
                cleaned.add(measurements.get(i));
            }
        }

        return cleaned;
    }

    private List<Double> winsorizeOutliers(List<Double> measurements, List<Integer> outlierIndices) {
        if (outlierIndices.isEmpty()) {
            return new ArrayList<>(measurements);
        }

        List<Double> winsorized = new ArrayList<>(measurements);
        double[] sortedClean = removeOutliers(measurements, outlierIndices).stream()
                .mapToDouble(Double::doubleValue).sorted().toArray();

        if (sortedClean.length == 0) {
            return winsorized;
        }

        double lowerBound = sortedClean[0];
        double upperBound = sortedClean[sortedClean.length - 1];

        for (int index : outlierIndices) {
            double value = measurements.get(index);
            if (value < lowerBound) {
                winsorized.set(index, lowerBound);
            } else if (value > upperBound) {
                winsorized.set(index, upperBound);
            }
        }

        return winsorized;
    }

    private List<Double> transformMeasurements(List<Double> measurements, List<Integer> outlierIndices) {
        // Apply log transformation to reduce impact of extreme values
        List<Double> transformed = new ArrayList<>();

        double minValue = measurements.stream().mapToDouble(Double::doubleValue).min().orElse(1.0);
        double offset = minValue <= 0 ? Math.abs(minValue) + 1 : 0;

        for (double value : measurements) {
            transformed.add(Math.log(value + offset));
        }

        return transformed;
    }

    private Map<String, Object> calculateOutlierStatistics(List<Double> measurements, List<Integer> outlierIndices) {
        Map<String, Object> stats = new HashMap<>();

        if (outlierIndices.isEmpty()) {
            return stats;
        }

        List<Double> outlierValues = outlierIndices.stream()
                .map(measurements::get)
                .collect(Collectors.toList());

        stats.put("outlierMean", calculateMean(outlierValues));
        stats.put("outlierStdDev", calculateStandardDeviation(outlierValues));
        stats.put("outlierMin", outlierValues.stream().mapToDouble(Double::doubleValue).min().orElse(0.0));
        stats.put("outlierMax", outlierValues.stream().mapToDouble(Double::doubleValue).max().orElse(0.0));

        return stats;
    }

    private String buildAnalysisReason(Map<DetectionMethod, List<Integer>> methodResults,
                                     double outlierRate, HandlingStrategy strategy) {
        if (outlierRate == 0) {
            return "No outliers detected by any method";
        }

        long methodsWithOutliers = methodResults.values().stream()
                .mapToLong(List::size)
                .filter(count -> count > 0)
                .count();

        return String.format("%.1f%% outliers detected by %d/%d methods, recommended strategy: %s",
                           outlierRate * 100, methodsWithOutliers, methodResults.size(), strategy);
    }

    private Map<DetectionMethod, Double> initializeMethodWeights() {
        Map<DetectionMethod, Double> weights = new HashMap<>();
        weights.put(DetectionMethod.IQR, 1.0);
        weights.put(DetectionMethod.MODIFIED_Z_SCORE, 1.2);
        weights.put(DetectionMethod.GRUBBS_TEST, 1.1);
        weights.put(DetectionMethod.DIXON_TEST, 0.8);
        weights.put(DetectionMethod.HAMPEL_FILTER, 1.0);
        weights.put(DetectionMethod.ISOLATION_APPROXIMATION, 0.9);
        return weights;
    }

    // Statistical calculation helpers

    private double calculateMean(List<Double> values) {
        return values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }

    private double calculateStandardDeviation(List<Double> values) {
        if (values.size() <= 1) return 0.0;

        double mean = calculateMean(values);
        double variance = values.stream()
                .mapToDouble(value -> Math.pow(value - mean, 2))
                .sum() / (values.size() - 1);
        return Math.sqrt(variance);
    }

    private double calculateMedian(double[] sortedValues) {
        if (sortedValues.length == 0) return 0.0;
        if (sortedValues.length % 2 == 1) {
            return sortedValues[sortedValues.length / 2];
        } else {
            return (sortedValues[sortedValues.length / 2 - 1] + sortedValues[sortedValues.length / 2]) / 2.0;
        }
    }

    private double calculateMAD(List<Double> measurements) {
        if (measurements.isEmpty()) return 0.0;

        double median = calculateMedian(measurements.stream().mapToDouble(Double::doubleValue).sorted().toArray());
        double[] deviations = measurements.stream()
                .mapToDouble(value -> Math.abs(value - median))
                .sorted()
                .toArray();

        return calculateMedian(deviations);
    }

    private double calculateIQR(double[] sortedValues) {
        if (sortedValues.length < 4) return 0.0;

        double q1 = calculatePercentile(sortedValues, 0.25);
        double q3 = calculatePercentile(sortedValues, 0.75);
        return q3 - q1;
    }

    private double calculateTrimmedMean(List<Double> measurements, double trimPercent) {
        if (measurements.size() < 4) return calculateMean(measurements);

        double[] sortedValues = measurements.stream().mapToDouble(Double::doubleValue).sorted().toArray();
        int trimCount = (int) (sortedValues.length * trimPercent);

        double sum = 0;
        int count = 0;
        for (int i = trimCount; i < sortedValues.length - trimCount; i++) {
            sum += sortedValues[i];
            count++;
        }

        return count > 0 ? sum / count : 0.0;
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

    private double calculateGrubbsCriticalValue(int n) {
        // Approximation of Grubbs critical values
        if (n < 3) return Double.MAX_VALUE;

        double tCritical = 2.0; // Simplified t-critical value approximation
        return (n - 1) / Math.sqrt(n) * Math.sqrt(tCritical * tCritical / (n - 2 + tCritical * tCritical));
    }

    private double calculateDixonCriticalValue(int n) {
        // Approximation of Dixon Q-test critical values at 95% confidence
        if (n <= 3) return 0.970;
        if (n <= 7) return 0.568;
        if (n <= 10) return 0.466;
        if (n <= 14) return 0.546;
        return 0.525; // For larger samples
    }

    private double calculateIsolationScore(double value, List<Double> measurements) {
        // Simplified isolation score - distance from cluster center
        double mean = calculateMean(measurements);
        double stdDev = calculateStandardDeviation(measurements);

        if (stdDev == 0) return 0.0;

        double zScore = Math.abs(value - mean) / stdDev;
        return Math.tanh(zScore / 3.0); // Normalize to [0,1] range
    }
}