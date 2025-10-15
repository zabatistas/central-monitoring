package com.upr.monitoring.centralmonitoring.controller;

import java.util.Map;
import java.util.Set;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.upr.monitoring.centralmonitoring.model.MetricStorageRequest;
import com.upr.monitoring.centralmonitoring.service.MetricsStorageService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/metrics-storage")
@Tag(name = "Metrics Storage", description = "Operations for storing and retrieving metrics in memory")
public class MetricsStorageController {

    private final MetricsStorageService metricsStorageService;

    public MetricsStorageController(MetricsStorageService metricsStorageService) {
        this.metricsStorageService = metricsStorageService;
    }

    @Operation(
        summary = "Store metric data",
        description = "Adds a new metric with its associated data to the storage map"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Metric successfully stored",
                    content = @Content(mediaType = "text/plain", schema = @Schema(type = "string"))),
        @ApiResponse(responseCode = "400", description = "Invalid metric data",
                    content = @Content(mediaType = "text/plain", schema = @Schema(type = "string"))),
        @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = "text/plain", schema = @Schema(type = "string")))
    })
    @PostMapping("/add")
    public ResponseEntity<String> addMetric(
            @Parameter(description = "Metric data to be stored", required = true)
            @RequestBody MetricStorageRequest request) {
        
        try {
            metricsStorageService.storeMetric(request.getMetricKey(), request.getMetricData());
            return ResponseEntity.ok("Metric '" + request.getMetricKey() + "' successfully stored");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error storing metric: " + e.getMessage());
        }
    }

    @Operation(
        summary = "Get all stored metrics",
        description = "Retrieves all metrics currently stored in the map"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "All metrics retrieved successfully",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = "application/json"))
    })
    @GetMapping("/all")
    public ResponseEntity<Map<String, Object>> getAllMetrics() {
        Map<String, Object> allMetrics = metricsStorageService.getAllMetrics();
        return ResponseEntity.ok(allMetrics);
    }

    @Operation(
        summary = "Check if metric exists",
        description = "Verifies whether a specific metric key exists in the storage"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Metric existence check completed",
                    content = @Content(mediaType = "application/json", schema = @Schema(type = "boolean"))),
        @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = "application/json"))
    })
    @GetMapping("/exists/{metricKey}")
    public ResponseEntity<Boolean> checkMetricExists(
            @Parameter(description = "Key of the metric to check", required = true)
            @PathVariable String metricKey) {
        
        boolean exists = metricsStorageService.metricExists(metricKey);
        return ResponseEntity.ok(exists);
    }

    @Operation(
        summary = "Get specific metric",
        description = "Retrieves the data for a specific metric by its key"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Metric found and retrieved",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", description = "Metric not found",
                    content = @Content(mediaType = "text/plain", schema = @Schema(type = "string"))),
        @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = "text/plain", schema = @Schema(type = "string")))
    })
    @GetMapping("/{metricKey}")
    public ResponseEntity<Object> getMetric(
            @Parameter(description = "Key of the metric to retrieve", required = true)
            @PathVariable String metricKey) {
        
        Object metricData = metricsStorageService.getMetric(metricKey);
        if (metricData != null) {
            return ResponseEntity.ok(metricData);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(
        summary = "Get all metric keys",
        description = "Retrieves all metric keys currently stored in the map"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "All metric keys retrieved successfully",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = "application/json"))
    })
    @GetMapping("/keys")
    public ResponseEntity<Set<String>> getAllMetricKeys() {
        Set<String> keys = metricsStorageService.getAllMetricKeys();
        return ResponseEntity.ok(keys);
    }

    @Operation(
        summary = "Delete specific metric",
        description = "Removes a specific metric from the storage by its key"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Metric successfully deleted",
                    content = @Content(mediaType = "text/plain", schema = @Schema(type = "string"))),
        @ApiResponse(responseCode = "404", description = "Metric not found",
                    content = @Content(mediaType = "text/plain", schema = @Schema(type = "string"))),
        @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = "text/plain", schema = @Schema(type = "string")))
    })
    @DeleteMapping("/{metricKey}")
    public ResponseEntity<String> deleteMetric(
            @Parameter(description = "Key of the metric to delete", required = true)
            @PathVariable String metricKey) {
        
        Object removedMetric = metricsStorageService.removeMetric(metricKey);
        if (removedMetric != null) {
            return ResponseEntity.ok("Metric '" + metricKey + "' successfully deleted");
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(
        summary = "Clear all metrics",
        description = "Removes all metrics from the storage"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "All metrics successfully cleared",
                    content = @Content(mediaType = "text/plain", schema = @Schema(type = "string"))),
        @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = "text/plain", schema = @Schema(type = "string")))
    })
    @DeleteMapping("/all")
    public ResponseEntity<String> clearAllMetrics() {
        metricsStorageService.clearAllMetrics();
        return ResponseEntity.ok("All metrics successfully cleared");
    }

    @Operation(
        summary = "Get metrics count",
        description = "Returns the total number of metrics currently stored"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Metrics count retrieved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(type = "integer"))),
        @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = "application/json"))
    })
    @GetMapping("/count")
    public ResponseEntity<Integer> getMetricsCount() {
        int count = metricsStorageService.getMetricsCount();
        return ResponseEntity.ok(count);
    }
}