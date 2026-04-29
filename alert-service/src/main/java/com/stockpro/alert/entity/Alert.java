package com.stockpro.alert.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Entity
@Table(name = "alerts")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int alertId;

    // userId of who should see this alert
    @Column(nullable = false)
    private int recipientId;

    // Alert category — one of: LOW_STOCK, OVERSTOCK, PO_PENDING,
    // OVERDUE_RECEIPT, SYSTEM
    @Column(nullable = false)
    private String type;

    // Severity — INFO, WARNING, or CRITICAL
    // CRITICAL alerts also trigger email via JavaMailSender
    @Column(nullable = false)
    private String severity;

    // Short headline shown in notification badge
    @Column(nullable = false)
    private String title;

    // Full alert message with details
    @Column(columnDefinition = "TEXT")
    private String message;

    // Which product triggered this alert (0 = not product-related)
    private int relatedProductId;

    // Which warehouse this alert is about (0 = not warehouse-related)
    private int relatedWarehouseId;

    // Delivery channel: IN_APP, EMAIL, BOTH
    private String channel;

    // Has the user opened/viewed this alert?
    @Column(nullable = false)
    private boolean isRead = false;

    // Has the user confirmed action on this alert?
    @Column(nullable = false)
    private boolean isAcknowledged = false;

    // When this alert was created
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.isRead = false;
        this.isAcknowledged = false;
        if (this.channel == null) this.channel = "IN_APP";
        if (this.severity == null) this.severity = "INFO";
    }
}