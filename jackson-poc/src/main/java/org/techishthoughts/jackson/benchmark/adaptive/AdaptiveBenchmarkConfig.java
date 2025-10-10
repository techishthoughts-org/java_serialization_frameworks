package org.techishthoughts.jackson.benchmark.adaptive;

import java.time.Duration;

/**
 * Configuration class for adaptive benchmarking parameters.
 * Provides tunable settings for statistical rigor and measurement accuracy.
 */
public class AdaptiveBenchmarkConfig {

    // Statistical Configuration
    private final double confidenceLevel;
    private final double marginOfError;
    private final double minimumEffect;
    private final int minimumSampleSize;
    private final int maximumSampleSize;

    // Warmup Configuration
    private final int minimumWarmupIterations;
    private final int maximumWarmupIterations;
    private final Duration warmupTimeout;
    private final double jitStabilityThreshold;

    // Measurement Configuration
    private final Duration measurementTimeout;
    private final double coefficientOfVariationThreshold;
    private final int stabilityWindowSize;
    private final double outlierThreshold;

    // Stopping Criteria
    private final double convergenceThreshold;
    private final int convergenceWindowSize;
    private final Duration maxBenchmarkDuration;

    private AdaptiveBenchmarkConfig(Builder builder) {
        this.confidenceLevel = builder.confidenceLevel;
        this.marginOfError = builder.marginOfError;
        this.minimumEffect = builder.minimumEffect;
        this.minimumSampleSize = builder.minimumSampleSize;
        this.maximumSampleSize = builder.maximumSampleSize;
        this.minimumWarmupIterations = builder.minimumWarmupIterations;
        this.maximumWarmupIterations = builder.maximumWarmupIterations;
        this.warmupTimeout = builder.warmupTimeout;
        this.jitStabilityThreshold = builder.jitStabilityThreshold;
        this.measurementTimeout = builder.measurementTimeout;
        this.coefficientOfVariationThreshold = builder.coefficientOfVariationThreshold;
        this.stabilityWindowSize = builder.stabilityWindowSize;
        this.outlierThreshold = builder.outlierThreshold;
        this.convergenceThreshold = builder.convergenceThreshold;
        this.convergenceWindowSize = builder.convergenceWindowSize;
        this.maxBenchmarkDuration = builder.maxBenchmarkDuration;
    }

    /**
     * Creates a default configuration suitable for most benchmarking scenarios.
     */
    public static AdaptiveBenchmarkConfig defaultConfig() {
        return new Builder()
                .confidenceLevel(0.95)
                .marginOfError(0.05)
                .minimumEffect(0.01)
                .minimumSampleSize(50)
                .maximumSampleSize(10000)
                .minimumWarmupIterations(10)
                .maximumWarmupIterations(1000)
                .warmupTimeout(Duration.ofMinutes(5))
                .jitStabilityThreshold(0.05)
                .measurementTimeout(Duration.ofMinutes(30))
                .coefficientOfVariationThreshold(0.02)
                .stabilityWindowSize(20)
                .outlierThreshold(2.5)
                .convergenceThreshold(0.01)
                .convergenceWindowSize(10)
                .maxBenchmarkDuration(Duration.ofHours(1))
                .build();
    }

    /**
     * Creates a configuration optimized for high precision measurements.
     */
    public static AdaptiveBenchmarkConfig highPrecisionConfig() {
        return new Builder()
                .confidenceLevel(0.99)
                .marginOfError(0.01)
                .minimumEffect(0.005)
                .minimumSampleSize(100)
                .maximumSampleSize(50000)
                .minimumWarmupIterations(50)
                .maximumWarmupIterations(5000)
                .warmupTimeout(Duration.ofMinutes(10))
                .jitStabilityThreshold(0.01)
                .measurementTimeout(Duration.ofHours(1))
                .coefficientOfVariationThreshold(0.005)
                .stabilityWindowSize(50)
                .outlierThreshold(3.0)
                .convergenceThreshold(0.005)
                .convergenceWindowSize(25)
                .maxBenchmarkDuration(Duration.ofHours(6))
                .build();
    }

