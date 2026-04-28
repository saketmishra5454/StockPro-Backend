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

    // SKU must be unique — no two products can have the same SKU
    @Column(unique = true, nullable = false)
    private String sku;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String category;

    private String brand;

    // Unit of measure: "pcs", "kg", "litre", "box", etc.
    private String unitOfMeasure;

    // What we pay the supplier per unit
    @Column(nullable = false)
    private double costPrice;

    // What we charge the customer per unit
    @Column(nullable = false)
    private double sellingPrice;

    // Low-stock alert threshold — alert fires when stock < reorderLevel
    private int reorderLevel;

    // Overstock alert threshold — alert fires when stock > maxStockLevel
    private int maxStockLevel;

    // Days between placing a PO and receiving goods
    private int leadTimeDays;

    // URL to product image stored in S3 or local storage
    private String imageUrl;

    // Soft delete — false = deactivated, record stays in DB
    @Column(nullable = false)
    private boolean isActive = true;

    // Barcode string — used by warehouse staff scanner
    @Column(unique = true)
    private String barcode;

    // Auto-set isActive = true when product is first created
    @PrePersist
    public void prePersist() {
        this.isActive = true;
    }
}