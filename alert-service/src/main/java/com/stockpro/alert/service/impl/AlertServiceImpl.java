package com.stockpro.alert.service.impl;

import com.stockpro.alert.entity.Alert;
import com.stockpro.alert.dto.ProductDto;
import com.stockpro.alert.dto.WarehouseDto;
import com.stockpro.alert.feign.ProductClient;
import com.stockpro.alert.feign.WarehouseClient;
import com.stockpro.alert.repository.AlertRepository;
import com.stockpro.alert.service.AlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlertServiceImpl implements AlertService {

    private static final int BROADCAST_RECIPIENT_ID = 0;

    private final AlertRepository alertRepository;
    private final JavaMailSender mailSender;
    private final ProductClient productClient;
    private final WarehouseClient warehouseClient;

    @Override
    public Alert sendAlert(Alert alert) {
        Alert sanitized = sanitize(alert);
        Alert saved = alertRepository.save(sanitized);

        if ("CRITICAL".equals(saved.getSeverity())) {
            log.info("Critical alert created, triggering email for alertId={}", saved.getAlertId());
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

    @Override
    @Transactional
    public void sendLowStockAlert(int productId, int warehouseId, int currentQty) {
        boolean alertExists = alertRepository.existsUnacknowledgedLowStockAlert(
                productId, warehouseId);

        if (alertExists) {
            log.debug("Skipping duplicate LOW_STOCK alert for product={}, warehouse={}",
                    productId, warehouseId);
            return;
        }

        String severity = currentQty == 0 ? "CRITICAL" : "WARNING";
        String title = currentQty == 0
                ? "Out of stock - immediate action required"
                : "Low stock warning";
        ProductDto product = findProduct(productId);
        WarehouseDto warehouse = findWarehouse(warehouseId);
        String productLabel = productLabel(productId, product);
        String warehouseLabel = warehouseLabel(warehouseId, warehouse);
        String message = String.format(
                "%s at %s has %d units available. Raise a purchase order or transfer stock.",
                productLabel, warehouseLabel, currentQty);

        Alert alert = new Alert();
        alert.setRecipientId(BROADCAST_RECIPIENT_ID);
        alert.setType("LOW_STOCK");
        alert.setSeverity(severity);
        alert.setTitle(title);
        alert.setMessage(message);
        alert.setRelatedProductId(productId);
        alert.setRelatedWarehouseId(warehouseId);
        alert.setChannel("CRITICAL".equals(severity) ? "BOTH" : "IN_APP");

        sendAlert(alert);
    }

    @Override
    @Transactional
    public void sendOverstockAlert(int productId, int warehouseId, int currentQty, int maxStockLevel) {
        boolean alertExists = alertRepository.existsUnacknowledgedOverstockAlert(
                productId, warehouseId);

        if (alertExists) {
            log.debug("Skipping duplicate OVERSTOCK alert for product={}, warehouse={}",
                    productId, warehouseId);
            return;
        }

        ProductDto product = findProduct(productId);
        WarehouseDto warehouse = findWarehouse(warehouseId);
        Alert alert = new Alert();
        alert.setRecipientId(BROADCAST_RECIPIENT_ID);
        alert.setType("OVERSTOCK");
        alert.setSeverity("WARNING");
        alert.setTitle("Overstock warning");
        alert.setMessage(String.format(
                "%s at %s has %d units, exceeding the maximum stock level of %d.",
                productLabel(productId, product), warehouseLabel(warehouseId, warehouse), currentQty, maxStockLevel));
        alert.setRelatedProductId(productId);
        alert.setRelatedWarehouseId(warehouseId);
        alert.setChannel("IN_APP");

        sendAlert(alert);
    }

    @Override
    @Transactional
    public void sendBulkAlert(List<Integer> recipientIds, Alert template) {
        if (recipientIds == null || recipientIds.isEmpty()) {
            throw new IllegalArgumentException("At least one recipient is required.");
        }

        for (int recipientId : recipientIds) {
            Alert alert = new Alert();
            alert.setRecipientId(recipientId);
            alert.setType(template.getType());
            alert.setSeverity(template.getSeverity());
            alert.setTitle(template.getTitle());
            alert.setMessage(template.getMessage());
            alert.setRelatedProductId(template.getRelatedProductId());
            alert.setRelatedWarehouseId(template.getRelatedWarehouseId());
            alert.setChannel(template.getChannel());
            sendAlert(alert);
        }
        log.info("Bulk alert sent to {} recipients: {}", recipientIds.size(), template.getTitle());
    }

    @Override
    @Transactional
    public Alert markAsRead(int alertId) {
        Alert alert = getAlertById(alertId);
        alert.setRead(true);
        Alert saved = alertRepository.save(alert);
        log.debug("Alert {} marked as read", alertId);
        return saved;
    }

    @Override
    @Transactional
    public void markAllRead(int recipientId) {
        int updated = alertRepository.markAllAsReadForRecipient(recipientId);
        log.info("Marked {} alerts as read for recipient {}", updated, recipientId);
    }

    @Override
    @Transactional
    public Alert acknowledge(int alertId) {
        Alert alert = getAlertById(alertId);
        alert.setRead(true);
        alert.setAcknowledged(true);
        Alert saved = alertRepository.save(alert);
        log.info("Alert {} acknowledged", alertId);
        return saved;
    }

    @Override
    public List<Alert> getAll() {
        return alertRepository.findAllByOrderByCreatedAtDesc();
    }

    @Override
    public List<Alert> getByRecipient(int recipientId) {
        return alertRepository.findByRecipientIdInOrderByCreatedAtDesc(
                List.of(recipientId, BROADCAST_RECIPIENT_ID));
    }

    @Override
    public long getUnreadCount(int recipientId) {
        return alertRepository.countByRecipientIdInAndIsReadFalse(
                List.of(recipientId, BROADCAST_RECIPIENT_ID));
    }

    @Override
    public List<Alert> getUnacknowledged(int recipientId) {
        return alertRepository.findByRecipientIdInAndIsAcknowledgedFalseOrderByCreatedAtDesc(
                List.of(recipientId, BROADCAST_RECIPIENT_ID));
    }

    @Override
    @Transactional
    public void deleteAlert(int alertId) {
        if (!alertRepository.existsById(alertId)) {
            throw new RuntimeException("Alert not found with ID: " + alertId);
        }
        alertRepository.deleteById(alertId);
        log.info("Alert {} deleted", alertId);
    }

    @Override
    public void sendEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body == null ? "" : body);
            message.setFrom("noreply@stockpro.com");

            mailSender.send(message);
            log.info("Email sent successfully to: {}", to);

        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }

    private Alert getAlertById(int alertId) {
        return alertRepository.findById(alertId)
                .orElseThrow(() -> new RuntimeException("Alert not found: " + alertId));
    }

    private Alert sanitize(Alert alert) {
        if (alert == null) {
            throw new IllegalArgumentException("Alert details are required.");
        }
        if (alert.getRecipientId() < 0) {
            throw new IllegalArgumentException("Recipient ID cannot be negative.");
        }
        alert.setType(normalize(alert.getType(), "SYSTEM"));
        alert.setSeverity(normalize(alert.getSeverity(), "INFO"));
        alert.setChannel(normalize(alert.getChannel(), "IN_APP"));

        if (alert.getTitle() == null || alert.getTitle().isBlank()) {
            alert.setTitle(defaultTitle(alert.getType()));
        } else {
            alert.setTitle(alert.getTitle().trim());
        }
        if (alert.getMessage() == null) {
            alert.setMessage("");
        }
        return alert;
    }

    private String normalize(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String defaultTitle(String type) {
        return switch (type) {
            case "LOW_STOCK" -> "Low stock warning";
            case "OVERSTOCK" -> "Overstock warning";
            case "PO_PENDING" -> "PO requires approval";
            case "OVERDUE_RECEIPT" -> "Overdue receipt";
            default -> "System alert";
        };
    }

    private ProductDto findProduct(int productId) {
        try {
            return productClient == null ? null : productClient.getProductById(productId);
        } catch (Exception e) {
            log.warn("Could not enrich alert with product {}: {}", productId, e.getMessage());
            return null;
        }
    }

    private WarehouseDto findWarehouse(int warehouseId) {
        try {
            return warehouseClient == null ? null : warehouseClient.getWarehouseById(warehouseId);
        } catch (Exception e) {
            log.warn("Could not enrich alert with warehouse {}: {}", warehouseId, e.getMessage());
            return null;
        }
    }

    private String productLabel(int productId, ProductDto product) {
        if (product == null) {
            return "Product " + productId;
        }
        String sku = product.getSku() == null || product.getSku().isBlank()
                ? ""
                : " (SKU " + product.getSku() + ")";
        return product.getName() + sku;
    }

    private String warehouseLabel(int warehouseId, WarehouseDto warehouse) {
        if (warehouse == null || warehouse.getName() == null || warehouse.getName().isBlank()) {
            return "Warehouse " + warehouseId;
        }
        return warehouse.getName();
    }
}
