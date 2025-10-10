package org.techishthoughts.jackson.benchmark.statistical;

import java.util.List;

/**
 * Calculates confidence intervals for benchmark measurements using various statistical methods.
 * Supports t-distribution based intervals, bootstrap confidence intervals, and robust estimators.
 */
public class ConfidenceIntervalCalculator {

    private final double confidenceLevel;
    private final double alpha;

    public ConfidenceIntervalCalculator(double confidenceLevel) {
        if (confidenceLevel <= 0 || confidenceLevel >= 1) {
            throw new IllegalArgumentException("Confidence level must be between 0 and 1");
        }
        this.confidenceLevel = confidenceLevel;
        this.alpha = 1 - confidenceLevel;
    }

    /**
     * Represents a confidence interval with lower and upper bounds.
     */
    public static class ConfidenceInterval {
        private final double lowerBound;
        private final double upperBound;
        private final double mean;
        private final double marginOfError;
        private final double confidenceLevel;
        private final String method;

        public ConfidenceInterval(double lowerBound, double upperBound, double mean,
                                double marginOfError, double confidenceLevel, String method) {
            this.lowerBound = lowerBound;
            this.upperBound = upperBound;
            this.mean = mean;
            this.marginOfError = marginOfError;
            this.confidenceLevel = confidenceLevel;
            this.method = method;
        }

        public double getLowerBound() { return lowerBound; }
        public double getUpperBound() { return upperBound; }
        public double getMean() { return mean; }
        public double getMarginOfError() { return marginOfError; }
        public double getConfidenceLevel() { return confidenceLevel; }
        public String getMethod() { return method; }
        public double getWidth() { return upperBound - lowerBound; }
        public double getRelativeWidth() { return getWidth() / Math.abs(mean); }

        public boolean contains(double value) {
            return value >= lowerBound && value <= upperBound;
        }

        @Override
        public String toString() {
            return String.format("CI_%s(%.1f%%): [%.6f, %.6f], mean=%.6f, width=%.6f",
                    method, confidenceLevel * 100, lowerBound, upperBound, mean, getWidth());
        }
    }

    /**
     * Calculates a t-distribution based confidence interval for the mean.
     */
    public ConfidenceInterval calculateTConfidenceInterval(List<Double> measurements) {
        if (measurements.size() < 2) {
            double value = measurements.isEmpty() ? 0.0 : measurements.get(0);
            return new ConfidenceInterval(value, value, value, 0.0, confidenceLevel, "t-dist");
        }

        double mean = calculateMean(measurements);
        double stdDev = calculateStandardDeviation(measurements, mean);
        int n = measurements.size();
        int degreesOfFreedom = n - 1;

        double tCritical = calculateTCriticalValue(degreesOfFreedom, alpha / 2);
        double standardError = stdDev / Math.sqrt(n);
        double marginOfError = tCritical * standardError;

        double lowerBound = mean - marginOfError;
        double upperBound = mean + marginOfError;

        return new ConfidenceInterval(lowerBound, upperBound, mean,
                                    marginOfError, confidenceLevel, "t-dist");
    }

    /**
     * Calculates a bootstrap confidence interval using percentile method.
     */
    public ConfidenceInterval calculateBootstrapConfidenceInterval(List<Double> measurements, int bootstrapSamples) {
        if (measurements.size() < 2) {
            double value = measurements.isEmpty() ? 0.0 : measurements.get(0);
            return new ConfidenceInterval(value, value, value, 0.0, confidenceLevel, "bootstrap");
        }

        double[] bootstrapMeans = new double[bootstrapSamples];
        int n = measurements.size();

        // Generate bootstrap samples
        for (int i = 0; i < bootstrapSamples; i++) {
            double sum = 0;
            for (int j = 0; j < n; j++) {
                int randomIndex = (int) (Math.random() * n);
                sum += measurements.get(randomIndex);
            }
            bootstrapMeans[i] = sum / n;
        }

        // Sort bootstrap means
        java.util.Arrays.sort(bootstrapMeans);

        // Calculate percentiles
        double lowerPercentile = alpha / 2;
        double upperPercentile = 1 - alpha / 2;

        int lowerIndex = (int) Math.ceil(lowerPercentile * bootstrapSamples) - 1;
        int upperIndex = (int) Math.floor(upperPercentile * bootstrapSamples) - 1;

        lowerIndex = Math.max(0, Math.min(bootstrapSamples - 1, lowerIndex));
        upperIndex = Math.max(0, Math.min(bootstrapSamples - 1, upperIndex));

        double lowerBound = bootstrapMeans[lowerIndex];
        double upperBound = bootstrapMeans[upperIndex];
        double mean = calculateMean(measurements);
        double marginOfError = Math.max(mean - lowerBound, upperBound - mean);

        return new ConfidenceInterval(lowerBound, upperBound, mean,
                                    marginOfError, confidenceLevel, "bootstrap");
    }

