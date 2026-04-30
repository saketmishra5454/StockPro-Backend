package com.stockpro.report.feign;

import com.stockpro.report.dto.ProductDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

/**
 * ProductClient — Feign HTTP client for product-service.
 *
 * FIXED endpoints:
 *   OLD: GET /api/products — wrong, returns list not single item, wrong path
 *   NEW:
 *     GET /api/products/{id}  — get single product with costPrice for snapshot
 *     GET /api/products/all   — get all products for batch reports
 *
 * FIXED return type: ProductDto instead of Map<String,Object>
 */
@FeignClient(name = "product-service")
public interface ProductClient {

    /**
     * Get one product by ID — used in takeSnapshot to get costPrice.
     * Matches: GET /api/products/{id} in ProductResource.
     */
    @GetMapping("/api/products/{id}")
    ProductDto getProductById(@PathVariable int id);

    /**
     * Get all products — used in batch reports (top/slow movers).
     * Matches: GET /api/products/all in ProductResource.
     */
    @GetMapping("/api/products/all")
    List<ProductDto> getAllProducts();
}