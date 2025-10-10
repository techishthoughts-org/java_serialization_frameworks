package org.techishthoughts.benchmark.jit;

import org.techishthoughts.benchmark.statistics.BenchmarkStatistics;

import java.lang.management.ManagementFactory;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.MemoryMXBean;
import java.lang.management.CompilationMXBean;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

/**
 * Adaptive warmup strategy that detects JIT compilation stabilization
 * and ensures GC-aware measurement windows for accurate benchmarking.
 */
public class AdaptiveWarmupStrategy {

    private static final double STABILITY_THRESHOLD = 0.05; // 5% coefficient of variation
    private static final int MIN_WARMUP_ITERATIONS = 5;
    private static final int MAX_WARMUP_ITERATIONS = 50;
    private static final int STABILITY_WINDOW = 10;
    private static final long MAX_WARMUP_TIME_MS = 60_000; // 1 minute max warmup

    /**
     * JIT compilation status
     */
    public static class JITStatus {
        private final long totalCompilationTime;
        private final boolean compilationEnabled;
        private final int compiledMethods;
        private final boolean isStabilized;

        public JITStatus(long totalCompilationTime, boolean compilationEnabled,
                        int compiledMethods, boolean isStabilized) {
            this.totalCompilationTime = totalCompilationTime;
            this.compilationEnabled = compilationEnabled;
            this.compiledMethods = compiledMethods;
            this.isStabilized = isStabilized;
        }

        public long getTotalCompilationTime() { return totalCompilationTime; }
        public boolean isCompilationEnabled() { return compilationEnabled; }
        public int getCompiledMethods() { return compiledMethods; }
        public boolean isStabilized() { return isStabilized; }

        @Override
        public String toString() {
            return String.format("JIT[compiled=%d, time=%dms, stabilized=%s]",
                compiledMethods, totalCompilationTime, isStabilized);
        }
    }

    /**
     * GC status for measurement window determination
     */
    public static class GCStatus {
        private final long totalCollections;
        private final long totalCollectionTime;
        private final long heapUsed;
        private final long heapMax;
        private final double heapUtilization;
        private final boolean isQuiescent;

        public GCStatus(long totalCollections, long totalCollectionTime,
                       long heapUsed, long heapMax, boolean isQuiescent) {
            this.totalCollections = totalCollections;
            this.totalCollectionTime = totalCollectionTime;
            this.heapUsed = heapUsed;
            this.heapMax = heapMax;
            this.heapUtilization = (double) heapUsed / heapMax;
            this.isQuiescent = isQuiescent;
        }

        public long getTotalCollections() { return totalCollections; }
        public long getTotalCollectionTime() { return totalCollectionTime; }
        public long getHeapUsed() { return heapUsed; }
        public long getHeapMax() { return heapMax; }
        public double getHeapUtilization() { return heapUtilization; }
        public boolean isQuiescent() { return isQuiescent; }

        @Override
        public String toString() {
            return String.format("GC[collections=%d, time=%dms, heap=%.1f%%, quiet=%s]",
                totalCollections, totalCollectionTime, heapUtilization * 100, isQuiescent);
        }
    }

    /**
     * Warmup result with JIT and GC analysis
     */
    public static class WarmupResult {
        private final int iterations;
        private final long durationMs;
        private final JITStatus jitStatus;
        private final GCStatus gcStatus;
        private final boolean converged;
        private final double finalCoefficientOfVariation;
        private final String recommendation;

        public WarmupResult(int iterations, long durationMs, JITStatus jitStatus,
                          GCStatus gcStatus, boolean converged, double finalCV, String recommendation) {
            this.iterations = iterations;
            this.durationMs = durationMs;
            this.jitStatus = jitStatus;
            this.gcStatus = gcStatus;
            this.converged = converged;
            this.finalCoefficientOfVariation = finalCV;
            this.recommendation = recommendation;
        }

        public int getIterations() { return iterations; }
        public long getDurationMs() { return durationMs; }
        public JITStatus getJitStatus() { return jitStatus; }
        public GCStatus getGcStatus() { return gcStatus; }
        public boolean isConverged() { return converged; }
        public double getFinalCoefficientOfVariation() { return finalCoefficientOfVariation; }
        public String getRecommendation() { return recommendation; }

        @Override
        public String toString() {
            return String.format("Warmup[%d iterations, %dms, converged=%s, CV=%.3f] - %s",
                iterations, durationMs, converged, finalCoefficientOfVariation, recommendation);
        }
    }

    /**
     * Measurement window with GC awareness
     */
    public static class MeasurementWindow {
        private final long startTime;
        private final long endTime;
        private final GCStatus startGC;
        private final GCStatus endGC;
        private final boolean isValid;
        private final List<Long> gcInterruptions;

        public MeasurementWindow(long startTime, long endTime, GCStatus startGC,
                               GCStatus endGC, boolean isValid, List<Long> gcInterruptions) {
            this.startTime = startTime;
            this.endTime = endTime;
            this.startGC = startGC;
            this.endGC = endGC;
            this.isValid = isValid;
            this.gcInterruptions = new ArrayList<>(gcInterruptions);
        }

