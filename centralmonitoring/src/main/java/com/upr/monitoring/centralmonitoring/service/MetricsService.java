package com.upr.monitoring.centralmonitoring.service;

import java.util.List;
import java.util.Map;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.upr.monitoring.centralmonitoring.client.ThanosClient;
import com.upr.monitoring.centralmonitoring.model.MetricsResponseDto;

@Service
public class MetricsService {
    
    private ThanosClient thanosClient;

    private RabbitTemplate rabbitTemplate;

    private KafkaTemplate<String, String> kafkaTemplate;

    List<String> appIdList = new java.util.ArrayList<>();

    public MetricsService(ThanosClient thanosClient, RabbitTemplate rabbitTemplate, KafkaTemplate<String, String> kafkaTemplate) {
        this.thanosClient = thanosClient;
        this.rabbitTemplate = rabbitTemplate;
        this.kafkaTemplate = kafkaTemplate;
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

        // Send to Kafka
        kafkaTemplate.send("metrics-topic", dto.toString());

        // Send to RabbitMQ 
        rabbitTemplate.convertAndSend("metrics.exchange", "metrics." + appId, dto.toString());

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
}
