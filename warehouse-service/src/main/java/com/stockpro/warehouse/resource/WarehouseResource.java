package com.stockpro.warehouse.resource;

import com.stockpro.warehouse.dto.TransferRequest;
import com.stockpro.warehouse.entity.StockLevel;
import com.stockpro.warehouse.entity.Warehouse;
import com.stockpro.warehouse.service.WarehouseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class WarehouseResource {

    private final WarehouseService warehouseService;

    @PostMapping("/api/warehouses")
    public ResponseEntity<Warehouse> createWarehouse(@RequestBody Warehouse warehouse) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(warehouseService.createWarehouse(warehouse));
    }

    @GetMapping("/api/warehouses")
    public ResponseEntity<List<Warehouse>> getAllWarehouses() {
        return ResponseEntity.ok(warehouseService.getAllWarehouses());
    }

    @GetMapping("/api/warehouses/{id}")
    public ResponseEntity<Warehouse> getWarehouseById(@PathVariable int id) {
        return ResponseEntity.ok(warehouseService.getWarehouseById(id));
    }

    @PutMapping("/api/warehouses/{id}")
    public ResponseEntity<Warehouse> updateWarehouse(
            @PathVariable int id,
            @RequestBody Warehouse warehouse) {
        return ResponseEntity.ok(warehouseService.updateWarehouse(id, warehouse));
    }

    @PutMapping("/api/warehouses/deactivate/{id}")
    public ResponseEntity<Map<String, String>> deactivateWarehouse(@PathVariable int id) {
        warehouseService.deactivateWarehouse(id);
        return ResponseEntity.ok(Map.of("message", "Warehouse deactivated successfully"));
    }

    @PutMapping("/api/warehouses/activate/{id}")
    public ResponseEntity<Map<String, String>> activateWarehouse(@PathVariable int id) {
        warehouseService.activateWarehouse(id);
        return ResponseEntity.ok(Map.of("message", "Warehouse activated successfully"));
    }

    @PostMapping("/api/stock/initialize")
    public ResponseEntity<StockLevel> initializeStock(
            @RequestParam int warehouseId,
            @RequestParam int productId,
            @RequestParam int initialQuantity) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(warehouseService.initializeStock(warehouseId, productId, initialQuantity));
    }

    @GetMapping("/api/stock/{warehouseId}/{productId}")
    public ResponseEntity<StockLevel> getStockLevel(
            @PathVariable int warehouseId,
            @PathVariable int productId) {
        return ResponseEntity.ok(warehouseService.getStockLevel(warehouseId, productId));
    }

    @GetMapping("/api/stock/warehouse/{warehouseId}")
    public ResponseEntity<List<StockLevel>> getStockByWarehouse(@PathVariable int warehouseId) {
        return ResponseEntity.ok(warehouseService.getStockByWarehouse(warehouseId));
    }

    @GetMapping("/api/stock")
    public ResponseEntity<List<StockLevel>> getAllStockLevels() {
        return ResponseEntity.ok(warehouseService.getAllStockLevels());
    }

    @PutMapping("/api/stock/update")
    public ResponseEntity<StockLevel> updateStock(
            @RequestParam int warehouseId,
            @RequestParam int productId,
            @RequestParam int delta) {
        return ResponseEntity.ok(warehouseService.updateStock(warehouseId, productId, delta));
    }

    @PostMapping("/api/stock/reserve")
    public ResponseEntity<StockLevel> reserveStock(
            @RequestParam int warehouseId,
            @RequestParam int productId,
            @RequestParam int quantity) {
        return ResponseEntity.ok(warehouseService.reserveStock(warehouseId, productId, quantity));
    }

    @PostMapping("/api/stock/release")
    public ResponseEntity<StockLevel> releaseReservation(
            @RequestParam int warehouseId,
            @RequestParam int productId,
            @RequestParam int quantity) {
        return ResponseEntity.ok(warehouseService.releaseReservation(warehouseId, productId, quantity));
    }

    @PostMapping("/api/stock/transfer")
    public ResponseEntity<Map<String, String>> transferStock(
            @RequestBody TransferRequest request) {
        warehouseService.transferStock(
                request.getFromWarehouseId(),
                request.getToWarehouseId(),
                request.getProductId(),
                request.getQuantity());
        return ResponseEntity.ok(Map.of("message",
                "Transfer of " + request.getQuantity() + " units completed successfully"));
    }

    @GetMapping("/api/stock/low")
    public ResponseEntity<List<StockLevel>> getLowStockItems() {
        return ResponseEntity.ok(warehouseService.getLowStockItems());
    }
}
