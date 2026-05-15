package com.stockpro.supplier.service;

import com.stockpro.supplier.entity.Supplier;

import java.util.List;

public interface SupplierService {

    Supplier createSupplier(Supplier supplier);

    Supplier getById(int supplierId);

    List<Supplier> getAllSuppliers();

    List<Supplier> searchSuppliers(String name);

    Supplier updateSupplier(int supplierId, Supplier updatedSupplier);

    void deactivateSupplier(int supplierId);

    void activateSupplier(int supplierId);

    void deleteSupplier(int supplierId);

    List<Supplier> getByCity(String city);

    List<Supplier> getByCountry(String country);

    Supplier updateRating(int supplierId, double scoreGiven);

    List<Supplier> getActiveSuppliers();
}
