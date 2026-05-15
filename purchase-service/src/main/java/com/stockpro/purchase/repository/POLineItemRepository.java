package com.stockpro.purchase.repository;

import com.stockpro.purchase.entity.POLineItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface POLineItemRepository extends JpaRepository<POLineItem, Integer> {

    List<POLineItem> findByPoId(int poId);

    List<POLineItem> findByProductId(int productId);
    
    
    void deleteByPoId(int poId);

    @Query("SELECT COUNT(l) = 0 FROM POLineItem l " +
            "WHERE l.poId = :poId AND l.receivedQty < l.quantity")
    boolean areAllLinesFullyReceived(@Param("poId") int poId);
}
