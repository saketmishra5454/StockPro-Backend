package com.stockpro.supplier.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "suppliers")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Supplier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int supplierId;

    @Column(nullable = false)
    private String name;

    // Primary contact person at the supplier company
    private String contactPerson;

    // Business email for orders and communication
    @Column(unique = true)
    private String email;

    private String phone;

    @Column(columnDefinition = "TEXT")
    private String address;

    // City — used for geographic search and filtering
    private String city;

    // Country — used for geographic search and import/export tracking
    private String country;

    // Tax/GST registration number — unique per supplier
    @Column(unique = true)
    private String taxId;

    // Payment terms agreed with this supplier (NET-30, NET-60, ADVANCE, etc.)
    private String paymentTerms;

    // Expected days from PO creation to goods receipt
    private int leadTimeDays;

    // Average performance rating — calculated using weighted average
    private double rating = 0.0;

    // Total number of ratings received — needed for weighted average calculation
    private int ratingCount = 0;

    // Soft delete — false = supplier is deactivated, no new POs allowed
    @Column(nullable = false)
    private boolean isActive = true;

    @PrePersist
    public void prePersist() {
        this.isActive = true;
        if (this.rating == 0.0) this.rating = 0.0;
        if (this.ratingCount == 0) this.ratingCount = 0;
    }
}