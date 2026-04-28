package com.stockpro.warehouse.service.impl;

import com.stockpro.warehouse.entity.StockLevel;
import com.stockpro.warehouse.entity.Warehouse;
import com.stockpro.warehouse.event.StockMovementEvent;
import com.stockpro.warehouse.repository.StockLevelRepository;
import com.stockpro.warehouse.repository.WarehouseRepository;
import com.stockpro.warehouse.service.WarehouseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class WarehouseServiceImpl implements WarehouseService {

    private final WarehouseRepository warehouseRepository;
    private final StockLevelRepository stockLevelRepository;
    private final RabbitTemplate rabbitTemplate;

    // RabbitMQ exchange and routing keys — must match RabbitMQConfig declarations
    private static final String EXCHANGE        = "stockpro.exchange";
    private static final String MOVEMENT_KEY    = "stock.movement.warehouse";
    private static final String ALERT_KEY       = "stock.alert.lowstock";

    // ── Warehouse CRUD ─────────────────────────────────────────────

    @Override
    public Warehouse createWarehouse(Warehouse warehouse) {
        if (warehouseRepository.existsByName(warehouse.getName())) {
            throw new RuntimeException("Warehouse with name '" + warehouse.getName() + "' already exists.");
        }
        return warehouseRepository.save(warehouse);
    }

    @Override
    public Warehouse getWarehouseById(int warehouseId) {
        return warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new RuntimeException("Warehouse not found: " + warehouseId));
    }

    @Override
    public List<Warehouse> getAllWarehouses() {
        return warehouseRepository.findAll();
    }

    @Override
    public Warehouse updateWarehouse(int warehouseId, Warehouse updated) {
        Warehouse existing = getWarehouseById(warehouseId);

        if (updated.getName() != null)     existing.setName(updated.getName());
        if (updated.getLocation() != null) existing.setLocation(updated.getLocation());
        if (updated.getAddress() != null)  existing.setAddress(updated.getAddress());
        if (updated.getPhone() != null)    existing.setPhone(updated.getPhone());
        if (updated.getCapacity() > 0)     existing.setCapacity(updated.getCapacity());
        if (updated.getManagerId() > 0)    existing.setManagerId(updated.getManagerId());

        return warehouseRepository.save(existing);
    }

    @Override
    public void deactivateWarehouse(int warehouseId) {
        Warehouse warehouse = getWarehouseById(warehouseId);
        warehouse.setActive(false);
        warehouseRepository.save(warehouse);
    }

    // ── Stock Level Operations ──────────────────────────────────────

    @Override
    public StockLevel getStockLevel(int warehouseId, int productId) {
        return stockLevelRepository.findByWarehouseIdAndProductId(warehouseId, productId)
                .orElseThrow(() -> new RuntimeException(
                        "No stock record found for product " + productId +
                                " in warehouse " + warehouseId));
    }

    /**
     * Initialize stock for a product in a warehouse — creates the StockLevel row.
     * Call this when a new product is first assigned to a warehouse.
     */
    @Override
    @Transactional
    public StockLevel initializeStock(int warehouseId, int productId, int initialQuantity) {
        // Validate warehouse exists
        getWarehouseById(warehouseId);

        if (stockLevelRepository.existsByWarehouseIdAndProductId(warehouseId, productId)) {
            throw new RuntimeException(
                    "Stock level already initialized for product " + productId +
                            " in warehouse " + warehouseId);
        }

        StockLevel sl = new StockLevel();
        sl.setWarehouseId(warehouseId);
        sl.setProductId(productId);
        sl.setQuantity(initialQuantity);
        sl.setReservedQuantity(0);

        return stockLevelRepository.save(sl);
    }

    /**
     * Update stock by a delta.
     * delta > 0 = stock received (GRN)
     * delta < 0 = stock issued/consumed
     *
     * Key rules:
     * - Cannot go below 0 (no negative stock)
     * - After update, publish event to RabbitMQ so movement-service records the audit trail
     * - If new quantity is very low, publish alert event too
     */
    @Override
    @Transactional
    public StockLevel updateStock(int warehouseId, int productId, int delta) {
        // Find existing stock level — create if doesn't exist (first time stock in)
        StockLevel sl = stockLevelRepository
                .findByWarehouseIdAndProductId(warehouseId, productId)
                .orElseGet(() -> {
                    StockLevel newSl = new StockLevel();
                    newSl.setWarehouseId(warehouseId);
                    newSl.setProductId(productId);
                    newSl.setQuantity(0);
                    newSl.setReservedQuantity(0);
                    return newSl;
                });

        int previousQuantity = sl.getQuantity();
        int newQuantity = previousQuantity + delta;

        // Guard: prevent negative stock
        if (newQuantity < 0) {
            throw new RuntimeException(
                    "Insufficient stock. Available: " + sl.getAvailableQuantity() +
                            ", Requested: " + Math.abs(delta));
        }

        sl.setQuantity(newQuantity);
        StockLevel saved = stockLevelRepository.save(sl);

        // Determine movement type for the audit event
        String movementType = delta > 0 ? "STOCK_IN" : "STOCK_OUT";

        // Publish to RabbitMQ → movement-service records the audit trail
        publishMovementEvent(saved, movementType, delta, previousQuantity, newQuantity);

        // If stock is now zero or critically low, publish an alert event
        if (newQuantity == 0) {
            publishAlertEvent(productId, warehouseId, newQuantity, "CRITICAL");
        }

        log.info("Stock updated: product={}, warehouse={}, delta={}, newQty={}",
                productId, warehouseId, delta, newQuantity);

        return saved;
    }

    /**
     * Reserve stock for an open order.
     * Increases reservedQuantity — these units are "locked" for this order.
     * availableQuantity = quantity - reservedQuantity decreases.
     */
    @Override
    @Transactional
    public StockLevel reserveStock(int warehouseId, int productId, int quantity) {
        StockLevel sl = getStockLevel(warehouseId, productId);

        // Cannot reserve more than what's available
        if (quantity > sl.getAvailableQuantity()) {
            throw new RuntimeException(
                    "Cannot reserve " + quantity + " units. Available: " + sl.getAvailableQuantity());
        }

        sl.setReservedQuantity(sl.getReservedQuantity() + quantity);
        StockLevel saved = stockLevelRepository.save(sl);

        log.info("Stock reserved: product={}, warehouse={}, reservedQty={}",
                productId, warehouseId, quantity);

        return saved;
    }

    /**
     * Release a reservation — frees up previously reserved stock.
     * Called when a PO is cancelled or a reservation expires.
     * Decreases reservedQuantity so availableQuantity goes back up.
     */
    @Override
    @Transactional
    public StockLevel releaseReservation(int warehouseId, int productId, int quantity) {
        StockLevel sl = getStockLevel(warehouseId, productId);

        // Cannot release more than what's currently reserved
        int releaseAmount = Math.min(quantity, sl.getReservedQuantity());
        sl.setReservedQuantity(sl.getReservedQuantity() - releaseAmount);

        StockLevel saved = stockLevelRepository.save(sl);

        log.info("Reservation released: product={}, warehouse={}, releasedQty={}",
                productId, warehouseId, releaseAmount);

        return saved;
    }

    /**
     * Transfer stock between two warehouses.
     *
     * MUST be @Transactional — if the credit to destination fails,
     * the debit from source is automatically rolled back.
     * This prevents stock disappearing (debited but never credited).
     *
     * Steps:
     * 1. Debit (subtract) from source warehouse
     * 2. Credit (add) to destination warehouse
     * 3. Publish TRANSFER_OUT event for source
     * 4. Publish TRANSFER_IN event for destination
     */
    @Override
    @Transactional
    public void transferStock(int fromWarehouseId, int toWarehouseId, int productId, int quantity) {
        // Validate both warehouses exist
        getWarehouseById(fromWarehouseId);
        getWarehouseById(toWarehouseId);

        if (fromWarehouseId == toWarehouseId) {
            throw new RuntimeException("Source and destination warehouse cannot be the same.");
        }

        if (quantity <= 0) {
            throw new RuntimeException("Transfer quantity must be greater than 0.");
        }

        // Step 1: Get source stock — check it has enough available
        StockLevel source = getStockLevel(fromWarehouseId, productId);
        if (source.getAvailableQuantity() < quantity) {
            throw new RuntimeException(
                    "Insufficient available stock in warehouse " + fromWarehouseId +
                            ". Available: " + source.getAvailableQuantity() +
                            ", Requested: " + quantity);
        }

        // Step 2: Debit from source
        int sourcePrevQty = source.getQuantity();
        source.setQuantity(source.getQuantity() - quantity);
        stockLevelRepository.save(source);

        // Step 3: Credit to destination (create stock level if it doesn't exist yet)
        StockLevel destination = stockLevelRepository
                .findByWarehouseIdAndProductId(toWarehouseId, productId)
                .orElseGet(() -> {
                    StockLevel newSl = new StockLevel();
                    newSl.setWarehouseId(toWarehouseId);
                    newSl.setProductId(productId);
                    newSl.setQuantity(0);
                    newSl.setReservedQuantity(0);
                    return newSl;
                });

        int destPrevQty = destination.getQuantity();
        destination.setQuantity(destination.getQuantity() + quantity);
        stockLevelRepository.save(destination);

        // Step 4: Publish TRANSFER_OUT event for source warehouse
        StockMovementEvent transferOut = new StockMovementEvent(
                productId, fromWarehouseId, "TRANSFER_OUT",
                -quantity, sourcePrevQty, source.getQuantity(),
                0, "TRANSFER-" + fromWarehouseId + "-" + toWarehouseId,
                LocalDateTime.now());
        rabbitTemplate.convertAndSend(EXCHANGE, "stock.movement.transfer", transferOut);

        // Step 5: Publish TRANSFER_IN event for destination warehouse
        StockMovementEvent transferIn = new StockMovementEvent(
                productId, toWarehouseId, "TRANSFER_IN",
                quantity, destPrevQty, destination.getQuantity(),
                0, "TRANSFER-" + fromWarehouseId + "-" + toWarehouseId,
                LocalDateTime.now());
        rabbitTemplate.convertAndSend(EXCHANGE, "stock.movement.transfer", transferIn);

        log.info("Stock transfer complete: product={}, from={}, to={}, qty={}",
                productId, fromWarehouseId, toWarehouseId, quantity);
    }

    /**
     * Get all stock levels below a safety threshold.
     * Used by alert-service and product-service.
     */
    @Override
    public List<StockLevel> getLowStockItems() {
        return stockLevelRepository.findAllLowStock();
    }

    @Override
    public List<StockLevel> getStockByWarehouse(int warehouseId) {
        return stockLevelRepository.findByWarehouseId(warehouseId);
    }

    // ── Private helpers ─────────────────────────────────────────────

    private void publishMovementEvent(StockLevel sl, String type,
                                      int delta, int prev, int next) {
        StockMovementEvent event = new StockMovementEvent(
                sl.getProductId(), sl.getWarehouseId(), type,
                delta, prev, next, 0, null, LocalDateTime.now());
        rabbitTemplate.convertAndSend(EXCHANGE, MOVEMENT_KEY, event);
    }

    private void publishAlertEvent(int productId, int warehouseId,
                                   int currentQty, String severity) {
        StockMovementEvent alertEvent = new StockMovementEvent(
                productId, warehouseId, "ALERT_" + severity,
                0, currentQty, currentQty, 0, null, LocalDateTime.now());
        rabbitTemplate.convertAndSend(EXCHANGE, ALERT_KEY, alertEvent);
    }
}