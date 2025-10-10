package org.techishthoughts.benchmark.statistics;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.apache.commons.math3.stat.inference.TTest;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.apache.commons.math3.distribution.TDistribution;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Comprehensive statistical analysis for JMH benchmark results.
 * Provides confidence intervals, outlier detection, significance testing,
 * and performance trend analysis.
 */
public class BenchmarkStatistics {

    private static final double DEFAULT_CONFIDENCE_LEVEL = 0.95;
    private static final double OUTLIER_THRESHOLD = 1.5; // IQR multiplier for outlier detection
    private static final double SIGNIFICANCE_LEVEL = 0.05;

    /**
     * Statistical summary of benchmark measurements
     */
    public static class StatisticalSummary {
        private final double mean;
        private final double median;
        private final double standardDeviation;
        private final double variance;
        private final double min;
        private final double max;
        private final double q1;
        private final double q3;
        private final double iqr;
        private final int sampleSize;
        private final ConfidenceInterval confidenceInterval;
        private final List<Double> outliers;
        private final double coefficientOfVariation;
        private final double skewness;
        private final double kurtosis;

        public StatisticalSummary(double mean, double median, double standardDeviation,
                                double variance, double min, double max, double q1, double q3,
                                int sampleSize, ConfidenceInterval confidenceInterval,
                                List<Double> outliers, double coefficientOfVariation,
                                double skewness, double kurtosis) {
            this.mean = mean;
            this.median = median;
            this.standardDeviation = standardDeviation;
            this.variance = variance;
            this.min = min;
            this.max = max;
            this.q1 = q1;
            this.q3 = q3;
            this.iqr = q3 - q1;
            this.sampleSize = sampleSize;
            this.confidenceInterval = confidenceInterval;
            this.outliers = new ArrayList<>(outliers);
            this.coefficientOfVariation = coefficientOfVariation;
            this.skewness = skewness;
            this.kurtosis = kurtosis;
        }

        // Getters
        public double getMean() { return mean; }
        public double getMedian() { return median; }
        public double getStandardDeviation() { return standardDeviation; }
        public double getVariance() { return variance; }
        public double getMin() { return min; }
        public double getMax() { return max; }
        public double getQ1() { return q1; }
        public double getQ3() { return q3; }
        public double getIqr() { return iqr; }
        public int getSampleSize() { return sampleSize; }
        public ConfidenceInterval getConfidenceInterval() { return confidenceInterval; }
        public List<Double> getOutliers() { return new ArrayList<>(outliers); }
        public double getCoefficientOfVariation() { return coefficientOfVariation; }
        public double getSkewness() { return skewness; }
        public double getKurtosis() { return kurtosis; }

        public boolean hasOutliers() { return !outliers.isEmpty(); }
        public boolean isNormallyDistributed() {
            // Simple normality check: skewness and kurtosis within reasonable bounds
            return Math.abs(skewness) < 2.0 && Math.abs(kurtosis) < 7.0;
        }

        public DataQuality getDataQuality() {
            double cv = getCoefficientOfVariation();
            boolean hasOutliers = hasOutliers();
            boolean isNormal = isNormallyDistributed();

            if (cv > 0.3 || hasOutliers || !isNormal) {
                return DataQuality.POOR;
            } else if (cv > 0.15) {
                return DataQuality.FAIR;
            } else if (cv > 0.05) {
                return DataQuality.GOOD;
            } else {
                return DataQuality.EXCELLENT;
            }
        }

        @Override
        public String toString() {
            return String.format(
                "StatisticalSummary{mean=%.6f, median=%.6f, std=%.6f, cv=%.3f%%, outliers=%d, quality=%s}",
                mean, median, standardDeviation, coefficientOfVariation * 100,
                outliers.size(), getDataQuality()
            );
        }
    }

    /**
     * Confidence interval for statistical estimates
     */
    public static class ConfidenceInterval {
        private final double lowerBound;
        private final double upperBound;
        private final double confidenceLevel;
        private final double marginOfError;

        public ConfidenceInterval(double lowerBound, double upperBound, double confidenceLevel) {
            this.lowerBound = lowerBound;
            this.upperBound = upperBound;
            this.confidenceLevel = confidenceLevel;
            this.marginOfError = (upperBound - lowerBound) / 2.0;
        }

        public double getLowerBound() { return lowerBound; }
        public double getUpperBound() { return upperBound; }
        public double getConfidenceLevel() { return confidenceLevel; }
        public double getMarginOfError() { return marginOfError; }
        public double getWidth() { return upperBound - lowerBound; }

        @Override
        public String toString() {
            return String.format("[%.6f, %.6f] (%.1f%% confidence)",
                lowerBound, upperBound, confidenceLevel * 100);
        }
    }

    /**
     * Data quality assessment
     */
    public enum DataQuality {
        EXCELLENT("Excellent - Very low variance, no outliers"),
        GOOD("Good - Low variance, minimal outliers"),
        FAIR("Fair - Moderate variance or few outliers"),
        POOR("Poor - High variance or many outliers");

        private final String description;

