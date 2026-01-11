package com.example.webserver;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfiguration {

    @Bean
    public RouteLocator routes(RouteLocatorBuilder builder) {
        return builder.routes()
                // User Service
                .route("user-service", r -> r
                        .path("/api/users/**", "/auth/**")
                        .uri("http://localhost:8081"))
                // Order Service
                .route("order-service", r -> r
                        .path("/api/orders/**")
                        .uri("http://localhost:8082"))
                // Product Service
                .route("product-service", r -> r
                        .path("/api/products/**")
                        .uri("http://localhost:8083"))
                .build();
    }
}


