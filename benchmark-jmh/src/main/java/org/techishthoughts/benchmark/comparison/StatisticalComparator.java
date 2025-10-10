package org.techishthoughts.benchmark.comparison;

import org.techishthoughts.benchmark.statistics.BenchmarkStatistics;
import org.techishthoughts.benchmark.statistics.BenchmarkStatistics.StatisticalSummary;
import org.techishthoughts.benchmark.statistics.BenchmarkStatistics.SignificanceTest;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Comprehensive statistical comparison utilities for serialization framework benchmarks.
 * Provides multi-dimensional framework comparison, ranking, and performance analysis.
 */
public class StatisticalComparator {

    /**
     * Performance comparison result between two frameworks
     */
    public static class ComparisonResult {
        private final String baselineFramework;
        private final String comparisonFramework;
        private final ComparisonMetric metric;
        private final double improvementRatio;
        private final double improvementPercentage;
        private final SignificanceTest significanceTest;
        private final ComparisonOutcome outcome;
        private final String interpretation;

        public ComparisonResult(String baselineFramework, String comparisonFramework,
                              ComparisonMetric metric, double improvementRatio,
                              SignificanceTest significanceTest) {
            this.baselineFramework = baselineFramework;
            this.comparisonFramework = comparisonFramework;
            this.metric = metric;
            this.improvementRatio = improvementRatio;
            this.improvementPercentage = (improvementRatio - 1.0) * 100.0;
            this.significanceTest = significanceTest;
            this.outcome = determineOutcome();
            this.interpretation = generateInterpretation();
        }

        private ComparisonOutcome determineOutcome() {
            if (!significanceTest.isSignificant()) {
                return ComparisonOutcome.NO_SIGNIFICANT_DIFFERENCE;
            }

            double absImprovement = Math.abs(improvementPercentage);
            if (improvementPercentage > 0) {
                if (absImprovement < 5) return ComparisonOutcome.SLIGHTLY_BETTER;
                if (absImprovement < 20) return ComparisonOutcome.MODERATELY_BETTER;
                return ComparisonOutcome.SIGNIFICANTLY_BETTER;
            } else {
                if (absImprovement < 5) return ComparisonOutcome.SLIGHTLY_WORSE;
                if (absImprovement < 20) return ComparisonOutcome.MODERATELY_WORSE;
                return ComparisonOutcome.SIGNIFICANTLY_WORSE;
            }
        }

        private String generateInterpretation() {
            String direction = improvementPercentage > 0 ? "better" : "worse";
            String magnitude = outcome.getMagnitude();

            if (outcome == ComparisonOutcome.NO_SIGNIFICANT_DIFFERENCE) {
                return String.format("No statistically significant difference in %s between %s and %s",
                    metric.getDisplayName(), comparisonFramework, baselineFramework);
            }

            return String.format("%s performs %s %s than %s in %s (%.1f%% %s, p=%.4f)",
                comparisonFramework, magnitude, direction, baselineFramework,
                metric.getDisplayName(), Math.abs(improvementPercentage), direction,
                significanceTest.getPValue());
        }

        // Getters
        public String getBaselineFramework() { return baselineFramework; }
        public String getComparisonFramework() { return comparisonFramework; }
        public ComparisonMetric getMetric() { return metric; }
        public double getImprovementRatio() { return improvementRatio; }
        public double getImprovementPercentage() { return improvementPercentage; }
        public SignificanceTest getSignificanceTest() { return significanceTest; }
        public ComparisonOutcome getOutcome() { return outcome; }
        public String getInterpretation() { return interpretation; }

        @Override
        public String toString() { return interpretation; }
    }

    /**
     * Comparison outcome categories
     */
    public enum ComparisonOutcome {
        SIGNIFICANTLY_BETTER("significantly"),
        MODERATELY_BETTER("moderately"),
        SLIGHTLY_BETTER("slightly"),
        NO_SIGNIFICANT_DIFFERENCE("not significantly"),
        SLIGHTLY_WORSE("slightly"),
        MODERATELY_WORSE("moderately"),
        SIGNIFICANTLY_WORSE("significantly");

        private final String magnitude;

        ComparisonOutcome(String magnitude) {
            this.magnitude = magnitude;
        }

