package com.stockpro.report.service;

import com.stockpro.report.dto.ProductMovementSummary;
import com.stockpro.report.entity.InventorySnapshot;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

 // ReportService interface — all analytics and reporting operations.

public interface ReportService {

    // ── Snapshot ───────────────────────────────────────────────────

    // Take a snapshot of all stock in one warehouse right now
    void takeSnapshot(int warehouseId);

    // Take snapshots for ALL known warehouses — called by scheduler
    void takeSnapshotAllWarehouses();

    // Get today's snapshots for a warehouse
    List<InventorySnapshot> getLatestSnapshot(int warehouseId);

    // ── Valuation ──────────────────────────────────────────────────

    // Total stock value across ALL warehouses
    double getTotalStockValue();

    // Stock value for one specific warehouse
    double getStockValueByWarehouse(int warehouseId);

    // ── Turnover ───────────────────────────────────────────────────


     // Inventory Turnover Rate for a warehouse over a date range.

    double getInventoryTurnover(int warehouseId, LocalDate from, LocalDate to);

    // ── Product Movement Reports ───────────────────────────────────

    // Low stock report — products currently below reorder level
    List<Map<String, Object>> getLowStockReport();

    // Top N products ranked by total units moved (in + out)
    List<ProductMovementSummary> getTopMovingProducts(int limit);

    // Bottom N products — minimal movement in last 30 days
    List<ProductMovementSummary> getSlowMovingProducts(int limit);


     // Dead stock — products with no movement in last 90 days.

    List<InventorySnapshot> getDeadStock();

    // ── PO Summary ────────────────────────────────────────────────

     // Purchase Order spend summary for a date range.

    Map<String, Object> getPOSummary(LocalDate from, LocalDate to);
}