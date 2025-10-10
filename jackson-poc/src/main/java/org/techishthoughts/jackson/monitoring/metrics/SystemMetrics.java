package org.techishthoughts.jackson.monitoring.metrics;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import com.sun.management.UnixOperatingSystemMXBean;

/**
 * System-level metrics collection for comprehensive monitoring.
 * Provides OS-level resource information including CPU, memory, and I/O.
 */
public class SystemMetrics {
    
    private final OperatingSystemMXBean osBean;
    private final MemoryMXBean memoryBean;
    private final RuntimeMXBean runtimeBean;
    private final MBeanServer mBeanServer;
    
    // Performance counters
    private final AtomicLong totalCpuTime = new AtomicLong(0);
    private final AtomicLong lastCpuTime = new AtomicLong(0);
    private final AtomicLong lastSystemTime = new AtomicLong(0);
    
    public SystemMetrics() {
        this.osBean = ManagementFactory.getOperatingSystemMXBean();
        this.memoryBean = ManagementFactory.getMemoryMXBean();
        this.runtimeBean = ManagementFactory.getRuntimeMXBean();
        this.mBeanServer = ManagementFactory.getPlatformMBeanServer();
    }
    
    /**
     * Get comprehensive system metrics snapshot.
     */
    public SystemMetricsSnapshot getSnapshot() {
        SystemMetricsSnapshot.Builder builder = SystemMetricsSnapshot.builder();
        
        // Basic OS information
        builder.osName(osBean.getName())
               .osVersion(osBean.getVersion())
               .osArch(osBean.getArch())
               .availableProcessors(osBean.getAvailableProcessors());
        
        // CPU metrics
        if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
            com.sun.management.OperatingSystemMXBean sunOsBean = 
                (com.sun.management.OperatingSystemMXBean) osBean;
            
            builder.systemCpuLoad(sunOsBean.getSystemCpuLoad())
                   .processCpuLoad(sunOsBean.getProcessCpuLoad())
                   .processCpuTime(sunOsBean.getProcessCpuTime());
        } else {
            builder.systemCpuLoad(osBean.getSystemLoadAverage());
        }
        
        // Memory metrics
        builder.totalPhysicalMemory(getTotalPhysicalMemory())
               .freePhysicalMemory(getFreePhysicalMemory())
               .totalSwapSpace(getTotalSwapSpace())
               .freeSwapSpace(getFreeSwapSpace())
               .heapMemoryUsed(memoryBean.getHeapMemoryUsage().getUsed())
               .heapMemoryMax(memoryBean.getHeapMemoryUsage().getMax())
               .nonHeapMemoryUsed(memoryBean.getNonHeapMemoryUsage().getUsed())
               .nonHeapMemoryMax(memoryBean.getNonHeapMemoryUsage().getMax());
        
        // Runtime metrics
        builder.jvmUptime(runtimeBean.getUptime())
               .jvmStartTime(runtimeBean.getStartTime());
        
        // File descriptor metrics (Unix systems)
        if (osBean instanceof UnixOperatingSystemMXBean) {
            UnixOperatingSystemMXBean unixBean = (UnixOperatingSystemMXBean) osBean;
            builder.openFileDescriptorCount(unixBean.getOpenFileDescriptorCount())
                   .maxFileDescriptorCount(unixBean.getMaxFileDescriptorCount());
        }
        
        // Additional JVM metrics
        builder.committedVirtualMemory(getCommittedVirtualMemory())
               .gcTime(getTotalGcTime())
               .gcCount(getTotalGcCount());
        
