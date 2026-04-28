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

    // +++++++++++++++++++++++++++++++ CREATE++++++++++++++++++++++++++++++++++++

    @PostMapping
    public ResponseEntity<Supplier> createSupplier(@RequestBody Supplier supplier) {
        Supplier saved = supplierService.createSupplier(supplier);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }


    //========================== READ==============================

    /**
     * GET /api/suppliers
     * Get all suppliers (active and inactive).
     */
    @GetMapping
    public ResponseEntity<List<Supplier>> getAllSuppliers() {
        return ResponseEntity.ok(supplierService.getAllSuppliers());
    }


     // GET /api/suppliers/active

    @GetMapping("/active")
    public ResponseEntity<List<Supplier>> getActiveSuppliers() {
        return ResponseEntity.ok(supplierService.getActiveSuppliers());
    }

    /**
     * GET /api/suppliers/{id}
     * Get one supplier by their database ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Supplier> getById(@PathVariable int id) {
        return ResponseEntity.ok(supplierService.getById(id));
    }

    // ++++++++++++++++++++++++++++++++++SEARCH++++++++++++++++++++++++++++++++


     // GET /api/suppliers/search?name=tech

    @GetMapping("/search")
    public ResponseEntity<List<Supplier>> searchByName(@RequestParam String name) {
        return ResponseEntity.ok(supplierService.searchSuppliers(name));
    }


     //Get all suppliers in a city.
     // e.g. GET /api/suppliers/city/Mumbai

    @GetMapping("/city/{city}")
    public ResponseEntity<List<Supplier>> getByCity(@PathVariable String city) {
        return ResponseEntity.ok(supplierService.getByCity(city));
    }


//      Get all suppliers in a country.
//      e.g. GET /api/suppliers/country/India

    @GetMapping("/country/{country}")
    public ResponseEntity<List<Supplier>> getByCountry(@PathVariable String country) {
        return ResponseEntity.ok(supplierService.getByCountry(country));
    }

    // ++++++++++++++++++++++++++ UPDATE+++++++++++++++++++++++++++


//      PUT /api/suppliers/{id}
//      Update supplier profile details.

    @PutMapping("/{id}")
    public ResponseEntity<Supplier> updateSupplier(
            @PathVariable int id,
            @RequestBody Supplier supplier) {
        return ResponseEntity.ok(supplierService.updateSupplier(id, supplier));
    }


    //+++++++++++++++++++++++ RATING++++++++++++++++++++++++++++++++


//      PUT /api/suppliers/{id}/rating
//      Submit a performance rating for a supplier after goods receipt.* Score must be between 1.0 and 5.0.

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

    // ==============================DEACTIVATE & DELETE==========================



//      PUT /api/suppliers/{id}/deactivate
//      Soft delete — sets isActive = false.

    @PutMapping("/{id}/deactivate")
    public ResponseEntity<Map<String, String>> deactivateSupplier(@PathVariable int id) {
        supplierService.deactivateSupplier(id);
        return ResponseEntity.ok(Map.of(
                "message", "Supplier deactivated successfully",
                "supplierId", String.valueOf(id)));
    }


//      DELETE /api/suppliers/{id}
//      Hard delete — permanently removes the supplier record.

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSupplier(@PathVariable int id) {
        supplierService.deleteSupplier(id);
        return ResponseEntity.noContent().build();
    }
}