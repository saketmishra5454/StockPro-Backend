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

    private String contactPerson;

    @Column(unique = true)
    private String email;

    private String phone;

    @Column(columnDefinition = "TEXT")
    private String address;

    private String city;

    private String country;

    @Column(unique = true)
    private String taxId;

    // Payment terms agreed with this supplier (NET-30, NET-60, ADVANCE, etc.)
    private String paymentTerms;

    private int leadTimeDays;

    private double rating = 0.0;

    private int ratingCount = 0;

    // Soft delete flag - inactive suppliers cannot be used for new POs
    @Column(nullable = false)
    private boolean isActive = true;

    @PrePersist
    public void prePersist() {
        this.isActive = true;
        if (this.rating == 0.0) this.rating = 0.0;
        if (this.ratingCount == 0) this.ratingCount = 0;
    }
}
