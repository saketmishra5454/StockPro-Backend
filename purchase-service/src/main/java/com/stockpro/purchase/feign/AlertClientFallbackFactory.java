package com.stockpro.purchase.feign;

import com.stockpro.purchase.dto.AlertRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AlertClientFallbackFactory implements FallbackFactory<AlertClient> {

    @Override
    public AlertClient create(Throwable cause) {
        return new AlertClient() {
            @Override
            public void sendAlert(AlertRequest alertRequest) {
                log.warn("alert-service unavailable; purchase workflow will continue without alert delivery: {}",
                        cause.getMessage());
            }
        };
    }
}
