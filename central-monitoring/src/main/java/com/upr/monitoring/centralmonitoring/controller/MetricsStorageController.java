package com.upr.monitoring.centralmonitoring.controller;

import java.util.List;
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
@Tag(name = "Metrics Storage", description = "Operations for storing and retrieving metrics by application ID")
public class MetricsStorageController {

    private final MetricsStorageService metricsStorageService;

    public MetricsStorageController(MetricsStorageService metricsStorageService) {
        this.metricsStorageService = metricsStorageService;
    }

    @Operation(
        summary = "Store metric name for application",
        description = "Stores a metric name for a specific application to enable later retrieval from Thanos monitoring system"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Metric name successfully stored",
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
            metricsStorageService.storeMetric(request.getApplicationId(), request.getMetric());
            return ResponseEntity.ok("Metric '" + request.getMetric() + "' successfully stored for application '" + request.getApplicationId() + "'");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error storing metric: " + e.getMessage());
        }
    }

    @Operation(
        summary = "Get all stored metrics",
        description = "Retrieves all applications and their associated metrics"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "All metrics retrieved successfully",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = "application/json"))
    })
    @GetMapping("/all")
    public ResponseEntity<Map<String, List<String>>> getAllMetrics() {
        Map<String, List<String>> allMetrics = metricsStorageService.getAllMetrics();
        return ResponseEntity.ok(allMetrics);
    }

    @Operation(
        summary = "Check if application exists",
        description = "Verifies whether a specific application has metrics stored"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Application existence check completed",
                    content = @Content(mediaType = "application/json", schema = @Schema(type = "boolean"))),
        @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = "application/json"))
    })
    @GetMapping("/exists/{applicationId}")
    public ResponseEntity<Boolean> checkApplicationExists(
            @Parameter(description = "ID of the application to check", required = true)
            @PathVariable String applicationId) {
        
        boolean exists = metricsStorageService.applicationExists(applicationId);
        return ResponseEntity.ok(exists);
    }

    @Operation(
        summary = "Get metrics for application",
        description = "Retrieves all metrics for a specific application by its ID"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Metrics found and retrieved",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", description = "Application not found",
                    content = @Content(mediaType = "text/plain", schema = @Schema(type = "string"))),
        @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = "text/plain", schema = @Schema(type = "string")))
    })
    @GetMapping("/{applicationId}")
    public ResponseEntity<List<String>> getMetrics(
            @Parameter(description = "ID of the application to retrieve metrics for", required = true)
            @PathVariable String applicationId) {
        
        List<String> metrics = metricsStorageService.getMetrics(applicationId);
        if (metrics != null) {
            return ResponseEntity.ok(metrics);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(
        summary = "Get all application IDs",
        description = "Retrieves all application IDs that have metrics stored"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "All application IDs retrieved successfully",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = "application/json"))
    })
    @GetMapping("/applications")
    public ResponseEntity<Set<String>> getAllApplicationIds() {
        Set<String> applicationIds = metricsStorageService.getAllApplicationIds();
        return ResponseEntity.ok(applicationIds);
    }

    @Operation(
        summary = "Delete application and all its metrics",
        description = "Removes an application and all its associated metrics from storage"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Application successfully deleted",
                    content = @Content(mediaType = "text/plain", schema = @Schema(type = "string"))),
        @ApiResponse(responseCode = "404", description = "Application not found",
                    content = @Content(mediaType = "text/plain", schema = @Schema(type = "string"))),
        @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = "text/plain", schema = @Schema(type = "string")))
    })
    @DeleteMapping("/{applicationId}")
    public ResponseEntity<String> deleteApplication(
            @Parameter(description = "ID of the application to delete", required = true)
            @PathVariable String applicationId) {
        
        List<String> removedMetrics = metricsStorageService.removeApplication(applicationId);
        if (removedMetrics != null) {
            return ResponseEntity.ok("Application '" + applicationId + "' and its " + removedMetrics.size() + " metrics successfully deleted");
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(
        summary = "Delete specific metric from application",
        description = "Removes a specific metric from an application"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Metric successfully deleted",
                    content = @Content(mediaType = "text/plain", schema = @Schema(type = "string"))),
        @ApiResponse(responseCode = "404", description = "Metric or application not found",
                    content = @Content(mediaType = "text/plain", schema = @Schema(type = "string"))),
        @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = "text/plain", schema = @Schema(type = "string")))
    })
    @DeleteMapping("/{applicationId}/metrics/{metric}")
    public ResponseEntity<String> deleteMetric(
            @Parameter(description = "ID of the application", required = true)
            @PathVariable String applicationId,
            @Parameter(description = "Metric to delete", required = true)
            @PathVariable String metric) {
        
        boolean removed = metricsStorageService.removeMetric(applicationId, metric);
        if (removed) {
            return ResponseEntity.ok("Metric '" + metric + "' successfully deleted from application '" + applicationId + "'");
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
        summary = "Get application count",
        description = "Returns the total number of applications with stored metrics"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Application count retrieved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(type = "integer"))),
        @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = "application/json"))
    })
    @GetMapping("/count/applications")
    public ResponseEntity<Integer> getApplicationCount() {
        int count = metricsStorageService.getApplicationCount();
        return ResponseEntity.ok(count);
    }

    @Operation(
        summary = "Get total metrics count",
        description = "Returns the total number of metrics across all applications"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Total metrics count retrieved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(type = "integer"))),
        @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = "application/json"))
    })
    @GetMapping("/count/metrics")
    public ResponseEntity<Integer> getTotalMetricsCount() {
        int count = metricsStorageService.getTotalMetricsCount();
        return ResponseEntity.ok(count);
    }
}