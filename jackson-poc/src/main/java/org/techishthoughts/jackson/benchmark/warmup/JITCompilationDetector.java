package org.techishthoughts.jackson.benchmark.warmup;

import org.techishthoughts.jackson.benchmark.adaptive.AdaptiveBenchmarkConfig;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.lang.management.CompilationMXBean;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Detects JIT compilation activity and determines when the JVM has stabilized
 * for accurate benchmark measurements. Uses multiple indicators including
 * compilation time, execution time variance, and JMX monitoring.
 */
public class JITCompilationDetector {

    private final AdaptiveBenchmarkConfig config;
    private final CompilationMXBean compilationBean;
    private final MBeanServer mBeanServer;

    // Performance tracking
    private final List<Double> executionTimes = new ArrayList<>();
    private final List<Long> compilationCounts = new ArrayList<>();
    private final AtomicLong lastCompilationTime = new AtomicLong(0);
    private long initialCompilationTime;
    private boolean initialized = false;

    public JITCompilationDetector(AdaptiveBenchmarkConfig config) {
        this.config = config;
        this.compilationBean = ManagementFactory.getCompilationMXBean();
        this.mBeanServer = ManagementFactory.getPlatformMBeanServer();
    }

    /**
     * Represents the current JIT compilation state.
     */
    public static class JITState {
        private final boolean isStable;
        private final boolean isWarmedUp;
        private final long totalCompilationTime;
        private final long recentCompilationTime;
        private final double executionTimeVariance;
        private final double coefficientOfVariation;
        private final int samplesAnalyzed;
        private final String stabilityReason;

        public JITState(boolean isStable, boolean isWarmedUp, long totalCompilationTime,
                       long recentCompilationTime, double executionTimeVariance,
                       double coefficientOfVariation, int samplesAnalyzed, String stabilityReason) {
            this.isStable = isStable;
            this.isWarmedUp = isWarmedUp;
            this.totalCompilationTime = totalCompilationTime;
            this.recentCompilationTime = recentCompilationTime;
            this.executionTimeVariance = executionTimeVariance;
            this.coefficientOfVariation = coefficientOfVariation;
            this.samplesAnalyzed = samplesAnalyzed;
            this.stabilityReason = stabilityReason;
        }

        public boolean isStable() { return isStable; }
        public boolean isWarmedUp() { return isWarmedUp; }
        public long getTotalCompilationTime() { return totalCompilationTime; }
        public long getRecentCompilationTime() { return recentCompilationTime; }
        public double getExecutionTimeVariance() { return executionTimeVariance; }
        public double getCoefficientOfVariation() { return coefficientOfVariation; }
        public int getSamplesAnalyzed() { return samplesAnalyzed; }
        public String getStabilityReason() { return stabilityReason; }

        @Override
        public String toString() {
            return String.format("JITState{stable=%s, warmed=%s, cv=%.4f, samples=%d, reason='%s'}",
                    isStable, isWarmedUp, coefficientOfVariation, samplesAnalyzed, stabilityReason);
        }
    }

    /**
     * Records an execution time and updates JIT compilation tracking.
     */
    public void recordExecution(double executionTimeMs) {
        if (!initialized) {
            initialize();
        }

        executionTimes.add(executionTimeMs);
        updateCompilationTracking();

        // Keep only recent measurements for stability analysis
        if (executionTimes.size() > config.getStabilityWindowSize() * 2) {
            executionTimes.remove(0);
        }
    }

    /**
     * Analyzes the current JIT state and determines stability.
     */
    public JITState analyzeJITState() {
        if (!initialized || executionTimes.size() < config.getStabilityWindowSize()) {
            return new JITState(false, false, getCurrentCompilationTime(), 0,
                              Double.MAX_VALUE, Double.MAX_VALUE, executionTimes.size(),
                              "Insufficient samples for analysis");
        }

        long totalCompilationTime = getCurrentCompilationTime();
        long recentCompilationTime = totalCompilationTime - lastCompilationTime.get();

        // Calculate execution time statistics
        double mean = calculateMean(executionTimes);
        double variance = calculateVariance(executionTimes, mean);
        double stdDev = Math.sqrt(variance);
        double coefficientOfVariation = mean > 0 ? stdDev / mean : Double.MAX_VALUE;

        // Check multiple stability criteria
        boolean compilationStable = isCompilationStable();
        boolean executionStable = coefficientOfVariation <= config.getJitStabilityThreshold();
        boolean trendStable = isExecutionTrendStable();
        boolean minimumWarmupMet = executionTimes.size() >= config.getMinimumWarmupIterations();

        boolean isStable = compilationStable && executionStable && trendStable;
        boolean isWarmedUp = isStable && minimumWarmupMet;

        String reason = buildStabilityReason(compilationStable, executionStable, trendStable, minimumWarmupMet);

        return new JITState(isStable, isWarmedUp, totalCompilationTime, recentCompilationTime,
                          variance, coefficientOfVariation, executionTimes.size(), reason);
    }

