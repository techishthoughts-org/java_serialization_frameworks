package org.techishthoughts.benchmark.regression;

import org.techishthoughts.benchmark.statistics.BenchmarkStatistics;
import org.techishthoughts.benchmark.statistics.BenchmarkStatistics.StatisticalSummary;
import org.techishthoughts.benchmark.statistics.BenchmarkStatistics.SignificanceTest;
import org.techishthoughts.benchmark.statistics.BenchmarkStatistics.TrendAnalysis;
import org.techishthoughts.benchmark.statistics.BenchmarkStatistics.TrendDirection;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Performance regression detection and monitoring system.
 * Provides automated detection of performance degradation, trend analysis,
 * and alerting mechanisms for continuous performance monitoring.
 */
public class PerformanceRegression {

    private static final double DEFAULT_REGRESSION_THRESHOLD = 0.05; // 5% performance degradation
    private static final double CRITICAL_REGRESSION_THRESHOLD = 0.20; // 20% performance degradation
    private static final int MIN_HISTORICAL_SAMPLES = 5;

    /**
     * Performance measurement data point
     */
    public static class PerformanceMeasurement {
        private final LocalDateTime timestamp;
        private final String framework;
        private final String metric;
        private final double value;
        private final double confidence;
        private final Map<String, Object> metadata;

        public PerformanceMeasurement(LocalDateTime timestamp, String framework, String metric,
                                    double value, double confidence, Map<String, Object> metadata) {
            this.timestamp = timestamp;
            this.framework = framework;
            this.metric = metric;
            this.value = value;
            this.confidence = confidence;
            this.metadata = new HashMap<>(metadata != null ? metadata : Collections.emptyMap());
        }

        // Getters
        public LocalDateTime getTimestamp() { return timestamp; }
        public String getFramework() { return framework; }
        public String getMetric() { return metric; }
        public double getValue() { return value; }
        public double getConfidence() { return confidence; }
        public Map<String, Object> getMetadata() { return new HashMap<>(metadata); }

        @Override
        public String toString() {
            return String.format("%s[%s]: %s=%.6f (confidence=%.3f) at %s",
                framework, metric, metric, value, confidence, timestamp);
        }
    }

    /**
     * Regression detection result
     */
    public static class RegressionResult {
        private final String framework;
        private final String metric;
        private final RegressionSeverity severity;
        private final double regressionMagnitude;
        private final SignificanceTest statisticalTest;
        private final TrendAnalysis trendAnalysis;
        private final PerformanceMeasurement baseline;
        private final PerformanceMeasurement current;
        private final List<String> alerts;
        private final LocalDateTime detectionTime;

        public RegressionResult(String framework, String metric, RegressionSeverity severity,
                              double regressionMagnitude, SignificanceTest statisticalTest,
                              TrendAnalysis trendAnalysis, PerformanceMeasurement baseline,
                              PerformanceMeasurement current, List<String> alerts) {
            this.framework = framework;
            this.metric = metric;
            this.severity = severity;
            this.regressionMagnitude = regressionMagnitude;
            this.statisticalTest = statisticalTest;
            this.trendAnalysis = trendAnalysis;
            this.baseline = baseline;
            this.current = current;
            this.alerts = new ArrayList<>(alerts);
            this.detectionTime = LocalDateTime.now();
        }

        // Getters
        public String getFramework() { return framework; }
        public String getMetric() { return metric; }
        public RegressionSeverity getSeverity() { return severity; }
        public double getRegressionMagnitude() { return regressionMagnitude; }
        public SignificanceTest getStatisticalTest() { return statisticalTest; }
        public TrendAnalysis getTrendAnalysis() { return trendAnalysis; }
        public PerformanceMeasurement getBaseline() { return baseline; }
        public PerformanceMeasurement getCurrent() { return current; }
        public List<String> getAlerts() { return new ArrayList<>(alerts); }
        public LocalDateTime getDetectionTime() { return detectionTime; }

        public boolean hasRegression() { return severity != RegressionSeverity.NO_REGRESSION; }

        @Override
        public String toString() {
            return String.format("Regression: %s[%s] - %s (%.1f%% degradation, p=%.4f)",
                framework, metric, severity, regressionMagnitude * 100,
                statisticalTest != null ? statisticalTest.getPValue() : 0.0);
        }
    }

    /**
     * Regression severity levels
     */
    public enum RegressionSeverity {
        NO_REGRESSION("No Regression", 0),
        MINOR("Minor Regression", 1),
        MODERATE("Moderate Regression", 2),
        MAJOR("Major Regression", 3),
        CRITICAL("Critical Regression", 4);