    /**
     * Calculates a robust confidence interval using median and MAD (Median Absolute Deviation).
     */
    public ConfidenceInterval calculateRobustConfidenceInterval(List<Double> measurements) {
        if (measurements.isEmpty()) {
            return new ConfidenceInterval(0.0, 0.0, 0.0, 0.0, confidenceLevel, "robust");
        }

        if (measurements.size() == 1) {
            double value = measurements.get(0);
            return new ConfidenceInterval(value, value, value, 0.0, confidenceLevel, "robust");
        }

        // Calculate median
        double[] sortedValues = measurements.stream()
                .mapToDouble(Double::doubleValue)
                .sorted()
                .toArray();

        double median = calculateMedian(sortedValues);

        // Calculate MAD (Median Absolute Deviation)
        double[] deviations = new double[sortedValues.length];
        for (int i = 0; i < sortedValues.length; i++) {
            deviations[i] = Math.abs(sortedValues[i] - median);
        }
        java.util.Arrays.sort(deviations);
        double mad = calculateMedian(deviations);

        // Scale MAD to approximate standard deviation
        double scaledMAD = mad * 1.4826; // Consistency factor for normal distribution

        // Calculate confidence interval
        double criticalValue = calculateTCriticalValue(measurements.size() - 1, alpha / 2);
        double marginOfError = criticalValue * scaledMAD / Math.sqrt(measurements.size());

        double lowerBound = median - marginOfError;
        double upperBound = median + marginOfError;

        return new ConfidenceInterval(lowerBound, upperBound, median,
                                    marginOfError, confidenceLevel, "robust");
    }

    /**
     * Calculates a confidence interval for the variance using chi-square distribution.
     */
    public ConfidenceInterval calculateVarianceConfidenceInterval(List<Double> measurements) {
        if (measurements.size() < 2) {
            return new ConfidenceInterval(0.0, Double.POSITIVE_INFINITY, 0.0,
                                        Double.POSITIVE_INFINITY, confidenceLevel, "variance");
        }

        double mean = calculateMean(measurements);
        double variance = calculateVariance(measurements, mean);
        int n = measurements.size();
        int degreesOfFreedom = n - 1;

        // Chi-square critical values
        double chiSquareLower = calculateChiSquareCriticalValue(degreesOfFreedom, 1 - alpha / 2);
        double chiSquareUpper = calculateChiSquareCriticalValue(degreesOfFreedom, alpha / 2);

        // Confidence interval for variance
        double lowerBound = (degreesOfFreedom * variance) / chiSquareLower;
        double upperBound = (degreesOfFreedom * variance) / chiSquareUpper;
        double marginOfError = Math.max(variance - lowerBound, upperBound - variance);

        return new ConfidenceInterval(lowerBound, upperBound, variance,
                                    marginOfError, confidenceLevel, "variance");
    }

    /**
     * Determines if the confidence interval width meets the desired precision criteria.
     */
    public boolean meetsPrecisionCriteria(ConfidenceInterval interval, double maxRelativeWidth) {
        return interval.getRelativeWidth() <= maxRelativeWidth;
    }

    /**
     * Estimates the sample size needed to achieve a desired margin of error.
     */
    public int estimateRequiredSampleSize(List<Double> measurements, double desiredMarginOfError) {
        if (measurements.size() < 2) {
            return 100; // Default conservative estimate
        }

        double mean = calculateMean(measurements);
        double stdDev = calculateStandardDeviation(measurements, mean);

        // Approximate critical value for large samples
        double zCritical = calculateZCriticalValue(alpha / 2);

        // Sample size formula: n = (z * σ / E)²
        double requiredN = Math.pow((zCritical * stdDev) / desiredMarginOfError, 2);

        return Math.max(2, (int) Math.ceil(requiredN));
    }

    // Private helper methods

    private double calculateMean(List<Double> values) {
        return values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }

    private double calculateStandardDeviation(List<Double> values, double mean) {
        if (values.size() <= 1) return 0.0;

        double variance = calculateVariance(values, mean);
        return Math.sqrt(variance);
    }

    private double calculateVariance(List<Double> values, double mean) {
        if (values.size() <= 1) return 0.0;

        double sumSquaredDiffs = values.stream()
                .mapToDouble(value -> Math.pow(value - mean, 2))
                .sum();
        return sumSquaredDiffs / (values.size() - 1);
    }

    private double calculateMedian(double[] sortedValues) {
        int n = sortedValues.length;
        if (n == 0) return 0.0;
        if (n % 2 == 1) {
            return sortedValues[n / 2];
        } else {
            return (sortedValues[n / 2 - 1] + sortedValues[n / 2]) / 2.0;
        }
    }

    private double calculateTCriticalValue(int degreesOfFreedom, double alpha) {
        // Approximation of t-distribution critical values
        // For production use, consider Apache Commons Math

        if (degreesOfFreedom >= 30) {
            // Use normal approximation for large degrees of freedom
            return calculateZCriticalValue(alpha);
        }

        // Simple approximation for small degrees of freedom
        double z = calculateZCriticalValue(alpha);
        double correction = z / (4 * degreesOfFreedom);
        return z + correction;
    }

    private double calculateZCriticalValue(double alpha) {
        // Standard normal critical values approximation
        if (alpha <= 0.001) return 3.291;
        if (alpha <= 0.005) return 2.576;
        if (alpha <= 0.01) return 2.326;
        if (alpha <= 0.025) return 1.960;
        if (alpha <= 0.05) return 1.645;
        if (alpha <= 0.1) return 1.282;

        // Linear interpolation for other values
        return -Math.sqrt(-2 * Math.log(alpha));
    }

    private double calculateChiSquareCriticalValue(int degreesOfFreedom, double alpha) {
        // Approximation of chi-square critical values
        // Wilson-Hilferty approximation

        double h = 2.0 / (9.0 * degreesOfFreedom);
        double z = calculateZCriticalValue(alpha);

        double chiSquareApprox = degreesOfFreedom * Math.pow(1 - h + z * Math.sqrt(h), 3);

        return Math.max(0, chiSquareApprox);
    }
}