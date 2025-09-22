package com.upr.monitoring.centralmonitoring.controller;

import org.springframework.web.bind.annotation.RestController;

import com.upr.monitoring.centralmonitoring.model.MetricsResponseDto;
import com.upr.monitoring.centralmonitoring.service.MetricsService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;



@RestController
public class MetricsController {

    private MetricsService metricsService;

    public MetricsController(MetricsService metricsService) {
        this.metricsService = metricsService;
    }
    

    @PostMapping("metrics/add-application")
    public ResponseEntity<String> postMethodName(@RequestBody String appId) {

        return metricsService.validateApplicationId(appId);
    }
    

    @GetMapping("metrics/{applicationId}")
    public MetricsResponseDto getMetricsForSpecificApplication(@PathVariable String applicationId) {
        return metricsService.getMetricsForSpecificApplication(applicationId);
    }
    

}
