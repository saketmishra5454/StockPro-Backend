package com.stockpro.purchase.service.impl;

import com.stockpro.purchase.dto.AlertRequest;
import com.stockpro.purchase.entity.POLineItem;
import com.stockpro.purchase.entity.PurchaseOrder;
import com.stockpro.purchase.feign.AlertClient;
import com.stockpro.purchase.feign.WarehouseClient;
import com.stockpro.purchase.repository.POLineItemRepository;
import com.stockpro.purchase.repository.PurchaseRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PurchaseServiceImplTest {

    @Mock
    private PurchaseRepository purchaseRepository;

    @Mock
    private POLineItemRepository lineItemRepository;

    @Mock
    private WarehouseClient warehouseClient;

    @Mock
    private AlertClient alertClient;

    @InjectMocks
    private PurchaseServiceImpl purchaseService;

    @Test
    void createPOForcesDraftLinksLineItemsAndCalculatesTotal() {
        PurchaseOrder po = purchaseOrder("APPROVED");
        POLineItem first = lineItem(11, 4, 10.0);
        POLineItem second = lineItem(12, 2, 25.0);

        when(purchaseRepository.save(po)).thenAnswer(invocation -> {
            PurchaseOrder saved = invocation.getArgument(0);
            saved.setPoId(7);
            return saved;
        });

        PurchaseOrder saved = purchaseService.createPO(po, List.of(first, second));

        assertThat(saved.getStatus()).isEqualTo("DRAFT");
        assertThat(saved.getTotalAmount()).isEqualTo(90.0);
        assertThat(first.getPoId()).isEqualTo(7);
        assertThat(first.getReceivedQty()).isZero();
        assertThat(first.getTotalCost()).isEqualTo(40.0);
        assertThat(second.getTotalCost()).isEqualTo(50.0);
        verify(lineItemRepository).save(first);
        verify(lineItemRepository).save(second);
    }

    @Test
    void createPORejectsEmptyLineItems() {
        PurchaseOrder po = purchaseOrder("DRAFT");

        assertThatThrownBy(() -> purchaseService.createPO(po, List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("line item");
        verify(purchaseRepository, never()).save(any(PurchaseOrder.class));
    }

    @Test
    void submitPOChangesDraftToPendingAndSendsAlert() {
        PurchaseOrder po = purchaseOrder("DRAFT");
        po.setPoId(7);

        when(purchaseRepository.findById(7)).thenReturn(Optional.of(po));
        when(purchaseRepository.save(po)).thenReturn(po);

        PurchaseOrder saved = purchaseService.submitPO(7);

        assertThat(saved.getStatus()).isEqualTo("PENDING");
        ArgumentCaptor<AlertRequest> captor = ArgumentCaptor.forClass(AlertRequest.class);
        verify(alertClient).sendAlert(captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo("PO_PENDING");
        assertThat(captor.getValue().getSeverity()).isEqualTo("WARNING");
        assertThat(captor.getValue().getRecipientId()).isZero();
    }

    @Test
    void submitPORejectsNonDraftStatus() {
        PurchaseOrder po = purchaseOrder("APPROVED");

        when(purchaseRepository.findById(7)).thenReturn(Optional.of(po));

        assertThatThrownBy(() -> purchaseService.submitPO(7))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Only DRAFT");
        verify(purchaseRepository, never()).save(any(PurchaseOrder.class));
    }

    @Test
    void approvePOChangesPendingToApprovedAndSendsAlertToCreator() {
        PurchaseOrder po = purchaseOrder("PENDING");
        po.setCreatedById(22);

        when(purchaseRepository.findById(7)).thenReturn(Optional.of(po));
        when(purchaseRepository.save(po)).thenReturn(po);

        PurchaseOrder saved = purchaseService.approvePO(7);

        assertThat(saved.getStatus()).isEqualTo("APPROVED");
        ArgumentCaptor<AlertRequest> captor = ArgumentCaptor.forClass(AlertRequest.class);
        verify(alertClient).sendAlert(captor.capture());
        assertThat(captor.getValue().getRecipientId()).isEqualTo(22);
        assertThat(captor.getValue().getType()).isEqualTo("PO_APPROVED");
    }

    @Test
    void rejectPOChangesPendingToRejectedAndStoresReason() {
        PurchaseOrder po = purchaseOrder("PENDING");

        when(purchaseRepository.findById(7)).thenReturn(Optional.of(po));
        when(purchaseRepository.save(po)).thenReturn(po);

        PurchaseOrder saved = purchaseService.rejectPO(7, "Budget hold");

        assertThat(saved.getStatus()).isEqualTo("REJECTED");
        assertThat(saved.getRejectionReason()).isEqualTo("Budget hold");
        verify(alertClient).sendAlert(any(AlertRequest.class));
    }

    @Test
    void receiveGoodsUpdatesLineItemStockAndMarksPOReceivedWhenAllLinesComplete() {
        PurchaseOrder po = purchaseOrder("APPROVED");
        po.setWarehouseId(3);
        POLineItem existing = lineItem(11, 10, 15.0);
        existing.setReceivedQty(4);
        POLineItem received = new POLineItem();
        received.setProductId(11);
        received.setReceivedQty(6);

        when(purchaseRepository.findById(7)).thenReturn(Optional.of(po));
        when(lineItemRepository.findByPoId(7)).thenReturn(List.of(existing));
        when(lineItemRepository.areAllLinesFullyReceived(7)).thenReturn(true);
        when(purchaseRepository.save(po)).thenReturn(po);

        PurchaseOrder saved = purchaseService.receiveGoods(7, List.of(received));

        assertThat(existing.getReceivedQty()).isEqualTo(10);
        assertThat(saved.getStatus()).isEqualTo("RECEIVED");
        assertThat(saved.getReceivedDate()).isEqualTo(LocalDate.now());
        verify(lineItemRepository).save(existing);
        verify(warehouseClient).updateStock(3, 11, 6);
    }

    @Test
    void receiveGoodsRejectsQuantityGreaterThanRemaining() {
        PurchaseOrder po = purchaseOrder("APPROVED");
        POLineItem existing = lineItem(11, 10, 15.0);
        existing.setReceivedQty(4);
        POLineItem received = new POLineItem();
        received.setProductId(11);
        received.setReceivedQty(7);

        when(purchaseRepository.findById(7)).thenReturn(Optional.of(po));
        when(lineItemRepository.findByPoId(7)).thenReturn(List.of(existing));

        assertThatThrownBy(() -> purchaseService.receiveGoods(7, List.of(received)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Remaining to receive: 6");
        verify(warehouseClient, never()).updateStock(3, 11, 7);
    }

    @Test
    void receiveGoodsFailsWhenWarehouseUpdateFails() {
        PurchaseOrder po = purchaseOrder("APPROVED");
        POLineItem existing = lineItem(11, 10, 15.0);
        POLineItem received = new POLineItem();
        received.setProductId(11);
        received.setReceivedQty(3);

        when(purchaseRepository.findById(7)).thenReturn(Optional.of(po));
        when(lineItemRepository.findByPoId(7)).thenReturn(List.of(existing));
        doThrow(new RuntimeException("warehouse down")).when(warehouseClient).updateStock(3, 11, 3);

        assertThatThrownBy(() -> purchaseService.receiveGoods(7, List.of(received)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Could not update warehouse stock");

        assertThat(existing.getReceivedQty()).isZero();
        verify(lineItemRepository, never()).save(existing);
        verify(purchaseRepository, never()).save(po);
    }

    @Test
    void cancelPOAllowsDraftAndStoresReason() {
        PurchaseOrder po = purchaseOrder("DRAFT");

        when(purchaseRepository.findById(7)).thenReturn(Optional.of(po));
        when(purchaseRepository.save(po)).thenReturn(po);

        PurchaseOrder saved = purchaseService.cancelPO(7, "No longer needed");

        assertThat(saved.getStatus()).isEqualTo("CANCELLED");
        assertThat(saved.getRejectionReason()).isEqualTo("No longer needed");
    }

    @Test
    void updatePOOnlyAllowsDraftOrders() {
        PurchaseOrder po = purchaseOrder("APPROVED");

        when(purchaseRepository.findById(7)).thenReturn(Optional.of(po));

        assertThatThrownBy(() -> purchaseService.updatePO(7, new PurchaseOrder()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Only DRAFT");
        verify(purchaseRepository, never()).save(any(PurchaseOrder.class));
    }

    @Test
    void getPOByIdThrowsWhenMissing() {
        when(purchaseRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> purchaseService.getPOById(99))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("99");
    }

    private PurchaseOrder purchaseOrder(String status) {
        PurchaseOrder po = new PurchaseOrder();
        po.setPoId(7);
        po.setSupplierId(2);
        po.setWarehouseId(3);
        po.setCreatedById(22);
        po.setStatus(status);
        po.setReferenceNumber("PO-2026-001");
        po.setExpectedDate(LocalDate.now().plusDays(7));
        return po;
    }

    private POLineItem lineItem(int productId, int quantity, double unitCost) {
        POLineItem lineItem = new POLineItem();
        lineItem.setProductId(productId);
        lineItem.setQuantity(quantity);
        lineItem.setUnitCost(unitCost);
        lineItem.setTotalCost(quantity * unitCost);
        return lineItem;
    }
}
