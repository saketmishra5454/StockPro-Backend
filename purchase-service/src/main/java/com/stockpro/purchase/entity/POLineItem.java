package com.stockpro.purchase.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "po_line_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class POLineItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int lineItemId;

    @Column(nullable = false)
    private int poId;

    @Column(nullable = false)
    private int productId;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private double unitCost;

    // Recalculated from quantity and unit cost before persistence
    private double totalCost;

    // Quantity already received against this PO line
    private int receivedQty;

    @PrePersist
    @PreUpdate
    public void computeTotalCost() {
        this.totalCost = this.quantity * this.unitCost;
    }

    public boolean isFullyReceived() {
        return this.receivedQty >= this.quantity;
    }

    public int getRemainingQty() {
        return this.quantity - this.receivedQty;
    }
}