        private final String description;
        private final int level;

        RegressionSeverity(String description, int level) {
            this.description = description;
            this.level = level;
        }

        public String getDescription() { return description; }
        public int getLevel() { return level; }
    }

    /**
     * Performance history tracker
     */
    public static class PerformanceHistory {
        private final String framework;
        private final Map<String, List<PerformanceMeasurement>> metricHistory;
        private final int maxHistorySize;

        public PerformanceHistory(String framework, int maxHistorySize) {
            this.framework = framework;
            this.metricHistory = new HashMap<>();
            this.maxHistorySize = maxHistorySize;
        }

        public void addMeasurement(PerformanceMeasurement measurement) {
            if (!framework.equals(measurement.getFramework())) {
                throw new IllegalArgumentException("Framework mismatch: expected " + framework +
                    ", got " + measurement.getFramework());
            }

            metricHistory.computeIfAbsent(measurement.getMetric(), k -> new ArrayList<>())
                .add(measurement);

            // Maintain size limit
            List<PerformanceMeasurement> history = metricHistory.get(measurement.getMetric());
            if (history.size() > maxHistorySize) {
                // Remove oldest entries, keeping chronological order
                history.sort(Comparator.comparing(PerformanceMeasurement::getTimestamp));
                while (history.size() > maxHistorySize) {
                    history.remove(0);
                }
            }
        }

        public List<PerformanceMeasurement> getHistory(String metric) {
            return new ArrayList<>(metricHistory.getOrDefault(metric, Collections.emptyList()));
        }

        public Set<String> getTrackedMetrics() {
            return new HashSet<>(metricHistory.keySet());
        }

        public boolean hasEnoughHistory(String metric) {
            return getHistory(metric).size() >= MIN_HISTORICAL_SAMPLES;
        }

        public PerformanceMeasurement getLatestMeasurement(String metric) {
            List<PerformanceMeasurement> history = getHistory(metric);
            if (history.isEmpty()) {
                return null;
            }
            return history.stream()
                .max(Comparator.comparing(PerformanceMeasurement::getTimestamp))
                .orElse(null);
        }

        public StatisticalSummary getBaselineStatistics(String metric) {
            List<PerformanceMeasurement> history = getHistory(metric);
            if (history.size() < MIN_HISTORICAL_SAMPLES) {
                return null;
            }

            // Use recent history (excluding the latest measurement) as baseline
            List<PerformanceMeasurement> baselineData = history.subList(0, history.size() - 1);
            double[] values = baselineData.stream()
                .mapToDouble(PerformanceMeasurement::getValue)
                .toArray();

            return BenchmarkStatistics.calculateSummary(values);
        }
    }

    /**
     * Regression detection configuration
     */
    public static class RegressionConfig {
        private final double minorThreshold;
        private final double moderateThreshold;
        private final double majorThreshold;
        private final double criticalThreshold;
        private final double significanceLevel;
        private final int minimumSamples;
        private final boolean enableTrendAnalysis;

        public RegressionConfig() {
            this(0.05, 0.10, 0.20, 0.50, 0.05, MIN_HISTORICAL_SAMPLES, true);
        }

        public RegressionConfig(double minorThreshold, double moderateThreshold,
                              double majorThreshold, double criticalThreshold,
                              double significanceLevel, int minimumSamples,
                              boolean enableTrendAnalysis) {
            this.minorThreshold = minorThreshold;
            this.moderateThreshold = moderateThreshold;
            this.majorThreshold = majorThreshold;
            this.criticalThreshold = criticalThreshold;
            this.significanceLevel = significanceLevel;
            this.minimumSamples = minimumSamples;
            this.enableTrendAnalysis = enableTrendAnalysis;
        }

        // Getters
        public double getMinorThreshold() { return minorThreshold; }
        public double getModerateThreshold() { return moderateThreshold; }
        public double getMajorThreshold() { return majorThreshold; }
        public double getCriticalThreshold() { return criticalThreshold; }
        public double getSignificanceLevel() { return significanceLevel; }
        public int getMinimumSamples() { return minimumSamples; }
        public boolean isEnableTrendAnalysis() { return enableTrendAnalysis; }
    }

    /**
     * Regression detector
     */
    public static class RegressionDetector {
        private final Map<String, PerformanceHistory> frameworkHistories;
        private final RegressionConfig config;

        public RegressionDetector(RegressionConfig config) {
            this.config = config;
            this.frameworkHistories = new HashMap<>();
        }

