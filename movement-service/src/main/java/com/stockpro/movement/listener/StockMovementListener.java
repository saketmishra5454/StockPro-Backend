package com.stockpro.movement.listener;

import com.stockpro.movement.dto.StockMovementEvent;
import com.stockpro.movement.entity.StockMovement;
import com.stockpro.movement.service.MovementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * StockMovementListener — listens to RabbitMQ for stock change events.
 *
 * How the flow works:
 * 1. Warehouse staff records stock in/out/transfer via warehouse-service API
 * 2. warehouse-service updates the StockLevel in its own DB
 * 3. warehouse-service publishes a StockMovementEvent to RabbitMQ exchange
 *    with routing key "stock.movement.warehouse"
 * 4. RabbitMQ routes it to "stock.movement.queue"
 *    (because queue is bound with pattern "stock.movement.#")
 * 5. THIS LISTENER picks up the event from the queue
 * 6. Converts the event into a StockMovement entity
 * 7. Calls recordMovement() which saves it as a write-once audit record
 *
 * WHY ASYNC (RabbitMQ) instead of direct REST call?
 *   warehouse-service doesn't wait for movement-service to respond.
 *   If movement-service is down, the event stays in the queue safely.
 *   When movement-service restarts, it processes all queued events.
 *   No stock operations are blocked by audit trail recording.
 *
 * FIXED from old file:
 *   Old: receives Map<String,Object> — crashes at runtime with casting errors
 *   New: receives typed StockMovementEvent DTO — Jackson handles deserialization
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StockMovementListener {

    private final MovementService movementService;

    /**
     * Listens to "stock.movement.queue" for all stock change events.
     *
     * @RabbitListener automatically:
     *   - Connects to RabbitMQ
     *   - Subscribes to "stock.movement.queue"
     *   - Deserializes incoming JSON to StockMovementEvent using Jackson
     *   - Calls this method for each message
     *   - Acknowledges the message after successful processing
     *   - If this method throws an exception, message goes back to queue
     */
    @RabbitListener(queues = "stock.movement.queue")
    public void onStockMovementEvent(StockMovementEvent event) {

        log.info("Received stock movement event: type={}, product={}, warehouse={}, qty={}",
                event.getMovementType(),
                event.getProductId(),
                event.getWarehouseId(),
                event.getQuantityChanged());

        try {
            // Convert the RabbitMQ event DTO to a StockMovement entity
            StockMovement movement = convertEventToMovement(event);

            // Save as immutable audit record
            // recordMovement() throws exception if trying to update — write-once enforced
            movementService.recordMovement(movement);

            log.info("Movement recorded successfully: movementId={}",
                    movement.getMovementId());

        } catch (Exception e) {
            // Log but don't rethrow — rethrowing would cause infinite retry loop
            // In production, you'd send failed messages to a Dead Letter Queue (DLQ)
            log.error("Failed to record movement event: product={}, warehouse={}, error={}",
                    event.getProductId(), event.getWarehouseId(), e.getMessage());
        }
    }

    /**
     * Converts a RabbitMQ event DTO to a StockMovement entity.
     *
     * Key mapping decisions:
     *   quantity = Math.abs(quantityChanged) — always positive in entity
     *   balanceAfter = newQuantity from event
     *   movementDate = from event if present, otherwise now()
     *   referenceId = 0 if event has no reference (automated movement)
     */
    private StockMovement convertEventToMovement(StockMovementEvent event) {
        StockMovement movement = new StockMovement();

        movement.setProductId(event.getProductId());
        movement.setWarehouseId(event.getWarehouseId());
        movement.setMovementType(event.getMovementType());

        // quantity is always stored as positive number
        // TRANSFER_OUT and STOCK_OUT have negative quantityChanged in event
        movement.setQuantity(Math.abs(event.getQuantityChanged()));

        movement.setBalanceAfter(event.getNewQuantity());
        movement.setPerformedBy(event.getPerformedBy());

        // Parse referenceId from string — "0" or null if no reference
        if (event.getReferenceId() != null && !event.getReferenceId().isEmpty()) {
            try {
                // referenceId may contain non-numeric strings like "TRANSFER-1-2"
                // In that case store 0 and put it in notes instead
                movement.setReferenceId(Integer.parseInt(event.getReferenceId()));
                movement.setReferenceType("PURCHASE_ORDER");
            } catch (NumberFormatException e) {
                movement.setReferenceId(0);
                movement.setReferenceType("TRANSFER");
                movement.setNotes("Reference: " + event.getReferenceId());
            }
        }

        // Use event timestamp if available, otherwise now
        movement.setMovementDate(
                event.getMovementDate() != null
                        ? event.getMovementDate()
                        : LocalDateTime.now());

        return movement;
    }
}