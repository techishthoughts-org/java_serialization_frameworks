package org.techishthoughts.jackson.monitoring.metrics;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.Notification;
import javax.management.NotificationEmitter;
import javax.management.NotificationListener;

import org.techishthoughts.jackson.monitoring.ResourceMonitor;

import com.sun.management.GarbageCollectionNotificationInfo;

/**
 * Comprehensive memory monitoring with heap analysis, GC tracking, and memory pool monitoring.
 * Provides detailed insights into JVM memory usage patterns and garbage collection behavior.
 */
public class MemoryMonitor implements ResourceMonitor, NotificationListener {
    
    private static final String MONITOR_NAME = "Memory_Monitor";
    
    private final MemoryMXBean memoryBean;
    private final List<MemoryPoolMXBean> memoryPools;
    private final List<GarbageCollectorMXBean> garbageCollectors;
    private final ScheduledExecutorService scheduler;
    private final AtomicBoolean monitoring = new AtomicBoolean(false);
    private final long samplingIntervalMs;
    
    // Memory metrics storage
    private final CircularBuffer<MemorySnapshot> memoryHistory;
    private final Map<String, MemoryPoolMetrics> poolMetrics = new ConcurrentHashMap<>();
    private final Map<String, GCMetrics> gcMetrics = new ConcurrentHashMap<>();
    
    // GC event tracking
    private final AtomicLong totalGcTime = new AtomicLong(0);
    private final AtomicLong totalGcCount = new AtomicLong(0);
    private final AtomicLong lastGcTime = new AtomicLong(0);
    
    // Memory leak detection
    private final Map<String, Long> memoryGrowthTracker = new ConcurrentHashMap<>();
    private final AtomicLong consecutiveGrowthCount = new AtomicLong(0);
    
    // Current metrics cache
    private volatile MemoryMetrics currentMetrics;
    
    public MemoryMonitor() {
        this(1000, 1000); // Default 1 second sampling, 1000 samples history
    }
    
