package com.stockpro.warehouse.repository;

import com.stockpro.warehouse.entity.StockLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface StockLevelRepository extends JpaRepository<StockLevel, Integer> {

    // Find stock for a specific product in a specific warehouse
    // This is the most common query — used in every stock update operation
    Optional<StockLevel> findByWarehouseIdAndProductId(int warehouseId, int productId);

    // Find all stock levels in a warehouse
    List<StockLevel> findByWarehouseId(int warehouseId);

    // Find all stock levels for a product across ALL warehouses
    List<StockLevel> findByProductId(int productId);


    @Query("SELECT s FROM StockLevel s WHERE s.warehouseId = :warehouseId AND s.quantity < :threshold")
    List<StockLevel> findLowStockByWarehouseId(
            @Param("warehouseId") int warehouseId,
            @Param("threshold") int threshold);

    @Query("SELECT s FROM StockLevel s WHERE s.quantity <= s.reservedQuantity OR s.quantity = 0")
    List<StockLevel> findAllLowStock();

    // Check if a stock level record exists for this warehouse+product combination
    boolean existsByWarehouseIdAndProductId(int warehouseId, int productId);

    // Sum total quantity of a product across all warehouses
    @Query("SELECT COALESCE(SUM(s.quantity), 0) FROM StockLevel s WHERE s.productId = :productId")
    int sumQuantityByProductId(@Param("productId") int productId);
}