package org.techishthoughts.payload.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for benchmark settings.
 * Centralizes all configurable parameters to avoid magic numbers.
 */
@Component
@ConfigurationProperties(prefix = "benchmark")
public class BenchmarkProperties {

    private PayloadGeneration payloadGeneration = new PayloadGeneration();
    private Performance performance = new Performance();
    private Limits limits = new Limits();

    public static class PayloadGeneration {
        private int defaultUserCount = 1000;
        private int maxAddressesPerUser = 5;
        private int maxOrdersPerUser = 10;
        private int maxSkillsPerUser = 15;
        private int maxLanguagesPerUser = 5;
        private int maxSocialConnectionsPerUser = 5;
        private int maxOrderItemsPerOrder = 5;
        private int maxTrackingEventsPerOrder = 8;

        // Getters and setters
        public int getDefaultUserCount() { return defaultUserCount; }
        public void setDefaultUserCount(int defaultUserCount) { this.defaultUserCount = defaultUserCount; }

        public int getMaxAddressesPerUser() { return maxAddressesPerUser; }
        public void setMaxAddressesPerUser(int maxAddressesPerUser) { this.maxAddressesPerUser = maxAddressesPerUser; }

        public int getMaxOrdersPerUser() { return maxOrdersPerUser; }
        public void setMaxOrdersPerUser(int maxOrdersPerUser) { this.maxOrdersPerUser = maxOrdersPerUser; }

        public int getMaxSkillsPerUser() { return maxSkillsPerUser; }
        public void setMaxSkillsPerUser(int maxSkillsPerUser) { this.maxSkillsPerUser = maxSkillsPerUser; }

        public int getMaxLanguagesPerUser() { return maxLanguagesPerUser; }
        public void setMaxLanguagesPerUser(int maxLanguagesPerUser) { this.maxLanguagesPerUser = maxLanguagesPerUser; }

        public int getMaxSocialConnectionsPerUser() { return maxSocialConnectionsPerUser; }
        public void setMaxSocialConnectionsPerUser(int maxSocialConnectionsPerUser) { this.maxSocialConnectionsPerUser = maxSocialConnectionsPerUser; }

        public int getMaxOrderItemsPerOrder() { return maxOrderItemsPerOrder; }
        public void setMaxOrderItemsPerOrder(int maxOrderItemsPerOrder) { this.maxOrderItemsPerOrder = maxOrderItemsPerOrder; }

        public int getMaxTrackingEventsPerOrder() { return maxTrackingEventsPerOrder; }
        public void setMaxTrackingEventsPerOrder(int maxTrackingEventsPerOrder) { this.maxTrackingEventsPerOrder = maxTrackingEventsPerOrder; }
    }

    public static class Performance {
        private int defaultIterations = 100;
        private int maxIterations = 1000;
        private long timeoutMs = 60000; // 1 minute
        private int maxPayloadSizeMb = 100;
        private boolean enableWarmup = true;
        private int warmupIterations = 10;

        // Getters and setters
        public int getDefaultIterations() { return defaultIterations; }
        public void setDefaultIterations(int defaultIterations) { this.defaultIterations = defaultIterations; }

        public int getMaxIterations() { return maxIterations; }
        public void setMaxIterations(int maxIterations) { this.maxIterations = maxIterations; }

        public long getTimeoutMs() { return timeoutMs; }
        public void setTimeoutMs(long timeoutMs) { this.timeoutMs = timeoutMs; }

        public int getMaxPayloadSizeMb() { return maxPayloadSizeMb; }
        public void setMaxPayloadSizeMb(int maxPayloadSizeMb) { this.maxPayloadSizeMb = maxPayloadSizeMb; }

        public boolean isEnableWarmup() { return enableWarmup; }
        public void setEnableWarmup(boolean enableWarmup) { this.enableWarmup = enableWarmup; }

        public int getWarmupIterations() { return warmupIterations; }
        public void setWarmupIterations(int warmupIterations) { this.warmupIterations = warmupIterations; }
    }

    public static class Limits {
        private int maxUsers = 50000;
        private int progressReportInterval = 1000;
        private double memoryThresholdPercent = 80.0;
        private boolean enableMemoryMonitoring = true;
        private boolean enableGcLogging = false;

        // Getters and setters
        public int getMaxUsers() { return maxUsers; }
        public void setMaxUsers(int maxUsers) { this.maxUsers = maxUsers; }

        public int getProgressReportInterval() { return progressReportInterval; }
        public void setProgressReportInterval(int progressReportInterval) { this.progressReportInterval = progressReportInterval; }

        public double getMemoryThresholdPercent() { return memoryThresholdPercent; }
        public void setMemoryThresholdPercent(double memoryThresholdPercent) { this.memoryThresholdPercent = memoryThresholdPercent; }

        public boolean isEnableMemoryMonitoring() { return enableMemoryMonitoring; }
        public void setEnableMemoryMonitoring(boolean enableMemoryMonitoring) { this.enableMemoryMonitoring = enableMemoryMonitoring; }

        public boolean isEnableGcLogging() { return enableGcLogging; }
        public void setEnableGcLogging(boolean enableGcLogging) { this.enableGcLogging = enableGcLogging; }
    }

    // Main getters and setters
    public PayloadGeneration getPayloadGeneration() { return payloadGeneration; }
    public void setPayloadGeneration(PayloadGeneration payloadGeneration) { this.payloadGeneration = payloadGeneration; }

    public Performance getPerformance() { return performance; }
    public void setPerformance(Performance performance) { this.performance = performance; }

    public Limits getLimits() { return limits; }
    public void setLimits(Limits limits) { this.limits = limits; }
}