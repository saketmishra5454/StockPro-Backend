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

    @PostMapping
    public ResponseEntity<StockMovement> recordMovement(@RequestBody StockMovement movement) {
        StockMovement saved = movementService.recordMovement(movement);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }


    @GetMapping("/all")
    public ResponseEntity<List<StockMovement>> getAllMovements() {
        return ResponseEntity.ok(movementService.getAllMovements());
    }

    @GetMapping("/product/{id}")
    public ResponseEntity<List<StockMovement>> getByProduct(@PathVariable int id) {
        return ResponseEntity.ok(movementService.getByProduct(id));
    }

    @GetMapping("/warehouse/{id}")
    public ResponseEntity<List<StockMovement>> getByWarehouse(@PathVariable int id) {
        return ResponseEntity.ok(movementService.getByWarehouse(id));
    }

    @GetMapping("/type/{type}")
    public ResponseEntity<List<StockMovement>> getByType(@PathVariable String type) {
        return ResponseEntity.ok(movementService.getByType(type));
    }

    @GetMapping("/date-range")
    public ResponseEntity<List<StockMovement>> getByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ResponseEntity.ok(movementService.getByDateRange(from, to));
    }

    @GetMapping("/history/{productId}/{warehouseId}")
    public ResponseEntity<List<StockMovement>> getMovementHistory(
            @PathVariable int productId,
            @PathVariable int warehouseId) {
        return ResponseEntity.ok(movementService.getMovementHistory(productId, warehouseId));
    }

    @GetMapping("/reference/{referenceId}/{referenceType}")
    public ResponseEntity<List<StockMovement>> getByReference(
            @PathVariable int referenceId,
            @PathVariable String referenceType) {
        return ResponseEntity.ok(movementService.getByReference(referenceId, referenceType));
    }

    @GetMapping("/stock-in/{productId}")
    public ResponseEntity<java.util.Map<String, Object>> getStockIn(@PathVariable int productId) {
        int total = movementService.getStockIn(productId);
        return ResponseEntity.ok(java.util.Map.of(
                "productId", productId,
                "totalStockIn", total));
    }

    @GetMapping("/stock-out/{productId}")
    public ResponseEntity<java.util.Map<String, Object>> getStockOut(@PathVariable int productId) {
        int total = movementService.getStockOut(productId);
        return ResponseEntity.ok(java.util.Map.of(
                "productId", productId,
                "totalStockOut", total));
    }
}
