package com.stockpro.alert.scheduler;

import com.stockpro.alert.dto.StockLevelDto;
import com.stockpro.alert.feign.WarehouseClient;
import com.stockpro.alert.service.AlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;


 //LowStockScheduler — proactively checks for low stock every 15 minutes.

@Component
@RequiredArgsConstructor
@Slf4j
public class LowStockScheduler {

    private final WarehouseClient warehouseClient;
    private final AlertService alertService;


     // Runs every 15 minutes (900,000 milliseconds).
     // Calls warehouse-service to get all low stock items.

    @Scheduled(fixedRate = 900000)
    public void checkLowStock() {
        log.info("Running scheduled low-stock check...");

        try {
            List<StockLevelDto> lowStockItems = warehouseClient.getLowStockItems();

            if (lowStockItems == null || lowStockItems.isEmpty()) {
                log.info("No low stock items found in scheduled check.");
                return;
            }

            log.info("Found {} low stock items in scheduled check.", lowStockItems.size());

            for (StockLevelDto item : lowStockItems) {
                try {
                    // sendLowStockAlert checks for duplicates internally
                    // so running every 15 minutes won't spam users
                    alertService.sendLowStockAlert(
                            item.getProductId(),
                            item.getWarehouseId(),
                            item.getQuantity());
                } catch (Exception e) {
                    log.warn("Failed to create alert for product={}, warehouse={}: {}",
                            item.getProductId(), item.getWarehouseId(), e.getMessage());
                }
            }

        } catch (Exception e) {
            // warehouse-service might be down — log and wait for next cycle
            log.error("Scheduled low-stock check failed (warehouse-service may be down): {}",
                    e.getMessage());
        }
    }
}