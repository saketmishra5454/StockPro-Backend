package com.stockpro.report.feign;

import com.stockpro.report.dto.StockLevelDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

/**
 * WarehouseClient — Feign HTTP client for warehouse-service.
 *
 * FIXED endpoints:
 *   OLD: GET /api/stock/all — this endpoint does NOT exist in warehouse-service
 *   NEW:
 *     GET /api/stock/warehouse/{warehouseId} — get all stock for one warehouse
 *     GET /api/stock/low                     — get low stock items
 *
 * FIXED return type: List<StockLevelDto> instead of List<Map<String,Object>>
 */
@FeignClient(name = "warehouse-service")
public interface WarehouseClient {

    /**
     * Get all stock levels in a specific warehouse.
     * Called during takeSnapshot(warehouseId).
     * Matches: GET /api/stock/warehouse/{warehouseId} in WarehouseResource.
     */
    @GetMapping("/api/stock/warehouse/{warehouseId}")
    List<StockLevelDto> getStockByWarehouse(@PathVariable int warehouseId);

    /**
     * Get all low stock items across all warehouses.
     * Used in getLowStockReport().
     * Matches: GET /api/stock/low in WarehouseResource.
     */
    @GetMapping("/api/stock/low")
    List<StockLevelDto> getLowStockItems();
}