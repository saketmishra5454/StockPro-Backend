package com.stockpro.purchase.resource;

import com.stockpro.purchase.entity.POLineItem;
import com.stockpro.purchase.entity.PurchaseOrder;
import com.stockpro.purchase.service.PurchaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/purchase-orders")
@RequiredArgsConstructor
public class PurchaseResource {

    private final PurchaseService purchaseService;

    public static class CreatePORequest {
        public PurchaseOrder purchaseOrder;
        public List<POLineItem> lineItems;
    }

    public static class ReceiveGoodsRequest {
        public List<POLineItem> receivedItems;
    }

    @PostMapping
    public ResponseEntity<PurchaseOrder> createPO(@RequestBody CreatePORequest request) {
        PurchaseOrder created = purchaseService.createPO(
                request.purchaseOrder, request.lineItems);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/{id}/submit")
    public ResponseEntity<PurchaseOrder> submitPO(@PathVariable int id) {
        return ResponseEntity.ok(purchaseService.submitPO(id));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<PurchaseOrder> approvePO(
            @PathVariable int id,
            @RequestHeader(value = "X-User-Role", defaultValue = "STAFF") String userRole) {

        if (!"MANAGER".equals(userRole) && !"ADMIN".equals(userRole)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .build();
        }
        return ResponseEntity.ok(purchaseService.approvePO(id));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<PurchaseOrder> rejectPO(
            @PathVariable int id,
            @RequestBody Map<String, String> body,
            @RequestHeader(value = "X-User-Role", defaultValue = "STAFF") String userRole) {

        if (!"MANAGER".equals(userRole) && !"ADMIN".equals(userRole)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(
                purchaseService.rejectPO(id, body.get("reason")));
    }

    @PostMapping("/{id}/receive")
    public ResponseEntity<PurchaseOrder> receiveGoods(
            @PathVariable int id,
            @RequestBody ReceiveGoodsRequest request) {
        return ResponseEntity.ok(
                purchaseService.receiveGoods(id, request.receivedItems));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<PurchaseOrder> cancelPO(
            @PathVariable int id,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(
                purchaseService.cancelPO(id, body.get("reason")));
    }


    @PutMapping("/{id}")
    public ResponseEntity<PurchaseOrder> updatePO(
            @PathVariable int id,
            @RequestBody PurchaseOrder purchaseOrder) {
        return ResponseEntity.ok(purchaseService.updatePO(id, purchaseOrder));
    }

    @GetMapping
    public ResponseEntity<List<PurchaseOrder>> getAllPOs() {
        return ResponseEntity.ok(purchaseService.getAllPOs());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PurchaseOrder> getPOById(@PathVariable int id) {
        return ResponseEntity.ok(purchaseService.getPOById(id));
    }

    @GetMapping("/{id}/lines")
    public ResponseEntity<List<POLineItem>> getLineItems(@PathVariable int id) {
        return ResponseEntity.ok(purchaseService.getLineItemsByPO(id));
    }

    @GetMapping("/supplier/{supplierId}")
    public ResponseEntity<List<PurchaseOrder>> getBySupplier(@PathVariable int supplierId) {
        return ResponseEntity.ok(purchaseService.getPOsBySupplier(supplierId));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<PurchaseOrder>> getByStatus(@PathVariable String status) {
        return ResponseEntity.ok(purchaseService.getPOsByStatus(status));
    }

    @GetMapping("/warehouse/{warehouseId}")
    public ResponseEntity<List<PurchaseOrder>> getByWarehouse(@PathVariable int warehouseId) {
        return ResponseEntity.ok(purchaseService.getPOsByWarehouse(warehouseId));
    }

    @GetMapping("/date-range")
    public ResponseEntity<List<PurchaseOrder>> getByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return ResponseEntity.ok(purchaseService.getPOsByDateRange(start, end));
    }
}
