package com.stockpro.purchase.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;


@Entity
@Table(name = "purchase_orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int poId;

    @Column(nullable = false)
    private int supplierId;

    @Column(nullable = false)
    private int warehouseId;

    // Who created this PO (Purchase Officer userId from auth-service)
    @Column(nullable = false)
    private int createdById;

    // DRAFT / PENDING / APPROVED / PARTIALLY_RECEIVED / RECEIVED / REJECTED / CANCELLED
    @Column(nullable = false)
    private String status;

    private double totalAmount;

    // Date the PO was created — set automatically in createPO()
    @Column(nullable = false, updatable = false)
    private LocalDate orderDate;

    // Expected delivery date — set by Purchase Officer when creating PO
    private LocalDate expectedDate;

    // Actual date goods were received — set when GRN is recorded
    private LocalDate receivedDate;

    @Column(columnDefinition = "TEXT")
    private String notes;

    // Human-readable PO reference (e.g. "PO-2026-001")
    @Column(unique = true)
    private String referenceNumber;

    // Reason for rejection or cancellation
    private String rejectionReason;

    @PrePersist
    public void prePersist() {
        this.orderDate = LocalDate.now();
        if (this.status == null) {
            this.status = "DRAFT";
        }
    }
}