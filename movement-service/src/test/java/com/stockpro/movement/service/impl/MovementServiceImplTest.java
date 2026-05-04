package com.stockpro.movement.service.impl;

import com.stockpro.movement.entity.StockMovement;
import com.stockpro.movement.repository.MovementRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MovementServiceImplTest {

    @Mock
    private MovementRepository movementRepository;

    @InjectMocks
    private MovementServiceImpl movementService;

    @Test
    void recordMovementSavesNewMovement() {
        StockMovement movement = movement("STOCK_IN", 25);

        when(movementRepository.save(movement)).thenReturn(movement);

        StockMovement saved = movementService.recordMovement(movement);

        assertThat(saved).isSameAs(movement);
        verify(movementRepository).save(movement);
    }

    @Test
    void recordMovementRejectsExistingMovementId() {
        StockMovement movement = movement("STOCK_IN", 25);
        movement.setMovementId(7);

        assertThatThrownBy(() -> movementService.recordMovement(movement))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("immutable")
                .hasMessageContaining("7");
        verify(movementRepository, never()).save(any(StockMovement.class));
    }

    @Test
    void getByProductDelegatesToRepository() {
        StockMovement movement = movement("STOCK_IN", 25);

        when(movementRepository.findByProductIdOrderByMovementDateDesc(11)).thenReturn(List.of(movement));

        assertThat(movementService.getByProduct(11)).containsExactly(movement);
    }

    @Test
    void getByWarehouseDelegatesToRepository() {
        StockMovement movement = movement("STOCK_OUT", 5);

        when(movementRepository.findByWarehouseIdOrderByMovementDateDesc(3)).thenReturn(List.of(movement));

        assertThat(movementService.getByWarehouse(3)).containsExactly(movement);
    }

    @Test
    void getByTypeDelegatesToRepository() {
        StockMovement movement = movement("TRANSFER_IN", 10);

        when(movementRepository.findByMovementTypeOrderByMovementDateDesc("TRANSFER_IN"))
                .thenReturn(List.of(movement));

        assertThat(movementService.getByType("TRANSFER_IN")).containsExactly(movement);
    }

    @Test
    void getByDateRangeDelegatesWhenRangeIsValid() {
        LocalDateTime from = LocalDateTime.of(2026, 5, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 5, 4, 23, 59);
        StockMovement movement = movement("STOCK_IN", 25);

        when(movementRepository.findByMovementDateBetweenOrderByMovementDateDesc(from, to))
                .thenReturn(List.of(movement));

        assertThat(movementService.getByDateRange(from, to)).containsExactly(movement);
    }

    @Test
    void getByDateRangeRejectsStartAfterEnd() {
        LocalDateTime from = LocalDateTime.of(2026, 5, 4, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 5, 1, 0, 0);

        assertThatThrownBy(() -> movementService.getByDateRange(from, to))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Start date");
        verify(movementRepository, never()).findByMovementDateBetweenOrderByMovementDateDesc(any(), any());
    }

    @Test
    void getMovementHistoryDelegatesToRepository() {
        StockMovement movement = movement("STOCK_OUT", 5);

        when(movementRepository.findByProductIdAndWarehouseIdOrderByMovementDateDesc(11, 3))
                .thenReturn(List.of(movement));

        assertThat(movementService.getMovementHistory(11, 3)).containsExactly(movement);
    }

    @Test
    void stockTotalsUseExpectedMovementTypes() {
        when(movementRepository.sumQuantityByProductIdAndType(11, "STOCK_IN")).thenReturn(100);
        when(movementRepository.sumQuantityByProductIdAndType(11, "STOCK_OUT")).thenReturn(40);

        assertThat(movementService.getStockIn(11)).isEqualTo(100);
        assertThat(movementService.getStockOut(11)).isEqualTo(40);
    }

    @Test
    void getAllMovementsSortsByMovementDateDescending() {
        StockMovement movement = movement("STOCK_IN", 25);

        when(movementRepository.findAll(eq(Sort.by(Sort.Direction.DESC, "movementDate"))))
                .thenReturn(List.of(movement));

        assertThat(movementService.getAllMovements()).containsExactly(movement);
    }

    private StockMovement movement(String type, int quantity) {
        StockMovement movement = new StockMovement();
        movement.setProductId(11);
        movement.setWarehouseId(3);
        movement.setMovementType(type);
        movement.setQuantity(quantity);
        movement.setReferenceId(20);
        movement.setReferenceType("PURCHASE_ORDER");
        movement.setMovementDate(LocalDateTime.of(2026, 5, 4, 12, 0));
        movement.setBalanceAfter(100);
        return movement;
    }
}
