package com.stockpro.supplier.resource;

import com.stockpro.supplier.entity.Supplier;
import com.stockpro.supplier.service.SupplierService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/suppliers")
@RequiredArgsConstructor
public class SupplierResource {

    private final SupplierService supplierService;

    @PostMapping
    public ResponseEntity<Supplier> createSupplier(@RequestBody Supplier supplier) {
        Supplier saved = supplierService.createSupplier(supplier);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }


    @GetMapping
    public ResponseEntity<List<Supplier>> getAllSuppliers() {
        return ResponseEntity.ok(supplierService.getAllSuppliers());
    }

    @GetMapping("/active")
    public ResponseEntity<List<Supplier>> getActiveSuppliers() {
        return ResponseEntity.ok(supplierService.getActiveSuppliers());
    }


    @GetMapping("/{id}")
    public ResponseEntity<Supplier> getById(@PathVariable int id) {
        return ResponseEntity.ok(supplierService.getById(id));
    }

    @GetMapping("/search")
    public ResponseEntity<List<Supplier>> searchByName(@RequestParam String name) {
        return ResponseEntity.ok(supplierService.searchSuppliers(name));
    }

    @GetMapping("/city/{city}")
    public ResponseEntity<List<Supplier>> getByCity(@PathVariable String city) {
        return ResponseEntity.ok(supplierService.getByCity(city));
    }

    @GetMapping("/country/{country}")
    public ResponseEntity<List<Supplier>> getByCountry(@PathVariable String country) {
        return ResponseEntity.ok(supplierService.getByCountry(country));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Supplier> updateSupplier(
            @PathVariable int id,
            @RequestBody Supplier supplier) {
        return ResponseEntity.ok(supplierService.updateSupplier(id, supplier));
    }

    @PutMapping("/{id}/rating")
    public ResponseEntity<Supplier> updateRating(
            @PathVariable int id,
            @RequestBody Map<String, Double> body) {

        Double score = body.get("score");

        if (score == null) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(supplierService.updateRating(id, score));
    }

    @PutMapping("/{id}/deactivate")
    public ResponseEntity<Map<String, String>> deactivateSupplier(@PathVariable int id) {
        supplierService.deactivateSupplier(id);
        return ResponseEntity.ok(Map.of(
                "message", "Supplier deactivated successfully",
                "supplierId", String.valueOf(id)));
    }

    @PutMapping("/{id}/activate")
    public ResponseEntity<Map<String, String>> activateSupplier(@PathVariable int id) {
        supplierService.activateSupplier(id);
        return ResponseEntity.ok(Map.of(
                "message", "Supplier activated successfully",
                "supplierId", String.valueOf(id)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSupplier(@PathVariable int id) {
        supplierService.deleteSupplier(id);
        return ResponseEntity.noContent().build();
    }
}
