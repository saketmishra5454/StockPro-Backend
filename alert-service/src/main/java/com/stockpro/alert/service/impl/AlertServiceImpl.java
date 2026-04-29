package com.stockpro.alert.service.impl;

import com.stockpro.alert.entity.Alert;
import com.stockpro.alert.repository.AlertRepository;
import com.stockpro.alert.service.AlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

 // AlertServiceImpl — all business logic for alert management.

@Service
@RequiredArgsConstructor
@Slf4j
public class AlertServiceImpl implements AlertService {

    private final AlertRepository alertRepository;
    private final JavaMailSender mailSender;

    /**
     * Send a single alert.
     * Saves to DB. If severity is CRITICAL and email is provided, also sends email.
     */
    @Override
    public Alert sendAlert(Alert alert) {
        Alert saved = alertRepository.save(alert);

        // Send email for CRITICAL alerts only
        if ("CRITICAL".equals(saved.getSeverity())) {
            log.info("CRITICAL alert created — triggering email for alertId={}",
                    saved.getAlertId());
            // Email is sent to a default admin address — in production,
            // you'd look up the recipient's email from auth-service
            sendEmail(
                    "admin@stockpro.com",
                    "[CRITICAL] " + saved.getTitle(),
                    saved.getMessage());
        }

        log.info("Alert sent: id={}, type={}, severity={}, recipient={}",
                saved.getAlertId(), saved.getType(),
                saved.getSeverity(), saved.getRecipientId());

        return saved;
    }


     // Create a LOW_STOCK alert for a product+warehouse.

    @Override
    @Transactional
    public void sendLowStockAlert(int productId, int warehouseId, int currentQty) {
        // Check for existing unacknowledged alert — prevent spam
        boolean alertExists = alertRepository.existsUnacknowledgedLowStockAlert(
                productId, warehouseId);

        if (alertExists) {
            log.debug("Skipping duplicate LOW_STOCK alert for product={}, warehouse={}",
                    productId, warehouseId);
            return;
        }

        // Determine severity based on quantity
        String severity  = currentQty == 0 ? "CRITICAL" : "WARNING";
        String title     = currentQty == 0
                ? "OUT OF STOCK — Immediate Action Required"
                : "Low Stock Warning";
        String message   = String.format(
                "Product ID %d in Warehouse ID %d has %d units remaining. " +
                        "This is below the minimum reorder level. Please raise a Purchase Order.",
                productId, warehouseId, currentQty);

        Alert alert = new Alert();
        alert.setRecipientId(0);           // 0 = broadcast to all Inventory Managers
        alert.setType("LOW_STOCK");
        alert.setSeverity(severity);
        alert.setTitle(title);
        alert.setMessage(message);
        alert.setRelatedProductId(productId);
        alert.setRelatedWarehouseId(warehouseId);
        alert.setChannel("CRITICAL".equals(severity) ? "BOTH" : "IN_APP");

        sendAlert(alert);  // this also sends email if CRITICAL
    }

     // Send the same alert to multiple recipients.

    @Override
    @Transactional
    public void sendBulkAlert(List<Integer> recipientIds, String title, String message) {
        for (int recipientId : recipientIds) {
            Alert alert = new Alert();
            alert.setRecipientId(recipientId);
            alert.setType("SYSTEM");
            alert.setSeverity("INFO");
            alert.setTitle(title);
            alert.setMessage(message);
            alert.setChannel("IN_APP");
            alertRepository.save(alert);
        }
        log.info("Bulk alert sent to {} recipients: {}", recipientIds.size(), title);
    }


     // Mark a single alert as read.
     // Removes it from the unread badge count.

    @Override
    @Transactional
    public Alert markAsRead(int alertId) {
        Alert alert = getAlertById(alertId);
        alert.setRead(true);
        Alert saved = alertRepository.save(alert);
        log.debug("Alert {} marked as read", alertId);
        return saved;
    }


     // Mark ALL alerts for a recipient as read.

    @Override
    @Transactional
    public void markAllRead(int recipientId) {
        int updated = alertRepository.markAllAsReadForRecipient(recipientId);
        log.info("Marked {} alerts as read for recipient {}", updated, recipientId);
    }


      //Acknowledge an alert — user has seen and acted on it.
     // Removes it from the pending action queue.

    @Override
    @Transactional
    public Alert acknowledge(int alertId) {
        Alert alert = getAlertById(alertId);
        alert.setRead(true);             // also mark as read when acknowledging
        alert.setAcknowledged(true);
        Alert saved = alertRepository.save(alert);
        log.info("Alert {} acknowledged", alertId);
        return saved;
    }


     // Get all alerts for a recipient — their full notification inbox.
     // Ordered most recent first.

    @Override
    public List<Alert> getByRecipient(int recipientId) {
        return alertRepository.findByRecipientIdOrderByCreatedAtDesc(recipientId);
    }

     //Count unread alerts for a recipient.

    @Override
    public long getUnreadCount(int recipientId) {
        return alertRepository.countByRecipientIdAndIsReadFalse(recipientId);
    }


     // Get all unacknowledged alerts for a recipient.

    @Override
    public List<Alert> getUnacknowledged(int recipientId) {
        return alertRepository
                .findByRecipientIdAndIsAcknowledgedFalseOrderByCreatedAtDesc(recipientId);
    }


     // Delete an alert permanently.
      //Usually only done by Admin for cleanup.

    @Override
    @Transactional
    public void deleteAlert(int alertId) {
        if (!alertRepository.existsById(alertId)) {
            throw new RuntimeException("Alert not found with ID: " + alertId);
        }
        alertRepository.deleteById(alertId);
        log.info("Alert {} deleted", alertId);
    }


     // Send an email via JavaMailSender (Gmail SMTP).

    @Override
    public void sendEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            message.setFrom("StockPro Alerts <noreply@stockpro.com>");

            mailSender.send(message);
            log.info("Email sent successfully to: {}", to);

        } catch (Exception e) {
            // Email failure should NOT block alert creation
            // The alert is already saved in DB and visible in-app
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }

    // ── Private helper ──────────────────────────────────────────────

    private Alert getAlertById(int alertId) {
        return alertRepository.findById(alertId)
                .orElseThrow(() -> new RuntimeException("Alert not found: " + alertId));
    }
}