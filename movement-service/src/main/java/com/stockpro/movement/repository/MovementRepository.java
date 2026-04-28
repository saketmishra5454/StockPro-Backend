package com.stockpro.movement.repository;

import com.stockpro.movement.entity.StockMovement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;


@Repository
public interface MovementRepository extends JpaRepository<StockMovement, Integer> {

    // All movements for a product — full history across all warehouses
    List<StockMovement> findByProductIdOrderByMovementDateDesc(int productId);

    // All movements in a warehouse — what happened in this location
    List<StockMovement> findByWarehouseIdOrderByMovementDateDesc(int warehouseId);

    // All movements of a specific type — e.g. all WRITE_OFFs, all TRANSFERs
    List<StockMovement> findByMovementTypeOrderByMovementDateDesc(String movementType);

    // All movements within a time window — for date range reports
    List<StockMovement> findByMovementDateBetweenOrderByMovementDateDesc(
            LocalDateTime from, LocalDateTime to);

    // All movements performed by a specific user
    List<StockMovement> findByPerformedByOrderByMovementDateDesc(int performedBy);

    // Movement history for ONE product in ONE warehouse — most useful for drill-down
    List<StockMovement> findByProductIdAndWarehouseIdOrderByMovementDateDesc(
            int productId, int warehouseId);

    // All movements linked to a reference document (e.g. PO ID)
    List<StockMovement> findByReferenceIdAndReferenceType(int referenceId, String referenceType);


    @Query("SELECT COALESCE(SUM(m.quantity), 0) FROM StockMovement m " +
            "WHERE m.productId = :productId AND m.movementType = :movementType")
    int sumQuantityByProductIdAndType(
            @Param("productId") int productId,
            @Param("movementType") String movementType);


    @Query("SELECT COALESCE(SUM(m.quantity), 0) FROM StockMovement m " +
            "WHERE m.productId = :productId AND m.movementType IN :types")
    int sumQuantityByProductIdAndTypeIn(
            @Param("productId") int productId,
            @Param("types") List<String> types);

    // Count movements by type — for dashboard stats
    long countByMovementType(String movementType);

    // Latest movement for a product+warehouse combo — gives current balanceAfter
    @Query("SELECT m FROM StockMovement m WHERE m.productId = :productId " +
            "AND m.warehouseId = :warehouseId ORDER BY m.movementDate DESC LIMIT 1")
    java.util.Optional<StockMovement> findLatestByProductAndWarehouse(
            @Param("productId") int productId,
            @Param("warehouseId") int warehouseId);
}