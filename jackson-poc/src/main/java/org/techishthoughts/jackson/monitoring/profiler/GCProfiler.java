package org.techishthoughts.jackson.monitoring.profiler;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
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
import com.sun.management.GcInfo;

/**
 * Advanced GC profiling with detailed analysis of garbage collection events.
 * Provides comprehensive insights into GC performance, patterns, and potential issues.
 */
public class GCProfiler implements ResourceMonitor, NotificationListener {
    
    private static final String MONITOR_NAME = "GC_Profiler";
    
    private final List<GarbageCollectorMXBean> gcBeans;
    private final ScheduledExecutorService scheduler;
    private final AtomicBoolean monitoring = new AtomicBoolean(false);
    private final long samplingIntervalMs;
    
    // GC event storage
    private final List<GCEvent> gcEvents = new CopyOnWriteArrayList<>();
    private final Map<String, GCCollectorMetrics> collectorMetrics = new ConcurrentHashMap<>();
    private final CircularBuffer<GCSnapshot> gcHistory;
    
    // Performance analysis
    private final AtomicLong totalGcTime = new AtomicLong(0);
    private final AtomicLong totalGcCount = new AtomicLong(0);
    private final AtomicLong lastMajorGcTime = new AtomicLong(0);
    private final AtomicLong lastMinorGcTime = new AtomicLong(0);
    private final AtomicLong consecutiveLongGcs = new AtomicLong(0);
    
    // Thresholds for analysis
    private final long longGcThresholdMs = 100; // 100ms threshold for long GCs
    private final double memoryPressureThreshold = 0.9; // 90% memory usage threshold
    private final int maxGcEvents = 10000; // Maximum GC events to store
    
    // Current metrics cache
    private volatile GCProfileMetrics currentMetrics;
    
    public GCProfiler() {
        this(5000, 1000); // Default 5 second sampling, 1000 samples history
    }
    
