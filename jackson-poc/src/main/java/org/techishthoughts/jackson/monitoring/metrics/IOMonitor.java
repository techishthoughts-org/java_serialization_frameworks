package org.techishthoughts.jackson.monitoring.metrics;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.techishthoughts.jackson.monitoring.ResourceMonitor;

import com.sun.management.UnixOperatingSystemMXBean;

/**
 * Comprehensive I/O performance monitoring for file system operations and network I/O.
 * Tracks disk usage, file descriptor usage, and I/O performance metrics.
 */
public class IOMonitor implements ResourceMonitor {
    
    private static final String MONITOR_NAME = "IO_Monitor";
    
    private final MBeanServer mBeanServer;
    private final ScheduledExecutorService scheduler;
    private final AtomicBoolean monitoring = new AtomicBoolean(false);
    private final long samplingIntervalMs;
    
    // I/O metrics storage
    private final CircularBuffer<IOSnapshot> ioHistory;
    private final Map<String, FileSystemMetrics> fileSystemMetrics = new ConcurrentHashMap<>();
    private final Map<String, DiskMetrics> diskMetrics = new ConcurrentHashMap<>();
    
    // I/O counters
    private final AtomicLong totalBytesRead = new AtomicLong(0);
    private final AtomicLong totalBytesWritten = new AtomicLong(0);
    private final AtomicLong totalReadOperations = new AtomicLong(0);
    private final AtomicLong totalWriteOperations = new AtomicLong(0);
    
    // Performance tracking
    private final AtomicLong lastReadTime = new AtomicLong(0);
    private final AtomicLong lastWriteTime = new AtomicLong(0);
    
    // Current metrics cache
    private volatile IOMetrics currentMetrics;
    
    public IOMonitor() {
        this(1000, 1000); // Default 1 second sampling, 1000 samples history
    }
    
