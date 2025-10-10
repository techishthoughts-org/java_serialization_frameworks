package org.techishthoughts.jackson.monitoring;

import java.util.Map;

/**
 * Base interface for all resource monitors in the system.
 * Provides common functionality for resource monitoring implementations.
 */
public interface ResourceMonitor {
    
    /**
     * Start monitoring resources.
     */
    void startMonitoring();
    
    /**
     * Stop monitoring resources.
     */
    void stopMonitoring();
    
    /**
     * Check if monitoring is currently active.
     * @return true if monitoring is active, false otherwise
     */
    boolean isMonitoring();
    
    /**
     * Get current snapshot of monitored metrics.
     * @return Map containing current metric values
     */
    Map<String, Object> getCurrentMetrics();
    
    /**
     * Get the name of this monitor.
     * @return monitor name
     */
    String getMonitorName();
    
    /**
     * Reset all collected metrics.
     */
    void reset();
}