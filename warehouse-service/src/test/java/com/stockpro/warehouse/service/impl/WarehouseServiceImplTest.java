package com.stockpro.warehouse.service.impl;

import com.stockpro.warehouse.entity.StockLevel;
import com.stockpro.warehouse.entity.Warehouse;
import com.stockpro.warehouse.event.StockMovementEvent;
import com.stockpro.warehouse.repository.StockLevelRepository;
import com.stockpro.warehouse.repository.WarehouseRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WarehouseServiceImplTest {

    @Mock
    private WarehouseRepository warehouseRepository;

    @Mock
    private StockLevelRepository stockLevelRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private WarehouseServiceImpl warehouseService;

    @Test
    void createWarehouseSavesWhenNameIsUnique() {
        Warehouse warehouse = warehouse("Mumbai Central");

        when(warehouseRepository.existsByName("Mumbai Central")).thenReturn(false);
        when(warehouseRepository.save(warehouse)).thenReturn(warehouse);

        Warehouse saved = warehouseService.createWarehouse(warehouse);

        assertThat(saved).isSameAs(warehouse);
        verify(warehouseRepository).save(warehouse);
    }

    @Test
    void createWarehouseRejectsDuplicateName() {
        Warehouse warehouse = warehouse("Mumbai Central");

        when(warehouseRepository.existsByName("Mumbai Central")).thenReturn(true);

        assertThatThrownBy(() -> warehouseService.createWarehouse(warehouse))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Mumbai Central");
        verify(warehouseRepository, never()).save(any(Warehouse.class));
    }

    @Test
    void activateWarehouseMarksWarehouseActive() {
        Warehouse warehouse = warehouse("Mumbai Central");
        warehouse.setActive(false);

        when(warehouseRepository.findById(1)).thenReturn(Optional.of(warehouse));

        warehouseService.activateWarehouse(1);

        assertThat(warehouse.isActive()).isTrue();
        verify(warehouseRepository).save(warehouse);
    }

    @Test
    void initializeStockCreatesFirstStockRecord() {
        Warehouse warehouse = warehouse("Mumbai Central");

        when(warehouseRepository.findById(1)).thenReturn(Optional.of(warehouse));
        when(stockLevelRepository.existsByWarehouseIdAndProductId(1, 7)).thenReturn(false);
        when(stockLevelRepository.save(any(StockLevel.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StockLevel stockLevel = warehouseService.initializeStock(1, 7, 25);

        assertThat(stockLevel.getWarehouseId()).isEqualTo(1);
        assertThat(stockLevel.getProductId()).isEqualTo(7);
        assertThat(stockLevel.getQuantity()).isEqualTo(25);
        assertThat(stockLevel.getReservedQuantity()).isZero();
    }

    @Test
    void initializeStockRejectsExistingStockRecord() {
        Warehouse warehouse = warehouse("Mumbai Central");

        when(warehouseRepository.findById(1)).thenReturn(Optional.of(warehouse));
        when(stockLevelRepository.existsByWarehouseIdAndProductId(1, 7)).thenReturn(true);

        assertThatThrownBy(() -> warehouseService.initializeStock(1, 7, 25))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("already initialized");
        verify(stockLevelRepository, never()).save(any(StockLevel.class));
    }

    @Test
    void updateStockAddsQuantityAndPublishesMovementEvent() {
        StockLevel stockLevel = stockLevel(1, 7, 10, 0);

        when(warehouseRepository.findById(1)).thenReturn(Optional.of(warehouse("Mumbai Central")));
        when(stockLevelRepository.findByWarehouseIdAndProductId(1, 7)).thenReturn(Optional.of(stockLevel));
        when(stockLevelRepository.save(stockLevel)).thenReturn(stockLevel);

        StockLevel updated = warehouseService.updateStock(1, 7, 5);

        assertThat(updated.getQuantity()).isEqualTo(15);
        verify(rabbitTemplate).convertAndSend(
                eq("stockpro.exchange"),
                eq("stock.movement.warehouse"),
                any(StockMovementEvent.class));
    }

    @Test
    void updateStockRejectsNegativeResult() {
        StockLevel stockLevel = stockLevel(1, 7, 10, 0);

        when(warehouseRepository.findById(1)).thenReturn(Optional.of(warehouse("Mumbai Central")));
        when(stockLevelRepository.findByWarehouseIdAndProductId(1, 7)).thenReturn(Optional.of(stockLevel));

        assertThatThrownBy(() -> warehouseService.updateStock(1, 7, -11))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Insufficient stock");
        verify(stockLevelRepository, never()).save(any(StockLevel.class));
        verify(rabbitTemplate, never()).convertAndSend(any(String.class), any(String.class), any(Object.class));
    }

    @Test
    void updateStockPublishesCriticalAlertWhenQuantityBecomesZero() {
        StockLevel stockLevel = stockLevel(1, 7, 10, 0);

        when(warehouseRepository.findById(1)).thenReturn(Optional.of(warehouse("Mumbai Central")));
        when(stockLevelRepository.findByWarehouseIdAndProductId(1, 7)).thenReturn(Optional.of(stockLevel));
        when(stockLevelRepository.save(stockLevel)).thenReturn(stockLevel);

        warehouseService.updateStock(1, 7, -10);

        verify(rabbitTemplate).convertAndSend(
                eq("stockpro.exchange"),
                eq("stock.alert.lowstock"),
                any(StockMovementEvent.class));
    }

    @Test
    void reserveStockIncreasesReservedQuantityWhenAvailable() {
        StockLevel stockLevel = stockLevel(1, 7, 20, 5);

        when(stockLevelRepository.findByWarehouseIdAndProductId(1, 7)).thenReturn(Optional.of(stockLevel));
        when(stockLevelRepository.save(stockLevel)).thenReturn(stockLevel);

        StockLevel reserved = warehouseService.reserveStock(1, 7, 10);

        assertThat(reserved.getReservedQuantity()).isEqualTo(15);
        verify(stockLevelRepository).save(stockLevel);
    }

    @Test
    void reserveStockRejectsQuantityGreaterThanAvailable() {
        StockLevel stockLevel = stockLevel(1, 7, 20, 5);

        when(stockLevelRepository.findByWarehouseIdAndProductId(1, 7)).thenReturn(Optional.of(stockLevel));

        assertThatThrownBy(() -> warehouseService.reserveStock(1, 7, 16))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Available: 15");
        verify(stockLevelRepository, never()).save(any(StockLevel.class));
    }

    @Test
    void releaseReservationCapsReleaseAtReservedQuantity() {
        StockLevel stockLevel = stockLevel(1, 7, 20, 5);

        when(stockLevelRepository.findByWarehouseIdAndProductId(1, 7)).thenReturn(Optional.of(stockLevel));
        when(stockLevelRepository.save(stockLevel)).thenReturn(stockLevel);

        StockLevel released = warehouseService.releaseReservation(1, 7, 10);

        assertThat(released.getReservedQuantity()).isZero();
        verify(stockLevelRepository).save(stockLevel);
    }

    @Test
    void transferStockDebitsSourceCreditsDestinationAndPublishesEvents() {
        StockLevel source = stockLevel(1, 7, 20, 2);
        StockLevel destination = stockLevel(2, 7, 5, 0);

        when(warehouseRepository.findById(1)).thenReturn(Optional.of(warehouse("Mumbai Central")));
        when(warehouseRepository.findById(2)).thenReturn(Optional.of(warehouse("Delhi North")));
        when(stockLevelRepository.findByWarehouseIdAndProductId(1, 7)).thenReturn(Optional.of(source));
        when(stockLevelRepository.findByWarehouseIdAndProductId(2, 7)).thenReturn(Optional.of(destination));

        warehouseService.transferStock(1, 2, 7, 10);

        assertThat(source.getQuantity()).isEqualTo(10);
        assertThat(destination.getQuantity()).isEqualTo(15);
        verify(stockLevelRepository).save(source);
        verify(stockLevelRepository).save(destination);
        verify(rabbitTemplate, times(2)).convertAndSend(
                eq("stockpro.exchange"),
                eq("stock.movement.transfer"),
                any(StockMovementEvent.class));
    }

    @Test
    void transferStockRejectsSameWarehouse() {
        when(warehouseRepository.findById(1)).thenReturn(Optional.of(warehouse("Mumbai Central")));

        assertThatThrownBy(() -> warehouseService.transferStock(1, 1, 7, 10))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("cannot be the same");
        verify(stockLevelRepository, never()).save(any(StockLevel.class));
    }

    @Test
    void getLowStockItemsDelegatesToRepository() {
        StockLevel stockLevel = stockLevel(1, 7, 0, 0);

        when(stockLevelRepository.findAllLowStock()).thenReturn(List.of(stockLevel));

        assertThat(warehouseService.getLowStockItems()).containsExactly(stockLevel);
    }

    private Warehouse warehouse(String name) {
        Warehouse warehouse = new Warehouse();
        warehouse.setName(name);
        warehouse.setLocation("Mumbai");
        warehouse.setAddress("Industrial Estate");
        warehouse.setPhone("9999999999");
        warehouse.setCapacity(1000);
        warehouse.setManagerId(12);
        warehouse.setActive(true);
        return warehouse;
    }

    private StockLevel stockLevel(int warehouseId, int productId, int quantity, int reservedQuantity) {
        StockLevel stockLevel = new StockLevel();
        stockLevel.setWarehouseId(warehouseId);
        stockLevel.setProductId(productId);
        stockLevel.setQuantity(quantity);
        stockLevel.setReservedQuantity(reservedQuantity);
        return stockLevel;
    }
}