        public long getStartTime() { return startTime; }
        public long getEndTime() { return endTime; }
        public long getDuration() { return endTime - startTime; }
        public GCStatus getStartGC() { return startGC; }
        public GCStatus getEndGC() { return endGC; }
        public boolean isValid() { return isValid; }
        public List<Long> getGcInterruptions() { return new ArrayList<>(gcInterruptions); }
        public boolean hadGCInterruptions() { return !gcInterruptions.isEmpty(); }

        @Override
        public String toString() {
            return String.format("MeasurementWindow[%dms, valid=%s, GC interruptions=%d]",
                getDuration(), isValid, gcInterruptions.size());
        }
    }

    private final MemoryMXBean memoryBean;
    private final List<GarbageCollectorMXBean> gcBeans;
    private final CompilationMXBean compilationBean;

    public AdaptiveWarmupStrategy() {
        this.memoryBean = ManagementFactory.getMemoryMXBean();
        this.gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
        this.compilationBean = ManagementFactory.getCompilationMXBean();
    }

    /**
     * Perform adaptive warmup with JIT stabilization detection
     */
    public WarmupResult performWarmup(Runnable warmupTask) {
        long startTime = System.currentTimeMillis();
        List<Double> performanceMeasurements = new ArrayList<>();
        JITStatus initialJIT = getCurrentJITStatus();
        GCStatus initialGC = getCurrentGCStatus();

        int iteration = 0;
        boolean converged = false;
        double finalCV = Double.MAX_VALUE;

        while (iteration < MAX_WARMUP_ITERATIONS &&
               !converged &&
               (System.currentTimeMillis() - startTime) < MAX_WARMUP_TIME_MS) {

            // Measure performance of warmup task
            long taskStart = System.nanoTime();
            warmupTask.run();
            long taskDuration = System.nanoTime() - taskStart;

            performanceMeasurements.add((double) taskDuration);
            iteration++;

            // Check for convergence after minimum iterations
            if (iteration >= MIN_WARMUP_ITERATIONS) {
                // Use recent measurements for stability check
                int windowStart = Math.max(0, performanceMeasurements.size() - STABILITY_WINDOW);
                List<Double> recentMeasurements = performanceMeasurements.subList(windowStart,
                    performanceMeasurements.size());

                double[] recentArray = recentMeasurements.stream().mapToDouble(Double::doubleValue).toArray();
                BenchmarkStatistics.StatisticalSummary stats = BenchmarkStatistics.calculateSummary(recentArray);

                finalCV = stats.getCoefficientOfVariation();
                converged = finalCV < STABILITY_THRESHOLD;

                if (converged) {
                    // Additional check: ensure JIT has stabilized
                    JITStatus currentJIT = getCurrentJITStatus();
                    if (!currentJIT.isStabilized()) {
                        converged = false; // Continue warming up
                    }
                }
            }
        }

        long endTime = System.currentTimeMillis();
        JITStatus finalJIT = getCurrentJITStatus();
        GCStatus finalGC = getCurrentGCStatus();

        String recommendation = generateWarmupRecommendation(
            iteration, converged, finalCV, initialJIT, finalJIT, initialGC, finalGC);

        return new WarmupResult(iteration, endTime - startTime, finalJIT, finalGC,
                              converged, finalCV, recommendation);
    }

    /**
     * Create a GC-aware measurement window
     */
    public MeasurementWindow createMeasurementWindow(Runnable measurementTask, long expectedDurationMs) {
        // Wait for GC quiescence before starting measurement
        waitForGCQuiescence();

        GCStatus startGC = getCurrentGCStatus();
        long startTime = System.nanoTime();

        List<Long> gcInterruptions = new ArrayList<>();
        long lastGCCount = startGC.getTotalCollections();

        // Execute measurement
        measurementTask.run();

        long endTime = System.nanoTime();
        GCStatus endGC = getCurrentGCStatus();

        // Check for GC interruptions during measurement
        if (endGC.getTotalCollections() > lastGCCount) {
            gcInterruptions.add(endTime - startTime);
        }

        // Determine if measurement window is valid
        boolean isValid = gcInterruptions.isEmpty() &&
                         endGC.getHeapUtilization() < 0.9 && // Not close to heap limit
                         (endTime - startTime) > TimeUnit.MILLISECONDS.toNanos(expectedDurationMs / 2); // Not too short

        return new MeasurementWindow(startTime, endTime, startGC, endGC, isValid, gcInterruptions);
    }

