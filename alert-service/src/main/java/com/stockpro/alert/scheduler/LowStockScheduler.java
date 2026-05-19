package com.stockpro.alert.scheduler;

import com.stockpro.alert.dto.ProductDto;
import com.stockpro.alert.dto.StockLevelDto;
import com.stockpro.alert.dto.WarehouseDto;
import com.stockpro.alert.feign.ProductClient;
import com.stockpro.alert.feign.WarehouseClient;
import com.stockpro.alert.service.AlertService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class LowStockScheduler {

    private final WarehouseClient warehouseClient;
    private final ProductClient productClient;
    private final AlertService alertService;

    @CircuitBreaker(name = "warehouseService", fallbackMethod = "schedulerFallback")
    @Scheduled(fixedRate = 900000)
    public void checkStockHealth() {
        log.info("Running scheduled stock-health check...");

        try {
            List<StockLevelDto> stockLevels = loadStockLevels();

            if (stockLevels == null || stockLevels.isEmpty()) {
                log.info("No stock levels found in scheduled check.");
                return;
            }

            for (StockLevelDto item : stockLevels) {
                checkStockLevel(item);
            }
        } catch (Exception e) {
            log.error("Scheduled stock-health check failed: {}", e.getMessage());
            runLowStockFallback();
        }
    }

    private List<StockLevelDto> loadStockLevels() {
        try {
            return warehouseClient.getAllStockLevels();
        } catch (Exception e) {
            log.warn("Could not use /api/stock for stock-health check, falling back to warehouse-by-warehouse lookup: {}",
                    e.getMessage());
        }

        List<WarehouseDto> warehouses = warehouseClient.getAllWarehouses();
        return warehouses.stream()
                .flatMap(warehouse -> warehouseClient.getStockByWarehouse(warehouse.getWarehouseId()).stream())
                .toList();
    }

    private void checkStockLevel(StockLevelDto item) {
        try {
            ProductDto product = productClient.getProductById(item.getProductId());
            if (product == null) {
                log.warn("Skipping stock-health check for product={}, warehouse={} because product-service returned no product.",
                        item.getProductId(), item.getWarehouseId());
                return;
            }
            if (!product.isActive()) {
                log.debug("Skipping inactive product={} during stock-health check.", item.getProductId());
                return;
            }
            int availableQty = item.getQuantity() - item.getReservedQuantity();

            if (product.getReorderLevel() > 0 && availableQty < product.getReorderLevel()) {
                alertService.sendLowStockAlert(
                        item.getProductId(),
                        item.getWarehouseId(),
                        availableQty);
            }

            if (product.getMaxStockLevel() > 0 && item.getQuantity() > product.getMaxStockLevel()) {
                alertService.sendOverstockAlert(
                        item.getProductId(),
                        item.getWarehouseId(),
                        item.getQuantity(),
                        product.getMaxStockLevel());
            }
        } catch (Exception e) {
            log.warn("Failed to evaluate stock health for product={}, warehouse={}: {}",
                    item.getProductId(), item.getWarehouseId(), e.getMessage());
        }
    }

    private void runLowStockFallback() {
        try {
            List<StockLevelDto> lowStockItems = warehouseClient.getLowStockItems();
            for (StockLevelDto item : lowStockItems) {
                alertService.sendLowStockAlert(
                        item.getProductId(),
                        item.getWarehouseId(),
                        item.getQuantity() - item.getReservedQuantity());
            }
        } catch (Exception e) {
            log.warn("Low-stock fallback check also failed: {}", e.getMessage());
        }
    }

    public void schedulerFallback(Exception ex) {
        log.error("Warehouse-service unavailable for low-stock check: {}", ex.getMessage());
    }
}
