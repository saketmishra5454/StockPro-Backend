package com.stockpro.product.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "products")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int productId;

    @Column(unique = true, nullable = false)
    private String sku;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String category;

    private String brand;

    private String unitOfMeasure;

    @Column(nullable = false)
    private double costPrice;

    @Column(nullable = false)
    private double sellingPrice;

    // Low-stock alert threshold
    private int reorderLevel;

    // Overstock alert threshold
    private int maxStockLevel;

    // Expected supplier lead time in days
    private int leadTimeDays;

    private String imageUrl;

    // Soft delete flag - inactive products remain available for history
    @Column(nullable = false)
    private boolean isActive = true;

    // Optional scanner identifier used by warehouse operations
    @Column(unique = true)
    private String barcode;

    @PrePersist
    public void prePersist() {
        this.isActive = true;
    }
}