    /**
     * Creates a configuration optimized for quick results with reasonable accuracy.
     */
    public static AdaptiveBenchmarkConfig quickConfig() {
        return new Builder()
                .confidenceLevel(0.90)
                .marginOfError(0.10)
                .minimumEffect(0.05)
                .minimumSampleSize(20)
                .maximumSampleSize(1000)
                .minimumWarmupIterations(5)
                .maximumWarmupIterations(100)
                .warmupTimeout(Duration.ofMinutes(1))
                .jitStabilityThreshold(0.10)
                .measurementTimeout(Duration.ofMinutes(5))
                .coefficientOfVariationThreshold(0.10)
                .stabilityWindowSize(5)
                .outlierThreshold(2.0)
                .convergenceThreshold(0.05)
                .convergenceWindowSize(5)
                .maxBenchmarkDuration(Duration.ofMinutes(15))
                .build();
    }

    // Getters
    public double getConfidenceLevel() { return confidenceLevel; }
    public double getMarginOfError() { return marginOfError; }
    public double getMinimumEffect() { return minimumEffect; }
    public int getMinimumSampleSize() { return minimumSampleSize; }
    public int getMaximumSampleSize() { return maximumSampleSize; }
    public int getMinimumWarmupIterations() { return minimumWarmupIterations; }
    public int getMaximumWarmupIterations() { return maximumWarmupIterations; }
    public Duration getWarmupTimeout() { return warmupTimeout; }
    public double getJitStabilityThreshold() { return jitStabilityThreshold; }
    public Duration getMeasurementTimeout() { return measurementTimeout; }
    public double getCoefficientOfVariationThreshold() { return coefficientOfVariationThreshold; }
    public int getStabilityWindowSize() { return stabilityWindowSize; }
    public double getOutlierThreshold() { return outlierThreshold; }
    public double getConvergenceThreshold() { return convergenceThreshold; }
    public int getConvergenceWindowSize() { return convergenceWindowSize; }
    public Duration getMaxBenchmarkDuration() { return maxBenchmarkDuration; }

    /**
     * Builder pattern for creating AdaptiveBenchmarkConfig instances.
     */
    public static class Builder {
        private double confidenceLevel = 0.95;
        private double marginOfError = 0.05;
        private double minimumEffect = 0.01;
        private int minimumSampleSize = 50;
        private int maximumSampleSize = 10000;
        private int minimumWarmupIterations = 10;
        private int maximumWarmupIterations = 1000;
        private Duration warmupTimeout = Duration.ofMinutes(5);
        private double jitStabilityThreshold = 0.05;
        private Duration measurementTimeout = Duration.ofMinutes(30);
        private double coefficientOfVariationThreshold = 0.02;
        private int stabilityWindowSize = 20;
        private double outlierThreshold = 2.5;
        private double convergenceThreshold = 0.01;
        private int convergenceWindowSize = 10;
        private Duration maxBenchmarkDuration = Duration.ofHours(1);

        public Builder confidenceLevel(double confidenceLevel) {
            if (confidenceLevel <= 0 || confidenceLevel >= 1) {
                throw new IllegalArgumentException("Confidence level must be between 0 and 1");
            }
            this.confidenceLevel = confidenceLevel;
            return this;
        }

        public Builder marginOfError(double marginOfError) {
            if (marginOfError <= 0 || marginOfError >= 1) {
                throw new IllegalArgumentException("Margin of error must be between 0 and 1");
            }
            this.marginOfError = marginOfError;
            return this;
        }

        public Builder minimumEffect(double minimumEffect) {
            if (minimumEffect <= 0) {
                throw new IllegalArgumentException("Minimum effect must be positive");
            }
            this.minimumEffect = minimumEffect;
            return this;
        }

        public Builder minimumSampleSize(int minimumSampleSize) {
            if (minimumSampleSize <= 0) {
                throw new IllegalArgumentException("Minimum sample size must be positive");
            }
            this.minimumSampleSize = minimumSampleSize;
            return this;
        }

        public Builder maximumSampleSize(int maximumSampleSize) {
            if (maximumSampleSize <= 0) {
                throw new IllegalArgumentException("Maximum sample size must be positive");
            }
            this.maximumSampleSize = maximumSampleSize;
            return this;
        }

        public Builder minimumWarmupIterations(int minimumWarmupIterations) {
            if (minimumWarmupIterations < 0) {
                throw new IllegalArgumentException("Minimum warmup iterations must be non-negative");
            }
            this.minimumWarmupIterations = minimumWarmupIterations;
            return this;
        }

