package com.stockpro.alert.resource;

import com.stockpro.alert.entity.Alert;
import com.stockpro.alert.service.AlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

 //AlertResource — REST controller for all alert endpoints.

@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
public class AlertResource {

    private final AlertService alertService;

    // CREATE

    @PostMapping
    public ResponseEntity<Alert> sendAlert(@RequestBody Alert alert) {
        return ResponseEntity.status(HttpStatus.CREATED).body(alertService.sendAlert(alert));
    }


     // POST /api/alerts/bulk
     // Send the same alert to multiple recipients at once.

    @PostMapping("/bulk")
    public ResponseEntity<Map<String, String>> sendBulkAlert(
            @RequestBody Map<String, Object> request) {

        @SuppressWarnings("unchecked")
        List<Integer> recipientIds = (List<Integer>) request.get("recipientIds");
        String title   = (String) request.get("title");
        String message = (String) request.get("message");

        alertService.sendBulkAlert(recipientIds, title, message);
        return ResponseEntity.ok(Map.of(
                "message", "Bulk alert sent to " + recipientIds.size() + " recipients"));
    }

    // READ
     // Get all alerts for a user — their full notification inbox.
    //e.g. GET /api/alerts/recipient/3

    @GetMapping("/recipient/{recipientId}")
    public ResponseEntity<List<Alert>> getByRecipient(@PathVariable int recipientId) {
        return ResponseEntity.ok(alertService.getByRecipient(recipientId));
    }


     // Get the count of unread alerts — drives the notification badge number.
      // e.g. GET /api/alerts/unread-count/3 → { "recipientId": 3, "unreadCount": 5 }

    @GetMapping("/unread-count/{recipientId}")
    public ResponseEntity<Map<String, Object>> getUnreadCount(@PathVariable int recipientId) {
        long count = alertService.getUnreadCount(recipientId);
        return ResponseEntity.ok(Map.of(
                "recipientId", recipientId,
                "unreadCount", count));
    }


     // Get alerts that user has NOT yet confirmed action on.
     // These appear in the "Pending Action" section of the alert centre.

    @GetMapping("/unacknowledged/{recipientId}")
    public ResponseEntity<List<Alert>> getUnacknowledged(@PathVariable int recipientId) {
        return ResponseEntity.ok(alertService.getUnacknowledged(recipientId));
    }

    // MARK AS READ

    @PutMapping("/{id}/read")
    public ResponseEntity<Alert> markAsRead(@PathVariable int id) {
        return ResponseEntity.ok(alertService.markAsRead(id));
    }


     // PUT /api/alerts/read-all/{recipientId}

    @PutMapping("/read-all/{recipientId}")
    public ResponseEntity<Map<String, String>> markAllRead(@PathVariable int recipientId) {
        alertService.markAllRead(recipientId);
        return ResponseEntity.ok(Map.of("message",
                "All alerts marked as read for recipient " + recipientId));
    }

    // ══════════════════════════════════════════════════════════════
    // ACKNOWLEDGE
    // ══════════════════════════════════════════════════════════════

     //PUT /api/alerts/{id}/acknowledge

    @PutMapping("/{id}/acknowledge")
    public ResponseEntity<Alert> acknowledge(@PathVariable int id) {
        return ResponseEntity.ok(alertService.acknowledge(id));
    }

    // ══════════════════════════════════════════════════════════════
    // DELETE
    // ══════════════════════════════════════════════════════════════


     // DELETE /api/alerts/{id}
     // Permanently delete an alert.

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteAlert(@PathVariable int id) {
        alertService.deleteAlert(id);
        return ResponseEntity.ok(Map.of("message", "Alert " + id + " deleted successfully"));
    }
}