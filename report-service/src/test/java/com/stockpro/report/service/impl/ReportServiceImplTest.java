package com.stockpro.report.service.impl;

import com.stockpro.report.dto.ProductDto;
import com.stockpro.report.dto.ProductMovementSummary;
import com.stockpro.report.dto.StockLevelDto;
import com.stockpro.report.dto.StockMovementDto;
import com.stockpro.report.entity.InventorySnapshot;
import com.stockpro.report.feign.MovementClient;
import com.stockpro.report.feign.ProductClient;
import com.stockpro.report.feign.WarehouseClient;
import com.stockpro.report.repository.ReportRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceImplTest {

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private WarehouseClient warehouseClient;

    @Mock
    private ProductClient productClient;

    @Mock
    private MovementClient movementClient;

    @InjectMocks
    private ReportServiceImpl reportService;

    @Test
    void takeSnapshotSavesUnsnapshottedStockWithProductCost() {
        StockLevelDto stock = stockLevel(1, 11, 8, 2);
        ProductDto product = product(11, "Bolt", true, 12.5);

        when(warehouseClient.getStockByWarehouse(1)).thenReturn(List.of(stock));
        when(reportRepository.existsByWarehouseIdAndProductIdAndSnapshotDate(1, 11, LocalDate.now()))
                .thenReturn(false);
        when(productClient.getProductById(11)).thenReturn(product);

        reportService.takeSnapshot(1);

        ArgumentCaptor<InventorySnapshot> captor = ArgumentCaptor.forClass(InventorySnapshot.class);
        verify(reportRepository).save(captor.capture());
        assertThat(captor.getValue().getWarehouseId()).isEqualTo(1);
        assertThat(captor.getValue().getProductId()).isEqualTo(11);
        assertThat(captor.getValue().getQuantity()).isEqualTo(8);
        assertThat(captor.getValue().getStockValue()).isEqualTo(100.0);
        assertThat(captor.getValue().getSnapshotDate()).isEqualTo(LocalDate.now());
    }

    @Test
    void takeSnapshotSkipsExistingSnapshotForToday() {
        StockLevelDto stock = stockLevel(1, 11, 8, 2);

        when(warehouseClient.getStockByWarehouse(1)).thenReturn(List.of(stock));
        when(reportRepository.existsByWarehouseIdAndProductIdAndSnapshotDate(1, 11, LocalDate.now()))
                .thenReturn(true);

        reportService.takeSnapshot(1);

        verify(productClient, never()).getProductById(11);
        verify(reportRepository, never()).save(any(InventorySnapshot.class));
    }

    @Test
    void takeSnapshotUsesZeroCostWhenProductServiceFails() {
        StockLevelDto stock = stockLevel(1, 11, 8, 2);

        when(warehouseClient.getStockByWarehouse(1)).thenReturn(List.of(stock));
        when(reportRepository.existsByWarehouseIdAndProductIdAndSnapshotDate(1, 11, LocalDate.now()))
                .thenReturn(false);
        when(productClient.getProductById(11)).thenThrow(new RuntimeException("product down"));

        reportService.takeSnapshot(1);

        ArgumentCaptor<InventorySnapshot> captor = ArgumentCaptor.forClass(InventorySnapshot.class);
        verify(reportRepository).save(captor.capture());
        assertThat(captor.getValue().getStockValue()).isZero();
    }

    @Test
    void valuationMethodsReturnZeroWhenRepositoryReturnsNull() {
        when(reportRepository.sumTotalStockValue()).thenReturn(null);
        when(reportRepository.sumStockValueByWarehouse(1)).thenReturn(null);

        assertThat(reportService.getTotalStockValue()).isZero();
        assertThat(reportService.getStockValueByWarehouse(1)).isZero();
    }

    @Test
    void inventoryTurnoverUsesStockOutCostAndAverageInventory() {
        LocalDate from = LocalDate.of(2026, 5, 1);
        LocalDate to = LocalDate.of(2026, 5, 4);

        when(movementClient.getMovementsByDateRange(anyString(), anyString())).thenReturn(List.of(
                movement(11, 1, "STOCK_OUT", 10, 5.0),
                movement(12, 1, "STOCK_IN", 8, 3.0),
                movement(13, 1, "STOCK_OUT", 5, 2.0)
        ));
        when(reportRepository.avgStockValueByWarehouseAndDateRange(1, from, to)).thenReturn(40.0);

        assertThat(reportService.getInventoryTurnover(1, from, to)).isEqualTo(1.5);
    }

    @Test
    void inventoryTurnoverReturnsZeroWhenNoSnapshotAverageExists() {
        LocalDate from = LocalDate.of(2026, 5, 1);
        LocalDate to = LocalDate.of(2026, 5, 4);

        when(movementClient.getMovementsByDateRange(anyString(), anyString())).thenReturn(List.of());
        when(reportRepository.avgStockValueByWarehouseAndDateRange(1, from, to)).thenReturn(0.0);

        assertThat(reportService.getInventoryTurnover(1, from, to)).isZero();
    }

    @Test
    void lowStockReportEnrichesRowsWithProductDetails() {
        when(warehouseClient.getLowStockItems()).thenReturn(List.of(stockLevel(1, 11, 3, 1)));
        when(productClient.getProductById(11)).thenReturn(product(11, "Bolt", true, 12.5));

        List<Map<String, Object>> report = reportService.getLowStockReport();

        assertThat(report).hasSize(1);
        assertThat(report.get(0)).containsEntry("warehouseId", 1);
        assertThat(report.get(0)).containsEntry("productId", 11);
        assertThat(report.get(0)).containsEntry("availableQuantity", 2);
        assertThat(report.get(0)).containsEntry("productName", "Bolt");
        assertThat(report.get(0)).containsEntry("unitCost", 12.5);
    }

    @Test
    void topMovingProductsRanksActiveProductsByTotalMovementDescending() {
        ProductDto bolt = product(11, "Bolt", true, 12.5);
        ProductDto nut = product(12, "Nut", true, 5.0);
        ProductDto inactive = product(13, "Washer", false, 3.0);

        when(productClient.getAllProducts()).thenReturn(List.of(bolt, nut, inactive));
        when(movementClient.getMovementsByDateRange(anyString(), anyString())).thenReturn(List.of(
                movement(11, 1, "STOCK_IN", 10, 2.0),
                movement(11, 1, "STOCK_OUT", 5, 2.0),
                movement(12, 1, "STOCK_IN", 3, 1.0),
                movement(13, 1, "STOCK_IN", 100, 1.0)
        ));

        List<ProductMovementSummary> result = reportService.getTopMovingProducts(2);

        assertThat(result).extracting(ProductMovementSummary::getProductId).containsExactly(11, 12);
        assertThat(result.get(0).getTotalUnitsMoved()).isEqualTo(15);
    }

    @Test
    void slowMovingProductsRanksActiveProductsAscending() {
        ProductDto bolt = product(11, "Bolt", true, 12.5);
        ProductDto nut = product(12, "Nut", true, 5.0);

        when(productClient.getAllProducts()).thenReturn(List.of(bolt, nut));
        when(movementClient.getMovementsByDateRange(anyString(), anyString())).thenReturn(List.of(
                movement(11, 1, "STOCK_IN", 10, 2.0),
                movement(12, 1, "STOCK_OUT", 2, 1.0)
        ));

        List<ProductMovementSummary> result = reportService.getSlowMovingProducts(2);

        assertThat(result).extracting(ProductMovementSummary::getProductId).containsExactly(12, 11);
    }

    @Test
    void getDeadStockUsesNinetyDayCutoff() {
        InventorySnapshot snapshot = snapshot(1, 11, 0, 0.0, LocalDate.now());

        when(reportRepository.findSlowMovingStock(LocalDate.now().minusDays(90))).thenReturn(List.of(snapshot));

        assertThat(reportService.getDeadStock()).containsExactly(snapshot);
    }

    @Test
    void poSummaryAggregatesOnlyStockInMovements() {
        LocalDate from = LocalDate.of(2026, 5, 1);
        LocalDate to = LocalDate.of(2026, 5, 4);

        when(movementClient.getMovementsByDateRange(anyString(), anyString())).thenReturn(List.of(
                movement(11, 1, "STOCK_IN", 10, 5.0),
                movement(11, 1, "STOCK_OUT", 3, 5.0),
                movement(12, 2, "STOCK_IN", 4, 7.5)
        ));

        Map<String, Object> summary = reportService.getPOSummary(from, to);

        assertThat(summary).containsEntry("totalStockInMovements", 2);
        assertThat(summary).containsEntry("totalSpend", 80.0);
        assertThat((Map<Integer, Double>) summary.get("spendByWarehouse"))
                .containsEntry(1, 50.0)
                .containsEntry(2, 30.0);
        assertThat((Map<Integer, Integer>) summary.get("unitsByProduct"))
                .containsEntry(11, 10)
                .containsEntry(12, 4);
    }

    private StockLevelDto stockLevel(int warehouseId, int productId, int quantity, int reservedQuantity) {
        return new StockLevelDto(99, warehouseId, productId, quantity, reservedQuantity, "A-1");
    }

    private ProductDto product(int productId, String name, boolean active, double costPrice) {
        return new ProductDto(productId, "SKU-" + productId, name, "Fasteners", "StockPro",
                costPrice, costPrice * 1.5, 5, active);
    }

    private StockMovementDto movement(int productId, int warehouseId, String type, int quantity, double unitCost) {
        return new StockMovementDto(1, productId, warehouseId, type, quantity, unitCost,
                0, null, LocalDateTime.now(), 100);
    }

    private InventorySnapshot snapshot(int warehouseId, int productId, int quantity,
                                       double stockValue, LocalDate snapshotDate) {
        InventorySnapshot snapshot = new InventorySnapshot();
        snapshot.setWarehouseId(warehouseId);
        snapshot.setProductId(productId);
        snapshot.setQuantity(quantity);
        snapshot.setStockValue(stockValue);
        snapshot.setSnapshotDate(snapshotDate);
        return snapshot;
    }
}
