package com.stockpro.report.feign;

import com.stockpro.report.dto.StockMovementDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * MovementClient — Feign HTTP client for movement-service.
 *
 * FIXED endpoints:
 *   OLD: GET /api/movements — wrong path, returns all (no filtering)
 *   NEW:
 *     GET /api/movements/all                        — all movements (admin)
 *     GET /api/movements/product/{id}               — by product
 *     GET /api/movements/date-range?from=&to=       — by date range (COGS calc)
 *     GET /api/movements/stock-out/{productId}      — total STOCK_OUT quantity
 *     GET /api/movements/stock-in/{productId}       — total STOCK_IN quantity
 *
 * FIXED return type: StockMovementDto / Map<String,Object> for aggregate endpoints
 */
@FeignClient(name = "movement-service")
public interface MovementClient {

    /**
     * Get all movements — used for platform-wide analytics.
     * Matches: GET /api/movements/all in MovementResource.
     */
    @GetMapping("/api/movements/all")
    List<StockMovementDto> getAllMovements();

    /**
     * Get all movements for a specific product.
     * Used to calculate per-product movement velocity (top/slow movers).
     * Matches: GET /api/movements/product/{id} in MovementResource.
     */
    @GetMapping("/api/movements/product/{productId}")
    List<StockMovementDto> getMovementsByProduct(@PathVariable int productId);

    /**
     * Get movements within a date range.
     * Used to calculate COGS for inventory turnover.
     * Matches: GET /api/movements/date-range?from=&to= in MovementResource.
     *
     * @param from ISO datetime: 2026-04-01T00:00:00
     * @param to   ISO datetime: 2026-04-30T23:59:59
     */
    @GetMapping("/api/movements/date-range")
    List<StockMovementDto> getMovementsByDateRange(
            @RequestParam String from,
            @RequestParam String to);

    /**
     * Get total STOCK_OUT quantity for a product.
     * Returns Map: { "productId": 5, "totalStockOut": 800 }
     * Used in turnover and COGS calculations.
     */
    @GetMapping("/api/movements/stock-out/{productId}")
    java.util.Map<String, Object> getStockOut(@PathVariable int productId);
}