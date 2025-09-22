package com.upr.monitoring.centralmonitoring;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CentralmonitoringApplication {

	public static void main(String[] args) {
		SpringApplication.run(CentralmonitoringApplication.class, args);
	}

}
