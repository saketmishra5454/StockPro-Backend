package com.stockpro.alert.listener;

import com.stockpro.alert.dto.StockMovementEvent;
import com.stockpro.alert.service.AlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;


 // StockAlertListener — listens to stock.alert.queue for alert-triggering events.

@Component
@RequiredArgsConstructor
@Slf4j
public class StockAlertListener {

    private final AlertService alertService;


     // Listens to "stock.alert.queue" for critical stock events.
     // Creates a LOW_STOCK or CRITICAL alert automatically.

    @RabbitListener(queues = "stock.alert.queue")
    public void onStockAlertEvent(StockMovementEvent event) {

        log.info("Stock alert event received: product={}, warehouse={}, newQty={}, type={}",
                event.getProductId(), event.getWarehouseId(),
                event.getNewQuantity(), event.getMovementType());

        try {
            // Determine severity based on quantity
            String severity = event.getNewQuantity() == 0 ? "CRITICAL" : "WARNING";

            // Call service to create alert — which also sends email if CRITICAL
            alertService.sendLowStockAlert(
                    event.getProductId(),
                    event.getWarehouseId(),
                    event.getNewQuantity());

            log.info("Low stock alert created via RabbitMQ: product={}, warehouse={}, severity={}",
                    event.getProductId(), event.getWarehouseId(), severity);

        } catch (Exception e) {
            log.error("Failed to create stock alert for product={}, warehouse={}: {}",
                    event.getProductId(), event.getWarehouseId(), e.getMessage());
        }
    }
}