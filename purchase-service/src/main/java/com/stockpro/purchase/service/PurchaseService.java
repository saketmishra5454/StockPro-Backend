package com.stockpro.purchase.service;

import com.stockpro.purchase.entity.POLineItem;
import com.stockpro.purchase.entity.PurchaseOrder;

import java.time.LocalDate;
import java.util.List;


public interface PurchaseService {

    // ── PO Lifecycle ────────────────────────────────────────────────

    // Create a new PO — always starts with status DRAFT
    PurchaseOrder createPO(PurchaseOrder purchaseOrder, List<POLineItem> lineItems);

    // Submit for approval — DRAFT → PENDING, notifies approvers via AlertClient
    PurchaseOrder submitPO(int poId);

    // Approve — PENDING → APPROVED (only MANAGER or ADMIN role)
    PurchaseOrder approvePO(int poId);

    // Reject with reason — PENDING → REJECTED
    PurchaseOrder rejectPO(int poId, String reason);


    PurchaseOrder receiveGoods(int poId, List<POLineItem> receivedItems);

    // Cancel — only DRAFT or PENDING can be cancelled
    PurchaseOrder cancelPO(int poId, String reason);

    // Update PO details — only allowed when status is DRAFT
    PurchaseOrder updatePO(int poId, PurchaseOrder updated);

    // ── Query Methods ────────────────────────────────────────────────

    PurchaseOrder getPOById(int poId);

    List<PurchaseOrder> getAllPOs();

    List<PurchaseOrder> getPOsBySupplier(int supplierId);

    List<PurchaseOrder> getPOsByStatus(String status);

    List<PurchaseOrder> getPOsByWarehouse(int warehouseId);

    List<PurchaseOrder> getPOsByCreator(int createdById);

    List<PurchaseOrder> getPOsByDateRange(LocalDate startDate, LocalDate endDate);

    // Get all line items for a PO
    List<POLineItem> getLineItemsByPO(int poId);
}