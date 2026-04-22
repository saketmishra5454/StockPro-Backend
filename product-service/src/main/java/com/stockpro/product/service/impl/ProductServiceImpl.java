package com.stockpro.product.service.impl;

import com.stockpro.product.dto.StockLevelDto;
import com.stockpro.product.entity.Product;
import com.stockpro.product.feign.WarehouseClient;
import com.stockpro.product.repository.ProductRepository;
import com.stockpro.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final WarehouseClient warehouseClient;   // Feign client — calls warehouse-service


    // * Create a new product.


    @Override
    public Product createProduct(Product product) {
        // Prevent duplicate SKU
        if (product.getSku() != null &&
                productRepository.findBySku(product.getSku()).isPresent()) {
            throw new RuntimeException("A product with SKU '" + product.getSku() + "' already exists.");
        }
        return productRepository.save(product);
    }

     //Get product by integer ID.

    @Override
    public Product getById(int productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found with ID: " + productId));
    }

    @Override
    public Optional<Product> getBySku(String sku) {
        return productRepository.findBySku(sku);
    }

     // Get all products in a category.

    @Override
    public List<Product> getByCategory(String category) {
        return productRepository.findByCategory(category);
    }


     // Get all products from a brand.

    @Override
    public List<Product> getByBrand(String brand) {
        return productRepository.findByBrand(brand);
    }

    /**
     * Search products by partial name, case-insensitive.
     * e.g. "bolt" finds "M6 Bolt", "BOLT-SS", "hex bolt kit"
     */
    @Override
    public List<Product> searchProducts(String name) {
        return productRepository.findByNameContainingIgnoreCase(name);
    }


//      Update product — only updates non-null fields from the request.

    @Override
    public Product updateProduct(int productId, Product updatedProduct) {
        Product existing = getById(productId);

        if (updatedProduct.getName() != null)
            existing.setName(updatedProduct.getName());

        if (updatedProduct.getDescription() != null)
            existing.setDescription(updatedProduct.getDescription());

        if (updatedProduct.getCategory() != null)
            existing.setCategory(updatedProduct.getCategory());

        if (updatedProduct.getBrand() != null)
            existing.setBrand(updatedProduct.getBrand());

        if (updatedProduct.getUnitOfMeasure() != null)
            existing.setUnitOfMeasure(updatedProduct.getUnitOfMeasure());

        if (updatedProduct.getCostPrice() > 0)
            existing.setCostPrice(updatedProduct.getCostPrice());

        if (updatedProduct.getSellingPrice() > 0)
            existing.setSellingPrice(updatedProduct.getSellingPrice());

        if (updatedProduct.getReorderLevel() > 0)
            existing.setReorderLevel(updatedProduct.getReorderLevel());

        if (updatedProduct.getMaxStockLevel() > 0)
            existing.setMaxStockLevel(updatedProduct.getMaxStockLevel());

        if (updatedProduct.getLeadTimeDays() > 0)
            existing.setLeadTimeDays(updatedProduct.getLeadTimeDays());

        if (updatedProduct.getImageUrl() != null)
            existing.setImageUrl(updatedProduct.getImageUrl());

        if (updatedProduct.getBarcode() != null)
            existing.setBarcode(updatedProduct.getBarcode());

        return productRepository.save(existing);
    }


    @Override
    public void deactivateProduct(int productId) {
        Product product = getById(productId);
        product.setActive(false);
        productRepository.save(product);
    }

    @Override
    public void deleteProduct(int productId) {
        if (!productRepository.existsById(productId)) {
            throw new RuntimeException("Product not found with ID: " + productId);
        }
        productRepository.deleteById(productId);
    }

     // Get all products in the catalogue.

    @Override
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

     //Get product by barcode — used in warehouse scanner workflow.

    @Override
    public Optional<Product> getByBarcode(String barcode) {
        return productRepository.findByBarcode(barcode);
    }


    @Override
    public List<Product> getLowStockProducts() {
        try {
            // Ask warehouse-service for all stock levels below threshold
            List<StockLevelDto> lowStockItems = warehouseClient.getLowStockItems();

            if (lowStockItems == null || lowStockItems.isEmpty()) {
                return Collections.emptyList();
            }

           // Collect the productIds that have low stock
            Set<Integer> lowStockProductIds = lowStockItems.stream()
                    .map(s -> s.getProductId().intValue())
                    .collect(Collectors.toSet());

            //Fetch full product details for those IDs from our own DB
            return productRepository.findAll().stream()
                    .filter(p -> lowStockProductIds.contains(p.getProductId()))
                    .filter(Product::isActive)   // only show active products
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.warn("Could not fetch low stock data from warehouse-service: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}