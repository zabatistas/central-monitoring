package com.upr.monitoring.centralmonitoring.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request object for storing metric data for an application")
public class MetricStorageRequest {

    @Schema(description = "Unique identifier for the application", 
            example = "app1", 
            required = true)
    private String applicationId;
    
    @Schema(description = "The metric to be stored for the application", 
            example = "cpu_usage_75.5_percent")
    private String metric;

    public String getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    public String getMetric() {
        return metric;
    }

    public void setMetric(String metric) {
        this.metric = metric;
    }
}