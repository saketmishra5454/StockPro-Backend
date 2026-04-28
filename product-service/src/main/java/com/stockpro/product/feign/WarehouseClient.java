package com.stockpro.product.feign;

import com.stockpro.product.dto.StockLevelDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

/**
 * WarehouseClient — Feign interface that makes HTTP calls to warehouse-service.
 *
 * How FeignClient works:
 * 1. @FeignClient(name = "warehouse-service") tells Spring to create an HTTP
 *    client that talks to the service registered as "warehouse-service" in Eureka.
 * 2. The actual IP and port of warehouse-service is resolved via Eureka.
 *    "lb://" = load-balanced — if warehouse-service runs on multiple instances,
 *    Feign picks one automatically.
 * 3. You call warehouseClient.getLowStockItems() like a normal Java method.
 *    Behind the scenes, Feign sends: GET http://warehouse-service/api/stock/low
 *    and deserializes the JSON response into List<StockLevelDto>.
 *
 * IMPORTANT: The endpoint path "/api/stock/low" matches exactly what
 * WarehouseResource in warehouse-service exposes (@GetMapping("/api/stock/low")).
 * If the path doesn't match, you get a 404 from warehouse-service.
 */
@FeignClient(name = "warehouse-service")
public interface WarehouseClient {

    /**
     * Calls GET /api/stock/low on warehouse-service.
     * Returns all stock records where quantity is below the reorder threshold.
     * This is the actual endpoint defined in warehouse-service's WarehouseResource.
     */
    @GetMapping("/api/stock/low")
    List<StockLevelDto> getLowStockItems();
}