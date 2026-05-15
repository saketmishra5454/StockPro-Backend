package com.stockpro.product.feign;

import com.stockpro.product.dto.StockLevelDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@FeignClient(name = "warehouse-service")
public interface WarehouseClient {


    @GetMapping("/api/stock/low")
    List<StockLevelDto> getLowStockItems();
}
