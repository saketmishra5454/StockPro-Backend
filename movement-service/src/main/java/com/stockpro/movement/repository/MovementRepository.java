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

    List<StockMovement> findByProductIdOrderByMovementDateDesc(int productId);

    List<StockMovement> findByWarehouseIdOrderByMovementDateDesc(int warehouseId);

    List<StockMovement> findByMovementTypeOrderByMovementDateDesc(String movementType);

    List<StockMovement> findByMovementDateBetweenOrderByMovementDateDesc(
            LocalDateTime from, LocalDateTime to);

    List<StockMovement> findByPerformedByOrderByMovementDateDesc(int performedBy);

    List<StockMovement> findByProductIdAndWarehouseIdOrderByMovementDateDesc(
            int productId, int warehouseId);

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

    long countByMovementType(String movementType);

    @Query("SELECT m FROM StockMovement m WHERE m.productId = :productId " +
            "AND m.warehouseId = :warehouseId ORDER BY m.movementDate DESC LIMIT 1")
    java.util.Optional<StockMovement> findLatestByProductAndWarehouse(
            @Param("productId") int productId,
            @Param("warehouseId") int warehouseId);
}
