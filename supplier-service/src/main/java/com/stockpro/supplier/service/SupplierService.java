package com.stockpro.supplier.service;

import com.stockpro.supplier.entity.Supplier;

import java.util.List;


public interface SupplierService {

    // Create a new supplier profile
    Supplier createSupplier(Supplier supplier);

    // Get one supplier by database ID
    Supplier getById(int supplierId);

    // Get all suppliers (active and inactive)
    List<Supplier> getAllSuppliers();

    // Search by partial name — case-insensitive
    List<Supplier> searchSuppliers(String name);

    // Update supplier profile details
    Supplier updateSupplier(int supplierId, Supplier updatedSupplier);

    // Soft delete — sets isActive = false
    // Deactivated suppliers cannot receive new POs
    void deactivateSupplier(int supplierId);

    void activateSupplier(int supplierId);

    // Hard delete — permanently removes the record
    // Use only for suppliers with no PO history
    void deleteSupplier(int supplierId);

    // Get all suppliers in a city
    List<Supplier> getByCity(String city);

    // Get all suppliers in a country
    List<Supplier> getByCountry(String country);


    Supplier updateRating(int supplierId, double scoreGiven);

    // Get only active suppliers — used when creating new POs
    List<Supplier> getActiveSuppliers();
}
