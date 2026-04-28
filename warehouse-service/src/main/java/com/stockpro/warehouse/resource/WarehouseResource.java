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


    //============= WAREHOUSE ENDPOINTS — /api/warehouses==============

    @PostMapping("/api/warehouses")
    public ResponseEntity<Warehouse> createWarehouse(@RequestBody Warehouse warehouse) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(warehouseService.createWarehouse(warehouse));
    }


     //Get all warehouses.

    @GetMapping("/api/warehouses")
    public ResponseEntity<List<Warehouse>> getAllWarehouses() {
        return ResponseEntity.ok(warehouseService.getAllWarehouses());
    }


     // Get one warehouse by ID.

    @GetMapping("/api/warehouses/{id}")
    public ResponseEntity<Warehouse> getWarehouseById(@PathVariable int id) {
        return ResponseEntity.ok(warehouseService.getWarehouseById(id));
    }


     // Update warehouse details (name, address, capacity, manager, phone).

    @PutMapping("/api/warehouses/{id}")
    public ResponseEntity<Warehouse> updateWarehouse(
            @PathVariable int id,
            @RequestBody Warehouse warehouse) {
        return ResponseEntity.ok(warehouseService.updateWarehouse(id, warehouse));
    }


    //Deactivate a warehouse (soft delete).

    @PutMapping("/api/warehouses/deactivate/{id}")
    public ResponseEntity<Map<String, String>> deactivateWarehouse(@PathVariable int id) {
        warehouseService.deactivateWarehouse(id);
        return ResponseEntity.ok(Map.of("message", "Warehouse deactivated successfully"));
    }


    //================== STOCK ENDPOINTS — /api/stock======================


     // Initialize stock for a product in a warehouse (first time setup).

    @PostMapping("/api/stock/initialize")
    public ResponseEntity<StockLevel> initializeStock(
            @RequestParam int warehouseId,
            @RequestParam int productId,
            @RequestParam int initialQuantity) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(warehouseService.initializeStock(warehouseId, productId, initialQuantity));
    }


     //Get current stock level for a specific product in a specific warehouse.

    @GetMapping("/api/stock/{warehouseId}/{productId}")
    public ResponseEntity<StockLevel> getStockLevel(
            @PathVariable int warehouseId,
            @PathVariable int productId) {
        return ResponseEntity.ok(warehouseService.getStockLevel(warehouseId, productId));
    }


     // Get all stock levels in a warehouse.

    @GetMapping("/api/stock/warehouse/{warehouseId}")
    public ResponseEntity<List<StockLevel>> getStockByWarehouse(@PathVariable int warehouseId) {
        return ResponseEntity.ok(warehouseService.getStockByWarehouse(warehouseId));
    }


     // Update stock by a delta (positive = stock in, negative = stock out).

    @PutMapping("/api/stock/update")
    public ResponseEntity<StockLevel> updateStock(
            @RequestParam int warehouseId,
            @RequestParam int productId,
            @RequestParam int delta) {
        return ResponseEntity.ok(warehouseService.updateStock(warehouseId, productId, delta));
    }


     //Reserve stock for an open order.

    @PostMapping("/api/stock/reserve")
    public ResponseEntity<StockLevel> reserveStock(
            @RequestParam int warehouseId,
            @RequestParam int productId,
            @RequestParam int quantity) {
        return ResponseEntity.ok(warehouseService.reserveStock(warehouseId, productId, quantity));
    }


     // POST /api/stock/release
      //Release a stock reservation (PO cancelled, reservation expired).

    @PostMapping("/api/stock/release")
    public ResponseEntity<StockLevel> releaseReservation(
            @RequestParam int warehouseId,
            @RequestParam int productId,
            @RequestParam int quantity) {
        return ResponseEntity.ok(warehouseService.releaseReservation(warehouseId, productId, quantity));
    }


     // POST /api/stock/transfer
      //Transfer stock between two warehouses.

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


     // Get all stock levels that are critically low.

    @GetMapping("/api/stock/low")
    public ResponseEntity<List<StockLevel>> getLowStockItems() {
        return ResponseEntity.ok(warehouseService.getLowStockItems());
    }
}