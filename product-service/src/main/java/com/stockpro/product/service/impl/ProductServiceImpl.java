package com.stockpro.product.service.impl;

import com.stockpro.product.dto.StockLevelDto;
import com.stockpro.product.entity.Product;
import com.stockpro.product.exception.DuplicateProductException;
import com.stockpro.product.exception.ProductNotFoundException;
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
    private final WarehouseClient warehouseClient;

    @Override
    public Product createProduct(Product product) {
        validateProductForCreate(product);
        product.setSku(product.getSku().trim());
        product.setName(product.getName().trim());
        product.setBarcode(blankToNull(product.getBarcode()));

        if (productRepository.findBySku(product.getSku()).isPresent()) {
            throw new DuplicateProductException("A product with SKU '" + product.getSku() + "' already exists.");
        }
        if (product.getBarcode() != null && productRepository.findByBarcode(product.getBarcode()).isPresent()) {
            throw new DuplicateProductException("A product with barcode '" + product.getBarcode() + "' already exists.");
        }
        return productRepository.save(product);
    }

    @Override
    public Product getById(int productId) {
        validateId(productId, "Product ID");
        return productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
    }

    @Override
    public Optional<Product> getBySku(String sku) {
        if (sku == null || sku.isBlank()) {
            throw new IllegalArgumentException("SKU is required.");
        }
        return productRepository.findBySku(sku);
    }

    // Get all products in a category
    @Override
    public List<Product> getByCategory(String category) {
        if (category == null || category.isBlank()) {
            throw new IllegalArgumentException("Category is required.");
        }
        return productRepository.findByCategory(category);
    }

    @Override
    public List<Product> getByBrand(String brand) {
        if (brand == null || brand.isBlank()) {
            throw new IllegalArgumentException("Brand is required.");
        }
        return productRepository.findByBrand(brand);
    }


    @Override
    public List<Product> searchProducts(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Search term is required.");
        }
        return productRepository.searchCatalog(name.trim());
    }

    @Override
    public Product updateProduct(int productId, Product updatedProduct) {
        validateId(productId, "Product ID");
        if (updatedProduct == null) {
            throw new IllegalArgumentException("Product update payload is required.");
        }
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

        if (updatedProduct.getBarcode() != null) {
            String barcode = blankToNull(updatedProduct.getBarcode());
            if (barcode != null) {
                productRepository.findByBarcode(barcode)
                        .filter(product -> product.getProductId() != productId)
                        .ifPresent(product -> {
                            throw new DuplicateProductException("A product with barcode '" + barcode + "' already exists.");
                        });
            }
            existing.setBarcode(barcode);
        }

        validateStockThresholds(existing);

        return productRepository.save(existing);
    }

    @Override
    public void deactivateProduct(int productId) {
        validateId(productId, "Product ID");
        Product product = getById(productId);
        product.setActive(false);
        productRepository.save(product);
    }

    @Override
    public void activateProduct(int productId) {
        validateId(productId, "Product ID");
        Product product = getById(productId);
        product.setActive(true);
        productRepository.save(product);
    }

    @Override
    public void deleteProduct(int productId) {
        validateId(productId, "Product ID");
        if (!productRepository.existsById(productId)) {
            throw new ProductNotFoundException(productId);
        }
        productRepository.deleteById(productId);
    }

    @Override
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    @Override
    public Optional<Product> getByBarcode(String barcode) {
        if (barcode == null || barcode.isBlank()) {
            throw new IllegalArgumentException("Barcode is required.");
        }
        return productRepository.findByBarcode(barcode);
    }

    @Override
    public List<Product> getLowStockProducts() {
        try {
            List<StockLevelDto> lowStockItems = warehouseClient.getLowStockItems();

            if (lowStockItems == null || lowStockItems.isEmpty()) {
                return Collections.emptyList();
            }

            Set<Integer> lowStockProductIds = lowStockItems.stream()
                    .map(s -> s.getProductId().intValue())
                    .collect(Collectors.toSet());

            return productRepository.findAll().stream()
                    .filter(p -> lowStockProductIds.contains(p.getProductId()))
                    .filter(Product::isActive)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.warn("Could not fetch low stock data from warehouse-service: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private void validateProductForCreate(Product product) {
        if (product == null) {
            throw new IllegalArgumentException("Product payload is required.");
        }
        if (product.getSku() == null || product.getSku().isBlank()) {
            throw new IllegalArgumentException("SKU is required.");
        }
        if (product.getName() == null || product.getName().isBlank()) {
            throw new IllegalArgumentException("Product name is required.");
        }
        validatePrices(product);
        validateStockThresholds(product);
    }

    private void validatePrices(Product product) {
        if (product.getCostPrice() < 0) {
            throw new IllegalArgumentException("Cost price cannot be negative.");
        }
        if (product.getSellingPrice() < 0) {
            throw new IllegalArgumentException("Selling price cannot be negative.");
        }
    }

    private void validateStockThresholds(Product product) {
        if (product.getReorderLevel() < 0) {
            throw new IllegalArgumentException("Reorder level cannot be negative.");
        }
        if (product.getMaxStockLevel() < 0) {
            throw new IllegalArgumentException("Maximum stock level cannot be negative.");
        }
        if (product.getMaxStockLevel() > 0 && product.getReorderLevel() > product.getMaxStockLevel()) {
            throw new IllegalArgumentException("Reorder level cannot exceed maximum stock level.");
        }
        if (product.getLeadTimeDays() < 0) {
            throw new IllegalArgumentException("Lead time cannot be negative.");
        }
    }

    private void validateId(int id, String fieldName) {
        if (id <= 0) {
            throw new IllegalArgumentException(fieldName + " must be greater than 0.");
        }
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
