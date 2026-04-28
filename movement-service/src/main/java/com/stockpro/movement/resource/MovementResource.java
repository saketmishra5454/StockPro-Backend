package com.stockpro.movement.resource;

import com.stockpro.movement.entity.StockMovement;
import com.stockpro.movement.service.MovementService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;


@RestController
@RequestMapping("/api/movements")
@RequiredArgsConstructor
public class MovementResource {

    private final MovementService movementService;


    // CREATE — manual movement recording


     // POST /api/movements

    @PostMapping
    public ResponseEntity<StockMovement> recordMovement(@RequestBody StockMovement movement) {
        StockMovement saved = movementService.recordMovement(movement);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }


    // READ — all GET endpoints (NO PUT or DELETE)


    /**
     * GET /api/movements/all
     * Get all movements — complete audit log. Most recent first.
     */
    @GetMapping("/all")
    public ResponseEntity<List<StockMovement>> getAllMovements() {
        return ResponseEntity.ok(movementService.getAllMovements());
    }


//      Get all movements for a product across all warehouses.
//      e.g. GET /api/movements/product/5

    @GetMapping("/product/{id}")
    public ResponseEntity<List<StockMovement>> getByProduct(@PathVariable int id) {
        return ResponseEntity.ok(movementService.getByProduct(id));
    }


//     * Get all movements that happened in a specific warehouse.
//     * e.g. GET /api/movements/warehouse/2

    @GetMapping("/warehouse/{id}")
    public ResponseEntity<List<StockMovement>> getByWarehouse(@PathVariable int id) {
        return ResponseEntity.ok(movementService.getByWarehouse(id));
    }


//     Get movements of a specific type.

    @GetMapping("/type/{type}")
    public ResponseEntity<List<StockMovement>> getByType(@PathVariable String type) {
        return ResponseEntity.ok(movementService.getByType(type));
    }


//     GET /api/movements/date-range?from=2026-04-01T00:00:00&to=2026-04-30T23:59:59
  //    Get movements within a date-time range.

    @GetMapping("/date-range")
    public ResponseEntity<List<StockMovement>> getByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ResponseEntity.ok(movementService.getByDateRange(from, to));
    }


//      Get movement history for ONE product in ONE warehouse.
//      e.g. GET /api/movements/history/5/1

    @GetMapping("/history/{productId}/{warehouseId}")
    public ResponseEntity<List<StockMovement>> getMovementHistory(
            @PathVariable int productId,
            @PathVariable int warehouseId) {
        return ResponseEntity.ok(movementService.getMovementHistory(productId, warehouseId));
    }


//      GET /api/movements/reference/{referenceId}/{referenceType}
//      Get all movements linked to a specific reference document.

    @GetMapping("/reference/{referenceId}/{referenceType}")
    public ResponseEntity<List<StockMovement>> getByReference(
            @PathVariable int referenceId,
            @PathVariable String referenceType) {
        return ResponseEntity.ok(movementService.getByReference(referenceId, referenceType));
    }


//     * GET /api/movements/stock-in/{productId}
//     * e.g. GET /api/movements/stock-in/5

    @GetMapping("/stock-in/{productId}")
    public ResponseEntity<java.util.Map<String, Object>> getStockIn(@PathVariable int productId) {
        int total = movementService.getStockIn(productId);
        return ResponseEntity.ok(java.util.Map.of(
                "productId", productId,
                "totalStockIn", total));
    }


//      GET /api/movements/stock-out/{productId}
//      e.g. GET /api/movements/stock-out/5

    @GetMapping("/stock-out/{productId}")
    public ResponseEntity<java.util.Map<String, Object>> getStockOut(@PathVariable int productId) {
        int total = movementService.getStockOut(productId);
        return ResponseEntity.ok(java.util.Map.of(
                "productId", productId,
                "totalStockOut", total));
    }
}