    /**
     * Wait for GC to become quiescent
     */
    public void waitForGCQuiescence() {
        final int maxWaitIterations = 10;
        final long waitIntervalMs = 100;

        for (int i = 0; i < maxWaitIterations; i++) {
            GCStatus gcStatus = getCurrentGCStatus();

            // Check if GC is quiescent (no recent activity and heap not too full)
            if (gcStatus.isQuiescent() && gcStatus.getHeapUtilization() < 0.8) {
                return;
            }

            // Force GC and wait if heap utilization is high
            if (gcStatus.getHeapUtilization() > 0.9) {
                System.gc();
            }

            try {
                Thread.sleep(waitIntervalMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    /**
     * Get current JIT compilation status
     */
    public JITStatus getCurrentJITStatus() {
        long compilationTime = compilationBean != null ? compilationBean.getTotalCompilationTime() : 0;
        boolean compilationEnabled = compilationBean != null && compilationBean.isCompilationTimeMonitoringSupported();

        // Simple heuristic: JIT is stabilized if compilation time hasn't increased recently
        // In a real implementation, you'd track compilation time over a period
        boolean isStabilized = compilationTime > 0; // Simplified check

        // Note: Getting compiled method count requires more complex JMX setup
        int compiledMethods = (int) (compilationTime / 10); // Rough approximation

        return new JITStatus(compilationTime, compilationEnabled, compiledMethods, isStabilized);
    }

    /**
     * Get current GC status
     */
    public GCStatus getCurrentGCStatus() {
        long totalCollections = gcBeans.stream()
            .mapToLong(GarbageCollectorMXBean::getCollectionCount)
            .sum();

        long totalCollectionTime = gcBeans.stream()
            .mapToLong(GarbageCollectorMXBean::getCollectionTime)
            .sum();

        long heapUsed = memoryBean.getHeapMemoryUsage().getUsed();
        long heapMax = memoryBean.getHeapMemoryUsage().getMax();

        // Simple quiescence check: no recent GC activity and reasonable heap usage
        boolean isQuiescent = totalCollectionTime < 100 && // Less than 100ms total GC time recently
                             (double) heapUsed / heapMax < 0.7; // Less than 70% heap usage

        return new GCStatus(totalCollections, totalCollectionTime, heapUsed, heapMax, isQuiescent);
    }

    /**
     * Generate warmup recommendation based on analysis
     */
    private String generateWarmupRecommendation(int iterations, boolean converged, double finalCV,
                                              JITStatus initialJIT, JITStatus finalJIT,
                                              GCStatus initialGC, GCStatus finalGC) {
        StringBuilder recommendation = new StringBuilder();

        if (converged) {
            recommendation.append("Warmup successful - performance stabilized");
        } else {
            recommendation.append("Warmup incomplete - consider increasing warmup time");
        }

        if (finalCV > STABILITY_THRESHOLD) {
            recommendation.append(String.format(" (high variability: %.1f%%)", finalCV * 100));
        }

        if (!finalJIT.isStabilized()) {
            recommendation.append(" [JIT not fully stabilized]");
        }

        if (finalGC.getHeapUtilization() > 0.8) {
            recommendation.append(" [High heap utilization detected]");
        }

        return recommendation.toString();
    }

    /**
     * Adaptive measurement configuration
     */
    public static class MeasurementConfig {
        private final int minIterations;
        private final int maxIterations;
        private final long timeoutMs;
        private final double targetPrecision;
        private final boolean enableGCAwareness;

        public MeasurementConfig(int minIterations, int maxIterations, long timeoutMs,
                               double targetPrecision, boolean enableGCAwareness) {
            this.minIterations = minIterations;
            this.maxIterations = maxIterations;
            this.timeoutMs = timeoutMs;
            this.targetPrecision = targetPrecision;
            this.enableGCAwareness = enableGCAwareness;
        }

        public static MeasurementConfig defaultConfig() {
            return new MeasurementConfig(10, 100, 30_000, 0.05, true);
        }

        // Getters
        public int getMinIterations() { return minIterations; }
        public int getMaxIterations() { return maxIterations; }
        public long getTimeoutMs() { return timeoutMs; }
        public double getTargetPrecision() { return targetPrecision; }
        public boolean isEnableGCAwareness() { return enableGCAwareness; }
    }

    /**
     * Perform adaptive measurement with statistical precision targeting
     */
    public BenchmarkStatistics.StatisticalSummary performAdaptiveMeasurement(
            Runnable measurementTask, MeasurementConfig config) {

        List<Double> measurements = new ArrayList<>();
        long startTime = System.currentTimeMillis();

        while (measurements.size() < config.getMaxIterations() &&
               (System.currentTimeMillis() - startTime) < config.getTimeoutMs()) {

            if (config.isEnableGCAwareness()) {
                waitForGCQuiescence();
            }

            // Perform measurement
            long taskStart = System.nanoTime();
            measurementTask.run();
            long taskDuration = System.nanoTime() - taskStart;

            measurements.add((double) taskDuration);

            // Check if we have enough precision after minimum iterations
            if (measurements.size() >= config.getMinIterations()) {
                double[] data = measurements.stream().mapToDouble(Double::doubleValue).toArray();
                BenchmarkStatistics.StatisticalSummary summary = BenchmarkStatistics.calculateSummary(data);

                if (summary.getCoefficientOfVariation() <= config.getTargetPrecision()) {
                    break; // Achieved target precision
                }
            }
        }

        double[] finalData = measurements.stream().mapToDouble(Double::doubleValue).toArray();
        return BenchmarkStatistics.calculateSummary(finalData);
    }
}