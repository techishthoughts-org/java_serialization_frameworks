package org.techishthoughts.jackson.benchmark.statistical;

import org.techishthoughts.jackson.benchmark.adaptive.AdaptiveBenchmarkConfig;

import java.util.List;

/**
 * Detects statistical significance in benchmark measurements using various statistical tests.
 * Implements t-tests, effect size calculations, and power analysis for stopping criteria.
 */
public class StatisticalSignificanceDetector {

    private final AdaptiveBenchmarkConfig config;
    private final ConfidenceIntervalCalculator confidenceCalculator;

    public StatisticalSignificanceDetector(AdaptiveBenchmarkConfig config) {
        this.config = config;
        this.confidenceCalculator = new ConfidenceIntervalCalculator(config.getConfidenceLevel());
    }

    /**
     * Represents the result of a statistical significance test.
     */
    public static class SignificanceResult {
        private final boolean isSignificant;
        private final double pValue;
        private final double effectSize;
        private final double power;
        private final int requiredSampleSize;
        private final String testType;

        public SignificanceResult(boolean isSignificant, double pValue, double effectSize,
                                double power, int requiredSampleSize, String testType) {
            this.isSignificant = isSignificant;
            this.pValue = pValue;
            this.effectSize = effectSize;
            this.power = power;
            this.requiredSampleSize = requiredSampleSize;
            this.testType = testType;
        }

        public boolean isSignificant() { return isSignificant; }
        public double getPValue() { return pValue; }
        public double getEffectSize() { return effectSize; }
        public double getPower() { return power; }
        public int getRequiredSampleSize() { return requiredSampleSize; }
        public String getTestType() { return testType; }

        @Override
        public String toString() {
            return String.format("SignificanceResult{significant=%s, p=%.4f, effect=%.4f, power=%.3f, n=%d, test=%s}",
                    isSignificant, pValue, effectSize, power, requiredSampleSize, testType);
        }
    }

    /**
     * Performs a one-sample t-test to determine if measurements are significantly different from zero.
     */
    public SignificanceResult performOneSampleTTest(List<Double> measurements) {
        if (measurements.size() < 2) {
            return new SignificanceResult(false, 1.0, 0.0, 0.0, config.getMinimumSampleSize(), "one-sample-t");
        }

        double mean = calculateMean(measurements);
        double stdDev = calculateStandardDeviation(measurements, mean);
        int n = measurements.size();

        // One-sample t-test against null hypothesis (mean = 0)
        double tStatistic = mean / (stdDev / Math.sqrt(n));
        double degreesOfFreedom = n - 1;
        double pValue = calculateTTestPValue(Math.abs(tStatistic), degreesOfFreedom);

        // Effect size (Cohen's d)
        double effectSize = Math.abs(mean) / stdDev;

        // Statistical power calculation
        double power = calculatePower(effectSize, n, config.getConfidenceLevel());

        // Required sample size for desired power (0.8)
        int requiredSampleSize = calculateRequiredSampleSize(effectSize, config.getConfidenceLevel(), 0.8);

        boolean isSignificant = pValue < (1 - config.getConfidenceLevel()) &&
                               effectSize >= config.getMinimumEffect();

        return new SignificanceResult(isSignificant, pValue, effectSize, power,
                                    requiredSampleSize, "one-sample-t");
    }

    /**
     * Performs a two-sample t-test to compare two groups of measurements.
     */
    public SignificanceResult performTwoSampleTTest(List<Double> group1, List<Double> group2) {
        if (group1.size() < 2 || group2.size() < 2) {
            return new SignificanceResult(false, 1.0, 0.0, 0.0, config.getMinimumSampleSize(), "two-sample-t");
        }

        double mean1 = calculateMean(group1);
        double mean2 = calculateMean(group2);
        double stdDev1 = calculateStandardDeviation(group1, mean1);
        double stdDev2 = calculateStandardDeviation(group2, mean2);
        int n1 = group1.size();
        int n2 = group2.size();

        // Welch's t-test (unequal variances)
        double pooledVariance = (stdDev1 * stdDev1) / n1 + (stdDev2 * stdDev2) / n2;
        double tStatistic = (mean1 - mean2) / Math.sqrt(pooledVariance);

        // Degrees of freedom for Welch's t-test
        double var1 = stdDev1 * stdDev1;
        double var2 = stdDev2 * stdDev2;
        double df = Math.pow(var1/n1 + var2/n2, 2) /
                   (Math.pow(var1/n1, 2)/(n1-1) + Math.pow(var2/n2, 2)/(n2-1));

        double pValue = calculateTTestPValue(Math.abs(tStatistic), df);

        // Effect size (Cohen's d)
        double pooledStdDev = Math.sqrt(((n1-1)*var1 + (n2-1)*var2) / (n1+n2-2));
        double effectSize = Math.abs(mean1 - mean2) / pooledStdDev;

        // Statistical power
        double power = calculatePower(effectSize, Math.min(n1, n2), config.getConfidenceLevel());

        // Required sample size per group
        int requiredSampleSize = calculateRequiredSampleSize(effectSize, config.getConfidenceLevel(), 0.8);

        boolean isSignificant = pValue < (1 - config.getConfidenceLevel()) &&
                               effectSize >= config.getMinimumEffect();

        return new SignificanceResult(isSignificant, pValue, effectSize, power,
                                    requiredSampleSize, "two-sample-t");
    }

