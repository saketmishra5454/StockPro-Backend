package com.stockpro.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {

    @Bean
    public RouteLocator routes(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("auth-service", r -> r.path("/api/auth/**")
                        .filters(f -> f.circuitBreaker(c -> c.setName("auth-service")
                                .setFallbackUri("forward:/fallback/auth-service")))
                        .uri("lb://auth-service"))
                .route("product-service", r -> r.path("/api/products/**")
                        .filters(f -> f.circuitBreaker(c -> c.setName("product-service")
                                .setFallbackUri("forward:/fallback/product-service")))
                        .uri("lb://product-service"))
                .route("warehouse-service", r -> r.path("/api/warehouses/**", "/api/stock/**")
                        .filters(f -> f.circuitBreaker(c -> c.setName("warehouse-service")
                                .setFallbackUri("forward:/fallback/warehouse-service")))
                        .uri("lb://warehouse-service"))
                .route("purchase-service", r -> r.path("/api/purchase-orders/**")
                        .filters(f -> f.circuitBreaker(c -> c.setName("purchase-service")
                                .setFallbackUri("forward:/fallback/purchase-service")))
                        .uri("lb://purchase-service"))
                .route("supplier-service", r -> r.path("/api/suppliers/**")
                        .filters(f -> f.circuitBreaker(c -> c.setName("supplier-service")
                                .setFallbackUri("forward:/fallback/supplier-service")))
                        .uri("lb://supplier-service"))
                .route("movement-service", r -> r.path("/api/movements/**")
                        .filters(f -> f.circuitBreaker(c -> c.setName("movement-service")
                                .setFallbackUri("forward:/fallback/movement-service")))
                        .uri("lb://movement-service"))
                .route("alert-service", r -> r.path("/api/alerts/**")
                        .filters(f -> f.circuitBreaker(c -> c.setName("alert-service")
                                .setFallbackUri("forward:/fallback/alert-service")))
                        .uri("lb://alert-service"))
                .route("report-service", r -> r.path("/api/reports/**")
                        .filters(f -> f.circuitBreaker(c -> c.setName("report-service")
                                .setFallbackUri("forward:/fallback/report-service")))
                        .uri("lb://report-service"))
                .build();
    }
}
