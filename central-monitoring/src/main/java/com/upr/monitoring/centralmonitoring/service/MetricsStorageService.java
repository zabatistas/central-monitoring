package com.upr.monitoring.centralmonitoring.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

@Service
public class MetricsStorageService {
    
    // Using ConcurrentHashMap for thread-safe operations
    private final Map<String, List<String>> metricsStorage = new ConcurrentHashMap<>();

    /**
     * Stores a metric for the given application ID
     * @param applicationId The unique identifier for the application
     * @param metric The metric to be associated with the application
     */
    public void storeMetric(String applicationId, String metric) {
        if (applicationId == null || applicationId.trim().isEmpty()) {
            throw new IllegalArgumentException("Application ID cannot be null or empty");
        }
        if (metric == null) {
            throw new IllegalArgumentException("Metric cannot be null");
        }
        
        metricsStorage.computeIfAbsent(applicationId, k -> new ArrayList<>()).add(metric);
    }

    /**
     * Retrieves metrics for a specific application ID
     * @param applicationId The application ID to retrieve metrics for
     * @return The list of metrics for the application, or null if not found
     */
    public List<String> getMetrics(String applicationId) {
        if (applicationId == null || applicationId.trim().isEmpty()) {
            return null;
        }
        return metricsStorage.get(applicationId);
    }

    /**
     * Checks if an application has metrics stored
     * @param applicationId The application ID to check
     * @return true if the application has metrics, false otherwise
     */
    public boolean applicationExists(String applicationId) {
        if (applicationId == null || applicationId.trim().isEmpty()) {
            return false;
        }
        return metricsStorage.containsKey(applicationId);
    }

    /**
     * Retrieves all stored metrics
     * @return A map containing all application IDs and their associated metrics
     */
    public Map<String, List<String>> getAllMetrics() {
        return new ConcurrentHashMap<>(metricsStorage);
    }

    /**
     * Retrieves all application IDs
     * @return A set containing all application IDs
     */
    public Set<String> getAllApplicationIds() {
        return metricsStorage.keySet();
    }

    /**
     * Removes all metrics for an application
     * @param applicationId The application ID to remove metrics for
     * @return The removed metrics list, or null if not found
     */
    public List<String> removeApplication(String applicationId) {
        if (applicationId == null || applicationId.trim().isEmpty()) {
            return null;
        }
        return metricsStorage.remove(applicationId);
    }

    /**
     * Removes a specific metric from an application
     * @param applicationId The application ID
     * @param metric The metric to remove
     * @return true if the metric was removed, false if not found
     */
    public boolean removeMetric(String applicationId, String metric) {
        if (applicationId == null || applicationId.trim().isEmpty() || metric == null) {
            return false;
        }
        List<String> metrics = metricsStorage.get(applicationId);
        if (metrics != null) {
            boolean removed = metrics.remove(metric);
            // If the list becomes empty, remove the application entry
            if (metrics.isEmpty()) {
                metricsStorage.remove(applicationId);
            }
            return removed;
        }
        return false;
    }

    /**
     * Clears all stored metrics
     */
    public void clearAllMetrics() {
        metricsStorage.clear();
    }

    /**
     * Gets the number of applications with stored metrics
     * @return The count of applications
     */
    public int getApplicationCount() {
        return metricsStorage.size();
    }

    /**
     * Gets the total number of metrics across all applications
     * @return The total count of all metrics
     */
    public int getTotalMetricsCount() {
        return metricsStorage.values().stream()
                .mapToInt(List::size)
                .sum();
    }
}