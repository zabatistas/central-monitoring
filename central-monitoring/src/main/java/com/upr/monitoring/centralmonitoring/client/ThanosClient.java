package com.upr.monitoring.centralmonitoring.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class ThanosClient {

    @Value("${thanos.base-url}")
    private String thanosBaseUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    // Maybe incremental approach: first fetch specific metrics, like CPU usage, memory usage, etc.
    // Then expand to more complex queries as needed.
    public Map<String,Object> fetchMetrics(String appId) {
        // Prometheus/Thanos query API endpoint
        log.info("Fetching metrics for appId: {}", appId);

        // Build the PromQL query
        String promql = "{application_id=\"" + appId + "\"}";

        // Encode the query for URL safety
        String encodedQuery = URLEncoder.encode(promql, StandardCharsets.UTF_8);

        URI uri = UriComponentsBuilder.fromUriString(thanosBaseUrl + "/api/v1/query")
                .queryParam("query", encodedQuery)
                .build(true)   // 'true' prevents double-encoding
                .toUri();

        return restTemplate.getForObject(uri, Map.class);


    }

    public Map<String, Object> fetchApplicationIdLabelValues() {
        String url = UriComponentsBuilder
                .fromUriString(thanosBaseUrl + "/api/v1/label/application_id/values")
                .toUriString();

        return restTemplate.getForObject(url, Map.class);
    }

    public Map<String, String> fetchSpecificMetrics(String appId, List<String> metricNames) {
        // Prometheus/Thanos query API endpoint
        log.info("Fetching specific metrics '{}' for appId: {}", metricNames, appId);

        Map<String, String> results = new HashMap<>();
        String metrics = String.join("|", metricNames);
        // Build the PromQL query
        String promql = "{__name__=~\"" + metrics + "\"," + "application_id=\"" + appId + "\"}";

        // Encode the query for URL safety
        String encodedQuery = URLEncoder.encode(promql, StandardCharsets.UTF_8);

        URI uri = UriComponentsBuilder.fromUriString(thanosBaseUrl + "/api/v1/query")
                .queryParam("query", encodedQuery)
                .build(true)   // 'true' prevents double-encoding
                .toUri();
        log.info("Thanos query URI: {}", uri.toString());
        Map<String, Object> response = restTemplate.getForObject(uri, Map.class);
        // Parse the response to extract metric values
        Map<String, String> extractedValues = extractMetricValues(response);
        return extractedValues;
    }

    private Map<String, String> extractMetricValues(Map<String, Object> response) {
        Map<String, String> metricValues = new HashMap<>();
        // Extract the "data" field from the response
        Map<String, Object> data = (Map<String, Object>) response.get("data");
        if (data != null) {
            // Extract the "result" field from the data
            List<Map<String, Object>> results = (List<Map<String, Object>>) data.get("result");
            for (Map<String, Object> result : results) {
                String metricName = (String) result.get("metric");
                String metricValue = (String) result.get("value");
                metricValues.put(metricName, metricValue);
            }
        }
        return metricValues;
    }



}
