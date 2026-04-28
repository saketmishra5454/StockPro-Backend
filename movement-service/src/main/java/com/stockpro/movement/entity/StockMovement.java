package com.stockpro.movement.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Entity
@Table(name = "stock_movements")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int movementId;

    // Which product's stock changed
    @Column(nullable = false)
    private int productId;

    // Which warehouse the change happened in
    @Column(nullable = false)
    private int warehouseId;

    // Type of movement — one of the 7 types listed above
    @Column(nullable = false)
    private String movementType;

    // How many units changed — always positive
    // The movementType tells you direction (in or out)
    @Column(nullable = false)
    private int quantity;

    // Reference to the source document that caused this movement
    // e.g. PO ID for STOCK_IN, Issue Order ID for STOCK_OUT
    private int referenceId;

    // Type of reference: "PURCHASE_ORDER", "ISSUE_ORDER", "TRANSFER", "MANUAL"
    private String referenceType;

    // Cost per unit at time of movement — for valuation calculations
    private double unitCost;

    // userId of who performed this operation
    // 0 = automated (RabbitMQ event), >0 = real user
    private int performedBy;

    // Optional notes or reason for this movement
    @Column(columnDefinition = "TEXT")
    private String notes;

    // Exact timestamp when this movement occurred
    @Column(nullable = false)
    private LocalDateTime movementDate;

    // Stock quantity AFTER this movement was applied
    // Enables historical stock-on-hand reconstruction
    private int balanceAfter;

    // Auto-set movementDate if not provided
    @PrePersist
    public void prePersist() {
        if (this.movementDate == null) {
            this.movementDate = LocalDateTime.now();
        }
    }
}