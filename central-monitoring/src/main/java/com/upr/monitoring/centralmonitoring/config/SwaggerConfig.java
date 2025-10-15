package com.upr.monitoring.centralmonitoring.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Value("${server.port:8081}")
    private String serverPort;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .servers(List.of(
                        new Server().url("http://localhost:" + serverPort).description("Development Server"),
                        new Server().url("https://your-production-domain.com").description("Production Server")
                ))
                .info(new Info()
                        .title("Central Monitoring API")
                        .description("API for managing metrics collection and alert rules in the central monitoring system")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("UPR Monitoring Team")
                                .email("monitoring@upr.edu")
                                .url("https://github.com/zabatistas/central-monitoring"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")));
    }
}