        public String getMagnitude() { return magnitude; }
    }

    /**
     * Metrics for performance comparison
     */
    public enum ComparisonMetric {
        THROUGHPUT("Throughput", true, "ops/sec"),
        LATENCY("Average Latency", false, "μs"),
        P95_LATENCY("95th Percentile Latency", false, "μs"),
        P99_LATENCY("99th Percentile Latency", false, "μs"),
        MEMORY_USAGE("Memory Usage", false, "MB"),
        SERIALIZATION_SIZE("Serialization Size", false, "bytes"),
        GC_FREQUENCY("GC Frequency", false, "events/sec"),
        CPU_USAGE("CPU Usage", false, "%");

        private final String displayName;
        private final boolean higherIsBetter;
        private final String unit;

        ComparisonMetric(String displayName, boolean higherIsBetter, String unit) {
            this.displayName = displayName;
            this.higherIsBetter = higherIsBetter;
            this.unit = unit;
        }

        public String getDisplayName() { return displayName; }
        public boolean isHigherBetter() { return higherIsBetter; }
        public String getUnit() { return unit; }
    }

    /**
     * Framework performance data
     */
    public static class FrameworkPerformance {
        private final String frameworkName;
        private final Map<ComparisonMetric, StatisticalSummary> metrics;
        private final Map<ComparisonMetric, double[]> rawData;

        public FrameworkPerformance(String frameworkName) {
            this.frameworkName = frameworkName;
            this.metrics = new EnumMap<>(ComparisonMetric.class);
            this.rawData = new EnumMap<>(ComparisonMetric.class);
        }

        public void addMetric(ComparisonMetric metric, double[] data) {
            rawData.put(metric, Arrays.copyOf(data, data.length));
            metrics.put(metric, BenchmarkStatistics.calculateSummary(data));
        }

        public String getFrameworkName() { return frameworkName; }
        public StatisticalSummary getMetric(ComparisonMetric metric) { return metrics.get(metric); }
        public double[] getRawData(ComparisonMetric metric) {
            double[] data = rawData.get(metric);
            return data != null ? Arrays.copyOf(data, data.length) : null;
        }
        public Set<ComparisonMetric> getAvailableMetrics() { return metrics.keySet(); }
    }

    /**
     * Multi-dimensional framework ranking
     */
    public static class FrameworkRanking {
        private final List<RankedFramework> rankings;
        private final ComparisonMetric primaryMetric;
        private final String methodology;

        public FrameworkRanking(List<RankedFramework> rankings, ComparisonMetric primaryMetric,
                              String methodology) {
            this.rankings = new ArrayList<>(rankings);
            this.primaryMetric = primaryMetric;
            this.methodology = methodology;
        }

