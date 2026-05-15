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
                .route("auth-service", r -> r.path("/api/auth/**").uri("lb://auth-service"))
                .route("product-service", r -> r.path("/api/products/**").uri("lb://product-service"))
                .route("warehouse-service", r -> r.path("/api/warehouses/**", "/api/stock/**").uri("lb://warehouse-service"))
                .route("purchase-service", r -> r.path("/api/purchase-orders/**").uri("lb://purchase-service"))
                .route("supplier-service", r -> r.path("/api/suppliers/**").uri("lb://supplier-service"))
                .route("movement-service", r -> r.path("/api/movements/**").uri("lb://movement-service"))
                .route("alert-service", r -> r.path("/api/alerts/**").uri("lb://alert-service"))
                .route("report-service", r -> r.path("/api/reports/**").uri("lb://report-service"))
                .build();
    }
}
