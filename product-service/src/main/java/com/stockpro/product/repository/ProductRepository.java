package com.stockpro.product.repository;

import com.stockpro.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Integer> {

    Optional<Product> findBySku(String sku);

    List<Product> findByCategory(String category);

    List<Product> findByBrand(String brand);

    List<Product> findByIsActive(boolean isActive);

    Optional<Product> findByBarcode(String barcode);

    List<Product> findByNameContainingIgnoreCase(String name);

    @Query("""
            SELECT p FROM Product p
            WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :term, '%'))
               OR LOWER(p.sku) LIKE LOWER(CONCAT('%', :term, '%'))
               OR LOWER(COALESCE(p.category, '')) LIKE LOWER(CONCAT('%', :term, '%'))
               OR LOWER(COALESCE(p.brand, '')) LIKE LOWER(CONCAT('%', :term, '%'))
               OR LOWER(COALESCE(p.barcode, '')) LIKE LOWER(CONCAT('%', :term, '%'))
            """)
    List<Product> searchCatalog(@Param("term") String term);

    long countByCategory(String category);

    List<Product> findByReorderLevelGreaterThan(int level);
}
