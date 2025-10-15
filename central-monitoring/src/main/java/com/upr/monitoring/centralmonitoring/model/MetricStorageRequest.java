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
@Schema(description = "Request object for storing metric data")
public class MetricStorageRequest {

    @Schema(description = "Unique key identifier for the metric", 
            example = "cpu_usage_app1", 
            required = true)
    private String metricKey;
    
    @Schema(description = "The metric data to be stored (can be any type of object)", 
            example = "{\"value\": 75.5, \"unit\": \"percentage\", \"timestamp\": \"2023-10-03T14:30:00\"}")
    private Object metricData;

    public String getMetricKey() {
        return metricKey;
    }

    public void setMetricKey(String metricKey) {
        this.metricKey = metricKey;
    }

    public Object getMetricData() {
        return metricData;
    }

    public void setMetricData(Object metricData) {
        this.metricData = metricData;
    }
}