package com.stockpro.purchase.repository;

import com.stockpro.purchase.entity.PurchaseOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PurchaseRepository extends JpaRepository<PurchaseOrder, Integer> {

    List<PurchaseOrder> findBySupplierId(int supplierId);

    List<PurchaseOrder> findByStatus(String status);

    List<PurchaseOrder> findByWarehouseId(int warehouseId);

    List<PurchaseOrder> findByCreatedById(int createdById);

    List<PurchaseOrder> findByOrderDateBetween(LocalDate startDate, LocalDate endDate);

    List<PurchaseOrder> findBySupplierIdAndStatus(int supplierId, String status);

    Optional<PurchaseOrder> findByReferenceNumber(String referenceNumber);

    long countByStatus(String status);
}
