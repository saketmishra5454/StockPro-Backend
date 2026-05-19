package com.stockpro.report.feign;

import com.stockpro.report.dto.StockLevelDto;
import com.stockpro.report.dto.WarehouseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "warehouse-service", fallbackFactory = WarehouseClientFallbackFactory.class)
public interface WarehouseClient {

    @GetMapping("/api/warehouses")
    List<WarehouseDto> getAllWarehouses();

    @GetMapping("/api/stock")
    List<StockLevelDto> getAllStockLevels();


    @GetMapping("/api/stock/warehouse/{warehouseId}")
    List<StockLevelDto> getStockByWarehouse(@PathVariable int warehouseId);


    @GetMapping("/api/stock/low")
    List<StockLevelDto> getLowStockItems();
}
