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

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportResource {

    private final ReportService reportService;

    @PostMapping("/snapshot/{warehouseId}")
    public ResponseEntity<Map<String, String>> takeSnapshot(@PathVariable int warehouseId) {
        reportService.takeSnapshot(warehouseId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message",
                        "Snapshot taken for warehouse " + warehouseId));
    }

    @PostMapping("/snapshot/all")
    public ResponseEntity<Map<String, String>> takeSnapshotAll() {
        reportService.takeSnapshotAllWarehouses();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "Snapshot taken for all warehouses"));
    }

    @GetMapping("/snapshot/{warehouseId}")
    public ResponseEntity<List<InventorySnapshot>> getLatestSnapshot(
            @PathVariable int warehouseId) {
        return ResponseEntity.ok(reportService.getLatestSnapshot(warehouseId));
    }

    @GetMapping("/stock-value/total")
    public ResponseEntity<Map<String, Object>> getTotalStockValue() {
        double total = reportService.getTotalStockValue();
        return ResponseEntity.ok(Map.of(
                "totalStockValue", total,
                "currency", "INR",
                "asOf", LocalDate.now().toString()));
    }

    @GetMapping("/stock-value/warehouse/{warehouseId}")
    public ResponseEntity<Map<String, Object>> getStockValueByWarehouse(
            @PathVariable int warehouseId) {
        double value = reportService.getStockValueByWarehouse(warehouseId);
        return ResponseEntity.ok(Map.of(
                "warehouseId", warehouseId,
                "stockValue", value,
                "asOf", LocalDate.now().toString()));
    }

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

    @GetMapping("/low-stock")
    public ResponseEntity<List<Map<String, Object>>> getLowStockReport() {
        return ResponseEntity.ok(reportService.getLowStockReport());
    }

    @GetMapping("/top-moving")
    public ResponseEntity<List<ProductMovementSummary>> getTopMovingProducts(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(reportService.getTopMovingProducts(limit));
    }

    @GetMapping("/slow-moving")
    public ResponseEntity<List<ProductMovementSummary>> getSlowMovingProducts(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(reportService.getSlowMovingProducts(limit));
    }


    @GetMapping("/dead-stock")
    public ResponseEntity<List<InventorySnapshot>> getDeadStock() {
        return ResponseEntity.ok(reportService.getDeadStock());
    }

    @GetMapping("/po-summary")
    public ResponseEntity<Map<String, Object>> getPOSummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(reportService.getPOSummary(from, to));
    }
}
