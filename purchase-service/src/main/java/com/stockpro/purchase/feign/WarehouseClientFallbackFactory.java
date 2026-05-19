package com.stockpro.purchase.feign;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class WarehouseClientFallbackFactory implements FallbackFactory<WarehouseClient> {

    @Override
    public WarehouseClient create(Throwable cause) {
        return (warehouseId, productId, delta) -> {
            log.error("warehouse-service unavailable while updating stock: warehouseId={}, productId={}, delta={}, cause={}",
                    warehouseId, productId, delta, cause.getMessage());
            throw new IllegalStateException("Warehouse stock update is temporarily unavailable.", cause);
        };
    }
}
