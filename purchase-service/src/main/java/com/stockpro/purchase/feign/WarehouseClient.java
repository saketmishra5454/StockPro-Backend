package com.stockpro.purchase.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;


@FeignClient(name = "warehouse-service")
public interface WarehouseClient {


    @PutMapping("/api/stock/update")
    void updateStock(
            @RequestParam int warehouseId,
            @RequestParam int productId,
            @RequestParam int delta);
}