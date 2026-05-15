package com.stockpro.supplier.repository;

import com.stockpro.supplier.entity.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, Integer> {

    List<Supplier> findByCity(String city);

    List<Supplier> findByCountry(String country);

    List<Supplier> findByNameContainingIgnoreCase(String name);

    @Query("""
            SELECT s FROM Supplier s
            WHERE LOWER(s.name) LIKE LOWER(CONCAT('%', :term, '%'))
               OR LOWER(COALESCE(s.city, '')) LIKE LOWER(CONCAT('%', :term, '%'))
               OR LOWER(COALESCE(s.country, '')) LIKE LOWER(CONCAT('%', :term, '%'))
               OR LOWER(COALESCE(s.email, '')) LIKE LOWER(CONCAT('%', :term, '%'))
            """)
    List<Supplier> searchDirectory(@Param("term") String term);

    List<Supplier> findByIsActive(boolean isActive);

    Optional<Supplier> findByTaxId(String taxId);

    long countByIsActive(boolean isActive);

    boolean existsByEmail(String email);

    boolean existsByTaxId(String taxId);

    List<Supplier> findByRatingGreaterThanEqual(double minRating);

    List<Supplier> findByCityAndIsActive(String city, boolean isActive);

    List<Supplier> findByCountryAndIsActive(String country, boolean isActive);
}