    /**
     * Determines if JIT compilation activity has stabilized.
     */
    public boolean isJITStable() {
        return analyzeJITState().isStable();
    }

    /**
     * Determines if the warmup phase is complete.
     */
    public boolean isWarmupComplete() {
        return analyzeJITState().isWarmedUp();
    }

    /**
     * Gets the recommended number of additional warmup iterations.
     */
    public int getRecommendedWarmupIterations() {
        JITState state = analyzeJITState();

        if (state.isWarmedUp()) {
            return 0;
        }

        // Estimate additional iterations needed based on current stability
        int currentIterations = executionTimes.size();
        int minimumNeeded = Math.max(0, config.getMinimumWarmupIterations() - currentIterations);

        if (!state.isStable()) {
            // If not stable, recommend more iterations based on CV
            double targetCV = config.getJitStabilityThreshold();
            double currentCV = state.getCoefficientOfVariation();

            if (currentCV > targetCV) {
                // Estimate additional iterations needed to reach target CV
                double ratio = currentCV / targetCV;
                int additionalIterations = (int) (currentIterations * ratio * 0.5);
                return Math.min(additionalIterations, config.getMaximumWarmupIterations() - currentIterations);
            }
        }

        return Math.max(minimumNeeded, 10); // At least 10 more iterations if not warmed up
    }

    /**
     * Resets the detector state for a new benchmark run.
     */
    public void reset() {
        executionTimes.clear();
        compilationCounts.clear();
        lastCompilationTime.set(0);
        initialized = false;
    }

    /**
     * Gets detailed JIT compilation statistics.
     */
    public Map<String, Object> getCompilationStatistics() {
        Map<String, Object> stats = new HashMap<>();

        stats.put("totalCompilationTime", getCurrentCompilationTime());
        stats.put("compilationSupported", compilationBean != null && compilationBean.isCompilationTimeMonitoringSupported());

        if (executionTimes.size() > 1) {
            double mean = calculateMean(executionTimes);
            double stdDev = Math.sqrt(calculateVariance(executionTimes, mean));
            stats.put("executionTimeMean", mean);
            stats.put("executionTimeStdDev", stdDev);
            stats.put("coefficientOfVariation", mean > 0 ? stdDev / mean : Double.MAX_VALUE);
        }

        // JMX compilation counters
        try {
            stats.put("c1CompilationCount", getC1CompilationCount());
            stats.put("c2CompilationCount", getC2CompilationCount());
        } catch (Exception e) {
            // JMX access might fail in some environments
            stats.put("jmxError", e.getMessage());
        }

        return stats;
    }

    // Private implementation methods

    private void initialize() {
        if (compilationBean != null && compilationBean.isCompilationTimeMonitoringSupported()) {
            initialCompilationTime = compilationBean.getTotalCompilationTime();
            lastCompilationTime.set(initialCompilationTime);
        } else {
            initialCompilationTime = 0;
            lastCompilationTime.set(0);
        }
        initialized = true;
    }

    private void updateCompilationTracking() {
        long currentCompilationTime = getCurrentCompilationTime();
        compilationCounts.add(currentCompilationTime - initialCompilationTime);

        // Keep only recent compilation data
        if (compilationCounts.size() > config.getStabilityWindowSize()) {
            compilationCounts.remove(0);
        }
    }

    private long getCurrentCompilationTime() {
        if (compilationBean != null && compilationBean.isCompilationTimeMonitoringSupported()) {
            return compilationBean.getTotalCompilationTime();
        }
        return 0;
    }

