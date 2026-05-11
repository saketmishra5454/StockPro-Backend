package com.stockpro.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class GatewayConfig {

    @Bean
    public RouteLocator routes(RouteLocatorBuilder builder) {
        return builder.routes()
            .route("auth-service",     r -> r.path("/api/auth/**").uri("lb://auth-service"))
            .route("product-service",  r -> r.path("/api/products/**").uri("lb://product-service"))
            .route("warehouse-service",r -> r.path("/api/warehouses/**", "/api/stock/**").uri("lb://warehouse-service"))
            .route("purchase-service", r -> r.path("/api/purchase-orders/**").uri("lb://purchase-service"))
            .route("supplier-service", r -> r.path("/api/suppliers/**").uri("lb://supplier-service"))
            .route("movement-service", r -> r.path("/api/movements/**").uri("lb://movement-service"))
            .route("alert-service",    r -> r.path("/api/alerts/**").uri("lb://alert-service"))
            .route("report-service",   r -> r.path("/api/reports/**").uri("lb://report-service"))
            .build();
    }

   /* @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(
                "http://127.0.0.1:4300",
                "http://localhost:4300",
                "http://127.0.0.1:4200",
                "http://localhost:4200"
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of(
                HttpHeaders.AUTHORIZATION,
                HttpHeaders.CONTENT_TYPE,
                "X-User-Id",
                "X-User-Email",
                "X-User-Role"
        ));
        config.setExposedHeaders(List.of("X-Auth-Error"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsWebFilter(source);
    }*/
}
