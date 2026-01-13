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
                .route("user-service", r -> r
                        .path("/api/users/**")
                        .uri("http://user-service:8081"))
                .route("order-service", r -> r
                        .path("/api/orders/**")
                        .uri("http://order-service:8082"))
                .route("product-service", r -> r
                        .path("/api/products/**")
                        .uri("http://product-service:8083"))
                .build();
    }

}
