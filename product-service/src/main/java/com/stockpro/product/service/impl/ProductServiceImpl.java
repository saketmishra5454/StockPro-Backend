package com.stockpro.product.service.impl;

import com.stockpro.product.dto.StockLevelDto;
import com.stockpro.product.entity.Product;
import com.stockpro.product.exception.DuplicateProductException;
import com.stockpro.product.exception.ProductNotFoundException;
import com.stockpro.product.feign.WarehouseClient;
import com.stockpro.product.repository.ProductRepository;
import com.stockpro.product.service.ProductService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
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
    @Caching(
            put = @CachePut(cacheNames = "productById", key = "#result.productId"),
            evict = {
                    @CacheEvict(cacheNames = "products", allEntries = true),
                    @CacheEvict(cacheNames = "products-all", allEntries = true),
                    @CacheEvict(cacheNames = "productsAll", allEntries = true),
                    @CacheEvict(cacheNames = "productByCategory", allEntries = true),
                    @CacheEvict(cacheNames = "productByBrand", allEntries = true),
                    @CacheEvict(cacheNames = "productSearch", allEntries = true),
                    @CacheEvict(cacheNames = "productLowStock", allEntries = true)
            }
    )
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
    @Cacheable(cacheNames = {"productById", "products"}, key = "#productId")
    public Product getById(int productId) {
        validateId(productId, "Product ID");
        return productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
    }

    @Override
    @Cacheable(cacheNames = "productBySku", key = "#sku == null ? '' : #sku.trim()", unless = "#result == null || #result.isEmpty()")
    public Optional<Product> getBySku(String sku) {
        if (sku == null || sku.isBlank()) {
            throw new IllegalArgumentException("SKU is required.");
        }
        return productRepository.findBySku(sku);
    }

    // Get all products in a category
    @Override
    @Cacheable(cacheNames = "productByCategory", key = "#category == null ? '' : #category.trim()")
    public List<Product> getByCategory(String category) {
        if (category == null || category.isBlank()) {
            throw new IllegalArgumentException("Category is required.");
        }
        return productRepository.findByCategory(category);
    }

    @Override
    @Cacheable(cacheNames = "productByBrand", key = "#brand == null ? '' : #brand.trim()")
    public List<Product> getByBrand(String brand) {
        if (brand == null || brand.isBlank()) {
            throw new IllegalArgumentException("Brand is required.");
        }
        return productRepository.findByBrand(brand);
    }


    @Override
    @Cacheable(cacheNames = "productSearch", key = "#name == null ? '' : #name.trim()")
    public List<Product> searchProducts(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Search term is required.");
        }
        return productRepository.searchCatalog(name.trim());
    }

    @Override
    @Caching(
            put = @CachePut(cacheNames = "productById", key = "#productId"),
            evict = {
                    @CacheEvict(cacheNames = "products", allEntries = true),
                    @CacheEvict(cacheNames = "products-all", allEntries = true),
                    @CacheEvict(cacheNames = "productsAll", allEntries = true),
                    @CacheEvict(cacheNames = "productByCategory", allEntries = true),
                    @CacheEvict(cacheNames = "productByBrand", allEntries = true),
                    @CacheEvict(cacheNames = "productByBarcode", allEntries = true),
                    @CacheEvict(cacheNames = "productSearch", allEntries = true),
                    @CacheEvict(cacheNames = "productLowStock", allEntries = true)
            }
    )
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
    @Caching(evict = {
            @CacheEvict(cacheNames = "products", allEntries = true),
            @CacheEvict(cacheNames = "products-all", allEntries = true),
            @CacheEvict(cacheNames = "productById", key = "#productId"),
            @CacheEvict(cacheNames = "productsAll", allEntries = true),
            @CacheEvict(cacheNames = "productByCategory", allEntries = true),
            @CacheEvict(cacheNames = "productByBrand", allEntries = true),
            @CacheEvict(cacheNames = "productSearch", allEntries = true),
            @CacheEvict(cacheNames = "productLowStock", allEntries = true)
    })
    public void deactivateProduct(int productId) {
        validateId(productId, "Product ID");
        Product product = getById(productId);
        product.setActive(false);
        productRepository.save(product);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(cacheNames = "productById", key = "#productId"),
            @CacheEvict(cacheNames = "productsAll", allEntries = true),
            @CacheEvict(cacheNames = "productByCategory", allEntries = true),
            @CacheEvict(cacheNames = "productByBrand", allEntries = true),
            @CacheEvict(cacheNames = "productSearch", allEntries = true),
            @CacheEvict(cacheNames = "productLowStock", allEntries = true)
    })
    public void activateProduct(int productId) {
        validateId(productId, "Product ID");
        Product product = getById(productId);
        product.setActive(true);
        productRepository.save(product);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(cacheNames = "productById", key = "#productId"),
            @CacheEvict(cacheNames = "productsAll", allEntries = true),
            @CacheEvict(cacheNames = "productBySku", allEntries = true),
            @CacheEvict(cacheNames = "productByBarcode", allEntries = true),
            @CacheEvict(cacheNames = "productByCategory", allEntries = true),
            @CacheEvict(cacheNames = "productByBrand", allEntries = true),
            @CacheEvict(cacheNames = "productSearch", allEntries = true),
            @CacheEvict(cacheNames = "productLowStock", allEntries = true)
    })
    public void deleteProduct(int productId) {
        validateId(productId, "Product ID");
        if (!productRepository.existsById(productId)) {
            throw new ProductNotFoundException(productId);
        }
        productRepository.deleteById(productId);
    }

    @Override
    @Cacheable(cacheNames = {"productsAll", "products-all"}, key = "'all'")
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    @Override
    @Cacheable(cacheNames = "productByBarcode", key = "#barcode == null ? '' : #barcode.trim()", unless = "#result == null || #result.isEmpty()")
    public Optional<Product> getByBarcode(String barcode) {
        if (barcode == null || barcode.isBlank()) {
            throw new IllegalArgumentException("Barcode is required.");
        }
        return productRepository.findByBarcode(barcode);
    }

    @Override
    @Cacheable(cacheNames = "productLowStock", key = "'active-low-stock'")
    public List<Product> getLowStockProducts() {
        try {
            List<StockLevelDto> lowStockItems = fetchLowStockFromWarehouse();

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

    @CircuitBreaker(name = "warehouseService", fallbackMethod = "lowStockFallback")
    private List<StockLevelDto> fetchLowStockFromWarehouse() {
        return warehouseClient.getLowStockItems();
    }

    private List<StockLevelDto> lowStockFallback(Exception ex) {
        log.warn("Warehouse-service unavailable for low stock check: {}", ex.getMessage());
        return Collections.emptyList();
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