    public GCProfiler(long samplingIntervalMs, int historySize) {
        this.samplingIntervalMs = samplingIntervalMs;
        this.gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "GC-Profiler");
            t.setDaemon(true);
            return t;
        });
        
        // Initialize circular buffer
        this.gcHistory = new CircularBuffer<>(historySize);
        
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
        GCProfileMetrics metrics = currentMetrics;
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
        gcEvents.clear();
        collectorMetrics.clear();
        gcHistory.clear();
        totalGcTime.set(0);
        totalGcCount.set(0);
        lastMajorGcTime.set(0);
        lastMinorGcTime.set(0);
        consecutiveLongGcs.set(0);
        currentMetrics = collectMetrics();
    }
    
    /**
     * Get detailed GC profile metrics.
     */
    public GCProfileMetrics getDetailedMetrics() {
        return currentMetrics;
    }
    
    /**
     * Get recent GC events.
     */
    public List<GCEvent> getRecentGCEvents(int limit) {
        if (gcEvents.isEmpty()) {
            return new java.util.ArrayList<>();
        }
        
        int startIndex = Math.max(0, gcEvents.size() - limit);
        return new java.util.ArrayList<>(gcEvents.subList(startIndex, gcEvents.size()));
    }
    
    /**
     * Get GC collector specific metrics.
     */
    public Map<String, GCCollectorMetrics> getCollectorMetrics() {
        return new HashMap<>(collectorMetrics);
    }
    
    /**
     * Analyze GC performance and identify potential issues.
     */
    public GCAnalysis analyzeGCPerformance() {
        if (gcEvents.isEmpty()) {
            return new GCAnalysis("No GC events recorded", false, new java.util.ArrayList<>());
        }
        
        java.util.List<String> issues = new java.util.ArrayList<>();
        boolean hasIssues = false;
        
        // Analyze GC frequency
        long totalTime = gcEvents.get(gcEvents.size() - 1).getTimestamp() - gcEvents.get(0).getTimestamp();
        double gcFrequency = (double) gcEvents.size() / (totalTime / 1000.0); // GCs per second
        
        if (gcFrequency > 1.0) {
            issues.add("High GC frequency: " + String.format("%.2f", gcFrequency) + " GCs/second");
            hasIssues = true;
        }
        
        // Analyze GC duration
        long longGcCount = gcEvents.stream().mapToLong(event -> event.getDuration() > longGcThresholdMs ? 1 : 0).sum();
        double longGcRatio = (double) longGcCount / gcEvents.size();
        
        if (longGcRatio > 0.1) { // More than 10% long GCs
            issues.add("High ratio of long GCs: " + String.format("%.2f%%", longGcRatio * 100));
            hasIssues = true;
        }
        
        // Analyze GC efficiency
        double totalGcTimePercent = (double) totalGcTime.get() / totalTime * 100;
        if (totalGcTimePercent > 5.0) { // More than 5% time spent in GC
            issues.add("High GC overhead: " + String.format("%.2f%%", totalGcTimePercent) + " time spent in GC");
            hasIssues = true;
        }
        
        // Analyze memory reclamation efficiency
        analyzeMemoryReclamationEfficiency(issues);
        
        String summary = hasIssues ? 
            "GC performance issues detected" : 
            "GC performance appears healthy";
        
        return new GCAnalysis(summary, hasIssues, issues);
    }
    
    @Override
    public void handleNotification(Notification notification, Object handback) {
        if (notification.getType().equals(GarbageCollectionNotificationInfo.GARBAGE_COLLECTION_NOTIFICATION)) {
            try {
                GarbageCollectionNotificationInfo info = GarbageCollectionNotificationInfo.from(
                    (javax.management.openmbean.CompositeData) notification.getUserData());
                
                processGCNotification(info);
            } catch (Exception e) {
                System.err.println("Error processing GC notification: " + e.getMessage());
            }
        }
    }
    
    private void setupGCNotifications() {
        for (GarbageCollectorMXBean gcBean : gcBeans) {
            if (gcBean instanceof NotificationEmitter) {
                NotificationEmitter emitter = (NotificationEmitter) gcBean;
                emitter.addNotificationListener(this, null, null);
            }
        }
    }
    
    private void processGCNotification(GarbageCollectionNotificationInfo info) {
        GcInfo gcInfo = info.getGcInfo();
        String gcName = info.getGcName();
        String gcAction = info.getGcAction();
        String gcCause = info.getGcCause();
        long duration = gcInfo.getDuration();
        long startTime = gcInfo.getStartTime();
        long endTime = gcInfo.getEndTime();
        
        // Determine GC type
        GCType gcType = determineGCType(gcName, gcAction);
        
        // Get memory usage before and after GC
        Map<String, MemoryUsage> memoryUsageBefore = gcInfo.getMemoryUsageBeforeGc();
        Map<String, MemoryUsage> memoryUsageAfter = gcInfo.getMemoryUsageAfterGc();
        
        // Calculate memory reclaimed
        long memoryFreed = calculateMemoryFreed(memoryUsageBefore, memoryUsageAfter);
        
        // Create GC event
        GCEvent gcEvent = new GCEvent(
            gcName, gcAction, gcCause, gcType, duration, startTime, endTime,
            memoryFreed, memoryUsageBefore, memoryUsageAfter, System.currentTimeMillis()
        );
        
        // Store GC event (limit storage)
        if (gcEvents.size() >= maxGcEvents) {
            gcEvents.remove(0); // Remove oldest event
        }
        gcEvents.add(gcEvent);
        
        // Update counters
        totalGcTime.addAndGet(duration);
        totalGcCount.incrementAndGet();
        
        if (gcType == GCType.MAJOR) {
            lastMajorGcTime.set(System.currentTimeMillis());
        } else {
            lastMinorGcTime.set(System.currentTimeMillis());
        }
        
        // Check for consecutive long GCs
        if (duration > longGcThresholdMs) {
            consecutiveLongGcs.incrementAndGet();
        } else {
            consecutiveLongGcs.set(0);
        }
        
        // Update collector metrics
        updateCollectorMetrics(gcName, gcAction, duration, memoryFreed);
    }
    
    private GCType determineGCType(String gcName, String gcAction) {
        String combined = (gcName + " " + gcAction).toLowerCase();
        
        if (combined.contains("young") || combined.contains("minor") || 
            combined.contains("scavenge") || combined.contains("parallel scavenge")) {
            return GCType.MINOR;
        } else if (combined.contains("old") || combined.contains("major") || 
                   combined.contains("full") || combined.contains("concurrent mark sweep") ||
                   combined.contains("g1") || combined.contains("parallel old")) {
            return GCType.MAJOR;
        } else {
            return GCType.UNKNOWN;
        }
    }
    
    private long calculateMemoryFreed(Map<String, MemoryUsage> before, Map<String, MemoryUsage> after) {
        long totalBefore = 0;
        long totalAfter = 0;
        
        for (MemoryUsage usage : before.values()) {
            totalBefore += usage.getUsed();
        }
        
        for (MemoryUsage usage : after.values()) {
            totalAfter += usage.getUsed();
        }
        
        return Math.max(0, totalBefore - totalAfter);
    }
    
    private void updateCollectorMetrics(String gcName, String gcAction, long duration, long memoryFreed) {
        GCCollectorMetrics existing = collectorMetrics.get(gcName);
        
        if (existing != null) {
            collectorMetrics.put(gcName, new GCCollectorMetrics(
                gcName,
                existing.getCollectionCount() + 1,
                existing.getTotalCollectionTime() + duration,
                Math.max(existing.getMaxCollectionTime(), duration),
                Math.min(existing.getMinCollectionTime(), duration),
                existing.getTotalMemoryFreed() + memoryFreed,
                System.currentTimeMillis()
            ));
        } else {
            collectorMetrics.put(gcName, new GCCollectorMetrics(
                gcName, 1, duration, duration, duration, memoryFreed, System.currentTimeMillis()
            ));
        }
    }
    
    private void analyzeMemoryReclamationEfficiency(java.util.List<String> issues) {
        if (gcEvents.size() < 5) {
            return; // Need at least 5 events for analysis
        }
        
        // Get recent events
        List<GCEvent> recentEvents = gcEvents.subList(
            Math.max(0, gcEvents.size() - 10), gcEvents.size());
        
        // Calculate average memory reclamation
        double avgMemoryFreed = recentEvents.stream()
                .mapToLong(GCEvent::getMemoryFreed)
                .average()
                .orElse(0);
        
        // Check if memory reclamation is decreasing
        long lowEfficiencyCount = recentEvents.stream()
                .mapToLong(event -> event.getMemoryFreed() < (avgMemoryFreed * 0.5) ? 1 : 0)
                .sum();
        
        if (lowEfficiencyCount > 3) {
            issues.add("Decreasing GC efficiency detected - possible memory leak");
        }
    }
    
    private void updateMetrics() {
        try {
            currentMetrics = collectMetrics();
        } catch (Exception e) {
            System.err.println("Error updating GC metrics: " + e.getMessage());
        }
    }
    
    private GCProfileMetrics collectMetrics() {
        long timestamp = System.currentTimeMillis();
        
        // Create GC snapshot
        GCSnapshot snapshot = new GCSnapshot(
            totalGcTime.get(),
            totalGcCount.get(),
            lastMajorGcTime.get(),
            lastMinorGcTime.get(),
            consecutiveLongGcs.get(),
            timestamp
        );
        
        gcHistory.add(snapshot);
        
        return new GCProfileMetrics(
            snapshot,
            new HashMap<>(collectorMetrics),
            gcEvents.size(),
            analyzeGCPerformance()
        );
    }
    
    // Data classes and enums
    public enum GCType {
        MINOR, MAJOR, UNKNOWN
    }
    
    public static class GCEvent {
        private final String gcName;
        private final String gcAction;
        private final String gcCause;
        private final GCType gcType;
        private final long duration;
        private final long startTime;
        private final long endTime;
        private final long memoryFreed;
        private final Map<String, MemoryUsage> memoryUsageBefore;
        private final Map<String, MemoryUsage> memoryUsageAfter;
        private final long timestamp;
        
        public GCEvent(String gcName, String gcAction, String gcCause, GCType gcType,
                      long duration, long startTime, long endTime, long memoryFreed,
                      Map<String, MemoryUsage> memoryUsageBefore,
                      Map<String, MemoryUsage> memoryUsageAfter, long timestamp) {
            this.gcName = gcName;
            this.gcAction = gcAction;
            this.gcCause = gcCause;
            this.gcType = gcType;
            this.duration = duration;
            this.startTime = startTime;
            this.endTime = endTime;
            this.memoryFreed = memoryFreed;
            this.memoryUsageBefore = memoryUsageBefore;
            this.memoryUsageAfter = memoryUsageAfter;
            this.timestamp = timestamp;
        }
        
        // Getters
        public String getGcName() { return gcName; }
        public String getGcAction() { return gcAction; }
        public String getGcCause() { return gcCause; }
        public GCType getGcType() { return gcType; }
        public long getDuration() { return duration; }
        public long getStartTime() { return startTime; }
        public long getEndTime() { return endTime; }
        public long getMemoryFreed() { return memoryFreed; }
        public Map<String, MemoryUsage> getMemoryUsageBefore() { return memoryUsageBefore; }
        public Map<String, MemoryUsage> getMemoryUsageAfter() { return memoryUsageAfter; }
        public long getTimestamp() { return timestamp; }
    }
    
    public static class GCSnapshot {
        private final long totalGcTime;
        private final long totalGcCount;
        private final long lastMajorGcTime;
        private final long lastMinorGcTime;
        private final long consecutiveLongGcs;
        private final long timestamp;
        
        public GCSnapshot(long totalGcTime, long totalGcCount, long lastMajorGcTime,
                         long lastMinorGcTime, long consecutiveLongGcs, long timestamp) {
            this.totalGcTime = totalGcTime;
            this.totalGcCount = totalGcCount;
            this.lastMajorGcTime = lastMajorGcTime;
            this.lastMinorGcTime = lastMinorGcTime;
            this.consecutiveLongGcs = consecutiveLongGcs;
            this.timestamp = timestamp;
        }
        
        // Getters
        public long getTotalGcTime() { return totalGcTime; }
        public long getTotalGcCount() { return totalGcCount; }
        public long getLastMajorGcTime() { return lastMajorGcTime; }
        public long getLastMinorGcTime() { return lastMinorGcTime; }
        public long getConsecutiveLongGcs() { return consecutiveLongGcs; }
        public long getTimestamp() { return timestamp; }
    }
    
    public static class GCProfileMetrics {
        private final GCSnapshot currentSnapshot;
        private final Map<String, GCCollectorMetrics> collectorMetrics;
        private final int totalGcEvents;
        private final GCAnalysis analysis;
        
        public GCProfileMetrics(GCSnapshot currentSnapshot, Map<String, GCCollectorMetrics> collectorMetrics,
                               int totalGcEvents, GCAnalysis analysis) {
            this.currentSnapshot = currentSnapshot;
            this.collectorMetrics = collectorMetrics;
            this.totalGcEvents = totalGcEvents;
            this.analysis = analysis;
        }
        
        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("totalGcTime", currentSnapshot.getTotalGcTime());
            map.put("totalGcCount", currentSnapshot.getTotalGcCount());
            map.put("lastMajorGcTime", currentSnapshot.getLastMajorGcTime());
            map.put("lastMinorGcTime", currentSnapshot.getLastMinorGcTime());
            map.put("consecutiveLongGcs", currentSnapshot.getConsecutiveLongGcs());
            map.put("totalGcEvents", totalGcEvents);
            map.put("analysisSummary", analysis.getSummary());
            map.put("hasIssues", analysis.hasIssues());
            map.put("timestamp", currentSnapshot.getTimestamp());
            return map;
        }
        
        // Getters
        public GCSnapshot getCurrentSnapshot() { return currentSnapshot; }
        public Map<String, GCCollectorMetrics> getCollectorMetrics() { return collectorMetrics; }
        public int getTotalGcEvents() { return totalGcEvents; }
        public GCAnalysis getAnalysis() { return analysis; }
    }
    
    public static class GCCollectorMetrics {
        private final String name;
        private final long collectionCount;
        private final long totalCollectionTime;
        private final long maxCollectionTime;
        private final long minCollectionTime;
        private final long totalMemoryFreed;
        private final long timestamp;
        
        public GCCollectorMetrics(String name, long collectionCount, long totalCollectionTime,
                                 long maxCollectionTime, long minCollectionTime,
                                 long totalMemoryFreed, long timestamp) {
            this.name = name;
            this.collectionCount = collectionCount;
            this.totalCollectionTime = totalCollectionTime;
            this.maxCollectionTime = maxCollectionTime;
            this.minCollectionTime = minCollectionTime;
            this.totalMemoryFreed = totalMemoryFreed;
            this.timestamp = timestamp;
        }
        
        public double getAverageCollectionTime() {
            return collectionCount > 0 ? (double) totalCollectionTime / collectionCount : 0;
        }
        
        // Getters
        public String getName() { return name; }
        public long getCollectionCount() { return collectionCount; }
        public long getTotalCollectionTime() { return totalCollectionTime; }
        public long getMaxCollectionTime() { return maxCollectionTime; }
        public long getMinCollectionTime() { return minCollectionTime; }
        public long getTotalMemoryFreed() { return totalMemoryFreed; }
        public long getTimestamp() { return timestamp; }
    }
    
    public static class GCAnalysis {
        private final String summary;
        private final boolean hasIssues;
        private final List<String> issues;
        
        public GCAnalysis(String summary, boolean hasIssues, List<String> issues) {
            this.summary = summary;
            this.hasIssues = hasIssues;
            this.issues = issues;
        }
        
        // Getters
        public String getSummary() { return summary; }
        public boolean hasIssues() { return hasIssues; }
        public List<String> getIssues() { return issues; }
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
        
        public synchronized void clear() {
            head = 0;
            size = 0;
            java.util.Arrays.fill(buffer, null);
        }
    }
}