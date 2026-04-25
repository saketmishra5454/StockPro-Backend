package com.stockpro.purchase.repository;

import com.stockpro.purchase.entity.PurchaseOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;


@Repository
public interface PurchaseRepository extends JpaRepository<PurchaseOrder, Integer> {

    // All POs from a specific supplier
    List<PurchaseOrder> findBySupplierId(int supplierId);

    // All POs with a specific status (DRAFT, PENDING, APPROVED, etc.)
    List<PurchaseOrder> findByStatus(String status);

    // All POs for a specific warehouse
    List<PurchaseOrder> findByWarehouseId(int warehouseId);

    // All POs created by a specific Purchase Officer
    List<PurchaseOrder> findByCreatedById(int createdById);

    // POs created within a date range — for reports and filtering
    List<PurchaseOrder> findByOrderDateBetween(LocalDate startDate, LocalDate endDate);

    // Find by supplier AND status together — e.g. all PENDING POs from supplier 3
    List<PurchaseOrder> findBySupplierIdAndStatus(int supplierId, String status);

    // Find by human-readable reference number
    Optional<PurchaseOrder> findByReferenceNumber(String referenceNumber);

    // Count POs by status — used for dashboard KPIs
    long countByStatus(String status);
}