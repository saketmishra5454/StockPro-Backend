package com.stockpro.report.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_snapshots",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"warehouse_id", "product_id", "snapshot_date"},
                name = "uk_snapshot_warehouse_product_date"))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InventorySnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int snapshotId;

    @Column(name = "warehouse_id", nullable = false)
    private int warehouseId;

    @Column(name = "product_id", nullable = false)
    private int productId;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private double stockValue;

    // Business date represented by this inventory position
    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate;

    // Persistence timestamp for audit and troubleshooting
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        if (this.snapshotDate == null) {
            this.snapshotDate = LocalDate.now();
        }
    }
}