    public MemoryMonitor(long samplingIntervalMs, int historySize) {
        this.samplingIntervalMs = samplingIntervalMs;
        this.memoryBean = ManagementFactory.getMemoryMXBean();
        this.memoryPools = ManagementFactory.getMemoryPoolMXBeans();
        this.garbageCollectors = ManagementFactory.getGarbageCollectorMXBeans();
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "Memory-Monitor");
            t.setDaemon(true);
            return t;
        });
        
        // Initialize circular buffer
        this.memoryHistory = new CircularBuffer<>(historySize);
        
        // Setup GC notification listeners
        setupGCNotifications();
        
        // Initialize metrics
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
        MemoryMetrics metrics = currentMetrics;
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
        memoryHistory.clear();
        poolMetrics.clear();
        gcMetrics.clear();
        memoryGrowthTracker.clear();
        totalGcTime.set(0);
        totalGcCount.set(0);
        lastGcTime.set(0);
        consecutiveGrowthCount.set(0);
        currentMetrics = collectMetrics();
    }
    
    /**
     * Get detailed memory metrics including historical data.
     */
    public MemoryMetrics getDetailedMetrics() {
        return currentMetrics;
    }
    
    /**
     * Get memory pool specific metrics.
     */
    public Map<String, MemoryPoolMetrics> getMemoryPoolMetrics() {
        return new HashMap<>(poolMetrics);
    }
    
    /**
     * Get garbage collection metrics.
     */
    public Map<String, GCMetrics> getGCMetrics() {
        return new HashMap<>(gcMetrics);
    }
    
    /**
     * Check for potential memory leaks.
     */
    public MemoryLeakAnalysis analyzeMemoryLeaks() {
        if (memoryHistory.size() < 10) {
            return new MemoryLeakAnalysis(false, "Insufficient data for analysis", 0);
        }
        
        List<MemorySnapshot> snapshots = memoryHistory.getValues();
        
        // Check heap usage trend
        long growthCount = 0;
        double totalGrowth = 0;
        
        for (int i = 1; i < snapshots.size(); i++) {
            MemorySnapshot current = snapshots.get(i);
            MemorySnapshot previous = snapshots.get(i - 1);
            
            double growth = ((double) current.getHeapUsed() - previous.getHeapUsed()) / previous.getHeapUsed();
            if (growth > 0.01) { // 1% growth threshold
                growthCount++;
                totalGrowth += growth;
            }
        }
        
        double growthRate = growthCount / (double) (snapshots.size() - 1);
        boolean suspiciousGrowth = growthRate > 0.7 && totalGrowth > 0.2; // 70% samples showing growth, 20% total growth
        
        return new MemoryLeakAnalysis(suspiciousGrowth, 
            suspiciousGrowth ? "Suspicious memory growth pattern detected" : "No memory leak indicators found",
            totalGrowth);
    }
    
    /**
     * Get memory usage statistics.
     */
    public MemoryStatistics getStatistics() {
        if (memoryHistory.isEmpty()) {
            return new MemoryStatistics(0, 0, 0, 0, 0, 0, 0, 0);
        }
        
        List<MemorySnapshot> snapshots = memoryHistory.getValues();
        
        long[] heapValues = snapshots.stream().mapToLong(MemorySnapshot::getHeapUsed).toArray();
        long[] nonHeapValues = snapshots.stream().mapToLong(MemorySnapshot::getNonHeapUsed).toArray();
        
        return new MemoryStatistics(
            calculateMean(heapValues),
            calculateMin(heapValues),
            calculateMax(heapValues),
            calculatePercentile(heapValues, 0.95),
            calculateMean(nonHeapValues),
            calculateMin(nonHeapValues),
            calculateMax(nonHeapValues),
            calculatePercentile(nonHeapValues, 0.95)
        );
    }
    
    @Override
    public void handleNotification(Notification notification, Object handback) {
        if (notification.getType().equals(GarbageCollectionNotificationInfo.GARBAGE_COLLECTION_NOTIFICATION)) {
            GarbageCollectionNotificationInfo info = GarbageCollectionNotificationInfo.from(
                (javax.management.openmbean.CompositeData) notification.getUserData());
            
            String gcName = info.getGcName();
            String gcAction = info.getGcAction();
            long duration = info.getGcInfo().getDuration();
            
            totalGcTime.addAndGet(duration);
            totalGcCount.incrementAndGet();
            lastGcTime.set(System.currentTimeMillis());
            
            // Update GC metrics
            GCMetrics existingMetrics = gcMetrics.get(gcName);
            if (existingMetrics != null) {
                gcMetrics.put(gcName, new GCMetrics(
                    gcName,
                    gcAction,
                    existingMetrics.getCollectionCount() + 1,
                    existingMetrics.getCollectionTime() + duration,
                    duration,
                    System.currentTimeMillis()
                ));
            } else {
                gcMetrics.put(gcName, new GCMetrics(gcName, gcAction, 1, duration, duration, System.currentTimeMillis()));
            }
        }
    }
    
    private void setupGCNotifications() {
        for (GarbageCollectorMXBean gcBean : garbageCollectors) {
            if (gcBean instanceof NotificationEmitter) {
                NotificationEmitter emitter = (NotificationEmitter) gcBean;
                emitter.addNotificationListener(this, null, null);
            }
        }
    }
    
    private void updateMetrics() {
        try {
            currentMetrics = collectMetrics();
            
            // Update memory pool metrics
            updateMemoryPoolMetrics();
            
            // Check for memory growth patterns
            detectMemoryGrowthPatterns();
            
        } catch (Exception e) {
            System.err.println("Error updating memory metrics: " + e.getMessage());
        }
    }
    
    private MemoryMetrics collectMetrics() {
        long timestamp = System.currentTimeMillis();
        
        // Heap memory usage
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        
        // Non-heap memory usage
        MemoryUsage nonHeapUsage = memoryBean.getNonHeapMemoryUsage();
        
        // Create memory snapshot
        MemorySnapshot snapshot = new MemorySnapshot(
            heapUsage.getUsed(),
            heapUsage.getMax(),
            heapUsage.getCommitted(),
            nonHeapUsage.getUsed(),
            nonHeapUsage.getMax(),
            nonHeapUsage.getCommitted(),
            memoryBean.getObjectPendingFinalizationCount(),
            timestamp
        );
        
        memoryHistory.add(snapshot);
        
        return new MemoryMetrics(
            snapshot,
            new HashMap<>(poolMetrics),
            new HashMap<>(gcMetrics),
            totalGcTime.get(),
            totalGcCount.get(),
            lastGcTime.get()
        );
    }
    
    private void updateMemoryPoolMetrics() {
        for (MemoryPoolMXBean pool : memoryPools) {
            MemoryUsage usage = pool.getUsage();
            if (usage != null) {
                poolMetrics.put(pool.getName(), new MemoryPoolMetrics(
                    pool.getName(),
                    pool.getType().toString(),
                    usage.getUsed(),
                    usage.getMax(),
                    usage.getCommitted(),
                    pool.getUsageThreshold(),
                    pool.isUsageThresholdSupported() && pool.isUsageThresholdExceeded(),
                    System.currentTimeMillis()
                ));
            }
        }
    }
    
    private void detectMemoryGrowthPatterns() {
        if (memoryHistory.size() < 5) {
            return; // Need at least 5 samples
        }
        
        List<MemorySnapshot> recent = memoryHistory.getValues();
        MemorySnapshot latest = recent.get(recent.size() - 1);
        MemorySnapshot fiveAgo = recent.get(recent.size() - 5);
        
        double heapGrowth = ((double) latest.getHeapUsed() - fiveAgo.getHeapUsed()) / fiveAgo.getHeapUsed();
        
        if (heapGrowth > 0.05) { // 5% growth threshold
            consecutiveGrowthCount.incrementAndGet();
        } else {
            consecutiveGrowthCount.set(0);
        }
    }
    
    private double calculateMean(long[] values) {
        return java.util.Arrays.stream(values).average().orElse(0);
    }
    
    private long calculateMin(long[] values) {
        return java.util.Arrays.stream(values).min().orElse(0);
    }
    
    private long calculateMax(long[] values) {
        return java.util.Arrays.stream(values).max().orElse(0);
    }
    
    private long calculatePercentile(long[] values, double percentile) {
        long[] sorted = values.clone();
        java.util.Arrays.sort(sorted);
        int index = (int) Math.ceil(sorted.length * percentile) - 1;
        return sorted[Math.max(0, index)];
    }
    
    // Data classes
    public static class MemorySnapshot {
        private final long heapUsed;
        private final long heapMax;
        private final long heapCommitted;
        private final long nonHeapUsed;
        private final long nonHeapMax;
        private final long nonHeapCommitted;
        private final int objectsPendingFinalization;
        private final long timestamp;
        
        public MemorySnapshot(long heapUsed, long heapMax, long heapCommitted,
                            long nonHeapUsed, long nonHeapMax, long nonHeapCommitted,
                            int objectsPendingFinalization, long timestamp) {
            this.heapUsed = heapUsed;
            this.heapMax = heapMax;
            this.heapCommitted = heapCommitted;
            this.nonHeapUsed = nonHeapUsed;
            this.nonHeapMax = nonHeapMax;
            this.nonHeapCommitted = nonHeapCommitted;
            this.objectsPendingFinalization = objectsPendingFinalization;
            this.timestamp = timestamp;
        }
        
        // Getters
        public long getHeapUsed() { return heapUsed; }
        public long getHeapMax() { return heapMax; }
        public long getHeapCommitted() { return heapCommitted; }
        public long getNonHeapUsed() { return nonHeapUsed; }
        public long getNonHeapMax() { return nonHeapMax; }
        public long getNonHeapCommitted() { return nonHeapCommitted; }
        public int getObjectsPendingFinalization() { return objectsPendingFinalization; }
        public long getTimestamp() { return timestamp; }
    }
    
    public static class MemoryMetrics {
        private final MemorySnapshot currentSnapshot;
        private final Map<String, MemoryPoolMetrics> poolMetrics;
        private final Map<String, GCMetrics> gcMetrics;
        private final long totalGcTime;
        private final long totalGcCount;
        private final long lastGcTime;
        
        public MemoryMetrics(MemorySnapshot currentSnapshot, Map<String, MemoryPoolMetrics> poolMetrics,
                           Map<String, GCMetrics> gcMetrics, long totalGcTime, long totalGcCount, long lastGcTime) {
            this.currentSnapshot = currentSnapshot;
            this.poolMetrics = poolMetrics;
            this.gcMetrics = gcMetrics;
            this.totalGcTime = totalGcTime;
            this.totalGcCount = totalGcCount;
            this.lastGcTime = lastGcTime;
        }
        
        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("heapUsed", currentSnapshot.getHeapUsed());
            map.put("heapMax", currentSnapshot.getHeapMax());
            map.put("heapCommitted", currentSnapshot.getHeapCommitted());
            map.put("nonHeapUsed", currentSnapshot.getNonHeapUsed());
            map.put("nonHeapMax", currentSnapshot.getNonHeapMax());
            map.put("nonHeapCommitted", currentSnapshot.getNonHeapCommitted());
            map.put("objectsPendingFinalization", currentSnapshot.getObjectsPendingFinalization());
            map.put("totalGcTime", totalGcTime);
            map.put("totalGcCount", totalGcCount);
            map.put("lastGcTime", lastGcTime);
            map.put("timestamp", currentSnapshot.getTimestamp());
            return map;
        }
        
        // Getters
        public MemorySnapshot getCurrentSnapshot() { return currentSnapshot; }
        public Map<String, MemoryPoolMetrics> getPoolMetrics() { return poolMetrics; }
        public Map<String, GCMetrics> getGcMetrics() { return gcMetrics; }
        public long getTotalGcTime() { return totalGcTime; }
        public long getTotalGcCount() { return totalGcCount; }
        public long getLastGcTime() { return lastGcTime; }
    }
    
    public static class MemoryPoolMetrics {
        private final String name;
        private final String type;
        private final long used;
        private final long max;
        private final long committed;
        private final long usageThreshold;
        private final boolean thresholdExceeded;
        private final long timestamp;
        
        public MemoryPoolMetrics(String name, String type, long used, long max, long committed,
                               long usageThreshold, boolean thresholdExceeded, long timestamp) {
            this.name = name;
            this.type = type;
            this.used = used;
            this.max = max;
            this.committed = committed;
            this.usageThreshold = usageThreshold;
            this.thresholdExceeded = thresholdExceeded;
            this.timestamp = timestamp;
        }
        
        // Getters
        public String getName() { return name; }
        public String getType() { return type; }
        public long getUsed() { return used; }
        public long getMax() { return max; }
        public long getCommitted() { return committed; }
        public long getUsageThreshold() { return usageThreshold; }
        public boolean isThresholdExceeded() { return thresholdExceeded; }
        public long getTimestamp() { return timestamp; }
    }
    
    public static class GCMetrics {
        private final String name;
        private final String action;
        private final long collectionCount;
        private final long collectionTime;
        private final long lastCollectionDuration;
        private final long lastCollectionTime;
        
        public GCMetrics(String name, String action, long collectionCount, long collectionTime,
                       long lastCollectionDuration, long lastCollectionTime) {
            this.name = name;
            this.action = action;
            this.collectionCount = collectionCount;
            this.collectionTime = collectionTime;
            this.lastCollectionDuration = lastCollectionDuration;
            this.lastCollectionTime = lastCollectionTime;
        }
        
        // Getters
        public String getName() { return name; }
        public String getAction() { return action; }
        public long getCollectionCount() { return collectionCount; }
        public long getCollectionTime() { return collectionTime; }
        public long getLastCollectionDuration() { return lastCollectionDuration; }
        public long getLastCollectionTime() { return lastCollectionTime; }
    }
    
    public static class MemoryLeakAnalysis {
        private final boolean suspiciousGrowth;
        private final String analysis;
        private final double totalGrowthRate;
        
        public MemoryLeakAnalysis(boolean suspiciousGrowth, String analysis, double totalGrowthRate) {
            this.suspiciousGrowth = suspiciousGrowth;
            this.analysis = analysis;
            this.totalGrowthRate = totalGrowthRate;
        }
        
        // Getters
        public boolean isSuspiciousGrowth() { return suspiciousGrowth; }
        public String getAnalysis() { return analysis; }
        public double getTotalGrowthRate() { return totalGrowthRate; }
    }
    
    public static class MemoryStatistics {
        private final double heapMean;
        private final long heapMin;
        private final long heapMax;
        private final long heapP95;
        private final double nonHeapMean;
        private final long nonHeapMin;
        private final long nonHeapMax;
        private final long nonHeapP95;
        
        public MemoryStatistics(double heapMean, long heapMin, long heapMax, long heapP95,
                              double nonHeapMean, long nonHeapMin, long nonHeapMax, long nonHeapP95) {
            this.heapMean = heapMean;
            this.heapMin = heapMin;
            this.heapMax = heapMax;
            this.heapP95 = heapP95;
            this.nonHeapMean = nonHeapMean;
            this.nonHeapMin = nonHeapMin;
            this.nonHeapMax = nonHeapMax;
            this.nonHeapP95 = nonHeapP95;
        }
        
        // Getters
        public double getHeapMean() { return heapMean; }
        public long getHeapMin() { return heapMin; }
        public long getHeapMax() { return heapMax; }
        public long getHeapP95() { return heapP95; }
        public double getNonHeapMean() { return nonHeapMean; }
        public long getNonHeapMin() { return nonHeapMin; }
        public long getNonHeapMax() { return nonHeapMax; }
        public long getNonHeapP95() { return nonHeapP95; }
    }
    
    /**
     * Simple circular buffer implementation.
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
        
        public synchronized int size() {
            return size;
        }
    }
}