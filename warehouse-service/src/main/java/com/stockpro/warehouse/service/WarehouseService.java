package com.stockpro.warehouse.service;

import com.stockpro.warehouse.entity.StockLevel;
import com.stockpro.warehouse.entity.Warehouse;

import java.util.List;

/**
 * WarehouseService interface — contract for all warehouse and stock operations.
 */
public interface WarehouseService {

    // ── Warehouse CRUD ─────────────────────────────────────────────

    Warehouse createWarehouse(Warehouse warehouse);

    Warehouse getWarehouseById(int warehouseId);

    List<Warehouse> getAllWarehouses();

    Warehouse updateWarehouse(int warehouseId, Warehouse updated);

    // Soft delete — sets isActive = false
    void deactivateWarehouse(int warehouseId);

    // ── Stock Level Operations ──────────────────────────────────────

    // Get current stock for one product in one warehouse
    StockLevel getStockLevel(int warehouseId, int productId);

    /**
     * Update stock by a delta (positive = stock in, negative = stock out).
     * e.g. updateStock(1, 5, +100) adds 100 units of product 5 to warehouse 1
     *      updateStock(1, 5, -30)  removes 30 units
     */
    StockLevel updateStock(int warehouseId, int productId, int delta);

    /**
     * Reserve stock for an open PO or sales order.
     * Increases reservedQuantity — these units cannot be issued elsewhere.
     */
    StockLevel reserveStock(int warehouseId, int productId, int quantity);

    /**
     * Release a reservation — decreases reservedQuantity.
     * Called when a PO is cancelled or reservation expires.
     */
    StockLevel releaseReservation(int warehouseId, int productId, int quantity);

    /**
     * Transfer stock between warehouses.
     * Must be @Transactional — debit source and credit destination atomically.
     * Publishes StockMovementEvent to RabbitMQ after successful transfer.
     */
    void transferStock(int fromWarehouseId, int toWarehouseId, int productId, int quantity);

    // Get all stock levels below their reorder threshold
    List<StockLevel> getLowStockItems();

    // Get all stock levels in a specific warehouse
    List<StockLevel> getStockByWarehouse(int warehouseId);

    // Initialize stock for a product in a warehouse (first time setup)
    StockLevel initializeStock(int warehouseId, int productId, int initialQuantity);
}