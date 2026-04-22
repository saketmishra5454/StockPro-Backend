package com.stockpro.product.resource;

import com.stockpro.product.entity.Product;
import com.stockpro.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductResource {

    private final ProductService productService;


    @PostMapping
    public ResponseEntity<Product> createProduct(@RequestBody Product product) {
        Product saved = productService.createProduct(product);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }


     // Get all products in the catalogue (active and inactive).

    @GetMapping("/all")
    public ResponseEntity<List<Product>> getAllProducts() {
        return ResponseEntity.ok(productService.getAllProducts());
    }


     // Get one product by its database ID.

    @GetMapping("/{id}")
    public ResponseEntity<Product> getById(@PathVariable int id) {
        return ResponseEntity.ok(productService.getById(id));
    }


     // Get a product by its SKU code.
     // e.g. GET /api/products/sku/BOLT-M6-SS

    @GetMapping("/sku/{sku}")
    public ResponseEntity<Product> getBySku(@PathVariable String sku) {
        return productService.getBySku(sku)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }


     // Get all products in a category.
    //e.g. GET /api/products/category/Fasteners

    @GetMapping("/category/{category}")
    public ResponseEntity<List<Product>> getByCategory(@PathVariable String category) {
        return ResponseEntity.ok(productService.getByCategory(category));
    }


     //Get all products from a brand.
     // e.g. GET /api/products/brand/FastenTech

    @GetMapping("/brand/{brand}")
    public ResponseEntity<List<Product>> getByBrand(@PathVariable String brand) {
        return ResponseEntity.ok(productService.getByBrand(brand));
    }


     // Get a product by barcode — used by warehouse staff scanner.
     // e.g. GET /api/products/barcode/5901234123457

    @GetMapping("/barcode/{barcode}")
    public ResponseEntity<Product> getByBarcode(@PathVariable String barcode) {
        return productService.getByBarcode(barcode)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }


//     Search products by partial name, case-insensitive.
//     e.g. GET /api/products/search?name=bolt  finds "M6 Bolt", "Hex Bolt Kit"

    @GetMapping("/search")
    public ResponseEntity<List<Product>> searchProducts(@RequestParam String name) {
        return ResponseEntity.ok(productService.searchProducts(name));
    }


//      GET /api/products/low-stock

    @GetMapping("/low-stock")
    public ResponseEntity<List<Product>> getLowStockProducts() {
        return ResponseEntity.ok(productService.getLowStockProducts());
    }


//     * PUT /api/products/{id}

    @PutMapping("/{id}")
    public ResponseEntity<Product> updateProduct(
            @PathVariable int id,
            @RequestBody Product product) {
        return ResponseEntity.ok(productService.updateProduct(id, product));
    }


//     * PUT /api/products/deactivate/{id}
//     * Soft delete — sets isActive = false.


    @PutMapping("/deactivate/{id}")
    public ResponseEntity<Map<String, String>> deactivateProduct(@PathVariable int id) {
        productService.deactivateProduct(id);
        return ResponseEntity.ok(Map.of("message", "Product deactivated successfully"));
    }


//     DELETE /api/products/{id}
//     Hard delete — permanently removes the product record.

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteProduct(@PathVariable int id) {
        productService.deleteProduct(id);
        return ResponseEntity.ok(Map.of("message", "Product deleted successfully"));
    }
}