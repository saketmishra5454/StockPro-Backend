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

    Optional<StockLevel> findByWarehouseIdAndProductId(int warehouseId, int productId);

    List<StockLevel> findByWarehouseId(int warehouseId);

    List<StockLevel> findByProductId(int productId);

    @Query("SELECT s FROM StockLevel s WHERE s.warehouseId = :warehouseId AND s.quantity < :threshold")
    List<StockLevel> findLowStockByWarehouseId(
            @Param("warehouseId") int warehouseId,
            @Param("threshold") int threshold);

    @Query("SELECT s FROM StockLevel s WHERE s.quantity <= s.reservedQuantity OR s.quantity = 0")
    List<StockLevel> findAllLowStock();

    boolean existsByWarehouseIdAndProductId(int warehouseId, int productId);

    @Query("SELECT COALESCE(SUM(s.quantity), 0) FROM StockLevel s WHERE s.productId = :productId")
    int sumQuantityByProductId(@Param("productId") int productId);
}
