package com.stockpro.warehouse.repository;

import com.stockpro.warehouse.entity.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface WarehouseRepository extends JpaRepository<Warehouse, Integer> {

    // Find all active or inactive warehouses
    List<Warehouse> findByIsActive(boolean isActive);

    // Find warehouse managed by a specific user
    List<Warehouse> findByManagerId(int managerId);

    // Find warehouses in a city/region
    List<Warehouse> findByLocation(String location);

    // Check if a warehouse name already exists (prevent duplicates)
    boolean existsByName(String name);

    // Count active warehouses
    long countByIsActive(boolean isActive);
}