        DataQuality(String description) {
            this.description = description;
        }

        public String getDescription() { return description; }
    }

    /**
     * Statistical significance test result
     */
    public static class SignificanceTest {
        private final double tStatistic;
        private final double pValue;
        private final boolean isSignificant;
        private final double effectSize;
        private final String interpretation;

        public SignificanceTest(double tStatistic, double pValue, boolean isSignificant,
                              double effectSize, String interpretation) {
            this.tStatistic = tStatistic;
            this.pValue = pValue;
            this.isSignificant = isSignificant;
            this.effectSize = effectSize;
            this.interpretation = interpretation;
        }

        public double getTStatistic() { return tStatistic; }
        public double getPValue() { return pValue; }
        public boolean isSignificant() { return isSignificant; }
        public double getEffectSize() { return effectSize; }
        public String getInterpretation() { return interpretation; }

        @Override
        public String toString() {
            return String.format("t=%.3f, p=%.6f (%s), effect size=%.3f (%s)",
                tStatistic, pValue, isSignificant ? "significant" : "not significant",
                effectSize, interpretation);
        }
    }

    /**
     * Calculate comprehensive statistical summary for a dataset
     */
    public static StatisticalSummary calculateSummary(double[] data) {
        return calculateSummary(data, DEFAULT_CONFIDENCE_LEVEL);
    }

    /**
     * Calculate comprehensive statistical summary with specified confidence level
     */
    public static StatisticalSummary calculateSummary(double[] data, double confidenceLevel) {
        if (data == null || data.length == 0) {
            throw new IllegalArgumentException("Data array cannot be null or empty");
        }

        DescriptiveStatistics stats = new DescriptiveStatistics(data);

        double mean = stats.getMean();
        double median = stats.getPercentile(50);
        double std = stats.getStandardDeviation();
        double variance = stats.getVariance();
        double min = stats.getMin();
        double max = stats.getMax();
        double q1 = stats.getPercentile(25);
        double q3 = stats.getPercentile(75);
        double skewness = stats.getSkewness();
        double kurtosis = stats.getKurtosis();

        int n = data.length;
        double cv = std / Math.abs(mean);

        // Calculate confidence interval for the mean
        ConfidenceInterval ci = calculateConfidenceInterval(data, confidenceLevel);

        // Detect outliers using IQR method
        List<Double> outliers = detectOutliers(data);

        return new StatisticalSummary(mean, median, std, variance, min, max, q1, q3,
                                    n, ci, outliers, cv, skewness, kurtosis);
    }

    /**
     * Calculate confidence interval for the mean
     */
    public static ConfidenceInterval calculateConfidenceInterval(double[] data, double confidenceLevel) {
        if (data.length < 2) {
            throw new IllegalArgumentException("Need at least 2 data points for confidence interval");
        }

        DescriptiveStatistics stats = new DescriptiveStatistics(data);
        double mean = stats.getMean();
        double std = stats.getStandardDeviation();
        int n = data.length;

        // Calculate t-critical value
        double alpha = 1.0 - confidenceLevel;
        int degreesOfFreedom = n - 1;
        TDistribution tDist = new TDistribution(degreesOfFreedom);
        double tCritical = tDist.inverseCumulativeProbability(1.0 - alpha / 2.0);

        // Calculate margin of error
        double standardError = std / Math.sqrt(n);
        double marginOfError = tCritical * standardError;

        double lowerBound = mean - marginOfError;
        double upperBound = mean + marginOfError;

        return new ConfidenceInterval(lowerBound, upperBound, confidenceLevel);
    }

    /**
     * Detect outliers using the IQR method
     */
    public static List<Double> detectOutliers(double[] data) {
        DescriptiveStatistics stats = new DescriptiveStatistics(data);
        double q1 = stats.getPercentile(25);
        double q3 = stats.getPercentile(75);
        double iqr = q3 - q1;

        double lowerFence = q1 - OUTLIER_THRESHOLD * iqr;
        double upperFence = q3 + OUTLIER_THRESHOLD * iqr;

        return Arrays.stream(data)
                .filter(value -> value < lowerFence || value > upperFence)
                .boxed()
                .collect(Collectors.toList());
    }

    /**
     * Remove outliers from dataset
     */
    public static double[] removeOutliers(double[] data) {
        List<Double> outliers = detectOutliers(data);
        Set<Double> outlierSet = new HashSet<>(outliers);

        return Arrays.stream(data)
                .filter(value -> !outlierSet.contains(value))
                .toArray();
    }

    /**
     * Compare two datasets using t-test
     */
    public static SignificanceTest compareDatasets(double[] baseline, double[] comparison) {
        return compareDatasets(baseline, comparison, SIGNIFICANCE_LEVEL);
    }