        public RegressionDetector() {
            this(new RegressionConfig());
        }

        public void addMeasurement(PerformanceMeasurement measurement) {
            frameworkHistories.computeIfAbsent(measurement.getFramework(),
                k -> new PerformanceHistory(k, 100))
                .addMeasurement(measurement);
        }

        public List<RegressionResult> detectRegressions() {
            List<RegressionResult> results = new ArrayList<>();

            for (PerformanceHistory history : frameworkHistories.values()) {
                for (String metric : history.getTrackedMetrics()) {
                    RegressionResult result = detectRegression(history, metric);
                    if (result != null) {
                        results.add(result);
                    }
                }
            }

            return results;
        }

        public RegressionResult detectRegression(String framework, String metric) {
            PerformanceHistory history = frameworkHistories.get(framework);
            if (history == null) {
                return null;
            }
            return detectRegression(history, metric);
        }

        private RegressionResult detectRegression(PerformanceHistory history, String metric) {
            if (!history.hasEnoughHistory(metric)) {
                return null; // Not enough data for regression detection
            }

            StatisticalSummary baseline = history.getBaselineStatistics(metric);
            PerformanceMeasurement current = history.getLatestMeasurement(metric);

            if (baseline == null || current == null) {
                return null;
            }

            // Calculate regression magnitude
            double baselineMean = baseline.getMean();
            double currentValue = current.getValue();

            // Assume lower values are better (e.g., latency, memory usage)
            double regressionMagnitude = (currentValue - baselineMean) / baselineMean;

            // Determine severity
            RegressionSeverity severity = determineSeverity(regressionMagnitude);

            // Perform statistical test
            List<PerformanceMeasurement> historicalData = history.getHistory(metric);
            double[] baselineValues = historicalData.subList(0, historicalData.size() - 1).stream()
                .mapToDouble(PerformanceMeasurement::getValue)
                .toArray();
            double[] currentValues = {currentValue};

            SignificanceTest statisticalTest = null;
            if (baselineValues.length >= 2) {
                try {
                    statisticalTest = BenchmarkStatistics.compareDatasets(baselineValues, currentValues);
                } catch (Exception e) {
                    System.err.println("Failed to perform statistical test: " + e.getMessage());
                }
            }

            // Trend analysis
            TrendAnalysis trendAnalysis = null;
            if (config.isEnableTrendAnalysis() && historicalData.size() >= 3) {
                double[] timePoints = new double[historicalData.size()];
                double[] values = new double[historicalData.size()];

                for (int i = 0; i < historicalData.size(); i++) {
                    PerformanceMeasurement measurement = historicalData.get(i);
                    timePoints[i] = i; // Simple sequential time points
                    values[i] = measurement.getValue();
                }

                trendAnalysis = BenchmarkStatistics.analyzeTrend(timePoints, values);
            }

            // Generate alerts
            List<String> alerts = generateAlerts(severity, regressionMagnitude, statisticalTest, trendAnalysis);

            return new RegressionResult(
                history.framework,
                metric,
                severity,
                regressionMagnitude,
                statisticalTest,
                trendAnalysis,
                createBaselineMeasurement(baseline, metric),
                current,
                alerts
            );
        }

        private RegressionSeverity determineSeverity(double regressionMagnitude) {
            double absRegression = Math.abs(regressionMagnitude);

            if (regressionMagnitude <= 0 || absRegression < config.getMinorThreshold()) {
                return RegressionSeverity.NO_REGRESSION;
            } else if (absRegression < config.getModerateThreshold()) {
                return RegressionSeverity.MINOR;
            } else if (absRegression < config.getMajorThreshold()) {
                return RegressionSeverity.MODERATE;
            } else if (absRegression < config.getCriticalThreshold()) {
                return RegressionSeverity.MAJOR;
            } else {
                return RegressionSeverity.CRITICAL;
            }
        }

        private PerformanceMeasurement createBaselineMeasurement(StatisticalSummary baseline, String metric) {
            return new PerformanceMeasurement(
                LocalDateTime.now().minusDays(1), // Approximate baseline time
                "baseline",
                metric,
                baseline.getMean(),
                1.0 - (baseline.getStandardDeviation() / baseline.getMean()),
                Map.of("type", "baseline", "samples", baseline.getSampleSize())
            );
        }

