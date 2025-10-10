package org.techishthoughts.jackson.monitoring.metrics;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.techishthoughts.jackson.monitoring.ResourceMonitor;

import com.sun.management.OperatingSystemMXBean;

/**
 * Real-time CPU monitoring with detailed thread-level analysis.
 * Provides comprehensive CPU usage metrics including system and process level data.
 */
public class CPUMonitor implements ResourceMonitor {
    
    private static final String MONITOR_NAME = "CPU_Monitor";
    
    private final OperatingSystemMXBean osBean;
    private final ThreadMXBean threadBean;
    private final ScheduledExecutorService scheduler;
    private final AtomicBoolean monitoring = new AtomicBoolean(false);
    private final long samplingIntervalMs;
    
    // CPU metrics storage
    private final AtomicLong lastSystemTime = new AtomicLong(0);
    private final AtomicLong lastProcessTime = new AtomicLong(0);
    private final AtomicLong lastTimestamp = new AtomicLong(0);
    
    // Rolling statistics
    private final CircularBuffer<Double> systemCpuHistory;
    private final CircularBuffer<Double> processCpuHistory;
    private final CircularBuffer<Long> cpuTimeHistory;
    
    // Per-thread CPU tracking
    private final Map<Long, ThreadCpuInfo> threadCpuMap = new ConcurrentHashMap<>();
    
    // Current metrics cache
    private volatile CPUMetrics currentMetrics;
    
    public CPUMonitor() {
        this(1000, 1000); // Default 1 second sampling, 1000 samples history
    }
    