    public IOMonitor(long samplingIntervalMs, int historySize) {
        this.samplingIntervalMs = samplingIntervalMs;
        this.mBeanServer = ManagementFactory.getPlatformMBeanServer();
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "IO-Monitor");
            t.setDaemon(true);
            return t;
        });
        
        // Initialize circular buffer
        this.ioHistory = new CircularBuffer<>(historySize);
        
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
        IOMetrics metrics = currentMetrics;
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
        ioHistory.clear();
        fileSystemMetrics.clear();
        diskMetrics.clear();
        totalBytesRead.set(0);
        totalBytesWritten.set(0);
        totalReadOperations.set(0);
        totalWriteOperations.set(0);
        lastReadTime.set(0);
        lastWriteTime.set(0);
        currentMetrics = collectMetrics();
    }
    
    /**
     * Get detailed I/O metrics including historical data.
     */
    public IOMetrics getDetailedMetrics() {
        return currentMetrics;
    }
    
    /**
     * Get file system specific metrics.
     */
    public Map<String, FileSystemMetrics> getFileSystemMetrics() {
        return new HashMap<>(fileSystemMetrics);
    }
    
    /**
     * Get disk specific metrics.
     */
    public Map<String, DiskMetrics> getDiskMetrics() {
        return new HashMap<>(diskMetrics);
    }
    
    /**
     * Record a read operation for monitoring.
     */
    public void recordReadOperation(long bytesRead, long durationMs) {
        totalBytesRead.addAndGet(bytesRead);
        totalReadOperations.incrementAndGet();
        lastReadTime.set(System.currentTimeMillis());
    }
    
    /**
     * Record a write operation for monitoring.
     */
    public void recordWriteOperation(long bytesWritten, long durationMs) {
        totalBytesWritten.addAndGet(bytesWritten);
        totalWriteOperations.incrementAndGet();
        lastWriteTime.set(System.currentTimeMillis());
    }
    
    /**
     * Get I/O performance statistics.
     */
    public IOStatistics getStatistics() {
        if (ioHistory.isEmpty()) {
            return new IOStatistics(0, 0, 0, 0, 0, 0);
        }
        
        java.util.List<IOSnapshot> snapshots = ioHistory.getValues();
        
        long[] readThroughput = snapshots.stream()
                .mapToLong(s -> s.getReadThroughput())
                .toArray();
        
        long[] writeThroughput = snapshots.stream()
                .mapToLong(s -> s.getWriteThroughput())
                .toArray();
        
        return new IOStatistics(
            calculateMean(readThroughput),
            calculateMax(readThroughput),
            calculateMean(writeThroughput),
            calculateMax(writeThroughput),
            snapshots.get(snapshots.size() - 1).getOpenFileDescriptors(),
            snapshots.get(snapshots.size() - 1).getMaxFileDescriptors()
        );
    }
    
    private void updateMetrics() {
        try {
            currentMetrics = collectMetrics();
            updateFileSystemMetrics();
            updateDiskMetrics();
        } catch (Exception e) {
            System.err.println("Error updating I/O metrics: " + e.getMessage());
        }
    }
    
    private IOMetrics collectMetrics() {
        long timestamp = System.currentTimeMillis();
        
        // File descriptor metrics
        long openFileDescriptors = -1;
        long maxFileDescriptors = -1;
        
        try {
            var osBean = ManagementFactory.getOperatingSystemMXBean();
            if (osBean instanceof UnixOperatingSystemMXBean) {
                UnixOperatingSystemMXBean unixBean = (UnixOperatingSystemMXBean) osBean;
                openFileDescriptors = unixBean.getOpenFileDescriptorCount();
                maxFileDescriptors = unixBean.getMaxFileDescriptorCount();
            }
        } catch (Exception e) {
            // Not available on this platform
        }
        
        // Calculate throughput (bytes per second)
        long readThroughput = calculateThroughput(totalBytesRead.get(), samplingIntervalMs);
        long writeThroughput = calculateThroughput(totalBytesWritten.get(), samplingIntervalMs);
        
        // Create I/O snapshot
        IOSnapshot snapshot = new IOSnapshot(
            totalBytesRead.get(),
            totalBytesWritten.get(),
            totalReadOperations.get(),
            totalWriteOperations.get(),
            readThroughput,
            writeThroughput,
            openFileDescriptors,
            maxFileDescriptors,
            timestamp
        );
        
        ioHistory.add(snapshot);
        
        return new IOMetrics(
            snapshot,
            new HashMap<>(fileSystemMetrics),
            new HashMap<>(diskMetrics)
        );
    }
    
    private void updateFileSystemMetrics() {
        try {
            FileSystem fileSystem = FileSystems.getDefault();
            for (FileStore store : fileSystem.getFileStores()) {
                try {
                    String name = store.name();
                    String type = store.type();
                    long totalSpace = store.getTotalSpace();
                    long usableSpace = store.getUsableSpace();
                    long unallocatedSpace = store.getUnallocatedSpace();
                    
                    fileSystemMetrics.put(name, new FileSystemMetrics(
                        name,
                        type,
                        totalSpace,
                        usableSpace,
                        unallocatedSpace,
                        System.currentTimeMillis()
                    ));
                } catch (IOException e) {
                    // Skip this file store if we can't read its metrics
                }
            }
        } catch (Exception e) {
            // Error accessing file system information
        }
    }
    
    private void updateDiskMetrics() {
        try {
            // Get disk I/O statistics from JMX if available
            updateJMXDiskMetrics();
            
            // Get disk space information
            updateDiskSpaceMetrics();
            
        } catch (Exception e) {
            // Platform-specific disk metrics may not be available
        }
    }
    
    private void updateJMXDiskMetrics() {
        try {
            // Try to get disk I/O metrics from operating system beans
            // This is platform-specific and may not be available on all systems
            ObjectName osName = new ObjectName("java.lang:type=OperatingSystem");
            
            // These attributes may not be available on all platforms
            try {
                Object readBytes = mBeanServer.getAttribute(osName, "SystemDiskReadBytes");
                Object writeBytes = mBeanServer.getAttribute(osName, "SystemDiskWriteBytes");
                
                if (readBytes instanceof Number && writeBytes instanceof Number) {
                    diskMetrics.put("system", new DiskMetrics(
                        "system",
                        ((Number) readBytes).longValue(),
                        ((Number) writeBytes).longValue(),
                        0, 0, 0, 0,
                        System.currentTimeMillis()
                    ));
                }
            } catch (Exception e) {
                // These attributes are not available on this platform
            }
        } catch (Exception e) {
            // JMX access failed
        }
    }
    
    private void updateDiskSpaceMetrics() {
        // Monitor common paths
        String[] pathsToMonitor = {
            System.getProperty("user.home"),
            System.getProperty("java.io.tmpdir"),
            "."
        };
        
        for (String pathStr : pathsToMonitor) {
            try {
                Path path = Paths.get(pathStr);
                File file = path.toFile();
                
                if (file.exists()) {
                    diskMetrics.put(pathStr, new DiskMetrics(
                        pathStr,
                        0, 0, // Read/write bytes not available from File API
                        file.getTotalSpace(),
                        file.getFreeSpace(),
                        file.getUsableSpace(),
                        file.getTotalSpace() - file.getFreeSpace(), // Used space
                        System.currentTimeMillis()
                    ));
                }
            } catch (Exception e) {
                // Skip this path if we can't access it
            }
        }
    }
    
    private long calculateThroughput(long totalBytes, long intervalMs) {
        if (intervalMs <= 0) {
            return 0;
        }
        return (totalBytes * 1000) / intervalMs; // bytes per second
    }
    
    private double calculateMean(long[] values) {
        return java.util.Arrays.stream(values).average().orElse(0);
    }
    
    private long calculateMax(long[] values) {
        return java.util.Arrays.stream(values).max().orElse(0);
    }
    
    // Data classes
    public static class IOSnapshot {
        private final long totalBytesRead;
        private final long totalBytesWritten;
        private final long totalReadOperations;
        private final long totalWriteOperations;
        private final long readThroughput;
        private final long writeThroughput;
        private final long openFileDescriptors;
        private final long maxFileDescriptors;
        private final long timestamp;
        
        public IOSnapshot(long totalBytesRead, long totalBytesWritten,
                         long totalReadOperations, long totalWriteOperations,
                         long readThroughput, long writeThroughput,
                         long openFileDescriptors, long maxFileDescriptors,
                         long timestamp) {
            this.totalBytesRead = totalBytesRead;
            this.totalBytesWritten = totalBytesWritten;
            this.totalReadOperations = totalReadOperations;
            this.totalWriteOperations = totalWriteOperations;
            this.readThroughput = readThroughput;
            this.writeThroughput = writeThroughput;
            this.openFileDescriptors = openFileDescriptors;
            this.maxFileDescriptors = maxFileDescriptors;
            this.timestamp = timestamp;
        }
        
        // Getters
        public long getTotalBytesRead() { return totalBytesRead; }
        public long getTotalBytesWritten() { return totalBytesWritten; }
        public long getTotalReadOperations() { return totalReadOperations; }
        public long getTotalWriteOperations() { return totalWriteOperations; }
        public long getReadThroughput() { return readThroughput; }
        public long getWriteThroughput() { return writeThroughput; }
        public long getOpenFileDescriptors() { return openFileDescriptors; }
        public long getMaxFileDescriptors() { return maxFileDescriptors; }
        public long getTimestamp() { return timestamp; }
    }
    
    public static class IOMetrics {
        private final IOSnapshot currentSnapshot;
        private final Map<String, FileSystemMetrics> fileSystemMetrics;
        private final Map<String, DiskMetrics> diskMetrics;
        
        public IOMetrics(IOSnapshot currentSnapshot,
                        Map<String, FileSystemMetrics> fileSystemMetrics,
                        Map<String, DiskMetrics> diskMetrics) {
            this.currentSnapshot = currentSnapshot;
            this.fileSystemMetrics = fileSystemMetrics;
            this.diskMetrics = diskMetrics;
        }
        
        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("totalBytesRead", currentSnapshot.getTotalBytesRead());
            map.put("totalBytesWritten", currentSnapshot.getTotalBytesWritten());
            map.put("totalReadOperations", currentSnapshot.getTotalReadOperations());
            map.put("totalWriteOperations", currentSnapshot.getTotalWriteOperations());
            map.put("readThroughput", currentSnapshot.getReadThroughput());
            map.put("writeThroughput", currentSnapshot.getWriteThroughput());
            map.put("openFileDescriptors", currentSnapshot.getOpenFileDescriptors());
            map.put("maxFileDescriptors", currentSnapshot.getMaxFileDescriptors());
            map.put("timestamp", currentSnapshot.getTimestamp());
            return map;
        }
        
        // Getters
        public IOSnapshot getCurrentSnapshot() { return currentSnapshot; }
        public Map<String, FileSystemMetrics> getFileSystemMetrics() { return fileSystemMetrics; }
        public Map<String, DiskMetrics> getDiskMetrics() { return diskMetrics; }
    }
    
    public static class FileSystemMetrics {
        private final String name;
        private final String type;
        private final long totalSpace;
        private final long usableSpace;
        private final long unallocatedSpace;
        private final long timestamp;
        
        public FileSystemMetrics(String name, String type, long totalSpace,
                               long usableSpace, long unallocatedSpace, long timestamp) {
            this.name = name;
            this.type = type;
            this.totalSpace = totalSpace;
            this.usableSpace = usableSpace;
            this.unallocatedSpace = unallocatedSpace;
            this.timestamp = timestamp;
        }
        
        // Getters
        public String getName() { return name; }
        public String getType() { return type; }
        public long getTotalSpace() { return totalSpace; }
        public long getUsableSpace() { return usableSpace; }
        public long getUnallocatedSpace() { return unallocatedSpace; }
        public long getUsedSpace() { return totalSpace - usableSpace; }
        public double getUsagePercentage() { 
            return totalSpace > 0 ? ((double) getUsedSpace() / totalSpace) * 100.0 : 0;
        }
        public long getTimestamp() { return timestamp; }
    }
    
    public static class DiskMetrics {
        private final String path;
        private final long readBytes;
        private final long writeBytes;
        private final long totalSpace;
        private final long freeSpace;
        private final long usableSpace;
        private final long usedSpace;
        private final long timestamp;
        
        public DiskMetrics(String path, long readBytes, long writeBytes,
                          long totalSpace, long freeSpace, long usableSpace, long usedSpace,
                          long timestamp) {
            this.path = path;
            this.readBytes = readBytes;
            this.writeBytes = writeBytes;
            this.totalSpace = totalSpace;
            this.freeSpace = freeSpace;
            this.usableSpace = usableSpace;
            this.usedSpace = usedSpace;
            this.timestamp = timestamp;
        }
        
        // Getters
        public String getPath() { return path; }
        public long getReadBytes() { return readBytes; }
        public long getWriteBytes() { return writeBytes; }
        public long getTotalSpace() { return totalSpace; }
        public long getFreeSpace() { return freeSpace; }
        public long getUsableSpace() { return usableSpace; }
        public long getUsedSpace() { return usedSpace; }
        public double getUsagePercentage() {
            return totalSpace > 0 ? ((double) usedSpace / totalSpace) * 100.0 : 0;
        }
        public long getTimestamp() { return timestamp; }
    }
    
    public static class IOStatistics {
        private final double avgReadThroughput;
        private final long maxReadThroughput;
        private final double avgWriteThroughput;
        private final long maxWriteThroughput;
        private final long currentOpenFileDescriptors;
        private final long maxFileDescriptors;
        
        public IOStatistics(double avgReadThroughput, long maxReadThroughput,
                          double avgWriteThroughput, long maxWriteThroughput,
                          long currentOpenFileDescriptors, long maxFileDescriptors) {
            this.avgReadThroughput = avgReadThroughput;
            this.maxReadThroughput = maxReadThroughput;
            this.avgWriteThroughput = avgWriteThroughput;
            this.maxWriteThroughput = maxWriteThroughput;
            this.currentOpenFileDescriptors = currentOpenFileDescriptors;
            this.maxFileDescriptors = maxFileDescriptors;
        }
        
        // Getters
        public double getAvgReadThroughput() { return avgReadThroughput; }
        public long getMaxReadThroughput() { return maxReadThroughput; }
        public double getAvgWriteThroughput() { return avgWriteThroughput; }
        public long getMaxWriteThroughput() { return maxWriteThroughput; }
        public long getCurrentOpenFileDescriptors() { return currentOpenFileDescriptors; }
        public long getMaxFileDescriptors() { return maxFileDescriptors; }
        public double getFileDescriptorUsage() {
            return maxFileDescriptors > 0 ? 
                ((double) currentOpenFileDescriptors / maxFileDescriptors) * 100.0 : 0;
        }
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
    }
}