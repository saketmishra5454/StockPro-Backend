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
        if (movement.getMovementId() != 0) {
            // Existing movement rows are immutable audit records
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

    @Override
    public List<StockMovement> getByProduct(int productId) {
        return movementRepository.findByProductIdOrderByMovementDateDesc(productId);
    }

    @Override
    public List<StockMovement> getByWarehouse(int warehouseId) {
        return movementRepository.findByWarehouseIdOrderByMovementDateDesc(warehouseId);
    }

    @Override
    public List<StockMovement> getByType(String movementType) {
        return movementRepository.findByMovementTypeOrderByMovementDateDesc(movementType);
    }

    @Override
    public List<StockMovement> getByDateRange(LocalDateTime from, LocalDateTime to) {
        if (from.isAfter(to)) {
            throw new RuntimeException("Start date cannot be after end date.");
        }
        return movementRepository.findByMovementDateBetweenOrderByMovementDateDesc(from, to);
    }

    @Override
    public List<StockMovement> getByReference(int referenceId, String referenceType) {
        return movementRepository.findByReferenceIdAndReferenceType(referenceId, referenceType);
    }

    @Override
    public List<StockMovement> getMovementHistory(int productId, int warehouseId) {
        return movementRepository.findByProductIdAndWarehouseIdOrderByMovementDateDesc(
                productId, warehouseId);
    }

    @Override
    public int getStockIn(int productId) {
        return movementRepository.sumQuantityByProductIdAndType(productId, "STOCK_IN");
    }

    @Override
    public int getStockOut(int productId) {
        return movementRepository.sumQuantityByProductIdAndType(productId, "STOCK_OUT");
    }

    @Override
    public List<StockMovement> getAllMovements() {
        return movementRepository.findAll(
                org.springframework.data.domain.Sort.by(
                        org.springframework.data.domain.Sort.Direction.DESC,
                        "movementDate"));
    }
}
