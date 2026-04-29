package com.stockpro.alert.feign;

import com.stockpro.alert.dto.StockLevelDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;


 // WarehouseClient — Feign HTTP client for warehouse-service.

@FeignClient(name = "warehouse-service")
public interface WarehouseClient {


      //Get all stock levels that are critically low.

    @GetMapping("/api/stock/low")
    List<StockLevelDto> getLowStockItems();
}