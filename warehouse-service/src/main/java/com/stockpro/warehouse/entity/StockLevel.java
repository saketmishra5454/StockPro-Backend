package com.stockpro.warehouse.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "stock_levels",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"warehouse_id", "product_id"},
                name = "uk_stock_warehouse_product"
        ))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockLevel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int stockId;

    @Column(name = "warehouse_id", nullable = false)
    private int warehouseId;

    @Column(name = "product_id", nullable = false)
    private int productId;

    @Column(nullable = false)
    private int quantity = 0;

    // Quantity reserved for pending orders or transfers
    @Column(nullable = false)
    private int reservedQuantity = 0;

    // Bin/aisle location within the warehouse (e.g. "A-12-3")
    private String location;

    // Automatically updated every time stock quantity changes
    private LocalDateTime lastUpdated;

    // Optimistic locking protects concurrent stock updates
    @Version
    private Long version;

    @Transient
    public int getAvailableQuantity() {
        return this.quantity - this.reservedQuantity;
    }

    @PrePersist
    @PreUpdate
    public void preUpdate() {
        this.lastUpdated = LocalDateTime.now();
    }
}