        public Builder maximumWarmupIterations(int maximumWarmupIterations) {
            if (maximumWarmupIterations <= 0) {
                throw new IllegalArgumentException("Maximum warmup iterations must be positive");
            }
            this.maximumWarmupIterations = maximumWarmupIterations;
            return this;
        }

        public Builder warmupTimeout(Duration warmupTimeout) {
            if (warmupTimeout.isNegative() || warmupTimeout.isZero()) {
                throw new IllegalArgumentException("Warmup timeout must be positive");
            }
            this.warmupTimeout = warmupTimeout;
            return this;
        }

        public Builder jitStabilityThreshold(double jitStabilityThreshold) {
            if (jitStabilityThreshold <= 0) {
                throw new IllegalArgumentException("JIT stability threshold must be positive");
            }
            this.jitStabilityThreshold = jitStabilityThreshold;
            return this;
        }

        public Builder measurementTimeout(Duration measurementTimeout) {
            if (measurementTimeout.isNegative() || measurementTimeout.isZero()) {
                throw new IllegalArgumentException("Measurement timeout must be positive");
            }
            this.measurementTimeout = measurementTimeout;
            return this;
        }

        public Builder coefficientOfVariationThreshold(double coefficientOfVariationThreshold) {
            if (coefficientOfVariationThreshold <= 0) {
                throw new IllegalArgumentException("Coefficient of variation threshold must be positive");
            }
            this.coefficientOfVariationThreshold = coefficientOfVariationThreshold;
            return this;
        }

        public Builder stabilityWindowSize(int stabilityWindowSize) {
            if (stabilityWindowSize <= 0) {
                throw new IllegalArgumentException("Stability window size must be positive");
            }
            this.stabilityWindowSize = stabilityWindowSize;
            return this;
        }

        public Builder outlierThreshold(double outlierThreshold) {
            if (outlierThreshold <= 0) {
                throw new IllegalArgumentException("Outlier threshold must be positive");
            }
            this.outlierThreshold = outlierThreshold;
            return this;
        }

        public Builder convergenceThreshold(double convergenceThreshold) {
            if (convergenceThreshold <= 0) {
                throw new IllegalArgumentException("Convergence threshold must be positive");
            }
            this.convergenceThreshold = convergenceThreshold;
            return this;
        }

        public Builder convergenceWindowSize(int convergenceWindowSize) {
            if (convergenceWindowSize <= 0) {
                throw new IllegalArgumentException("Convergence window size must be positive");
            }
            this.convergenceWindowSize = convergenceWindowSize;
            return this;
        }

        public Builder maxBenchmarkDuration(Duration maxBenchmarkDuration) {
            if (maxBenchmarkDuration.isNegative() || maxBenchmarkDuration.isZero()) {
                throw new IllegalArgumentException("Max benchmark duration must be positive");
            }
            this.maxBenchmarkDuration = maxBenchmarkDuration;
            return this;
        }

        public AdaptiveBenchmarkConfig build() {
            // Validation
            if (minimumSampleSize > maximumSampleSize) {
                throw new IllegalArgumentException("Minimum sample size cannot exceed maximum sample size");
            }
            if (minimumWarmupIterations > maximumWarmupIterations) {
                throw new IllegalArgumentException("Minimum warmup iterations cannot exceed maximum warmup iterations");
            }
            if (stabilityWindowSize > minimumSampleSize) {
                throw new IllegalArgumentException("Stability window size cannot exceed minimum sample size");
            }
            if (convergenceWindowSize > stabilityWindowSize) {
                throw new IllegalArgumentException("Convergence window size cannot exceed stability window size");
            }

            return new AdaptiveBenchmarkConfig(this);
        }
    }

    @Override
    public String toString() {
        return "AdaptiveBenchmarkConfig{" +
                "confidenceLevel=" + confidenceLevel +
                ", marginOfError=" + marginOfError +
                ", minimumEffect=" + minimumEffect +
                ", sampleSize=[" + minimumSampleSize + ", " + maximumSampleSize + "]" +
                ", warmupIterations=[" + minimumWarmupIterations + ", " + maximumWarmupIterations + "]" +
                ", cvThreshold=" + coefficientOfVariationThreshold +
                ", outlierThreshold=" + outlierThreshold +
                '}';
    }
}