        private List<String> generateAlerts(RegressionSeverity severity, double regressionMagnitude,
                                          SignificanceTest statisticalTest, TrendAnalysis trendAnalysis) {
            List<String> alerts = new ArrayList<>();

            switch (severity) {
                case CRITICAL:
                    alerts.add("CRITICAL PERFORMANCE REGRESSION DETECTED!");
                    alerts.add("Immediate attention required - performance degraded by " +
                              String.format("%.1f%%", regressionMagnitude * 100));
                    break;
                case MAJOR:
                    alerts.add("Major performance regression detected");
                    alerts.add("Performance monitoring recommended");
                    break;
                case MODERATE:
                    alerts.add("Moderate performance regression detected");
                    break;
                case MINOR:
                    alerts.add("Minor performance regression detected");
                    break;
            }

            if (statisticalTest != null && statisticalTest.isSignificant()) {
                alerts.add("Regression is statistically significant (p=" +
                          String.format("%.4f", statisticalTest.getPValue()) + ")");
            }

            if (trendAnalysis != null && trendAnalysis.getDirection() == TrendDirection.DEGRADING) {
                alerts.add("Trending performance degradation detected");
            }

            return alerts;
        }

        public Map<String, PerformanceHistory> getFrameworkHistories() {
            return new HashMap<>(frameworkHistories);
        }
    }

    /**
     * Regression monitoring system
     */
    public static class RegressionMonitor {
        private final RegressionDetector detector;
        private final List<RegressionListener> listeners;

        public RegressionMonitor(RegressionConfig config) {
            this.detector = new RegressionDetector(config);
            this.listeners = new ArrayList<>();
        }

        public void addListener(RegressionListener listener) {
            listeners.add(listener);
        }

        public void addMeasurement(PerformanceMeasurement measurement) {
            detector.addMeasurement(measurement);

            // Check for regressions after adding new measurement
            RegressionResult regression = detector.detectRegression(
                measurement.getFramework(),
                measurement.getMetric()
            );

            if (regression != null && regression.hasRegression()) {
                notifyListeners(regression);
            }
        }

        private void notifyListeners(RegressionResult regression) {
            for (RegressionListener listener : listeners) {
                try {
                    listener.onRegressionDetected(regression);
                } catch (Exception e) {
                    System.err.println("Failed to notify regression listener: " + e.getMessage());
                }
            }
        }

        public List<RegressionResult> getAllRegressions() {
            return detector.detectRegressions();
        }
    }

    /**
     * Regression event listener interface
     */
    public interface RegressionListener {
        void onRegressionDetected(RegressionResult regression);
    }

    /**
     * Console-based regression listener
     */
    public static class ConsoleRegressionListener implements RegressionListener {
        @Override
        public void onRegressionDetected(RegressionResult regression) {
            System.err.println("=== PERFORMANCE REGRESSION DETECTED ===");
            System.err.println(regression);
            for (String alert : regression.getAlerts()) {
                System.err.println("ALERT: " + alert);
            }
            System.err.println("========================================");
        }
    }

    /**
     * Generate a comprehensive regression report
     */
    public static String generateRegressionReport(List<RegressionResult> regressions) {
        StringBuilder report = new StringBuilder();
        report.append("=== Performance Regression Analysis Report ===\n\n");

        if (regressions.isEmpty()) {
            report.append("No performance regressions detected.\n");
            return report.toString();
        }

        // Group by severity
        Map<RegressionSeverity, List<RegressionResult>> bySeverity = regressions.stream()
            .collect(Collectors.groupingBy(RegressionResult::getSeverity));

        // Report by severity level
        for (RegressionSeverity severity : RegressionSeverity.values()) {
            List<RegressionResult> severityRegressions = bySeverity.get(severity);
            if (severityRegressions == null || severityRegressions.isEmpty()) {
                continue;
            }

            report.append(String.format("%s (%d detected):\n", severity.getDescription(), severityRegressions.size()));
            report.append("=" + "=".repeat(severity.getDescription().length()) + "\n");

            for (RegressionResult regression : severityRegressions) {
                report.append(String.format("• %s[%s]: %.1f%% degradation\n",
                    regression.getFramework(),
                    regression.getMetric(),
                    regression.getRegressionMagnitude() * 100));

                if (regression.getStatisticalTest() != null) {
                    report.append(String.format("  Statistical significance: p=%.4f (%s)\n",
                        regression.getStatisticalTest().getPValue(),
                        regression.getStatisticalTest().isSignificant() ? "significant" : "not significant"));
                }

                if (regression.getTrendAnalysis() != null) {
                    report.append(String.format("  Trend: %s\n", regression.getTrendAnalysis().getDirection()));
                }

                report.append("\n");
            }
        }

        return report.toString();
    }
}