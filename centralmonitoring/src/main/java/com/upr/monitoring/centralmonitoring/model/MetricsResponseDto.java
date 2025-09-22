package com.upr.monitoring.centralmonitoring.model;

import java.time.LocalDateTime;
import java.util.Map;

import lombok.Builder;

@Builder
public class MetricsResponseDto {

    private String applicationId;
    private LocalDateTime timestamp;
    private Map<String, Object> metrics;

    public MetricsResponseDto(String applicationId, LocalDateTime timestamp, Map<String, Object> metrics) {
        this.applicationId = applicationId;
        this.metrics = metrics;
        this.timestamp = timestamp;
    }

    public String getApplicationId() {
        return applicationId;
    }

    public Map<String, Object> getMetrics() {
        return metrics;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }


}
