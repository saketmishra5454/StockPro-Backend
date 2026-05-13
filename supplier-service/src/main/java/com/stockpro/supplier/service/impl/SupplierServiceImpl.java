package com.stockpro.supplier.service.impl;

import com.stockpro.supplier.entity.Supplier;
import com.stockpro.supplier.repository.SupplierRepository;
import com.stockpro.supplier.service.SupplierService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
@RequiredArgsConstructor
@Slf4j
public class SupplierServiceImpl implements SupplierService {

    private final SupplierRepository supplierRepository;


//     Create a new supplier.
//      Validates uniqueness of email and taxId before saving.

    @Override
    public Supplier createSupplier(Supplier supplier) {
        // Check for duplicate email
        if (supplier.getEmail() != null &&
                supplierRepository.existsByEmail(supplier.getEmail())) {
            throw new RuntimeException(
                    "A supplier with email '" + supplier.getEmail() + "' already exists.");
        }

        // Check for duplicate tax ID
        if (supplier.getTaxId() != null &&
                supplierRepository.existsByTaxId(supplier.getTaxId())) {
            throw new RuntimeException(
                    "A supplier with tax ID '" + supplier.getTaxId() + "' already exists.");
        }

        Supplier saved = supplierRepository.save(supplier);
        log.info("Supplier created: id={}, name={}", saved.getSupplierId(), saved.getName());
        return saved;
    }


//      Get one supplier by ID.
//      Throws RuntimeException if not found.

    @Override
    public Supplier getById(int supplierId) {
        return supplierRepository.findById(supplierId)
                .orElseThrow(() -> new RuntimeException(
                        "Supplier not found with ID: " + supplierId));
    }


//      Get all suppliers — active and inactive.
//      Used by Admin to see full supplier registry.

    @Override
    public List<Supplier> getAllSuppliers() {
        return supplierRepository.findAll();
    }


//      Search suppliers by partial name match, case-insensitive.
//     e.g. searchSuppliers("tech") returns "TechCorp", "FastTech", "TECH-IND"

    @Override
    public List<Supplier> searchSuppliers(String name) {
        return supplierRepository.findByNameContainingIgnoreCase(name);
    }


    @Override
    @Transactional
    public Supplier updateSupplier(int supplierId, Supplier updatedSupplier) {
        Supplier existing = getById(supplierId);

        if (updatedSupplier.getName() != null)
            existing.setName(updatedSupplier.getName());

        if (updatedSupplier.getContactPerson() != null)
            existing.setContactPerson(updatedSupplier.getContactPerson());

        if (updatedSupplier.getEmail() != null) {
            // Check email uniqueness if changing email
            if (!updatedSupplier.getEmail().equals(existing.getEmail()) &&
                    supplierRepository.existsByEmail(updatedSupplier.getEmail())) {
                throw new RuntimeException(
                        "Email '" + updatedSupplier.getEmail() + "' is already used by another supplier.");
            }
            existing.setEmail(updatedSupplier.getEmail());
        }

        if (updatedSupplier.getPhone() != null)
            existing.setPhone(updatedSupplier.getPhone());

        if (updatedSupplier.getAddress() != null)
            existing.setAddress(updatedSupplier.getAddress());

        if (updatedSupplier.getCity() != null)
            existing.setCity(updatedSupplier.getCity());

        if (updatedSupplier.getCountry() != null)
            existing.setCountry(updatedSupplier.getCountry());

        if (updatedSupplier.getPaymentTerms() != null)
            existing.setPaymentTerms(updatedSupplier.getPaymentTerms());

        if (updatedSupplier.getLeadTimeDays() > 0)
            existing.setLeadTimeDays(updatedSupplier.getLeadTimeDays());

        Supplier saved = supplierRepository.save(existing);
        log.info("Supplier updated: id={}", supplierId);
        return saved;
    }

//      Deactivate a supplier — soft delete.
//      Sets isActive = false. The record stays in the DB.

    @Override
    @Transactional
    public void deactivateSupplier(int supplierId) {
        Supplier supplier = getById(supplierId);
        supplier.setActive(false);
        supplierRepository.save(supplier);
        log.info("Supplier deactivated: id={}, name={}", supplierId, supplier.getName());
    }

    @Override
    @Transactional
    public void activateSupplier(int supplierId) {
        Supplier supplier = getById(supplierId);
        supplier.setActive(true);
        supplierRepository.save(supplier);
        log.info("Supplier activated: id={}, name={}", supplierId, supplier.getName());
    }


    @Override
    @Transactional
    public void deleteSupplier(int supplierId) {
        if (!supplierRepository.existsById(supplierId)) {
            throw new RuntimeException("Supplier not found with ID: " + supplierId);
        }
        supplierRepository.deleteById(supplierId);
        log.info("Supplier permanently deleted: id={}", supplierId);
    }


     // Get all suppliers in a specific city.

    @Override
    public List<Supplier> getByCity(String city) {
        return supplierRepository.findByCity(city);
    }


//      Get all suppliers in a specific country.
//     Used to filter domestic vs international suppliers.

    @Override
    public List<Supplier> getByCountry(String country) {
        return supplierRepository.findByCountry(country);
    }


     // Update supplier rating using weighted average formula.

    @Override
    @Transactional
    public Supplier updateRating(int supplierId, double scoreGiven) {
        // Validate score is within 1.0 to 5.0 range
        if (scoreGiven < 1.0 || scoreGiven > 5.0) {
            throw new RuntimeException(
                    "Rating score must be between 1.0 and 5.0. Received: " + scoreGiven);
        }

        Supplier supplier = getById(supplierId);

        double currentRating   = supplier.getRating();
        int    currentCount    = supplier.getRatingCount();

        // Weighted average formula
        double newRating = ((currentRating * currentCount) + scoreGiven) / (currentCount + 1);

        // Round to 2 decimal places for clean display
        newRating = Math.round(newRating * 100.0) / 100.0;

        supplier.setRating(newRating);
        supplier.setRatingCount(currentCount + 1);

        Supplier saved = supplierRepository.save(supplier);

        log.info("Supplier rating updated: id={}, newRating={}, totalRatings={}",
                supplierId, newRating, currentCount + 1);

        return saved;
    }


//      Get only active suppliers.
//      Used by purchase-service dropdown — only active suppliers can receive new POs.

    @Override
    public List<Supplier> getActiveSuppliers() {
        return supplierRepository.findByIsActive(true);
    }
}
