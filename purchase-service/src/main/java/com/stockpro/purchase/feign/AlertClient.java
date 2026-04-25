package com.stockpro.purchase.feign;

import com.stockpro.purchase.dto.AlertRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@FeignClient(name = "alert-service")
public interface AlertClient {


     // Create an alert in alert-service.
     // Used for PO_PENDING, PO_APPROVED, PO_REJECTED notifications.

    @PostMapping("/api/alerts")
    void sendAlert(@RequestBody AlertRequest alertRequest);
}