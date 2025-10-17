package com.upr.monitoring.centralmonitoring.controller;

import org.springframework.web.bind.annotation.RestController;

import com.upr.monitoring.centralmonitoring.model.MetricsResponseDto;
import com.upr.monitoring.centralmonitoring.service.MetricsService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;


@RestController
@Tag(name = "Metrics", description = "Operations related to metrics collection and application management")
public class MetricsController {

    private MetricsService metricsService;

    public MetricsController(MetricsService metricsService) {
        this.metricsService = metricsService;
    }
    

    @Operation(
        summary = "Add application for monitoring",
        description = "Validates and registers a new application ID for metrics collection"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Application successfully registered",
                    content = @Content(mediaType = "text/plain", schema = @Schema(type = "string"))),
        @ApiResponse(responseCode = "400", description = "Invalid application ID",
                    content = @Content(mediaType = "text/plain", schema = @Schema(type = "string"))),
        @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = "text/plain", schema = @Schema(type = "string")))
    })
    @PostMapping("metrics/add-application")
    public ResponseEntity<String> postMethodName(
            @Parameter(description = "Application ID to be registered for monitoring", required = true)
            @RequestBody String appId) {

        return metricsService.validateApplicationId(appId);
    }
    
    @Operation(
        summary = "Get metrics for specific application",
        description = "Retrieves the latest metrics data for a specific application by its ID"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Metrics successfully retrieved",
                    content = @Content(mediaType = "application/json", 
                                     schema = @Schema(implementation = MetricsResponseDto.class))),
        @ApiResponse(responseCode = "404", description = "Application not found",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = "application/json"))
    })
    @GetMapping("metrics/{applicationId}")
    public MetricsResponseDto getMetricsForSpecificApplication(
            @Parameter(description = "ID of the application to retrieve metrics for", required = true)
            @PathVariable String applicationId) {
        return metricsService.getMetricsForSpecificApplication(applicationId);
    }

        // { "application_id": "app123", "timestamp": "2024-11-12T10:00:00Z", "metrics": { "cpu_usage": "70%", "memory_consumption": "1.5GB", "latency": "30ms" } }
    

    //TODO:  We want an API that will monitor specific application metrics

    @Operation(
        summary = "Fetch specific metrics for application",
        description = "Retrieves specific metrics data for a given application by its ID"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Specific metrics successfully retrieved",
                    content = @Content(mediaType = "application/json", 
                                     schema = @Schema(implementation = MetricsResponseDto.class))),
        @ApiResponse(responseCode = "404", description = "Application not found",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = "application/json"))
    })
    @GetMapping("metrics/fetchSpecific/{applicationId}")
    public MetricsResponseDto fetchSpecificMetrics(
            @Parameter(description = "ID of the application to fetch specific metrics for", required = true)
            @PathVariable String applicationId) {
        return metricsService.getSpecificMetricsForApplication(applicationId);
    }


    
    


}
