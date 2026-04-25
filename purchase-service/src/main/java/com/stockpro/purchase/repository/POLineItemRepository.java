package com.stockpro.purchase.repository;

import com.stockpro.purchase.entity.POLineItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * POLineItemRepository — all queries for POLineItem.
 */
@Repository
public interface POLineItemRepository extends JpaRepository<POLineItem, Integer> {

    // Get all line items for a PO — used when loading PO details
    List<POLineItem> findByPoId(int poId);

    // Get all line items for a specific product — used in movement tracking
    List<POLineItem> findByProductId(int productId);

    // Delete all line items when a PO is cancelled
    void deleteByPoId(int poId);


    @Query("SELECT COUNT(l) = 0 FROM POLineItem l " +
            "WHERE l.poId = :poId AND l.receivedQty < l.quantity")
    boolean areAllLinesFullyReceived(@Param("poId") int poId);
}