    public CPUMonitor(long samplingIntervalMs, int historySize) {
        this.samplingIntervalMs = samplingIntervalMs;
        this.osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        this.threadBean = ManagementFactory.getThreadMXBean();
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "CPU-Monitor");
            t.setDaemon(true);
            return t;
        });
        
        // Initialize circular buffers
        this.systemCpuHistory = new CircularBuffer<>(historySize);
        this.processCpuHistory = new CircularBuffer<>(historySize);
        this.cpuTimeHistory = new CircularBuffer<>(historySize);
        
        // Enable thread CPU timing if supported
        if (threadBean.isThreadCpuTimeSupported() && !threadBean.isThreadCpuTimeEnabled()) {
            threadBean.setThreadCpuTimeEnabled(true);
        }
        
        // Initialize current metrics
        this.currentMetrics = collectMetrics();
    }
    
    @Override
    public void startMonitoring() {
        if (monitoring.compareAndSet(false, true)) {
            scheduler.scheduleAtFixedRate(this::updateMetrics, 0, samplingIntervalMs, TimeUnit.MILLISECONDS);
        }
    }
    
    @Override
    public void stopMonitoring() {
        if (monitoring.compareAndSet(true, false)) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                scheduler.shutdownNow();
            }
        }
    }
    
    @Override
    public boolean isMonitoring() {
        return monitoring.get();
    }
    
    @Override
    public Map<String, Object> getCurrentMetrics() {
        CPUMetrics metrics = currentMetrics;
        if (metrics == null) {
            return new HashMap<>();
        }
        return metrics.toMap();
    }
    
    @Override
    public String getMonitorName() {
        return MONITOR_NAME;
    }
    
    @Override
    public void reset() {
        systemCpuHistory.clear();
        processCpuHistory.clear();
        cpuTimeHistory.clear();
        threadCpuMap.clear();
        lastSystemTime.set(0);
        lastProcessTime.set(0);
        lastTimestamp.set(0);
        currentMetrics = collectMetrics();
    }
    
    /**
     * Get detailed CPU metrics including historical data.
     */
    public CPUMetrics getDetailedMetrics() {
        return currentMetrics;
    }
    
    /**
     * Get top CPU consuming threads.
     */
    public Map<Long, ThreadCpuInfo> getTopThreadsByCPU(int limit) {
        return threadCpuMap.entrySet().stream()
                .sorted((e1, e2) -> Double.compare(e2.getValue().getCpuUsage(), e1.getValue().getCpuUsage()))
                .limit(limit)
                .collect(java.util.stream.Collectors.toMap(
                    Map.Entry::getKey,
                    Map.Entry::getValue,
                    (e1, e2) -> e1,
                    java.util.LinkedHashMap::new
                ));
    }
    
    /**
     * Calculate CPU usage statistics.
     */
    public CPUStatistics getStatistics() {
        if (processCpuHistory.isEmpty()) {
            return new CPUStatistics(0, 0, 0, 0, 0);
        }
        
        double[] cpuValues = processCpuHistory.getValues().stream()
                .mapToDouble(Double::doubleValue)
                .toArray();
        
        double mean = java.util.Arrays.stream(cpuValues).average().orElse(0);
        double min = java.util.Arrays.stream(cpuValues).min().orElse(0);
        double max = java.util.Arrays.stream(cpuValues).max().orElse(0);
        
        // Calculate standard deviation
        double variance = java.util.Arrays.stream(cpuValues)
                .map(cpu -> Math.pow(cpu - mean, 2))
                .average().orElse(0);
        double stdDev = Math.sqrt(variance);
        
        // Calculate 95th percentile
        java.util.Arrays.sort(cpuValues);
        int p95Index = (int) Math.ceil(cpuValues.length * 0.95) - 1;
        double p95 = cpuValues[Math.max(0, p95Index)];
        
        return new CPUStatistics(mean, min, max, stdDev, p95);
    }
    
    private void updateMetrics() {
        try {
            currentMetrics = collectMetrics();
        } catch (Exception e) {
            // Log error but continue monitoring
            System.err.println("Error updating CPU metrics: " + e.getMessage());
        }
    }
    
    private CPUMetrics collectMetrics() {
        long timestamp = System.currentTimeMillis();
        
        // System CPU usage
        double systemCpu = osBean.getSystemCpuLoad();
        if (systemCpu >= 0) {
            systemCpuHistory.add(systemCpu * 100);
        }
        
        // Process CPU usage
        double processCpu = osBean.getProcessCpuLoad();
        if (processCpu >= 0) {
            processCpuHistory.add(processCpu * 100);
        }
        
        // Process CPU time
        long currentProcessCpuTime = osBean.getProcessCpuTime();
        cpuTimeHistory.add(currentProcessCpuTime);
        
        // Update thread CPU information
        updateThreadCpuInfo(timestamp);
        
        return new CPUMetrics(
            systemCpu >= 0 ? systemCpu * 100 : -1,
            processCpu >= 0 ? processCpu * 100 : -1,
            currentProcessCpuTime,
            osBean.getAvailableProcessors(),
            timestamp,
            new HashMap<>(threadCpuMap)
        );
    }
    
    private void updateThreadCpuInfo(long timestamp) {
        if (!threadBean.isThreadCpuTimeSupported()) {
            return;
        }
        
        long[] threadIds = threadBean.getAllThreadIds();
        for (long threadId : threadIds) {
            try {
                long cpuTime = threadBean.getThreadCpuTime(threadId);
                if (cpuTime >= 0) {
                    ThreadCpuInfo oldInfo = threadCpuMap.get(threadId);
                    
                    if (oldInfo != null) {
                        long timeDelta = timestamp - oldInfo.getLastUpdateTime();
                        long cpuDelta = cpuTime - oldInfo.getCpuTime();
                        
                        if (timeDelta > 0) {
                            // Calculate CPU usage percentage
                            double cpuUsage = (cpuDelta / 1_000_000.0) / timeDelta * 100.0;
                            threadCpuMap.put(threadId, new ThreadCpuInfo(
                                threadId,
                                threadBean.getThreadInfo(threadId).getThreadName(),
                                cpuTime,
                                cpuUsage,
                                timestamp
                            ));
                        }
                    } else {
                        // First time seeing this thread
                        threadCpuMap.put(threadId, new ThreadCpuInfo(
                            threadId,
                            threadBean.getThreadInfo(threadId).getThreadName(),
                            cpuTime,
                            0.0,
                            timestamp
                        ));
                    }
                }
            } catch (Exception e) {
                // Thread may have terminated, ignore
            }
        }
        
        // Remove terminated threads
        threadCpuMap.entrySet().removeIf(entry -> {
            long threadId = entry.getKey();
            return threadBean.getThreadInfo(threadId) == null;
        });
    }
    
    /**
     * CPU metrics snapshot.
     */
    public static class CPUMetrics {
        private final double systemCpuUsage;
        private final double processCpuUsage;
        private final long processCpuTime;
        private final int availableProcessors;
        private final long timestamp;
        private final Map<Long, ThreadCpuInfo> threadCpuMap;
        
        public CPUMetrics(double systemCpuUsage, double processCpuUsage, long processCpuTime,
                         int availableProcessors, long timestamp, Map<Long, ThreadCpuInfo> threadCpuMap) {
            this.systemCpuUsage = systemCpuUsage;
            this.processCpuUsage = processCpuUsage;
            this.processCpuTime = processCpuTime;
            this.availableProcessors = availableProcessors;
            this.timestamp = timestamp;
            this.threadCpuMap = threadCpuMap;
        }
        
        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("systemCpuUsage", systemCpuUsage);
            map.put("processCpuUsage", processCpuUsage);
            map.put("processCpuTime", processCpuTime);
            map.put("availableProcessors", availableProcessors);
            map.put("timestamp", timestamp);
            map.put("threadCount", threadCpuMap.size());
            return map;
        }
        
        // Getters
        public double getSystemCpuUsage() { return systemCpuUsage; }
        public double getProcessCpuUsage() { return processCpuUsage; }
        public long getProcessCpuTime() { return processCpuTime; }
        public int getAvailableProcessors() { return availableProcessors; }
        public long getTimestamp() { return timestamp; }
        public Map<Long, ThreadCpuInfo> getThreadCpuMap() { return threadCpuMap; }
    }
    
    /**
     * Thread CPU information.
     */
    public static class ThreadCpuInfo {
        private final long threadId;
        private final String threadName;
        private final long cpuTime;
        private final double cpuUsage;
        private final long lastUpdateTime;
        
        public ThreadCpuInfo(long threadId, String threadName, long cpuTime, 
                           double cpuUsage, long lastUpdateTime) {
            this.threadId = threadId;
            this.threadName = threadName;
            this.cpuTime = cpuTime;
            this.cpuUsage = cpuUsage;
            this.lastUpdateTime = lastUpdateTime;
        }
        
        // Getters
        public long getThreadId() { return threadId; }
        public String getThreadName() { return threadName; }
        public long getCpuTime() { return cpuTime; }
        public double getCpuUsage() { return cpuUsage; }
        public long getLastUpdateTime() { return lastUpdateTime; }
    }
    
    /**
     * CPU usage statistics.
     */
    public static class CPUStatistics {
        private final double mean;
        private final double min;
        private final double max;
        private final double stdDev;
        private final double p95;
        
        public CPUStatistics(double mean, double min, double max, double stdDev, double p95) {
            this.mean = mean;
            this.min = min;
            this.max = max;
            this.stdDev = stdDev;
            this.p95 = p95;
        }
        
        // Getters
        public double getMean() { return mean; }
        public double getMin() { return min; }
        public double getMax() { return max; }
        public double getStdDev() { return stdDev; }
        public double getP95() { return p95; }
    }
    
    /**
     * Simple circular buffer for storing historical data.
     */
    private static class CircularBuffer<T> {
        private final Object[] buffer;
        private final int capacity;
        private int head = 0;
        private int size = 0;
        
        public CircularBuffer(int capacity) {
            this.capacity = capacity;
            this.buffer = new Object[capacity];
        }
        
        public synchronized void add(T item) {
            buffer[head] = item;
            head = (head + 1) % capacity;
            if (size < capacity) {
                size++;
            }
        }
        
        @SuppressWarnings("unchecked")
        public synchronized java.util.List<T> getValues() {
            java.util.List<T> values = new java.util.ArrayList<>(size);
            int start = size < capacity ? 0 : head;
            for (int i = 0; i < size; i++) {
                values.add((T) buffer[(start + i) % capacity]);
            }
            return values;
        }
        
        public synchronized void clear() {
            head = 0;
            size = 0;
            java.util.Arrays.fill(buffer, null);
        }
        
        public synchronized boolean isEmpty() {
            return size == 0;
        }
    }
}