package com.stockpro.report.repository;

import com.stockpro.report.entity.InventorySnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;


  //ReportRepository — all queries for InventorySnapshot records.

@Repository
public interface ReportRepository extends JpaRepository<InventorySnapshot, Integer> {

    // All snapshots for a specific date — today's inventory state
    List<InventorySnapshot> findBySnapshotDate(LocalDate date);

    // All snapshots for a specific warehouse across all dates
    List<InventorySnapshot> findByWarehouseId(int warehouseId);

    // All snapshots for a specific product across all warehouses and dates
    List<InventorySnapshot> findByProductId(int productId);

    // Snapshots within a date range — for trend analysis
    List<InventorySnapshot> findBySnapshotDateBetween(LocalDate startDate, LocalDate endDate);

    // Latest snapshot for a specific warehouse+product combo
    Optional<InventorySnapshot> findTopByWarehouseIdAndProductIdOrderBySnapshotDateDesc(
            int warehouseId, int productId);

    // Check if snapshot already taken today for this warehouse+product
    boolean existsByWarehouseIdAndProductIdAndSnapshotDate(
            int warehouseId, int productId, LocalDate snapshotDate);


    //  Sum of stockValue for all products in a specific warehouse on latest date.
    //  This is the total inventory value of that warehouse right now.

    @Query("SELECT COALESCE(SUM(s.stockValue), 0.0) FROM InventorySnapshot s " +
            "WHERE s.warehouseId = :warehouseId AND s.snapshotDate = " +
            "(SELECT MAX(s2.snapshotDate) FROM InventorySnapshot s2 WHERE s2.warehouseId = :warehouseId)")
    Double sumStockValueByWarehouse(@Param("warehouseId") int warehouseId);


     //Total stock value across ALL warehouses from the most recent snapshot date.

    @Query("SELECT COALESCE(SUM(s.stockValue), 0.0) FROM InventorySnapshot s " +
            "WHERE s.snapshotDate = (SELECT MAX(s2.snapshotDate) FROM InventorySnapshot s2)")
    Double sumTotalStockValue();


     // Average stockValue for a product across all snapshot dates.

    @Query("SELECT COALESCE(AVG(s.stockValue), 0.0) FROM InventorySnapshot s " +
            "WHERE s.productId = :productId")
    Double avgTurnoverByProduct(@Param("productId") int productId);


     // Average inventory value for a warehouse within a date range.

    @Query("SELECT COALESCE(AVG(s.stockValue), 0.0) FROM InventorySnapshot s " +
            "WHERE s.warehouseId = :warehouseId " +
            "AND s.snapshotDate BETWEEN :startDate AND :endDate")
    Double avgStockValueByWarehouseAndDateRange(
            @Param("warehouseId") int warehouseId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);


     // Find products with zero quantity on latest snapshot date.

    @Query("SELECT s FROM InventorySnapshot s WHERE s.quantity = 0 " +
            "AND s.snapshotDate = (SELECT MAX(s2.snapshotDate) FROM InventorySnapshot s2)")
    List<InventorySnapshot> findZeroStockOnLatestDate();


    //  Find products where quantity has not changed across the last N snapshots.

    @Query("SELECT s FROM InventorySnapshot s " +
            "WHERE s.snapshotDate = (SELECT MAX(s2.snapshotDate) FROM InventorySnapshot s2) " +
            "AND EXISTS (" +
            "   SELECT 1 FROM InventorySnapshot s3 " +
            "   WHERE s3.productId = s.productId AND s3.warehouseId = s.warehouseId " +
            "   AND s3.snapshotDate <= :cutoffDate AND s3.quantity = s.quantity)")
    List<InventorySnapshot> findSlowMovingStock(@Param("cutoffDate") LocalDate cutoffDate);


     // Get distinct warehouse IDs that have at least one snapshot.

    @Query("SELECT DISTINCT s.warehouseId FROM InventorySnapshot s")
    List<Integer> findDistinctWarehouseIds();
}