package com.stockpro.purchase.service.impl;

import com.stockpro.purchase.dto.AlertRequest;
import com.stockpro.purchase.entity.POLineItem;
import com.stockpro.purchase.entity.PurchaseOrder;
import com.stockpro.purchase.feign.AlertClient;
import com.stockpro.purchase.feign.WarehouseClient;
import com.stockpro.purchase.repository.POLineItemRepository;
import com.stockpro.purchase.repository.PurchaseRepository;
import com.stockpro.purchase.service.PurchaseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PurchaseServiceImpl implements PurchaseService {

    private final PurchaseRepository purchaseRepository;
    private final POLineItemRepository lineItemRepository;
    private final WarehouseClient warehouseClient;
    private final AlertClient alertClient;

    @Override
    @Transactional
    public PurchaseOrder createPO(PurchaseOrder purchaseOrder, List<POLineItem> lineItems) {
        if (purchaseOrder == null) {
            throw new IllegalArgumentException("Purchase order details are required.");
        }

        List<POLineItem> safeLineItems = lineItems == null ? Collections.emptyList() : lineItems;
        if (safeLineItems.isEmpty()) {
            throw new IllegalArgumentException("At least one purchase order line item is required.");
        }

        validatePurchaseOrderHeader(purchaseOrder);

        // New purchase orders always enter the approval workflow as drafts
        purchaseOrder.setStatus("DRAFT");
        purchaseOrder.setTotalAmount(0);
        if (purchaseOrder.getReferenceNumber() == null || purchaseOrder.getReferenceNumber().isBlank()) {
            purchaseOrder.setReferenceNumber(generateReferenceNumber());
        } else {
            purchaseOrder.setReferenceNumber(purchaseOrder.getReferenceNumber().trim());
        }

        PurchaseOrder savedPO = purchaseRepository.save(purchaseOrder);

        double totalAmount = 0;
        for (POLineItem item : safeLineItems) {
            validateLineItem(item);
            item.setPoId(savedPO.getPoId());
            item.setReceivedQty(0);
            item.setTotalCost(item.getQuantity() * item.getUnitCost());
            lineItemRepository.save(item);
            totalAmount += item.getTotalCost();
        }

        savedPO.setTotalAmount(totalAmount);
        return purchaseRepository.save(savedPO);
    }

    @Override
    @Transactional
    public PurchaseOrder submitPO(int poId) {
        PurchaseOrder po = getPOById(poId);

        if (!"DRAFT".equals(po.getStatus())) {
            throw new RuntimeException(
                    "Cannot submit PO. Current status is " + po.getStatus() +
                            ". Only DRAFT POs can be submitted.");
        }

        po.setStatus("PENDING");
        PurchaseOrder saved = purchaseRepository.save(po);

        sendAlert(0, "PO_PENDING", "WARNING",
                "PO Requires Approval",
                "Purchase Order #" + po.getReferenceNumber() +
                        " has been submitted and requires your approval.",
                po.getWarehouseId());

        log.info("PO {} submitted for approval", poId);
        return saved;
    }

    @Override
    @Transactional
    public PurchaseOrder approvePO(int poId) {
        PurchaseOrder po = getPOById(poId);

        if (!"PENDING".equals(po.getStatus())) {
            throw new RuntimeException(
                    "Cannot approve PO. Current status is " + po.getStatus() +
                            ". Only PENDING POs can be approved.");
        }

        po.setStatus("APPROVED");
        PurchaseOrder saved = purchaseRepository.save(po);

        sendAlert(po.getCreatedById(), "PO_APPROVED", "INFO",
                "PO Approved",
                "Your Purchase Order #" + po.getReferenceNumber() + " has been approved.",
                po.getWarehouseId());

        log.info("PO {} approved", poId);
        return saved;
    }

    @Override
    @Transactional
    public PurchaseOrder rejectPO(int poId, String reason) {
        PurchaseOrder po = getPOById(poId);

        if (!"PENDING".equals(po.getStatus())) {
            throw new RuntimeException(
                    "Cannot reject PO. Current status is " + po.getStatus() +
                            ". Only PENDING POs can be rejected.");
        }

        po.setStatus("REJECTED");
        po.setRejectionReason(reason);
        PurchaseOrder saved = purchaseRepository.save(po);

        sendAlert(po.getCreatedById(), "PO_PENDING", "WARNING",
                "PO Rejected",
                "Your Purchase Order #" + po.getReferenceNumber() +
                        " was rejected. Reason: " + reason,
                po.getWarehouseId());

        log.info("PO {} rejected. Reason: {}", poId, reason);
        return saved;
    }

    @Override
    @Transactional
    public PurchaseOrder receiveGoods(int poId, List<POLineItem> receivedItems) {
        PurchaseOrder po = getPOById(poId);
        List<POLineItem> safeReceivedItems = receivedItems == null ? Collections.emptyList() : receivedItems;

        if (!"APPROVED".equals(po.getStatus()) &&
                !"PARTIALLY_RECEIVED".equals(po.getStatus())) {
            throw new RuntimeException(
                    "Cannot receive goods. PO status is " + po.getStatus() +
                            ". PO must be APPROVED or PARTIALLY_RECEIVED.");
        }

        if (safeReceivedItems.isEmpty()) {
            throw new IllegalArgumentException("At least one received line item is required.");
        }

        List<POLineItem> existingLines = lineItemRepository.findByPoId(poId);
        if (existingLines.isEmpty()) {
            throw new IllegalArgumentException("Purchase order has no line items to receive.");
        }

        for (POLineItem received : safeReceivedItems) {
            POLineItem matchingLine = existingLines.stream()
                    .filter(l -> l.getProductId() == received.getProductId())
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException(
                            "Product " + received.getProductId() +
                            " is not in this PO's line items."));

            int qtyToReceive = received.getReceivedQty();
            if (qtyToReceive <= 0) {
                throw new IllegalArgumentException("Received quantity must be greater than 0.");
            }

            int remaining = matchingLine.getRemainingQty();
            if (qtyToReceive > remaining) {
                throw new RuntimeException(
                        "Cannot receive " + qtyToReceive + " units of product " +
                                received.getProductId() + ". Remaining to receive: " + remaining);
            }

            try {
                // Receiving goods increases warehouse stock before PO receipt is persisted
                warehouseClient.updateStock(po.getWarehouseId(),
                        received.getProductId(), qtyToReceive);
                log.info("Stock updated: product={}, warehouse={}, qty=+{}",
                        received.getProductId(), po.getWarehouseId(), qtyToReceive);
            } catch (Exception e) {
                log.error("Failed to update stock for product {}: {}",
                        received.getProductId(), e.getMessage());
                throw new IllegalStateException(
                        "Could not update warehouse stock for received product " + received.getProductId(), e);
            }

            matchingLine.setReceivedQty(matchingLine.getReceivedQty() + qtyToReceive);
            lineItemRepository.save(matchingLine);
        }

        // PO status reflects aggregate receipt state across all line items
        boolean allReceived = lineItemRepository.areAllLinesFullyReceived(poId);

        if (allReceived) {
            po.setStatus("RECEIVED");
            po.setReceivedDate(LocalDate.now());
            log.info("PO {} fully received", poId);
        } else {
            po.setStatus("PARTIALLY_RECEIVED");
            log.info("PO {} partially received — more deliveries expected", poId);
        }

        return purchaseRepository.save(po);
    }

    @Override
    @Transactional
    public PurchaseOrder cancelPO(int poId, String reason) {
        PurchaseOrder po = getPOById(poId);

        if (!"DRAFT".equals(po.getStatus()) && !"PENDING".equals(po.getStatus())) {
            throw new RuntimeException(
                    "Cannot cancel PO. Current status is " + po.getStatus() +
                            ". Only DRAFT or PENDING POs can be cancelled.");
        }

        po.setStatus("CANCELLED");
        po.setRejectionReason(reason);
        PurchaseOrder saved = purchaseRepository.save(po);

        log.info("PO {} cancelled. Reason: {}", poId, reason);
        return saved;
    }

    @Override
    @Transactional
    public PurchaseOrder updatePO(int poId, PurchaseOrder updated) {
        PurchaseOrder existing = getPOById(poId);

        if (!"DRAFT".equals(existing.getStatus())) {
            throw new RuntimeException(
                    "Cannot update PO. Only DRAFT POs can be edited.");
        }

        if (updated.getSupplierId() > 0)    existing.setSupplierId(updated.getSupplierId());
        if (updated.getWarehouseId() > 0)   existing.setWarehouseId(updated.getWarehouseId());
        if (updated.getExpectedDate() != null) existing.setExpectedDate(updated.getExpectedDate());
        if (updated.getNotes() != null)     existing.setNotes(updated.getNotes());

        return purchaseRepository.save(existing);
    }

    @Override
    public PurchaseOrder getPOById(int poId) {
        return purchaseRepository.findById(poId)
                .orElseThrow(() -> new RuntimeException("Purchase Order not found: " + poId));
    }

    @Override
    public List<PurchaseOrder> getAllPOs() {
        return purchaseRepository.findAll();
    }

    @Override
    public List<PurchaseOrder> getPOsBySupplier(int supplierId) {
        return purchaseRepository.findBySupplierId(supplierId);
    }

    @Override
    public List<PurchaseOrder> getPOsByStatus(String status) {
        return purchaseRepository.findByStatus(status);
    }

    @Override
    public List<PurchaseOrder> getPOsByWarehouse(int warehouseId) {
        return purchaseRepository.findByWarehouseId(warehouseId);
    }

    @Override
    public List<PurchaseOrder> getPOsByCreator(int createdById) {
        return purchaseRepository.findByCreatedById(createdById);
    }

    @Override
    public List<PurchaseOrder> getPOsByDateRange(LocalDate startDate, LocalDate endDate) {
        return purchaseRepository.findByOrderDateBetween(startDate, endDate);
    }

    @Override
    public List<POLineItem> getLineItemsByPO(int poId) {
        return lineItemRepository.findByPoId(poId);
    }

    private void sendAlert(int recipientId, String type, String severity,
                           String title, String message, int warehouseId) {
        try {
            AlertRequest alert = new AlertRequest(
                    recipientId, type, severity, title, message, warehouseId);
            alertClient.sendAlert(alert);
        } catch (Exception e) {
            // Alert delivery must not block purchase-order state changes
            log.warn("Could not send alert (alert-service may be down): {}", e.getMessage());
        }
    }

    private void validatePurchaseOrderHeader(PurchaseOrder purchaseOrder) {
        if (purchaseOrder.getSupplierId() <= 0) {
            throw new IllegalArgumentException("Supplier is required.");
        }
        if (purchaseOrder.getWarehouseId() <= 0) {
            throw new IllegalArgumentException("Warehouse is required.");
        }
        if (purchaseOrder.getCreatedById() <= 0) {
            throw new IllegalArgumentException("Created-by user is required.");
        }
    }

    private void validateLineItem(POLineItem item) {
        if (item == null) {
            throw new IllegalArgumentException("Line item details are required.");
        }
        if (item.getProductId() <= 0) {
            throw new IllegalArgumentException("Line item product is required.");
        }
        if (item.getQuantity() <= 0) {
            throw new IllegalArgumentException("Line item quantity must be greater than 0.");
        }
        if (item.getUnitCost() < 0) {
            throw new IllegalArgumentException("Line item unit cost cannot be negative.");
        }
    }

    private String generateReferenceNumber() {
        return "PO-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS"));
    }
}