    /**
     * Compare two datasets using t-test with specified significance level
     */
    public static SignificanceTest compareDatasets(double[] baseline, double[] comparison,
                                                 double significanceLevel) {
        if (baseline.length < 2 || comparison.length < 2) {
            throw new IllegalArgumentException("Need at least 2 data points in each dataset");
        }

        TTest tTest = new TTest();
        double tStatistic = tTest.t(baseline, comparison);
        double pValue = tTest.tTest(baseline, comparison);
        boolean isSignificant = pValue < significanceLevel;

        // Calculate Cohen's d (effect size)
        DescriptiveStatistics baseStats = new DescriptiveStatistics(baseline);
        DescriptiveStatistics compStats = new DescriptiveStatistics(comparison);

        double meanDiff = compStats.getMean() - baseStats.getMean();
        double pooledStd = Math.sqrt(
            ((baseStats.getN() - 1) * baseStats.getVariance() +
             (compStats.getN() - 1) * compStats.getVariance()) /
            (baseStats.getN() + compStats.getN() - 2)
        );

        double cohensD = meanDiff / pooledStd;
        String effectInterpretation = interpretEffectSize(Math.abs(cohensD));

        return new SignificanceTest(tStatistic, pValue, isSignificant, cohensD, effectInterpretation);
    }

    /**
     * Interpret effect size (Cohen's d)
     */
    private static String interpretEffectSize(double absEffectSize) {
        if (absEffectSize < 0.2) {
            return "negligible";
        } else if (absEffectSize < 0.5) {
            return "small";
        } else if (absEffectSize < 0.8) {
            return "medium";
        } else {
            return "large";
        }
    }

    /**
     * Detect performance trends in time series data
     */
    public static class TrendAnalysis {
        private final double slope;
        private final double intercept;
        private final double rSquared;
        private final TrendDirection direction;
        private final boolean isSignificantTrend;
        private final String interpretation;

        public TrendAnalysis(double slope, double intercept, double rSquared,
                           TrendDirection direction, boolean isSignificantTrend, String interpretation) {
            this.slope = slope;
            this.intercept = intercept;
            this.rSquared = rSquared;
            this.direction = direction;
            this.isSignificantTrend = isSignificantTrend;
            this.interpretation = interpretation;
        }

        public double getSlope() { return slope; }
        public double getIntercept() { return intercept; }
        public double getRSquared() { return rSquared; }
        public TrendDirection getDirection() { return direction; }
        public boolean isSignificantTrend() { return isSignificantTrend; }
        public String getInterpretation() { return interpretation; }

        @Override
        public String toString() {
            return String.format("Trend: %s (slope=%.6f, R²=%.3f) - %s",
                direction, slope, rSquared, interpretation);
        }
    }

    public enum TrendDirection {
        IMPROVING, STABLE, DEGRADING, INSUFFICIENT_DATA
    }

    /**
     * Analyze performance trends over time
     */
    public static TrendAnalysis analyzeTrend(double[] timePoints, double[] measurements) {
        if (timePoints.length != measurements.length || timePoints.length < 3) {
            return new TrendAnalysis(0, 0, 0, TrendDirection.INSUFFICIENT_DATA,
                                   false, "Insufficient data for trend analysis");
        }

        SimpleRegression regression = new SimpleRegression();
        for (int i = 0; i < timePoints.length; i++) {
            regression.addData(timePoints[i], measurements[i]);
        }

        double slope = regression.getSlope();
        double intercept = regression.getIntercept();
        double rSquared = regression.getRSquared();

        // Determine trend direction (assuming lower values are better for performance metrics)
        TrendDirection direction;
        boolean isSignificant = rSquared > 0.5; // Simple threshold for significance

        if (Math.abs(slope) < 1e-6 || rSquared < 0.1) {
            direction = TrendDirection.STABLE;
        } else if (slope < 0) {
            direction = TrendDirection.IMPROVING; // Decreasing times = improvement
        } else {
            direction = TrendDirection.DEGRADING; // Increasing times = degradation
        }

        String interpretation = String.format("%s trend detected (R²=%.3f, %s)",
            direction, rSquared, isSignificant ? "significant" : "weak");

        return new TrendAnalysis(slope, intercept, rSquared, direction, isSignificant, interpretation);
    }

    /**
     * Calculate minimum sample size for desired precision
     */
    public static int calculateRequiredSampleSize(double[] pilotData, double desiredMarginOfError,
                                                double confidenceLevel) {
        if (pilotData.length < 2) {
            throw new IllegalArgumentException("Need pilot data for sample size calculation");
        }

        DescriptiveStatistics stats = new DescriptiveStatistics(pilotData);
        double std = stats.getStandardDeviation();

        double alpha = 1.0 - confidenceLevel;
        TDistribution tDist = new TDistribution(pilotData.length - 1);
        double tCritical = tDist.inverseCumulativeProbability(1.0 - alpha / 2.0);

        // Calculate required sample size
        double requiredN = Math.pow((tCritical * std) / desiredMarginOfError, 2);

        return Math.max((int) Math.ceil(requiredN), pilotData.length);
    }

    /**
     * Check if benchmark results are statistically stable
     */
    public static boolean isStable(double[] data, double maxCoefficientOfVariation) {
        StatisticalSummary summary = calculateSummary(data);
        return summary.getCoefficientOfVariation() <= maxCoefficientOfVariation &&
               !summary.hasOutliers();
    }
}