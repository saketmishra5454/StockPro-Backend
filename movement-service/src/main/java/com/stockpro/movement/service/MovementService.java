package com.stockpro.movement.service;

import com.stockpro.movement.entity.StockMovement;

import java.time.LocalDateTime;
import java.util.List;

public interface MovementService {


    StockMovement recordMovement(StockMovement movement);

    List<StockMovement> getByProduct(int productId);

    List<StockMovement> getByWarehouse(int warehouseId);

    List<StockMovement> getByType(String movementType);

    List<StockMovement> getByDateRange(LocalDateTime from, LocalDateTime to);

    List<StockMovement> getByReference(int referenceId, String referenceType);

    List<StockMovement> getMovementHistory(int productId, int warehouseId);

    int getStockIn(int productId);

    int getStockOut(int productId);

    List<StockMovement> getAllMovements();
}
