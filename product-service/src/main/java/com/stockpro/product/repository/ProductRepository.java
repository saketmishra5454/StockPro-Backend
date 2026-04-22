package com.stockpro.product.repository;

import com.stockpro.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface ProductRepository extends JpaRepository<Product, Integer> {

    // Find by unique SKU code — returns Optional because it may not exist
    Optional<Product> findBySku(String sku);

    // Find all products in a category (e.g. "Electronics", "Fasteners")
    List<Product> findByCategory(String category);

    // Find all products from a specific brand
    List<Product> findByBrand(String brand);

    // Find active products (isActive=true) or inactive (isActive=false)
    List<Product> findByIsActive(boolean isActive);

    // Find by barcode — used by warehouse staff scanner
    Optional<Product> findByBarcode(String barcode);

    // Search products by partial name, case-insensitive
    // e.g. searchProducts("bolt") finds "M6 Bolt", "BOLT-SS", "hex bolt"
    List<Product> findByNameContainingIgnoreCase(String name);

    // Count products per category — used in analytics/reports
    long countByCategory(String category);

    // Find products whose reorderLevel is above a threshold
    // Used with warehouse stock data to identify low-stock products
    List<Product> findByReorderLevelGreaterThan(int level);
}