package com.stockpro.supplier.repository;

import com.stockpro.supplier.entity.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface SupplierRepository extends JpaRepository<Supplier, Integer> {

    // Find all suppliers in a city — for geographic filtering
    List<Supplier> findByCity(String city);

    // Find all suppliers in a country
    List<Supplier> findByCountry(String country);

    // Search by partial name match, case-insensitive
    // e.g. "tech" finds "TechCorp", "FastTech Supplies", "TECH-IND"
    List<Supplier> findByNameContainingIgnoreCase(String name);

    // Get active suppliers (isActive=true) or deactivated (isActive=false)
    List<Supplier> findByIsActive(boolean isActive);

    // Find by tax ID — used to prevent duplicate supplier registrations
    Optional<Supplier> findByTaxId(String taxId);

    // Count active/inactive suppliers — used in admin dashboard stats
    long countByIsActive(boolean isActive);

    // Check if email already exists — prevent duplicate supplier emails
    boolean existsByEmail(String email);

    // Check if tax ID already exists — prevent duplicate registrations
    boolean existsByTaxId(String taxId);

    // Find suppliers with rating above a threshold — top performers
    List<Supplier> findByRatingGreaterThanEqual(double minRating);

    // Find by city AND active status together
    List<Supplier> findByCityAndIsActive(String city, boolean isActive);

    // Find by country AND active status together
    List<Supplier> findByCountryAndIsActive(String country, boolean isActive);
}