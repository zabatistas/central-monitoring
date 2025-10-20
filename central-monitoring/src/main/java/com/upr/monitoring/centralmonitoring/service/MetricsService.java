package com.upr.monitoring.centralmonitoring.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.upr.monitoring.centralmonitoring.client.ThanosClient;
import com.upr.monitoring.centralmonitoring.model.MetricsResponseDto;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class MetricsService {
    
    private ThanosClient thanosClient;

    private RabbitTemplate rabbitTemplate;

    private KafkaTemplate<String, String> kafkaTemplate;

    private ObjectMapper objectMapper;

    private MetricsStorageService metricsStorageService;

    List<String> appIdList = new java.util.ArrayList<>();

    Map<String, List<String>> appMetricsMap = new java.util.HashMap<>();

    // Default metrics to be added when an application is found
    private static final List<String> DEFAULT_METRICS = List.of(
        "container_cpu_usage_seconds_total",
        "container_cpu_cfs_throttled_seconds_total",
        "container_cpu_cfs_throttled_periods_total",
        "container_memory_usage_bytes",
        "container_memory_working_set_bytes",
        "container_memory_rss",
        "container_memory_cache",
        "container_fs_reads_bytes_total",
        "container_fs_writes_bytes_total",
        "container_fs_reads_total",
        "container_fs_writes_total",
        "container_network_receive_bytes_total",
        "container_network_transmit_bytes_total",
        "container_network_receive_packets_total",
        "container_network_transmit_packets_total",
        "container_network_receive_errors_total",
        "container_network_transmit_errors_total",
        "node_cpu_seconds_total",
        "node_memory_MemTotal_bytes",
        "node_memory_MemAvailable_bytes",
        "node_memory_MemFree_bytes",
        "node_disk_read_bytes_total",
        "node_disk_written_bytes_total",
        "node_disk_reads_completed_total",
        "node_disk_writes_completed_total",
        "node_network_receive_bytes_total",
        "node_network_transmit_bytes_total",
        "node_network_receive_packets_total",
        "node_network_transmit_packets_total",
        "kube_pod_container_resource_requests_cpu_cores",
        "kube_pod_container_resource_limits_cpu_cores",
        "kube_pod_container_resource_requests_memory_bytes",
        "kube_pod_container_resource_limits_memory_bytes",
        "kube_persistentvolume_capacity_bytes",
        "kube_persistentvolumeclaim_resource_requests_storage_bytes",
        "kube_pod_container_status_restarts_total"
    );

    public MetricsService(ThanosClient thanosClient, RabbitTemplate rabbitTemplate, 
                         KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper,
                         MetricsStorageService metricsStorageService) {
        this.thanosClient = thanosClient;
        this.rabbitTemplate = rabbitTemplate;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.metricsStorageService = metricsStorageService;
    }

    /**
     * Parses the metrics from Thanos response and extracts only the metrics data
     * @param thanosResponse The complete response from Thanos API
     * @return List of parsed metric objects containing metric metadata and values
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> parseMetricsFromThanosResponse(Map<String, Object> thanosResponse) {
        List<Map<String, Object>> parsedMetrics = new ArrayList<>();
        
        if (thanosResponse == null || !thanosResponse.containsKey("data")) {
            return parsedMetrics;
        }
        
        Map<String, Object> data = (Map<String, Object>) thanosResponse.get("data");
        if (data == null || !data.containsKey("result")) {
            return parsedMetrics;
        }
        
        List<Map<String, Object>> results = (List<Map<String, Object>>) data.get("result");
        if (results == null) {
            return parsedMetrics;
        }
        
        for (Map<String, Object> result : results) {
            Map<String, Object> parsedMetric = new HashMap<>();
            
            // Extract metric metadata
            if (result.containsKey("metric")) {
                Map<String, Object> metricInfo = (Map<String, Object>) result.get("metric");
                parsedMetric.put("metric", metricInfo);
            }
            
            // Extract metric value and timestamp
            if (result.containsKey("value")) {
                List<Object> value = (List<Object>) result.get("value");
                if (value != null && value.size() >= 2) {
                    parsedMetric.put("timestamp", value.get(0));
                    parsedMetric.put("value", value.get(1));
                }
            }
            
            parsedMetrics.add(parsedMetric);
        }
        
        return parsedMetrics;
    }

    public MetricsResponseDto getMetricsForSpecificApplication(String appId) {

        List<String> storedMetrics = metricsStorageService.getMetrics(appId);

        if (storedMetrics == null || storedMetrics.isEmpty()) {
            Map<String, Object> thanosResponse = thanosClient.fetchMetrics(appId);
        // Validate
        if (thanosResponse == null || thanosResponse.isEmpty()) {
            throw new RuntimeException("No metrics found for application ID: " + appId);
        }
        }

        Map<String, Object> thanosResponse = thanosClient.fetchSpecificMetrics(appId, storedMetrics);

        // Validate
        if (thanosResponse == null || thanosResponse.isEmpty()) {
            throw new RuntimeException("No specific metrics found for application ID: " + appId);
        }


        // Parse the metrics from Thanos response
        List<Map<String, Object>> parsedMetrics = parseMetricsFromThanosResponse(thanosResponse);
        
        // Limit to the first 10 entries if needed
        List<Map<String, Object>> limitedMetrics = parsedMetrics.stream()
                .limit(10)
                .collect(java.util.stream.Collectors.toList());
        
        // Create a map to store the parsed metrics
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("metrics", limitedMetrics);
        metrics.put("total_count", parsedMetrics.size());
        metrics.put("returned_count", limitedMetrics.size());
        


        // TODO: Refactor Dto
        MetricsResponseDto dto = MetricsResponseDto.builder()
                .applicationId(appId)
                .timestamp(java.time.LocalDateTime.now())
                .metrics(metrics)
                .build();
        // TODO: Extract to a separate method -> sendMetrics

        try {
            String jsonMessage = objectMapper.writeValueAsString(dto);
            
            // Send to Kafka
            kafkaTemplate.send("metrics-topic", jsonMessage);

            // Send to RabbitMQ 
            rabbitTemplate.convertAndSend("metrics.exchange", "metrics." + appId, jsonMessage);
        } catch (JsonProcessingException e) {
            // Log error and handle gracefully
            System.err.println("Error serializing metrics to JSON: " + e.getMessage());
            // Optionally, you could send a simplified error message or rethrow as RuntimeException
            throw new RuntimeException("Failed to serialize metrics data", e);
        }

        return dto;
    }

    public ResponseEntity<String> validateApplicationId(String appId) {

        if (appIdList.contains(appId)) {
            return ResponseEntity.ok("Application ID is already registered.");
        }

        Map<String, Object> response = thanosClient.fetchApplicationIdLabelValues();
        if (response != null && response.containsKey("data")) {
            Object data = response.get("data");
            if (data instanceof java.util.List) {
                java.util.List<?> appIds = (java.util.List<?>) data;
                if (appIds.contains(appId)) {
                    appIdList.add(appId);
                    
                    // Add default metrics that are available in Thanos
                    addAvailableDefaultMetrics(appId);
                    
                    return ResponseEntity.ok("Application ID is valid and default metrics have been configured.");
                } else {
                    return ResponseEntity.badRequest().body("Invalid Application ID.");
                }
            }
        }
        return ResponseEntity.status(500).body("Error validating Application ID.");
    }

    /**
     * Adds default metrics that are available in Thanos for the given application
     * @param appId The application ID to add default metrics for
     */
    private void addAvailableDefaultMetrics(String appId) {
        try {
            // Check which default metrics actually exist for this specific application
            List<String> availableDefaultMetrics = new ArrayList<>();
            
            for (String metricName : DEFAULT_METRICS) {
                // Query Thanos to see if this metric exists for the specific application
                if (metricExistsForApplication(metricName, appId)) {
                    availableDefaultMetrics.add(metricName);
                }
            }
            
            // Store the available default metrics for this application in metricsStorage
            if (!availableDefaultMetrics.isEmpty()) {
                metricsStorageService.storeMetrics(appId, availableDefaultMetrics);
                log.info("Added {} default metrics for application {}: {}", 
                        availableDefaultMetrics.size(), appId, availableDefaultMetrics);
            } else {
                log.warn("No default metrics are available for application {} in Thanos", appId);
            }
            
        } catch (Exception e) {
            log.error("Error adding default metrics for application {}: {}", appId, e.getMessage());
            // Don't fail the application registration if default metrics can't be added
        }
    }

    /**
     * Checks if a specific metric exists for a given application in Thanos
     * @param metricName The name of the metric to check
     * @param appId The application ID to check for
     * @return true if the metric exists for the application, false otherwise
     */
    @SuppressWarnings("unchecked")
    private boolean metricExistsForApplication(String metricName, String appId) {
        try {
            // Query Thanos for this specific metric and application
            Map<String, Object> response = thanosClient.fetchSpecificMetrics(appId, List.of(metricName));
            
            if (response != null && response.containsKey("data")) {
                Map<String, Object> data = (Map<String, Object>) response.get("data");
                if (data != null && data.containsKey("result")) {
                    List<Map<String, Object>> results = (List<Map<String, Object>>) data.get("result");
                    // If we get any results, the metric exists for this application
                    return results != null && !results.isEmpty();
                }
            }
            return false;
        } catch (Exception e) {
            log.debug("Error checking if metric {} exists for application {}: {}", metricName, appId, e.getMessage());
            return false;
        }
    }

    public void getMetricsForAllRegisteredApps() {
        for (String appId : appIdList) {
            getMetricsForSpecificApplication(appId);
        }
    }

    public MetricsResponseDto getSpecificMetricsForApplication(String applicationId) {
        List<String> metrics = appMetricsMap.get(applicationId);
        if (metrics == null || metrics.isEmpty()) {
            throw new RuntimeException("No specific metrics configured for application ID: " + applicationId);
        }
        // Fetch and filter metrics from Thanos based on the configured list
        

        throw new UnsupportedOperationException("Unimplemented method 'getSpecificMetricsForApplication'");
    }
}
