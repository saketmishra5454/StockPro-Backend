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
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
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

    private static final String EXCHANGE        = "stockpro.exchange";
    private static final String MOVEMENT_KEY    = "stock.movement.warehouse";
    private static final String ALERT_KEY       = "stock.alert.lowstock";

    @Override
    @Caching(
            put = @CachePut(cacheNames = "warehouseById", key = "#result.warehouseId"),
            evict = @CacheEvict(cacheNames = "warehousesAll", allEntries = true)
    )
    public Warehouse createWarehouse(Warehouse warehouse) {
        validateWarehouse(warehouse);
        if (warehouseRepository.existsByName(warehouse.getName())) {
            throw new RuntimeException("Warehouse with name '" + warehouse.getName() + "' already exists.");
        }
        warehouse.setUsedCapacity(0);
        return warehouseRepository.save(warehouse);
    }

    @Override
    public Warehouse getWarehouseById(int warehouseId) {
        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new RuntimeException("Warehouse not found: " + warehouseId));
        return syncWarehouseCapacity(warehouse);
    }

    @Override
    public List<Warehouse> getAllWarehouses() {
        return warehouseRepository.findAll().stream()
                .map(this::syncWarehouseCapacity)
                .toList();
    }

    @Override
    @Caching(
            put = @CachePut(cacheNames = "warehouseById", key = "#warehouseId"),
            evict = @CacheEvict(cacheNames = "warehousesAll", allEntries = true)
    )
    public Warehouse updateWarehouse(int warehouseId, Warehouse updated) {
        Warehouse existing = getWarehouseById(warehouseId);

        if (updated.getName() != null && !updated.getName().isBlank()) existing.setName(updated.getName().trim());
        if (updated.getLocation() != null) existing.setLocation(updated.getLocation());
        if (updated.getAddress() != null)  existing.setAddress(updated.getAddress());
        if (updated.getPhone() != null)    existing.setPhone(updated.getPhone());
        if (updated.getCapacity() > 0)     existing.setCapacity(updated.getCapacity());
        if (updated.getManagerId() > 0)    existing.setManagerId(updated.getManagerId());

        return warehouseRepository.save(existing);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(cacheNames = "warehouseById", key = "#warehouseId"),
            @CacheEvict(cacheNames = "warehousesAll", allEntries = true)
    })
    public void deactivateWarehouse(int warehouseId) {
        Warehouse warehouse = getWarehouseById(warehouseId);
        warehouse.setActive(false);
        warehouseRepository.save(warehouse);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(cacheNames = "warehouseById", key = "#warehouseId"),
            @CacheEvict(cacheNames = "warehousesAll", allEntries = true)
    })
    public void activateWarehouse(int warehouseId) {
        Warehouse warehouse = getWarehouseById(warehouseId);
        warehouse.setActive(true);
        warehouseRepository.save(warehouse);
    }

    @Override
    @Cacheable(cacheNames = "stockLevel", key = "#warehouseId + ':' + #productId")
    public StockLevel getStockLevel(int warehouseId, int productId) {
        return stockLevelRepository.findByWarehouseIdAndProductId(warehouseId, productId)
                .orElseThrow(() -> new RuntimeException(
                        "No stock record found for product " + productId +
                                " in warehouse " + warehouseId));
    }


    @Override
    @Transactional
    @Caching(
            put = @CachePut(cacheNames = "stockLevel", key = "#warehouseId + ':' + #productId"),
            evict = {
                    @CacheEvict(cacheNames = "stockLow", allEntries = true),
                    @CacheEvict(cacheNames = "stockByWarehouse", allEntries = true),
                    @CacheEvict(cacheNames = "stockAll", allEntries = true)
            }
    )
    public StockLevel initializeStock(int warehouseId, int productId, int initialQuantity) {
        validateStockInput(warehouseId, productId, initialQuantity);
        Warehouse warehouse = getWarehouseById(warehouseId);
        ensureCapacityAvailable(warehouse, initialQuantity);

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

        StockLevel saved = stockLevelRepository.save(sl);
        syncWarehouseCapacity(warehouseId);
        return saved;
    }


    @Override
    @Transactional
    @Caching(
            put = @CachePut(cacheNames = "stockLevel", key = "#warehouseId + ':' + #productId"),
            evict = {
                    @CacheEvict(cacheNames = "stockLow", allEntries = true),
                    @CacheEvict(cacheNames = "stockByWarehouse", allEntries = true),
                    @CacheEvict(cacheNames = "stockAll", allEntries = true)
            }
    )
    public StockLevel updateStock(int warehouseId, int productId, int delta) {
        if (warehouseId <= 0 || productId <= 0) {
            throw new IllegalArgumentException("Warehouse and product are required.");
        }
        if (delta == 0) {
            throw new IllegalArgumentException("Stock delta cannot be zero.");
        }
        Warehouse warehouse = getWarehouseById(warehouseId);

        StockLevel sl = stockLevelRepository
                .findByWarehouseIdAndProductId(warehouseId, productId)
                .orElseGet(() -> {
                    // First stock-in creates the warehouse/product stock record
                    StockLevel newSl = new StockLevel();
                    newSl.setWarehouseId(warehouseId);
                    newSl.setProductId(productId);
                    newSl.setQuantity(0);
                    newSl.setReservedQuantity(0);
                    return newSl;
                });

        int previousQuantity = sl.getQuantity();
        int newQuantity = previousQuantity + delta;

        if (delta > 0) {
            ensureCapacityAvailable(warehouse, delta);
        }

        if (newQuantity < 0) {
            throw new RuntimeException(
                    "Insufficient stock. Available: " + sl.getAvailableQuantity() +
                            ", Requested: " + Math.abs(delta));
        }
        // Reserved quantity is protected from stock-out operations
        if (delta < 0 && Math.abs(delta) > sl.getAvailableQuantity()) {
            throw new RuntimeException(
                    "Insufficient available stock. Available: " + sl.getAvailableQuantity() +
                            ", Requested: " + Math.abs(delta));
        }

        sl.setQuantity(newQuantity);
        StockLevel saved = stockLevelRepository.save(sl);
        syncWarehouseCapacity(warehouseId);

        String movementType = delta > 0 ? "STOCK_IN" : "STOCK_OUT";

        // Movement events maintain the cross-service audit trail
        publishMovementEvent(saved, movementType, delta, previousQuantity, newQuantity);

        if (newQuantity == 0) {
            publishAlertEvent(productId, warehouseId, newQuantity, "CRITICAL");
        }

        log.info("Stock updated: product={}, warehouse={}, delta={}, newQty={}",
                productId, warehouseId, delta, newQuantity);

        return saved;
    }


    @Override
    @Transactional
    @Caching(
            put = @CachePut(cacheNames = "stockLevel", key = "#warehouseId + ':' + #productId"),
            evict = {
                    @CacheEvict(cacheNames = "stockLow", allEntries = true),
                    @CacheEvict(cacheNames = "stockByWarehouse", allEntries = true),
                    @CacheEvict(cacheNames = "stockAll", allEntries = true)
            }
    )
    public StockLevel reserveStock(int warehouseId, int productId, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Reservation quantity must be greater than 0.");
        }
        StockLevel sl = getStockLevel(warehouseId, productId);

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


    @Override
    @Transactional
    @Caching(
            put = @CachePut(cacheNames = "stockLevel", key = "#warehouseId + ':' + #productId"),
            evict = {
                    @CacheEvict(cacheNames = "stockLow", allEntries = true),
                    @CacheEvict(cacheNames = "stockByWarehouse", allEntries = true),
                    @CacheEvict(cacheNames = "stockAll", allEntries = true)
            }
    )
    public StockLevel releaseReservation(int warehouseId, int productId, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Release quantity must be greater than 0.");
        }
        StockLevel sl = getStockLevel(warehouseId, productId);

        int releaseAmount = Math.min(quantity, sl.getReservedQuantity());
        sl.setReservedQuantity(sl.getReservedQuantity() - releaseAmount);

        StockLevel saved = stockLevelRepository.save(sl);

        log.info("Reservation released: product={}, warehouse={}, releasedQty={}",
                productId, warehouseId, releaseAmount);

        return saved;
    }


    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "stockLevel", allEntries = true),
            @CacheEvict(cacheNames = "stockLow", allEntries = true),
            @CacheEvict(cacheNames = "stockByWarehouse", allEntries = true),
            @CacheEvict(cacheNames = "stockAll", allEntries = true)
    })
    public void transferStock(int fromWarehouseId, int toWarehouseId, int productId, int quantity) {
        getWarehouseById(fromWarehouseId);
        Warehouse destinationWarehouse = getWarehouseById(toWarehouseId);

        if (fromWarehouseId == toWarehouseId) {
            throw new RuntimeException("Source and destination warehouse cannot be the same.");
        }

        if (quantity <= 0) {
            throw new RuntimeException("Transfer quantity must be greater than 0.");
        }
        ensureCapacityAvailable(destinationWarehouse, quantity);

        StockLevel source = getStockLevel(fromWarehouseId, productId);
        if (source.getAvailableQuantity() < quantity) {
            throw new RuntimeException(
                    "Insufficient available stock in warehouse " + fromWarehouseId +
                            ". Available: " + source.getAvailableQuantity() +
                            ", Requested: " + quantity);
        }

        int sourcePrevQty = source.getQuantity();
        source.setQuantity(source.getQuantity() - quantity);
        stockLevelRepository.save(source);

        StockLevel destination = stockLevelRepository
                .findByWarehouseIdAndProductId(toWarehouseId, productId)
                .orElseGet(() -> {
                    // Destination stock level is created on first transfer-in
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
        syncWarehouseCapacity(fromWarehouseId);
        syncWarehouseCapacity(toWarehouseId);

        StockMovementEvent transferOut = new StockMovementEvent(
                productId, fromWarehouseId, "TRANSFER_OUT",
                -quantity, sourcePrevQty, source.getQuantity(),
                0, "TRANSFER-" + fromWarehouseId + "-" + toWarehouseId,
                LocalDateTime.now());
        publishSafely("stock.movement.transfer", transferOut);

        StockMovementEvent transferIn = new StockMovementEvent(
                productId, toWarehouseId, "TRANSFER_IN",
                quantity, destPrevQty, destination.getQuantity(),
                0, "TRANSFER-" + fromWarehouseId + "-" + toWarehouseId,
                LocalDateTime.now());
        publishSafely("stock.movement.transfer", transferIn);

        log.info("Stock transfer complete: product={}, from={}, to={}, qty={}",
                productId, fromWarehouseId, toWarehouseId, quantity);
    }


    @Override
    @Cacheable(cacheNames = "stockLow", key = "'low'")
    public List<StockLevel> getLowStockItems() {
        return stockLevelRepository.findAllLowStock();
    }

    @Override
    @Cacheable(cacheNames = "stockByWarehouse", key = "#warehouseId")
    public List<StockLevel> getStockByWarehouse(int warehouseId) {
        return stockLevelRepository.findByWarehouseId(warehouseId);
    }

    @Override
    @Cacheable(cacheNames = "stockAll", key = "'all'")
    public List<StockLevel> getAllStockLevels() {
        return stockLevelRepository.findAll();
    }

    private void publishMovementEvent(StockLevel sl, String type,
                                      int delta, int prev, int next) {
        StockMovementEvent event = new StockMovementEvent(
                sl.getProductId(), sl.getWarehouseId(), type,
                delta, prev, next, 0, null, LocalDateTime.now());
        publishSafely(MOVEMENT_KEY, event);
    }

    private void publishAlertEvent(int productId, int warehouseId,
                                   int currentQty, String severity) {
        StockMovementEvent alertEvent = new StockMovementEvent(
                productId, warehouseId, "ALERT_" + severity,
                0, currentQty, currentQty, 0, null, LocalDateTime.now());
        publishSafely(ALERT_KEY, alertEvent);
    }

    // Publishing failures are logged without rolling back stock persistence
    private void publishSafely(String routingKey, StockMovementEvent event) {
        try {
            rabbitTemplate.convertAndSend(EXCHANGE, routingKey, event);
        } catch (Exception e) {
            log.warn("Could not publish stock event {} for product {} in warehouse {}: {}",
                    event.getMovementType(), event.getProductId(), event.getWarehouseId(), e.getMessage());
        }
    }

    private void validateWarehouse(Warehouse warehouse) {
        if (warehouse == null) {
            throw new IllegalArgumentException("Warehouse details are required.");
        }
        if (warehouse.getName() == null || warehouse.getName().isBlank()) {
            throw new IllegalArgumentException("Warehouse name is required.");
        }
        warehouse.setName(warehouse.getName().trim());
        if (warehouse.getManagerId() <= 0) {
            throw new IllegalArgumentException("Warehouse manager is required.");
        }
        if (warehouse.getCapacity() < 0) {
            throw new IllegalArgumentException("Warehouse capacity cannot be negative.");
        }
    }

    private void validateStockInput(int warehouseId, int productId, int quantity) {
        if (warehouseId <= 0 || productId <= 0) {
            throw new IllegalArgumentException("Warehouse and product are required.");
        }
        if (quantity < 0) {
            throw new IllegalArgumentException("Initial quantity cannot be negative.");
        }
    }

    private Warehouse syncWarehouseCapacity(int warehouseId) {
        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new RuntimeException("Warehouse not found: " + warehouseId));
        return syncWarehouseCapacity(warehouse);
    }

    private Warehouse syncWarehouseCapacity(Warehouse warehouse) {
        int usedCapacity = stockLevelRepository.sumQuantityByWarehouseId(warehouse.getWarehouseId());
        if (warehouse.getUsedCapacity() != usedCapacity) {
            warehouse.setUsedCapacity(usedCapacity);
            return warehouseRepository.save(warehouse);
        }
        return warehouse;
    }

    private void ensureCapacityAvailable(Warehouse warehouse, int incomingQuantity) {
        if (incomingQuantity <= 0 || warehouse.getCapacity() <= 0) {
            return;
        }

        int currentUsedCapacity = stockLevelRepository.sumQuantityByWarehouseId(warehouse.getWarehouseId());
        int projectedCapacity = currentUsedCapacity + incomingQuantity;
        if (projectedCapacity > warehouse.getCapacity()) {
            throw new RuntimeException(
                    "Warehouse capacity exceeded. Used: " + currentUsedCapacity +
                            ", Incoming: " + incomingQuantity +
                            ", Capacity: " + warehouse.getCapacity());
        }
    }
}
