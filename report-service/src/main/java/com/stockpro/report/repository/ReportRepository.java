package com.stockpro.report.repository;

import com.stockpro.report.entity.InventorySnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReportRepository extends JpaRepository<InventorySnapshot, Integer> {

    List<InventorySnapshot> findBySnapshotDate(LocalDate date);

    List<InventorySnapshot> findByWarehouseId(int warehouseId);

    List<InventorySnapshot> findByProductId(int productId);

    List<InventorySnapshot> findBySnapshotDateBetween(LocalDate startDate, LocalDate endDate);

    Optional<InventorySnapshot> findTopByWarehouseIdAndProductIdOrderBySnapshotDateDesc(
            int warehouseId, int productId);

    boolean existsByWarehouseIdAndProductIdAndSnapshotDate(
            int warehouseId, int productId, LocalDate snapshotDate);

    @Query("SELECT COALESCE(SUM(s.stockValue), 0.0) FROM InventorySnapshot s " +
            "WHERE s.warehouseId = :warehouseId AND s.snapshotDate = " +
            "(SELECT MAX(s2.snapshotDate) FROM InventorySnapshot s2 WHERE s2.warehouseId = :warehouseId)")
    Double sumStockValueByWarehouse(@Param("warehouseId") int warehouseId);

    @Query("SELECT COALESCE(SUM(s.stockValue), 0.0) FROM InventorySnapshot s " +
            "WHERE s.snapshotDate = (SELECT MAX(s2.snapshotDate) FROM InventorySnapshot s2)")
    Double sumTotalStockValue();

    @Query("SELECT COALESCE(AVG(s.stockValue), 0.0) FROM InventorySnapshot s " +
            "WHERE s.productId = :productId")
    Double avgTurnoverByProduct(@Param("productId") int productId);

    @Query("SELECT COALESCE(AVG(s.stockValue), 0.0) FROM InventorySnapshot s " +
            "WHERE s.warehouseId = :warehouseId " +
            "AND s.snapshotDate BETWEEN :startDate AND :endDate")
    Double avgStockValueByWarehouseAndDateRange(
            @Param("warehouseId") int warehouseId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT s FROM InventorySnapshot s WHERE s.quantity = 0 " +
            "AND s.snapshotDate = (SELECT MAX(s2.snapshotDate) FROM InventorySnapshot s2)")
    List<InventorySnapshot> findZeroStockOnLatestDate();

    @Query("SELECT s FROM InventorySnapshot s " +
            "WHERE s.snapshotDate = (SELECT MAX(s2.snapshotDate) FROM InventorySnapshot s2) " +
            "AND EXISTS (" +
            "   SELECT 1 FROM InventorySnapshot s3 " +
            "   WHERE s3.productId = s.productId AND s3.warehouseId = s.warehouseId " +
            "   AND s3.snapshotDate <= :cutoffDate AND s3.quantity = s.quantity)")
    List<InventorySnapshot> findSlowMovingStock(@Param("cutoffDate") LocalDate cutoffDate);

    @Query("SELECT DISTINCT s.warehouseId FROM InventorySnapshot s")
    List<Integer> findDistinctWarehouseIds();
}
