package com.stockpro.report.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;


 // InventorySnapshot — a point-in-time record of stock quantities and values.

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

    // Stock quantity at time of snapshot
    @Column(nullable = false)
    private int quantity;

    // quantity × costPrice at time of snapshot
    // Stored so historical valuations remain accurate even if costPrice changes
    @Column(nullable = false)
    private double stockValue;

    // The date this snapshot was taken (yyyy-MM-dd)
    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate;

    // Exact timestamp of creation — for debugging and audit
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