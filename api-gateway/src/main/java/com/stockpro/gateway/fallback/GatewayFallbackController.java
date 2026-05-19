package com.stockpro.gateway.fallback;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;

@RestController
public class GatewayFallbackController {

    @RequestMapping("/fallback/{serviceName}")
    public Mono<ResponseEntity<Map<String, Object>>> fallback(@PathVariable String serviceName) {
        Map<String, Object> body = Map.of(
                "timestamp", Instant.now().toString(),
                "status", HttpStatus.SERVICE_UNAVAILABLE.value(),
                "error", "Service temporarily unavailable",
                "service", serviceName,
                "message", "StockPro is protecting the platform from cascading failures. Please retry shortly."
        );
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body));
    }
}