        public List<RankedFramework> getRankings() { return new ArrayList<>(rankings); }
        public ComparisonMetric getPrimaryMetric() { return primaryMetric; }
        public String getMethodology() { return methodology; }
        public RankedFramework getBest() { return rankings.isEmpty() ? null : rankings.get(0); }
        public RankedFramework getWorst() { return rankings.isEmpty() ? null : rankings.get(rankings.size() - 1); }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("Framework Ranking by %s (%s):\n", primaryMetric.getDisplayName(), methodology));
            for (int i = 0; i < rankings.size(); i++) {
                RankedFramework rf = rankings.get(i);
                sb.append(String.format("%d. %s (Score: %.3f)\n", i + 1, rf.getFrameworkName(), rf.getScore()));
            }
            return sb.toString();
        }
    }

    /**
     * Individual framework ranking entry
     */
    public static class RankedFramework {
        private final String frameworkName;
        private final double score;
        private final Map<ComparisonMetric, Double> normalizedScores;

        public RankedFramework(String frameworkName, double score,
                             Map<ComparisonMetric, Double> normalizedScores) {
            this.frameworkName = frameworkName;
            this.score = score;
            this.normalizedScores = new EnumMap<>(normalizedScores);
        }

        public String getFrameworkName() { return frameworkName; }
        public double getScore() { return score; }
        public Map<ComparisonMetric, Double> getNormalizedScores() { return new EnumMap<>(normalizedScores); }
    }

    // ==================== COMPARISON METHODS ====================

    /**
     * Compare two frameworks on a specific metric
     */
    public static ComparisonResult compare(FrameworkPerformance baseline,
                                         FrameworkPerformance comparison,
                                         ComparisonMetric metric) {
        double[] baselineData = baseline.getRawData(metric);
        double[] comparisonData = comparison.getRawData(metric);

        if (baselineData == null || comparisonData == null) {
            throw new IllegalArgumentException("Missing data for metric: " + metric);
        }

        SignificanceTest significanceTest = BenchmarkStatistics.compareDatasets(baselineData, comparisonData);

        StatisticalSummary baselineSummary = baseline.getMetric(metric);
        StatisticalSummary comparisonSummary = comparison.getMetric(metric);

        // Calculate improvement ratio considering metric direction
        double improvementRatio;
        if (metric.isHigherBetter()) {
            // For throughput: higher is better
            improvementRatio = comparisonSummary.getMean() / baselineSummary.getMean();
        } else {
            // For latency/memory: lower is better
            improvementRatio = baselineSummary.getMean() / comparisonSummary.getMean();
        }

        return new ComparisonResult(
            baseline.getFrameworkName(),
            comparison.getFrameworkName(),
            metric,
            improvementRatio,
            significanceTest
        );
    }

    /**
     * Perform comprehensive multi-metric comparison
     */
    public static Map<ComparisonMetric, ComparisonResult> compareMultiMetric(
            FrameworkPerformance baseline,
            FrameworkPerformance comparison) {

        Map<ComparisonMetric, ComparisonResult> results = new EnumMap<>(ComparisonMetric.class);

        Set<ComparisonMetric> commonMetrics = new EnumSet<>(baseline.getAvailableMetrics());
        commonMetrics.retainAll(comparison.getAvailableMetrics());

        for (ComparisonMetric metric : commonMetrics) {
            try {
                ComparisonResult result = compare(baseline, comparison, metric);
                results.put(metric, result);
            } catch (Exception e) {
                System.err.println("Failed to compare metric " + metric + ": " + e.getMessage());
            }
        }

        return results;
    }

    /**
     * Rank frameworks using weighted scoring
     */
    public static FrameworkRanking rankFrameworks(List<FrameworkPerformance> frameworks,
                                                Map<ComparisonMetric, Double> weights) {
        if (frameworks.isEmpty()) {
            return new FrameworkRanking(Collections.emptyList(), null, "No frameworks provided");
        }

        // Find common metrics across all frameworks
        Set<ComparisonMetric> commonMetrics = new EnumSet<>(frameworks.get(0).getAvailableMetrics());
        for (FrameworkPerformance framework : frameworks) {
            commonMetrics.retainAll(framework.getAvailableMetrics());
        }

        if (commonMetrics.isEmpty()) {
            return new FrameworkRanking(Collections.emptyList(), null, "No common metrics found");
        }

        // Normalize scores for each metric
        Map<ComparisonMetric, Map<String, Double>> normalizedScores = new EnumMap<>(ComparisonMetric.class);

        for (ComparisonMetric metric : commonMetrics) {
            normalizedScores.put(metric, normalizeMetric(frameworks, metric));
        }

        // Calculate weighted composite scores
        List<RankedFramework> rankedFrameworks = new ArrayList<>();

        for (FrameworkPerformance framework : frameworks) {
            double compositeScore = 0.0;
            double totalWeight = 0.0;
            Map<ComparisonMetric, Double> frameworkNormalizedScores = new EnumMap<>(ComparisonMetric.class);

            for (ComparisonMetric metric : commonMetrics) {
                double weight = weights.getOrDefault(metric, 1.0);
                double normalizedScore = normalizedScores.get(metric).get(framework.getFrameworkName());

                frameworkNormalizedScores.put(metric, normalizedScore);
                compositeScore += normalizedScore * weight;
                totalWeight += weight;
            }

            if (totalWeight > 0) {
                compositeScore /= totalWeight;
            }

            rankedFrameworks.add(new RankedFramework(
                framework.getFrameworkName(),
                compositeScore,
                frameworkNormalizedScores
            ));
        }

        // Sort by composite score (descending)
        rankedFrameworks.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));

        ComparisonMetric primaryMetric = weights.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(commonMetrics.iterator().next());

        return new FrameworkRanking(rankedFrameworks, primaryMetric, "Weighted composite scoring");
    }

    /**
     * Rank frameworks using simple metric-based ranking
     */
    public static FrameworkRanking rankFrameworksByMetric(List<FrameworkPerformance> frameworks,
                                                        ComparisonMetric metric) {
        if (frameworks.isEmpty()) {
            return new FrameworkRanking(Collections.emptyList(), metric, "No frameworks provided");
        }

        List<RankedFramework> rankedFrameworks = frameworks.stream()
            .filter(f -> f.getAvailableMetrics().contains(metric))
            .map(f -> {
                StatisticalSummary summary = f.getMetric(metric);
                double score = metric.isHigherBetter() ? summary.getMean() : -summary.getMean();

                Map<ComparisonMetric, Double> scores = new EnumMap<>(ComparisonMetric.class);
                scores.put(metric, summary.getMean());

                return new RankedFramework(f.getFrameworkName(), score, scores);
            })
            .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
            .collect(Collectors.toList());

        return new FrameworkRanking(rankedFrameworks, metric, "Single metric ranking");
    }

    /**
     * Normalize scores for a specific metric across all frameworks
     */
    private static Map<String, Double> normalizeMetric(List<FrameworkPerformance> frameworks,
                                                     ComparisonMetric metric) {
        Map<String, Double> scores = new HashMap<>();

        // Collect raw scores
        List<Double> values = new ArrayList<>();
        for (FrameworkPerformance framework : frameworks) {
            StatisticalSummary summary = framework.getMetric(metric);
            if (summary != null) {
                double value = summary.getMean();
                scores.put(framework.getFrameworkName(), value);
                values.add(value);
            }
        }

        if (values.isEmpty()) {
            return scores;
        }

        // Find min/max for normalization
        double min = Collections.min(values);
        double max = Collections.max(values);
        double range = max - min;

        // Normalize to 0-1 scale
        Map<String, Double> normalizedScores = new HashMap<>();
        for (Map.Entry<String, Double> entry : scores.entrySet()) {
            double normalizedValue;
            if (range == 0) {
                normalizedValue = 0.5; // All values are the same
            } else {
                double rawNormalized = (entry.getValue() - min) / range;
                // Adjust for metric direction
                normalizedValue = metric.isHigherBetter() ? rawNormalized : (1.0 - rawNormalized);
            }
            normalizedScores.put(entry.getKey(), normalizedValue);
        }

        return normalizedScores;
    }

    /**
     * Generate a comprehensive comparison report
     */
    public static String generateComparisonReport(List<FrameworkPerformance> frameworks,
                                                Map<ComparisonMetric, Double> weights) {
        StringBuilder report = new StringBuilder();
        report.append("=== Serialization Framework Performance Comparison Report ===\n\n");

        // Overall ranking
        FrameworkRanking ranking = rankFrameworks(frameworks, weights);
        report.append("Overall Ranking:\n");
        report.append(ranking.toString()).append("\n");

        // Pairwise comparisons of top 3 frameworks
        List<RankedFramework> topFrameworks = ranking.getRankings().stream()
            .limit(3)
            .collect(Collectors.toList());

        if (topFrameworks.size() >= 2) {
            report.append("Detailed Comparisons (Top Performers):\n");
            report.append("=====================================\n");

            for (int i = 0; i < topFrameworks.size() - 1; i++) {
                String framework1 = topFrameworks.get(i).getFrameworkName();
                String framework2 = topFrameworks.get(i + 1).getFrameworkName();

                FrameworkPerformance fp1 = frameworks.stream()
                    .filter(f -> f.getFrameworkName().equals(framework1))
                    .findFirst().orElse(null);

                FrameworkPerformance fp2 = frameworks.stream()
                    .filter(f -> f.getFrameworkName().equals(framework2))
                    .findFirst().orElse(null);

                if (fp1 != null && fp2 != null) {
                    Map<ComparisonMetric, ComparisonResult> comparison = compareMultiMetric(fp2, fp1);
                    report.append(String.format("\n%s vs %s:\n", framework1, framework2));
                    for (ComparisonResult result : comparison.values()) {
                        report.append("  ").append(result.getInterpretation()).append("\n");
                    }
                }
            }
        }

        return report.toString();
    }
}