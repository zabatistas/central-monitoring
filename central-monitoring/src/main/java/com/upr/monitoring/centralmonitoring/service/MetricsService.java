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

@Service
public class MetricsService {
    
    private ThanosClient thanosClient;

    private RabbitTemplate rabbitTemplate;

    private KafkaTemplate<String, String> kafkaTemplate;

    private ObjectMapper objectMapper;

    List<String> appIdList = new java.util.ArrayList<>();

    Map<String, List<String>> appMetricsMap = new java.util.HashMap<>();

    public MetricsService(ThanosClient thanosClient, RabbitTemplate rabbitTemplate, 
                         KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.thanosClient = thanosClient;
        this.rabbitTemplate = rabbitTemplate;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
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

        Map<String, Object> thanosResponse = thanosClient.fetchMetrics(appId);
        // Validate
        if (thanosResponse == null || thanosResponse.isEmpty()) {
            throw new RuntimeException("No metrics found for application ID: " + appId);
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
                    return ResponseEntity.ok("Application ID is valid.");
                } else {
                    return ResponseEntity.badRequest().body("Invalid Application ID.");
                }
            }
        }
        return ResponseEntity.status(500).body("Error validating Application ID.");
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