    private boolean isCompilationStable() {
        if (compilationCounts.size() < config.getStabilityWindowSize() / 2) {
            return false;
        }

        // Check if compilation time has plateaued
        int windowSize = Math.min(10, compilationCounts.size());
        List<Long> recentCompilations = compilationCounts.subList(
                compilationCounts.size() - windowSize, compilationCounts.size());

        if (recentCompilations.size() < 2) return false;

        long firstValue = recentCompilations.get(0);
        long lastValue = recentCompilations.get(recentCompilations.size() - 1);

        // Compilation is stable if the increase is minimal
        double relativeIncrease = firstValue > 0 ? (double)(lastValue - firstValue) / firstValue : 0;
        return relativeIncrease < 0.1; // Less than 10% increase in compilation time
    }

    private boolean isExecutionTrendStable() {
        if (executionTimes.size() < config.getStabilityWindowSize()) {
            return false;
        }

        // Analyze recent execution times for stability trend
        int windowSize = Math.min(config.getStabilityWindowSize(), executionTimes.size());
        List<Double> recentTimes = executionTimes.subList(
                executionTimes.size() - windowSize, executionTimes.size());

        // Check if there's a significant trend in execution times
        double slope = calculateTrendSlope(recentTimes);
        double meanTime = calculateMean(recentTimes);

        // Trend is stable if slope is small relative to mean execution time
        return Math.abs(slope) < meanTime * 0.01; // Less than 1% trend
    }

    private double calculateTrendSlope(List<Double> values) {
        if (values.size() < 2) return 0.0;

        double n = values.size();
        double sumX = n * (n - 1) / 2; // Sum of indices 0, 1, 2, ... n-1
        double sumY = values.stream().mapToDouble(Double::doubleValue).sum();
        double sumXY = 0.0;
        double sumXX = (n - 1) * n * (2 * n - 1) / 6; // Sum of squares of indices

        for (int i = 0; i < values.size(); i++) {
            sumXY += i * values.get(i);
        }

        // Linear regression slope: (n*Σ(xy) - Σ(x)*Σ(y)) / (n*Σ(x²) - (Σ(x))²)
        double numerator = n * sumXY - sumX * sumY;
        double denominator = n * sumXX - sumX * sumX;

        return denominator != 0 ? numerator / denominator : 0.0;
    }

    private double calculateMean(List<Double> values) {
        return values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }

    private double calculateVariance(List<Double> values, double mean) {
        if (values.size() <= 1) return 0.0;

        double sumSquaredDiffs = values.stream()
                .mapToDouble(value -> Math.pow(value - mean, 2))
                .sum();
        return sumSquaredDiffs / (values.size() - 1);
    }

    private String buildStabilityReason(boolean compilationStable, boolean executionStable,
                                      boolean trendStable, boolean minimumWarmupMet) {
        List<String> issues = new ArrayList<>();

        if (!compilationStable) issues.add("compilation ongoing");
        if (!executionStable) issues.add("high execution variance");
        if (!trendStable) issues.add("execution trend unstable");
        if (!minimumWarmupMet) issues.add("minimum warmup not met");

        if (issues.isEmpty()) {
            return "JIT compilation stable";
        } else {
            return "Issues: " + String.join(", ", issues);
        }
    }

    private long getC1CompilationCount() {
        try {
            ObjectName c1CompilerName = new ObjectName("java.lang:type=Compilation,name=C1*");
            Set<ObjectName> names = mBeanServer.queryNames(c1CompilerName, null);
            long count = 0;
            for (ObjectName name : names) {
                Object value = mBeanServer.getAttribute(name, "TotalCompilation");
                if (value instanceof Number) {
                    count += ((Number) value).longValue();
                }
            }
            return count;
        } catch (Exception e) {
            return -1;
        }
    }

    private long getC2CompilationCount() {
        try {
            ObjectName c2CompilerName = new ObjectName("java.lang:type=Compilation,name=C2*");
            Set<ObjectName> names = mBeanServer.queryNames(c2CompilerName, null);
            long count = 0;
            for (ObjectName name : names) {
                Object value = mBeanServer.getAttribute(name, "TotalCompilation");
                if (value instanceof Number) {
                    count += ((Number) value).longValue();
                }
            }
            return count;
        } catch (Exception e) {
            return -1;
        }
    }
}