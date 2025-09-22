package com.upr.monitoring.centralmonitoring.component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.upr.monitoring.centralmonitoring.service.MetricsService;

@Component
public class ScheduledMetricsFetcher {


    private static final Logger log = LoggerFactory.getLogger(ScheduledMetricsFetcher.class);

    private MetricsService  metricsService;

    public ScheduledMetricsFetcher(MetricsService metricsService) {
        this.metricsService = metricsService;
    }

    // every 60 seconds
    @Scheduled(fixedRate = 60000, initialDelay = 10000)
    public void fetchAndLogMetrics() {
        try {

            metricsService.getMetricsForAllRegisteredApps();
        } catch (Exception e) {
            log.error("Error fetching metrics from Thanos", e);
        }
    }
}