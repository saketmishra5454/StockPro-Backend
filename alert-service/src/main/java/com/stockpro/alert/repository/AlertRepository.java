package com.stockpro.alert.repository;

import com.stockpro.alert.entity.Alert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

@Repository
public interface AlertRepository extends JpaRepository<Alert, Integer> {

    List<Alert> findAllByOrderByCreatedAtDesc();

    List<Alert> findByRecipientIdOrderByCreatedAtDesc(int recipientId);

    List<Alert> findByRecipientIdInOrderByCreatedAtDesc(Collection<Integer> recipientIds);

    List<Alert> findByRecipientIdAndIsReadFalseOrderByCreatedAtDesc(int recipientId);

    long countByRecipientIdAndIsReadFalse(int recipientId);

    long countByRecipientIdInAndIsReadFalse(Collection<Integer> recipientIds);

    List<Alert> findByIsAcknowledgedFalseOrderByCreatedAtDesc();

    List<Alert> findByRecipientIdAndIsAcknowledgedFalseOrderByCreatedAtDesc(int recipientId);

    List<Alert> findByRecipientIdInAndIsAcknowledgedFalseOrderByCreatedAtDesc(Collection<Integer> recipientIds);

    List<Alert> findByTypeOrderByCreatedAtDesc(String type);

    List<Alert> findBySeverityOrderByCreatedAtDesc(String severity);

    List<Alert> findByRelatedProductIdOrderByCreatedAtDesc(int relatedProductId);

    List<Alert> findByRelatedWarehouseIdOrderByCreatedAtDesc(int relatedWarehouseId);

    @Modifying
    @Transactional
    @Query("UPDATE Alert a SET a.isRead = true WHERE (a.recipientId = :recipientId OR a.recipientId = 0) AND a.isRead = false")
    int markAllAsReadForRecipient(@Param("recipientId") int recipientId);

    @Query("SELECT COUNT(a) > 0 FROM Alert a WHERE a.relatedProductId = :productId " +
            "AND a.relatedWarehouseId = :warehouseId AND a.type = 'LOW_STOCK' " +
            "AND a.isAcknowledged = false")
    boolean existsUnacknowledgedLowStockAlert(
            @Param("productId") int productId,
            @Param("warehouseId") int warehouseId);

    @Query("SELECT COUNT(a) > 0 FROM Alert a WHERE a.relatedProductId = :productId " +
            "AND a.relatedWarehouseId = :warehouseId AND a.type = 'OVERSTOCK' " +
            "AND a.isAcknowledged = false")
    boolean existsUnacknowledgedOverstockAlert(
            @Param("productId") int productId,
            @Param("warehouseId") int warehouseId);
}
