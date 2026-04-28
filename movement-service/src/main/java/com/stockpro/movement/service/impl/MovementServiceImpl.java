package com.stockpro.movement.service.impl;

import com.stockpro.movement.entity.StockMovement;
import com.stockpro.movement.repository.MovementRepository;
import com.stockpro.movement.service.MovementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;


@Service
@RequiredArgsConstructor
@Slf4j
public class MovementServiceImpl implements MovementService {

    private final MovementRepository movementRepository;


    @Override
    public StockMovement recordMovement(StockMovement movement) {
        // Write-once guard — reject any attempt to update an existing record
        if (movement.getMovementId() != 0) {
            throw new UnsupportedOperationException(
                    "Stock movements are immutable. Cannot update movement ID: " +
                            movement.getMovementId() +
                            ". To correct a mistake, create a new opposing movement entry.");
        }

        StockMovement saved = movementRepository.save(movement);
        log.info("Movement recorded: id={}, type={}, product={}, warehouse={}, qty={}",
                saved.getMovementId(), saved.getMovementType(),
                saved.getProductId(), saved.getWarehouseId(), saved.getQuantity());

        return saved;
    }


//      Get all movements for a product — complete history across all warehouses.
//      Ordered by movementDate DESC so most recent appears first.

    @Override
    public List<StockMovement> getByProduct(int productId) {
        return movementRepository.findByProductIdOrderByMovementDateDesc(productId);
    }


    //  Get all movements in a warehouse — everything that happened here.

    @Override
    public List<StockMovement> getByWarehouse(int warehouseId) {
        return movementRepository.findByWarehouseIdOrderByMovementDateDesc(warehouseId);
    }

//      Get movements filtered by type.
//      Used to see all WRITE_OFFs, all TRANSFERs, etc.

    @Override
    public List<StockMovement> getByType(String movementType) {
        return movementRepository.findByMovementTypeOrderByMovementDateDesc(movementType);
    }


//      Get movements within a date range.
//      Used for daily/monthly reports and CSV exports.

    @Override
    public List<StockMovement> getByDateRange(LocalDateTime from, LocalDateTime to) {
        if (from.isAfter(to)) {
            throw new RuntimeException("Start date cannot be after end date.");
        }
        return movementRepository.findByMovementDateBetweenOrderByMovementDateDesc(from, to);
    }


//      Get movements linked to a reference document.
//      e.g. getByReference(15, "PURCHASE_ORDER") → all movements from PO #15

    @Override
    public List<StockMovement> getByReference(int referenceId, String referenceType) {
        return movementRepository.findByReferenceIdAndReferenceType(referenceId, referenceType);
    }


     // Get complete movement history for one product in one warehouse.

    @Override
    public List<StockMovement> getMovementHistory(int productId, int warehouseId) {
        return movementRepository.findByProductIdAndWarehouseIdOrderByMovementDateDesc(
                productId, warehouseId);
    }


     // Total units received (STOCK_IN) for a product.

    @Override
    public int getStockIn(int productId) {
        return movementRepository.sumQuantityByProductIdAndType(productId, "STOCK_IN");
    }


     // Total units consumed (STOCK_OUT) for a product.

    @Override
    public int getStockOut(int productId) {
        return movementRepository.sumQuantityByProductIdAndType(productId, "STOCK_OUT");
    }


//      Get all movements — complete audit log.
//     Used by Admin to see full platform activity.

    @Override
    public List<StockMovement> getAllMovements() {
        return movementRepository.findAll(
                org.springframework.data.domain.Sort.by(
                        org.springframework.data.domain.Sort.Direction.DESC,
                        "movementDate"));
    }
}