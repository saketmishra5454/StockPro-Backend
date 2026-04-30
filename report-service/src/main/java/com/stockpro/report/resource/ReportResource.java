package com.stockpro.report.resource;

import com.stockpro.report.dto.ProductMovementSummary;
import com.stockpro.report.entity.InventorySnapshot;
import com.stockpro.report.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;


// * ReportResource — REST controller for all analytics endpoints.
// * Base path: /api/reports

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportResource {

    private final ReportService reportService;

    // ══════════════════════════════════════════════════════════════
    // SNAPSHOT
    // ══════════════════════════════════════════════════════════════


//     * POST /api/reports/snapshot/{warehouseId}
//     * Used when first setting up a warehouse (before midnight scheduler runs).
//     * e.g. POST /api/reports/snapshot/1

    @PostMapping("/snapshot/{warehouseId}")
    public ResponseEntity<Map<String, String>> takeSnapshot(@PathVariable int warehouseId) {
        reportService.takeSnapshot(warehouseId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message",
                        "Snapshot taken for warehouse " + warehouseId));
    }

//      POST /api/reports/snapshot/all
//      Manually trigger snapshots for all warehouses.

    @PostMapping("/snapshot/all")
    public ResponseEntity<Map<String, String>> takeSnapshotAll() {
        reportService.takeSnapshotAllWarehouses();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "Snapshot taken for all warehouses"));
    }


//      GET /api/reports/snapshot/{warehouseId}
//      Get today's snapshot for a warehouse.

    @GetMapping("/snapshot/{warehouseId}")
    public ResponseEntity<List<InventorySnapshot>> getLatestSnapshot(
            @PathVariable int warehouseId) {
        return ResponseEntity.ok(reportService.getLatestSnapshot(warehouseId));
    }

    // ══════════════════════════════════════════════════════════════
    // VALUATION
    // ══════════════════════════════════════════════════════════════


//     * GET /api/reports/stock-value/total
//     * Get total inventory value across ALL warehouses.

    @GetMapping("/stock-value/total")
    public ResponseEntity<Map<String, Object>> getTotalStockValue() {
        double total = reportService.getTotalStockValue();
        return ResponseEntity.ok(Map.of(
                "totalStockValue", total,
                "currency", "INR",
                "asOf", LocalDate.now().toString()));
    }


//      GET /api/reports/stock-value/warehouse/{warehouseId}
//      Get stock value for one warehouse.

    @GetMapping("/stock-value/warehouse/{warehouseId}")
    public ResponseEntity<Map<String, Object>> getStockValueByWarehouse(
            @PathVariable int warehouseId) {
        double value = reportService.getStockValueByWarehouse(warehouseId);
        return ResponseEntity.ok(Map.of(
                "warehouseId", warehouseId,
                "stockValue", value,
                "asOf", LocalDate.now().toString()));
    }

    // ══════════════════════════════════════════════════════════════
    // INVENTORY TURNOVER
    // ══════════════════════════════════════════════════════════════


//      GET /api/reports/turnover/{warehouseId}?from=2026-01-01&to=2026-04-30
//      Calculate inventory turnover rate for a warehouse and date range.

    @GetMapping("/turnover/{warehouseId}")
    public ResponseEntity<Map<String, Object>> getInventoryTurnover(
            @PathVariable int warehouseId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        double turnover = reportService.getInventoryTurnover(warehouseId, from, to);
        return ResponseEntity.ok(Map.of(
                "warehouseId", warehouseId,
                "turnoverRate", turnover,
                "from", from.toString(),
                "to", to.toString(),
                "interpretation",
                turnover > 0
                        ? "Inventory turned over " + turnover + " times in this period"
                        : "Insufficient data for turnover calculation"));
    }

    // ══════════════════════════════════════════════════════════════
    // PRODUCT MOVEMENT REPORTS
    // ══════════════════════════════════════════════════════════════


//     * GET /api/reports/low-stock
//     * Low stock report — products currently below reorder level.

    @GetMapping("/low-stock")
    public ResponseEntity<List<Map<String, Object>>> getLowStockReport() {
        return ResponseEntity.ok(reportService.getLowStockReport());
    }

    //      GET /api/reports/top-moving?limit=10
//     Top moving products ranked by total units moved in last 30 days.

    @GetMapping("/top-moving")
    public ResponseEntity<List<ProductMovementSummary>> getTopMovingProducts(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(reportService.getTopMovingProducts(limit));
    }


//     * GET /api/reports/slow-moving?limit=10
//     * Slow moving products — minimal movement in last 30 days.

    @GetMapping("/slow-moving")
    public ResponseEntity<List<ProductMovementSummary>> getSlowMovingProducts(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(reportService.getSlowMovingProducts(limit));
    }

    /**
     * GET /api/reports/dead-stock
     * Dead stock — no movement in 90+ days.
     */
    @GetMapping("/dead-stock")
    public ResponseEntity<List<InventorySnapshot>> getDeadStock() {
        return ResponseEntity.ok(reportService.getDeadStock());
    }

    // ══════════════════════════════════════════════════════════════
    // PO SUMMARY
    // ══════════════════════════════════════════════════════════════


//      GET /api/reports/po-summary?from=2026-01-01&to=2026-04-30
//      Purchase Order spend summary for a date range.
//      Shows total spend and breakdown by warehouse and product.

    @GetMapping("/po-summary")
    public ResponseEntity<Map<String, Object>> getPOSummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(reportService.getPOSummary(from, to));
    }
}