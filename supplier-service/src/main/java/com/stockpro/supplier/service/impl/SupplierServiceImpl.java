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

    @Override
    public Supplier createSupplier(Supplier supplier) {
        validateSupplier(supplier);
        supplier.setName(supplier.getName().trim());
        supplier.setEmail(blankToNull(supplier.getEmail()));
        supplier.setTaxId(blankToNull(supplier.getTaxId()));

        if (supplier.getEmail() != null &&
                supplierRepository.existsByEmail(supplier.getEmail())) {
            throw new RuntimeException(
                    "A supplier with email '" + supplier.getEmail() + "' already exists.");
        }

        if (supplier.getTaxId() != null &&
                supplierRepository.existsByTaxId(supplier.getTaxId())) {
            throw new RuntimeException(
                    "A supplier with tax ID '" + supplier.getTaxId() + "' already exists.");
        }

        Supplier saved = supplierRepository.save(supplier);
        log.info("Supplier created: id={}, name={}", saved.getSupplierId(), saved.getName());
        return saved;
    }

    @Override
    public Supplier getById(int supplierId) {
        return supplierRepository.findById(supplierId)
                .orElseThrow(() -> new RuntimeException(
                        "Supplier not found with ID: " + supplierId));
    }

    @Override
    public List<Supplier> getAllSuppliers() {
        return supplierRepository.findAll();
    }

    @Override
    public List<Supplier> searchSuppliers(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Search term is required.");
        }
        return supplierRepository.searchDirectory(name.trim());
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

        if (updatedSupplier.getTaxId() != null) {
            String taxId = blankToNull(updatedSupplier.getTaxId());
            if (taxId != null && !taxId.equals(existing.getTaxId()) && supplierRepository.existsByTaxId(taxId)) {
                throw new RuntimeException("Tax ID '" + taxId + "' is already used by another supplier.");
            }
            existing.setTaxId(taxId);
        }

        Supplier saved = supplierRepository.save(existing);
        log.info("Supplier updated: id={}", supplierId);
        return saved;
    }

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

    @Override
    public List<Supplier> getByCity(String city) {
        return supplierRepository.findByCity(city);
    }

    @Override
    public List<Supplier> getByCountry(String country) {
        return supplierRepository.findByCountry(country);
    }

    @Override
    @Transactional
    public Supplier updateRating(int supplierId, double scoreGiven) {
        if (scoreGiven < 1.0 || scoreGiven > 5.0) {
            throw new RuntimeException(
                    "Rating score must be between 1.0 and 5.0. Received: " + scoreGiven);
        }

        Supplier supplier = getById(supplierId);

        double currentRating   = supplier.getRating();
        int    currentCount    = supplier.getRatingCount();

        double newRating = ((currentRating * currentCount) + scoreGiven) / (currentCount + 1);

        newRating = Math.round(newRating * 100.0) / 100.0;

        supplier.setRating(newRating);
        supplier.setRatingCount(currentCount + 1);

        Supplier saved = supplierRepository.save(supplier);

        log.info("Supplier rating updated: id={}, newRating={}, totalRatings={}",
                supplierId, newRating, currentCount + 1);

        return saved;
    }

    @Override
    public List<Supplier> getActiveSuppliers() {
        return supplierRepository.findByIsActive(true);
    }

    private void validateSupplier(Supplier supplier) {
        if (supplier == null) {
            throw new IllegalArgumentException("Supplier details are required.");
        }
        if (supplier.getName() == null || supplier.getName().isBlank()) {
            throw new IllegalArgumentException("Supplier name is required.");
        }
        if (supplier.getLeadTimeDays() < 0) {
            throw new IllegalArgumentException("Lead time cannot be negative.");
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
