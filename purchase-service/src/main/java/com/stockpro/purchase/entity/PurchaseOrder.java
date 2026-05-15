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

    @Column(nullable = false)
    private int createdById;

    @Column(nullable = false)
    private String status;

    // Aggregate value of all line items
    private double totalAmount;

    // Set automatically when the PO is first created
    @Column(nullable = false, updatable = false)
    private LocalDate orderDate;

    private LocalDate expectedDate;

    private LocalDate receivedDate;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(unique = true)
    private String referenceNumber;

    // Captures rejection or cancellation reason when applicable
    private String rejectionReason;

    @PrePersist
    public void prePersist() {
        this.orderDate = LocalDate.now();
        if (this.status == null) {
            this.status = "DRAFT";
        }
    }
}
