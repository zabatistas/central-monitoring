package com.upr.monitoring.centralmonitoring.service;

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

    public MetricsResponseDto getMetricsForSpecificApplication(String appId) {

        Map<String, Object> metrics = thanosClient.fetchMetrics(appId);
        // Validate
        if (metrics == null || metrics.isEmpty()) {
            throw new RuntimeException("No metrics found for application ID: " + appId);
        }
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
