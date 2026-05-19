package com.stockpro.purchase.feign;

import com.stockpro.purchase.dto.AlertRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = "alert-service",
        url = "${stockpro.clients.alert-service.url:}",
        fallbackFactory = AlertClientFallbackFactory.class)
public interface AlertClient {

    @PostMapping("/api/alerts")
    void sendAlert(@RequestBody AlertRequest alertRequest);
}
