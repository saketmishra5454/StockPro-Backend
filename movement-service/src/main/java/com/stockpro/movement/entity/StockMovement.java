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

    @Column(nullable = false)
    private int productId;

    @Column(nullable = false)
    private int warehouseId;

    @Column(nullable = false)
    private String movementType;

    @Column(nullable = false)
    private int quantity;

    // Source document identifier when movement originated from another workflow
    private int referenceId;

    // Type of reference: "PURCHASE_ORDER", "ISSUE_ORDER", "TRANSFER", "MANUAL"
    private String referenceType;

    private double unitCost;

    // Zero indicates a system-generated movement
    private int performedBy;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(nullable = false)
    private LocalDateTime movementDate;

    // Enables historical stock-on-hand reconstruction
    private int balanceAfter;

    @PrePersist
    public void prePersist() {
        if (this.movementDate == null) {
            this.movementDate = LocalDateTime.now();
        }
    }
}
