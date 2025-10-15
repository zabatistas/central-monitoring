package com.upr.monitoring.centralmonitoring.service;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

@Service
public class MetricsStorageService {
    
    // Using ConcurrentHashMap for thread-safe operations
    private final Map<String, Object> metricsStorage = new ConcurrentHashMap<>();

    /**
     * Stores a metric with the given key and data
     * @param metricKey The unique identifier for the metric
     * @param metricData The data associated with the metric
     */
    public void storeMetric(String metricKey, Object metricData) {
        if (metricKey == null || metricKey.trim().isEmpty()) {
            throw new IllegalArgumentException("Metric key cannot be null or empty");
        }
        if (metricData == null) {
            throw new IllegalArgumentException("Metric data cannot be null");
        }
        
        metricsStorage.put(metricKey, metricData);
    }

    /**
     * Retrieves a specific metric by its key
     * @param metricKey The key of the metric to retrieve
     * @return The metric data, or null if not found
     */
    public Object getMetric(String metricKey) {
        if (metricKey == null || metricKey.trim().isEmpty()) {
            return null;
        }
        return metricsStorage.get(metricKey);
    }

    /**
     * Checks if a metric exists in the storage
     * @param metricKey The key to check
     * @return true if the metric exists, false otherwise
     */
    public boolean metricExists(String metricKey) {
        if (metricKey == null || metricKey.trim().isEmpty()) {
            return false;
        }
        return metricsStorage.containsKey(metricKey);
    }

    /**
     * Retrieves all stored metrics
     * @return A map containing all stored metrics
     */
    public Map<String, Object> getAllMetrics() {
        return new ConcurrentHashMap<>(metricsStorage);
    }

    /**
     * Retrieves all metric keys
     * @return A set containing all metric keys
     */
    public Set<String> getAllMetricKeys() {
        return metricsStorage.keySet();
    }

    /**
     * Removes a metric from storage
     * @param metricKey The key of the metric to remove
     * @return The removed metric data, or null if not found
     */
    public Object removeMetric(String metricKey) {
        if (metricKey == null || metricKey.trim().isEmpty()) {
            return null;
        }
        return metricsStorage.remove(metricKey);
    }

    /**
     * Clears all stored metrics
     */
    public void clearAllMetrics() {
        metricsStorage.clear();
    }

    /**
     * Gets the number of stored metrics
     * @return The count of stored metrics
     */
    public int getMetricsCount() {
        return metricsStorage.size();
    }
}