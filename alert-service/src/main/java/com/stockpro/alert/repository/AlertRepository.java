package com.stockpro.alert.repository;

import com.stockpro.alert.entity.Alert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


 // AlertRepository — all queries for Alert records.

@Repository
public interface AlertRepository extends JpaRepository<Alert, Integer> {

    // All alerts for a specific recipient — their notification inbox
    List<Alert> findByRecipientIdOrderByCreatedAtDesc(int recipientId);

    // Unread alerts for a recipient — drives the notification badge count
    List<Alert> findByRecipientIdAndIsReadFalseOrderByCreatedAtDesc(int recipientId);

    // Count unread alerts — for notification badge number
    long countByRecipientIdAndIsReadFalse(int recipientId);

    // All unacknowledged alerts — still in pending action queue
    List<Alert> findByIsAcknowledgedFalseOrderByCreatedAtDesc();

    // Unacknowledged alerts for a specific recipient
    List<Alert> findByRecipientIdAndIsAcknowledgedFalseOrderByCreatedAtDesc(int recipientId);

    // Filter by alert type
    List<Alert> findByTypeOrderByCreatedAtDesc(String type);

    // Filter by severity — find all CRITICAL alerts
    List<Alert> findBySeverityOrderByCreatedAtDesc(String severity);

    // Alerts related to a specific product — for product detail view
    List<Alert> findByRelatedProductIdOrderByCreatedAtDesc(int relatedProductId);

    // Alerts related to a specific warehouse
    List<Alert> findByRelatedWarehouseIdOrderByCreatedAtDesc(int relatedWarehouseId);


     // @Modifying + @Transactional required for UPDATE/DELETE @Query.

    @Modifying
    @Transactional
    @Query("UPDATE Alert a SET a.isRead = true WHERE a.recipientId = :recipientId AND a.isRead = false")
    int markAllAsReadForRecipient(@Param("recipientId") int recipientId);

    // Check if a LOW_STOCK alert already exists for this product+warehouse
    // Used to prevent duplicate alerts for the same issue
    @Query("SELECT COUNT(a) > 0 FROM Alert a WHERE a.relatedProductId = :productId " +
            "AND a.relatedWarehouseId = :warehouseId AND a.type = 'LOW_STOCK' " +
            "AND a.isAcknowledged = false")
    boolean existsUnacknowledgedLowStockAlert(
            @Param("productId") int productId,
            @Param("warehouseId") int warehouseId);
}