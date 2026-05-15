package com.stockpro.product.service;

import com.stockpro.product.entity.Product;

import java.util.List;
import java.util.Optional;

public interface ProductService {

    Product createProduct(Product product);

    Product getById(int productId);

    Optional<Product> getBySku(String sku);

    List<Product> getByCategory(String category);

    List<Product> getByBrand(String brand);

    List<Product> searchProducts(String name);

    Product updateProduct(int productId, Product updatedProduct);

    void deactivateProduct(int productId);

    void activateProduct(int productId);

    void deleteProduct(int productId);

    List<Product> getAllProducts();

    Optional<Product> getByBarcode(String barcode);

    List<Product> getLowStockProducts();
}
