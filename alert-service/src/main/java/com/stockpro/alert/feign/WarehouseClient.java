package com.stockpro.alert.feign;

import com.stockpro.alert.dto.StockLevelDto;
import com.stockpro.alert.dto.WarehouseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(
        name = "warehouse-service",
        url = "${stockpro.clients.warehouse-service.url:}",
        fallbackFactory = WarehouseClientFallbackFactory.class)
public interface WarehouseClient {

    @GetMapping("/api/stock/low")
    List<StockLevelDto> getLowStockItems();

    @GetMapping("/api/stock")
    List<StockLevelDto> getAllStockLevels();

    @GetMapping("/api/warehouses")
    List<WarehouseDto> getAllWarehouses();

    @GetMapping("/api/warehouses/{id}")
    WarehouseDto getWarehouseById(@PathVariable int id);

    @GetMapping("/api/stock/warehouse/{warehouseId}")
    List<StockLevelDto> getStockByWarehouse(@PathVariable int warehouseId);
}
