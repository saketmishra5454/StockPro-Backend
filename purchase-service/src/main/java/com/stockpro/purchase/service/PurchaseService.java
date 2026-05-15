package com.stockpro.purchase.service;

import com.stockpro.purchase.entity.POLineItem;
import com.stockpro.purchase.entity.PurchaseOrder;

import java.time.LocalDate;
import java.util.List;

public interface PurchaseService {

    PurchaseOrder createPO(PurchaseOrder purchaseOrder, List<POLineItem> lineItems);

    PurchaseOrder submitPO(int poId);

    PurchaseOrder approvePO(int poId);

    PurchaseOrder rejectPO(int poId, String reason);

    PurchaseOrder receiveGoods(int poId, List<POLineItem> receivedItems);

    PurchaseOrder cancelPO(int poId, String reason);

    PurchaseOrder updatePO(int poId, PurchaseOrder updated);

    PurchaseOrder getPOById(int poId);

    List<PurchaseOrder> getAllPOs();

    List<PurchaseOrder> getPOsBySupplier(int supplierId);

    List<PurchaseOrder> getPOsByStatus(String status);

    List<PurchaseOrder> getPOsByWarehouse(int warehouseId);

    List<PurchaseOrder> getPOsByCreator(int createdById);

    List<PurchaseOrder> getPOsByDateRange(LocalDate startDate, LocalDate endDate);

    List<POLineItem> getLineItemsByPO(int poId);
}
