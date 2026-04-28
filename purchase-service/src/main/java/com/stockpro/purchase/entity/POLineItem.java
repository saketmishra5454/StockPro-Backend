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

    // FK to purchase_orders table — which PO this line belongs to
    @Column(nullable = false)
    private int poId;

    // FK to product-service (no JPA join — different DB)
    @Column(nullable = false)
    private int productId;

    // How many units were ordered
    @Column(nullable = false)
    private int quantity;

    // Cost per unit at time of ordering
    @Column(nullable = false)
    private double unitCost;

    // quantity × unitCost — stored for historical accuracy
    private double totalCost;

    // How many units have actually been received so far
    // Starts at 0, increases as goods are received
    private int receivedQty;

    // Computes and sets totalCost before saving
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