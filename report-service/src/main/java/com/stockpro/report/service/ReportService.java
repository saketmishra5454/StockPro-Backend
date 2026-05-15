package com.stockpro.report.service;

import com.stockpro.report.dto.ProductMovementSummary;
import com.stockpro.report.entity.InventorySnapshot;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface ReportService {

    void takeSnapshot(int warehouseId);

    void takeSnapshotAllWarehouses();

    List<InventorySnapshot> getLatestSnapshot(int warehouseId);

    double getTotalStockValue();

    double getStockValueByWarehouse(int warehouseId);

    double getInventoryTurnover(int warehouseId, LocalDate from, LocalDate to);

    List<Map<String, Object>> getLowStockReport();

    List<ProductMovementSummary> getTopMovingProducts(int limit);

    List<ProductMovementSummary> getSlowMovingProducts(int limit);

    List<InventorySnapshot> getDeadStock();

    Map<String, Object> getPOSummary(LocalDate from, LocalDate to);
}
