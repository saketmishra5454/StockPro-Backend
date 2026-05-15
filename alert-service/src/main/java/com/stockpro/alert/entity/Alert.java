package com.stockpro.alert.entity;

import com.fasterxml.jackson.annotation.JsonAlias;
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

    @Column(nullable = false)
    private int recipientId;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private String severity;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String message;

    @JsonAlias({"productId", "related_product_id"})
    private int relatedProductId;

    @JsonAlias({"warehouseId", "related_warehouse_id"})
    private int relatedWarehouseId;

    private String channel;

    // Read state for recipient notification views
    @JsonAlias({"read"})
    @Column(nullable = false)
    private boolean isRead = false;

    // Acknowledgement state for operational follow-up
    @JsonAlias({"acknowledged"})
    @Column(nullable = false)
    private boolean isAcknowledged = false;

    // Set automatically when alert is created
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
