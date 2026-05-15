package com.stockpro.alert.resource;

import com.stockpro.alert.entity.Alert;
import com.stockpro.alert.service.AlertService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
public class AlertResource {

    private final AlertService alertService;

    @PostMapping
    public ResponseEntity<Alert> sendAlert(@RequestBody Alert alert) {
        return ResponseEntity.status(HttpStatus.CREATED).body(alertService.sendAlert(alert));
    }

    @PostMapping("/bulk")
    public ResponseEntity<Map<String, String>> sendBulkAlert(@RequestBody BulkAlertRequest request) {
        Alert template = new Alert();
        template.setType(request.getType());
        template.setSeverity(request.getSeverity());
        template.setTitle(request.getTitle());
        template.setMessage(request.getMessage());
        template.setRelatedProductId(request.getRelatedProductId());
        template.setRelatedWarehouseId(request.getRelatedWarehouseId());
        template.setChannel(request.getChannel());

        alertService.sendBulkAlert(request.getRecipientIds(), template);
        return ResponseEntity.ok(Map.of(
                "message", "Bulk alert sent to " + request.getRecipientIds().size() + " recipients"));
    }

    @PostMapping("/low-stock")
    public ResponseEntity<Map<String, String>> sendLowStockAlert(@RequestBody StockAlertRequest request) {
        alertService.sendLowStockAlert(
                request.getProductId(),
                request.getWarehouseId(),
                request.getCurrentQty());
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", "Low-stock alert processed"));
    }

    @PostMapping("/overstock")
    public ResponseEntity<Map<String, String>> sendOverstockAlert(@RequestBody StockAlertRequest request) {
        alertService.sendOverstockAlert(
                request.getProductId(),
                request.getWarehouseId(),
                request.getCurrentQty(),
                request.getMaxStockLevel());
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", "Overstock alert processed"));
    }

    @GetMapping
    public ResponseEntity<List<Alert>> getAll() {
        return ResponseEntity.ok(alertService.getAll());
    }

    @GetMapping("/recipient/{recipientId}")
    public ResponseEntity<List<Alert>> getByRecipient(@PathVariable int recipientId) {
        return ResponseEntity.ok(alertService.getByRecipient(recipientId));
    }

    @GetMapping("/unread-count/{recipientId}")
    public ResponseEntity<Map<String, Object>> getUnreadCount(@PathVariable int recipientId) {
        long count = alertService.getUnreadCount(recipientId);
        return ResponseEntity.ok(Map.of(
                "recipientId", recipientId,
                "unreadCount", count));
    }

    @GetMapping("/unacknowledged/{recipientId}")
    public ResponseEntity<List<Alert>> getUnacknowledged(@PathVariable int recipientId) {
        return ResponseEntity.ok(alertService.getUnacknowledged(recipientId));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<Alert> markAsRead(@PathVariable int id) {
        return ResponseEntity.ok(alertService.markAsRead(id));
    }

    @PutMapping("/read-all/{recipientId}")
    public ResponseEntity<Map<String, String>> markAllRead(@PathVariable int recipientId) {
        alertService.markAllRead(recipientId);
        return ResponseEntity.ok(Map.of("message",
                "All alerts marked as read for recipient " + recipientId));
    }

    @PutMapping("/{id}/acknowledge")
    public ResponseEntity<Alert> acknowledge(@PathVariable int id) {
        return ResponseEntity.ok(alertService.acknowledge(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteAlert(@PathVariable int id) {
        alertService.deleteAlert(id);
        return ResponseEntity.ok(Map.of("message", "Alert " + id + " deleted successfully"));
    }

    @Data
    static class BulkAlertRequest {
        private List<Integer> recipientIds;
        private String type;
        private String severity;
        private String title;
        private String message;
        private int relatedProductId;
        private int relatedWarehouseId;
        private String channel;
    }

    @Data
    static class StockAlertRequest {
        private int productId;
        private int warehouseId;
        private int currentQty;
        private int maxStockLevel;
    }
}
