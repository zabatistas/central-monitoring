package com.upr.monitoring.centralmonitoring.model;

import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "Request payload for creating or modifying alert rules")
public class AlertRequest {
    
    @Schema(description = "Unique identifier for the application", 
            example = "user-service", 
            required = true)
    private String applicationId;
    
    @Schema(description = "List of alert rules to be applied to the application. Each rule is a map containing alert configuration.",
            example = "[{\"alert\": \"HighCPUUsage\", \"expr\": \"cpu_usage > 80\", \"for\": \"5m\", \"labels\": {\"severity\": \"warning\"}, \"annotations\": {\"summary\": \"High CPU usage detected\"}}]",
            required = true)
    private List<Map<String, Object>> rules;
}
