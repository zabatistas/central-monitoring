package com.upr.monitoring.centralmonitoring.model;

import java.time.LocalDateTime;
import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

// { "application_id": "app123", "timestamp": "2024-11-12T10:00:00Z", "metrics": { "cpu_usage": "70%", "memory_consumption": "1.5GB", "latency": "30ms" } }

@Builder
@Schema(description = "Response containing metrics data for a specific application")
public class MetricsResponseDto {

    @Schema(description = "Unique identifier of the application", 
            example = "user-service")
    private String applicationId;
    
    @Schema(description = "Timestamp when the metrics were collected", 
            example = "2023-10-03T14:30:00")
    private LocalDateTime timestamp;
    
    @Schema(description = "Map containing various metrics and their values",
            example = "{\"cpu_usage\": 75.5, \"memory_usage\": 60.2, \"request_count\": 1250}")
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
