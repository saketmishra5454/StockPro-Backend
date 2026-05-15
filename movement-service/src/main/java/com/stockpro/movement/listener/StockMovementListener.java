package com.stockpro.movement.listener;

import com.stockpro.movement.dto.StockMovementEvent;
import com.stockpro.movement.entity.StockMovement;
import com.stockpro.movement.service.MovementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class StockMovementListener {

    private final MovementService movementService;


    @RabbitListener(queues = "stock.movement.queue")
    public void onStockMovementEvent(StockMovementEvent event) {

        log.info("Received stock movement event: type={}, product={}, warehouse={}, qty={}",
                event.getMovementType(),
                event.getProductId(),
                event.getWarehouseId(),
                event.getQuantityChanged());

        try {
            StockMovement movement = convertEventToMovement(event);

            // Movement records are immutable audit entries
            movementService.recordMovement(movement);

            log.info("Movement recorded successfully: movementId={}",
                    movement.getMovementId());

        } catch (Exception e) {
            log.error("Failed to record movement event: product={}, warehouse={}, error={}",
                    event.getProductId(), event.getWarehouseId(), e.getMessage());
        }
    }


    private StockMovement convertEventToMovement(StockMovementEvent event) {
        StockMovement movement = new StockMovement();

        movement.setProductId(event.getProductId());
        movement.setWarehouseId(event.getWarehouseId());
        movement.setMovementType(event.getMovementType());

        // TRANSFER_OUT and STOCK_OUT have negative quantityChanged in event
        movement.setQuantity(Math.abs(event.getQuantityChanged()));

        movement.setBalanceAfter(event.getNewQuantity());
        movement.setPerformedBy(event.getPerformedBy());

        if (event.getReferenceId() != null && !event.getReferenceId().isEmpty()) {
            try {
                movement.setReferenceId(Integer.parseInt(event.getReferenceId()));
                movement.setReferenceType("PURCHASE_ORDER");
            } catch (NumberFormatException e) {
                movement.setReferenceId(0);
                movement.setReferenceType("TRANSFER");
                movement.setNotes("Reference: " + event.getReferenceId());
            }
        }

        movement.setMovementDate(
                event.getMovementDate() != null
                        ? event.getMovementDate()
                        : LocalDateTime.now());

        return movement;
    }
}