        return builder.build();
    }
    
    /**
     * Get current CPU utilization percentage.
     */
    public double getCurrentCpuUtilization() {
        if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
            com.sun.management.OperatingSystemMXBean sunOsBean = 
                (com.sun.management.OperatingSystemMXBean) osBean;
            return sunOsBean.getProcessCpuLoad() * 100.0;
        }
        return -1.0; // Not available
    }
    
    /**
     * Get current memory utilization percentage.
     */
    public double getCurrentMemoryUtilization() {
        long totalPhysical = getTotalPhysicalMemory();
        long freePhysical = getFreePhysicalMemory();
        
        if (totalPhysical > 0) {
            return ((double) (totalPhysical - freePhysical) / totalPhysical) * 100.0;
        }
        return -1.0; // Not available
    }
    
    /**
     * Get heap memory utilization percentage.
     */
    public double getHeapUtilization() {
        long used = memoryBean.getHeapMemoryUsage().getUsed();
        long max = memoryBean.getHeapMemoryUsage().getMax();
        
        if (max > 0) {
            return ((double) used / max) * 100.0;
        }
        return -1.0; // Not available
    }
    
    private long getTotalPhysicalMemory() {
        if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
            return ((com.sun.management.OperatingSystemMXBean) osBean).getTotalPhysicalMemorySize();
        }
        return -1;
    }
    
    private long getFreePhysicalMemory() {
        if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
            return ((com.sun.management.OperatingSystemMXBean) osBean).getFreePhysicalMemorySize();
        }
        return -1;
    }
    
    private long getTotalSwapSpace() {
        if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
            return ((com.sun.management.OperatingSystemMXBean) osBean).getTotalSwapSpaceSize();
        }
        return -1;
    }
    
    private long getFreeSwapSpace() {
        if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
            return ((com.sun.management.OperatingSystemMXBean) osBean).getFreeSwapSpaceSize();
        }
        return -1;
    }
    
    private long getCommittedVirtualMemory() {
        if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
            return ((com.sun.management.OperatingSystemMXBean) osBean).getCommittedVirtualMemorySize();
        }
        return -1;
    }
    
    private long getTotalGcTime() {
        try {
            Long totalTime = (Long) mBeanServer.getAttribute(
                new ObjectName("java.lang:type=GarbageCollector,name=*"), 
                "CollectionTime"
            );
            return totalTime != null ? totalTime : 0;
        } catch (Exception e) {
            return 0;
        }
    }
    
    private long getTotalGcCount() {
        try {
            Long totalCount = (Long) mBeanServer.getAttribute(
                new ObjectName("java.lang:type=GarbageCollector,name=*"), 
                "CollectionCount"
            );
            return totalCount != null ? totalCount : 0;
        } catch (Exception e) {
            return 0;
        }
    }
    
    /**
     * Immutable snapshot of system metrics at a point in time.
     */
    public static class SystemMetricsSnapshot {
        private final String osName;
        private final String osVersion;
        private final String osArch;
        private final int availableProcessors;
        private final double systemCpuLoad;
        private final double processCpuLoad;
        private final long processCpuTime;
        private final long totalPhysicalMemory;
        private final long freePhysicalMemory;
        private final long totalSwapSpace;
        private final long freeSwapSpace;
        private final long heapMemoryUsed;
        private final long heapMemoryMax;
        private final long nonHeapMemoryUsed;
        private final long nonHeapMemoryMax;
        private final long jvmUptime;
        private final long jvmStartTime;
        private final long openFileDescriptorCount;
        private final long maxFileDescriptorCount;
        private final long committedVirtualMemory;
        private final long gcTime;
        private final long gcCount;
        private final long timestamp;
        
        private SystemMetricsSnapshot(Builder builder) {
            this.osName = builder.osName;
            this.osVersion = builder.osVersion;
            this.osArch = builder.osArch;
            this.availableProcessors = builder.availableProcessors;
            this.systemCpuLoad = builder.systemCpuLoad;
            this.processCpuLoad = builder.processCpuLoad;
            this.processCpuTime = builder.processCpuTime;
            this.totalPhysicalMemory = builder.totalPhysicalMemory;
            this.freePhysicalMemory = builder.freePhysicalMemory;
            this.totalSwapSpace = builder.totalSwapSpace;
            this.freeSwapSpace = builder.freeSwapSpace;
            this.heapMemoryUsed = builder.heapMemoryUsed;
            this.heapMemoryMax = builder.heapMemoryMax;
            this.nonHeapMemoryUsed = builder.nonHeapMemoryUsed;
            this.nonHeapMemoryMax = builder.nonHeapMemoryMax;
            this.jvmUptime = builder.jvmUptime;
            this.jvmStartTime = builder.jvmStartTime;
            this.openFileDescriptorCount = builder.openFileDescriptorCount;
            this.maxFileDescriptorCount = builder.maxFileDescriptorCount;
            this.committedVirtualMemory = builder.committedVirtualMemory;
            this.gcTime = builder.gcTime;
            this.gcCount = builder.gcCount;
            this.timestamp = System.currentTimeMillis();
        }
        
        public static Builder builder() {
            return new Builder();
        }
        
        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("osName", osName);
            map.put("osVersion", osVersion);
            map.put("osArch", osArch);
            map.put("availableProcessors", availableProcessors);
            map.put("systemCpuLoad", systemCpuLoad);
            map.put("processCpuLoad", processCpuLoad);
            map.put("processCpuTime", processCpuTime);
            map.put("totalPhysicalMemory", totalPhysicalMemory);
            map.put("freePhysicalMemory", freePhysicalMemory);
            map.put("totalSwapSpace", totalSwapSpace);
            map.put("freeSwapSpace", freeSwapSpace);
            map.put("heapMemoryUsed", heapMemoryUsed);
            map.put("heapMemoryMax", heapMemoryMax);
            map.put("nonHeapMemoryUsed", nonHeapMemoryUsed);
            map.put("nonHeapMemoryMax", nonHeapMemoryMax);
            map.put("jvmUptime", jvmUptime);
            map.put("jvmStartTime", jvmStartTime);
            map.put("openFileDescriptorCount", openFileDescriptorCount);
            map.put("maxFileDescriptorCount", maxFileDescriptorCount);
            map.put("committedVirtualMemory", committedVirtualMemory);
            map.put("gcTime", gcTime);
            map.put("gcCount", gcCount);
            map.put("timestamp", timestamp);
            return map;
        }
        
        // Getters
        public String getOsName() { return osName; }
        public String getOsVersion() { return osVersion; }
        public String getOsArch() { return osArch; }
        public int getAvailableProcessors() { return availableProcessors; }
        public double getSystemCpuLoad() { return systemCpuLoad; }
        public double getProcessCpuLoad() { return processCpuLoad; }
        public long getProcessCpuTime() { return processCpuTime; }
        public long getTotalPhysicalMemory() { return totalPhysicalMemory; }
        public long getFreePhysicalMemory() { return freePhysicalMemory; }
        public long getTotalSwapSpace() { return totalSwapSpace; }
        public long getFreeSwapSpace() { return freeSwapSpace; }
        public long getHeapMemoryUsed() { return heapMemoryUsed; }
        public long getHeapMemoryMax() { return heapMemoryMax; }
        public long getNonHeapMemoryUsed() { return nonHeapMemoryUsed; }
        public long getNonHeapMemoryMax() { return nonHeapMemoryMax; }
        public long getJvmUptime() { return jvmUptime; }
        public long getJvmStartTime() { return jvmStartTime; }
        public long getOpenFileDescriptorCount() { return openFileDescriptorCount; }
        public long getMaxFileDescriptorCount() { return maxFileDescriptorCount; }
        public long getCommittedVirtualMemory() { return committedVirtualMemory; }
        public long getGcTime() { return gcTime; }
        public long getGcCount() { return gcCount; }
        public long getTimestamp() { return timestamp; }
        
        public static class Builder {
            private String osName;
            private String osVersion;
            private String osArch;
            private int availableProcessors;
            private double systemCpuLoad = -1;
            private double processCpuLoad = -1;
            private long processCpuTime = -1;
            private long totalPhysicalMemory = -1;
            private long freePhysicalMemory = -1;
            private long totalSwapSpace = -1;
            private long freeSwapSpace = -1;
            private long heapMemoryUsed = -1;
            private long heapMemoryMax = -1;
            private long nonHeapMemoryUsed = -1;
            private long nonHeapMemoryMax = -1;
            private long jvmUptime = -1;
            private long jvmStartTime = -1;
            private long openFileDescriptorCount = -1;
            private long maxFileDescriptorCount = -1;
            private long committedVirtualMemory = -1;
            private long gcTime = -1;
            private long gcCount = -1;
            
            public Builder osName(String osName) { this.osName = osName; return this; }
            public Builder osVersion(String osVersion) { this.osVersion = osVersion; return this; }
            public Builder osArch(String osArch) { this.osArch = osArch; return this; }
            public Builder availableProcessors(int processors) { this.availableProcessors = processors; return this; }
            public Builder systemCpuLoad(double load) { this.systemCpuLoad = load; return this; }
            public Builder processCpuLoad(double load) { this.processCpuLoad = load; return this; }
            public Builder processCpuTime(long time) { this.processCpuTime = time; return this; }
            public Builder totalPhysicalMemory(long memory) { this.totalPhysicalMemory = memory; return this; }
            public Builder freePhysicalMemory(long memory) { this.freePhysicalMemory = memory; return this; }
            public Builder totalSwapSpace(long swap) { this.totalSwapSpace = swap; return this; }
            public Builder freeSwapSpace(long swap) { this.freeSwapSpace = swap; return this; }
            public Builder heapMemoryUsed(long memory) { this.heapMemoryUsed = memory; return this; }
            public Builder heapMemoryMax(long memory) { this.heapMemoryMax = memory; return this; }
            public Builder nonHeapMemoryUsed(long memory) { this.nonHeapMemoryUsed = memory; return this; }
            public Builder nonHeapMemoryMax(long memory) { this.nonHeapMemoryMax = memory; return this; }
            public Builder jvmUptime(long uptime) { this.jvmUptime = uptime; return this; }
            public Builder jvmStartTime(long startTime) { this.jvmStartTime = startTime; return this; }
            public Builder openFileDescriptorCount(long count) { this.openFileDescriptorCount = count; return this; }
            public Builder maxFileDescriptorCount(long count) { this.maxFileDescriptorCount = count; return this; }
            public Builder committedVirtualMemory(long memory) { this.committedVirtualMemory = memory; return this; }
            public Builder gcTime(long time) { this.gcTime = time; return this; }
            public Builder gcCount(long count) { this.gcCount = count; return this; }
            
            public SystemMetricsSnapshot build() {
                return new SystemMetricsSnapshot(this);
            }
        }
    }
}