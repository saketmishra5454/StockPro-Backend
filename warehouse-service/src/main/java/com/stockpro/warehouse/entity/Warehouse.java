package com.stockpro.warehouse.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "warehouses")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Warehouse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int warehouseId;

    @Column(nullable = false)
    private String name;

    // City or region (e.g. "Mumbai", "North Zone")
    private String location;

    // Full street address
    @Column(columnDefinition = "TEXT")
    private String address;

    // FK to users table in auth-service (no JPA join — different DB)
    private int managerId;

    // Total storage capacity in units
    private int capacity;

    // How much capacity is currently used
    private int usedCapacity;

    // Soft delete — false = closed warehouse
    @Column(nullable = false)
    private boolean isActive = true;

    private String phone;

    // Auto-set when warehouse record is first created
    @Column(nullable = false, updatable = false)
    private LocalDate createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDate.now();
        this.isActive = true;
    }
}