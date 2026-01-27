package com.temporal.jetstream.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI jetstreamOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Temporal Jetstream API")
                        .description("Airline Flight Lifecycle Orchestration Demo with Temporal")
                        .version("1.0.0"));
    }
}
