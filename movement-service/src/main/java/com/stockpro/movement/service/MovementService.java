package com.stockpro.movement.service;

import com.stockpro.movement.entity.StockMovement;

import java.time.LocalDateTime;
import java.util.List;

/**
 * MovementService interface — all operations on the audit trail.
 *
 * CRITICAL DESIGN RULE:
 *   There is NO updateMovement() or deleteMovement() method in this interface.
 *   This is intentional — audit trail records are write-once.
 *   Once recorded, a movement cannot be changed or removed.
 *   To correct a mistake: create a new opposing movement entry.
 */
public interface MovementService {

    /**
     * Record a new stock movement — INSERT ONLY.
     * Throws exception if movementId is already set (preventing updates).
     */
    StockMovement recordMovement(StockMovement movement);

    // Get all movements for a product across all warehouses
    List<StockMovement> getByProduct(int productId);

    // Get all movements in a specific warehouse
    List<StockMovement> getByWarehouse(int warehouseId);

    // Get movements of a specific type (STOCK_IN, STOCK_OUT, TRANSFER_IN, etc.)
    List<StockMovement> getByType(String movementType);

    // Get movements within a date range — for reports and exports
    List<StockMovement> getByDateRange(LocalDateTime from, LocalDateTime to);

    // Get movements linked to a reference document
    List<StockMovement> getByReference(int referenceId, String referenceType);

    // Get movement history for a specific product in a specific warehouse
    // Most detailed drill-down — shows complete history for one product-location
    List<StockMovement> getMovementHistory(int productId, int warehouseId);

    // Total units received (STOCK_IN) for a product across all warehouses
    int getStockIn(int productId);

    // Total units consumed/issued (STOCK_OUT) for a product across all warehouses
    int getStockOut(int productId);

    // Get all movements — for admin full audit view
    List<StockMovement> getAllMovements();
}