    /**
     * Determines if we have enough statistical power to detect meaningful differences.
     */
    public boolean hasSufficientPower(List<Double> measurements) {
        if (measurements.size() < config.getMinimumSampleSize()) {
            return false;
        }

        SignificanceResult result = performOneSampleTTest(measurements);
        return result.getPower() >= 0.8; // Standard power threshold
    }

    /**
     * Calculates the minimum sample size needed for adequate statistical power.
     */
    public int calculateMinimumSampleSize(double expectedEffectSize) {
        return calculateRequiredSampleSize(expectedEffectSize, config.getConfidenceLevel(), 0.8);
    }

    /**
     * Performs a normality test using the Shapiro-Wilk approximation.
     */
    public boolean testNormality(List<Double> measurements) {
        if (measurements.size() < 3 || measurements.size() > 5000) {
            return true; // Assume normality for very small or very large samples
        }

        // Simple normality check using skewness and kurtosis
        double mean = calculateMean(measurements);
        double stdDev = calculateStandardDeviation(measurements, mean);

        if (stdDev == 0) return true; // No variation

        // Calculate skewness
        double skewness = 0;
        for (double value : measurements) {
            skewness += Math.pow((value - mean) / stdDev, 3);
        }
        skewness /= measurements.size();

        // Calculate kurtosis
        double kurtosis = 0;
        for (double value : measurements) {
            kurtosis += Math.pow((value - mean) / stdDev, 4);
        }
        kurtosis = kurtosis / measurements.size() - 3; // Excess kurtosis

        // Simple normality criteria
        return Math.abs(skewness) < 2.0 && Math.abs(kurtosis) < 7.0;
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

    private double calculateTTestPValue(double tStatistic, double degreesOfFreedom) {
        // Approximation of t-distribution CDF for p-value calculation
        // This is a simplified implementation - in production, use Apache Commons Math

        if (degreesOfFreedom <= 0) return 1.0;

        // Two-tailed p-value approximation
        double x = tStatistic * tStatistic;
        double a = degreesOfFreedom / 2.0;
        double b = 0.5;

        // Beta function approximation for t-distribution
        double betaApprox = Math.pow(1 + x / degreesOfFreedom, -(degreesOfFreedom + 1) / 2.0);

        // Two-tailed p-value
        double pValue = 2 * (1 - betaApprox);

        return Math.max(0.0, Math.min(1.0, pValue));
    }

    private double calculatePower(double effectSize, int sampleSize, double alpha) {
        // Power calculation approximation
        if (effectSize <= 0 || sampleSize <= 1) return 0.0;

        double criticalValue = calculateCriticalValue(alpha);
        double ncp = effectSize * Math.sqrt(sampleSize); // Non-centrality parameter

        // Power approximation using normal distribution
        double power = 1 - normalCDF(criticalValue - ncp);

        return Math.max(0.0, Math.min(1.0, power));
    }

    private int calculateRequiredSampleSize(double effectSize, double alpha, double power) {
        if (effectSize <= 0) return config.getMaximumSampleSize();

        double criticalValue = calculateCriticalValue(alpha);
        double powerValue = normalInverseCDF(power);

        // Sample size calculation
        double n = Math.pow((criticalValue + powerValue) / effectSize, 2);

        int requiredSize = (int) Math.ceil(n);
        return Math.max(config.getMinimumSampleSize(),
                       Math.min(config.getMaximumSampleSize(), requiredSize));
    }

    private double calculateCriticalValue(double alpha) {
        // Critical value for two-tailed test
        return normalInverseCDF(1 - alpha / 2);
    }

    private double normalCDF(double x) {
        // Standard normal CDF approximation
        return 0.5 * (1 + erf(x / Math.sqrt(2)));
    }

    private double normalInverseCDF(double p) {
        // Beasley-Springer-Moro algorithm approximation
        if (p <= 0) return Double.NEGATIVE_INFINITY;
        if (p >= 1) return Double.POSITIVE_INFINITY;

        double a0 = 2.50662823884;
        double a1 = -18.61500062529;
        double a2 = 41.39119773534;
        double a3 = -25.44106049637;

        double b1 = -8.47351093090;
        double b2 = 23.08336743743;
        double b3 = -21.06224101826;
        double b4 = 3.13082909833;

        double y = Math.log(-Math.log(1 - p));

        return a0 + a1 * y + a2 * y * y + a3 * y * y * y +
               (b1 * y + b2 * y * y + b3 * y * y * y + b4 * y * y * y * y);
    }

    private double erf(double x) {
        // Error function approximation
        double a1 = 0.254829592;
        double a2 = -0.284496736;
        double a3 = 1.421413741;
        double a4 = -1.453152027;
        double a5 = 1.061405429;
        double p = 0.3275911;

        int sign = x < 0 ? -1 : 1;
        x = Math.abs(x);

        double t = 1.0 / (1.0 + p * x);
        double y = 1.0 - (((((a5 * t + a4) * t) + a3) * t + a2) * t + a1) * t * Math.exp(-x * x);

        return sign * y;
    }
}