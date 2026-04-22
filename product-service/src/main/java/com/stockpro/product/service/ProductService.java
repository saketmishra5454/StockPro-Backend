package com.stockpro.product.service;

import com.stockpro.product.entity.Product;

import java.util.List;
import java.util.Optional;

public interface ProductService {

    // Create a new product in the catalogue
    Product createProduct(Product product);

    // Get one product by its database ID
    Product getById(int productId);

    // Get one product by its SKU code (e.g. "BOLT-M6-SS")
    Optional<Product> getBySku(String sku);

    // Get all products in a category
    List<Product> getByCategory(String category);

    // Get all products from a brand
    List<Product> getByBrand(String brand);

    // Search products by partial name match, case-insensitive
    List<Product> searchProducts(String name);

    // Update product details
    Product updateProduct(int productId, Product updatedProduct);

    // Soft delete — sets isActive = false
    void deactivateProduct(int productId);

    // Hard delete — removes record from database permanently
    void deleteProduct(int productId);

    // Get all products
    List<Product> getAllProducts();

    // Get product by barcode
    Optional<Product> getByBarcode(String barcode);

    List<Product> getLowStockProducts();
}