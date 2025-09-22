package com.upr.monitoring.centralmonitoring.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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



}
