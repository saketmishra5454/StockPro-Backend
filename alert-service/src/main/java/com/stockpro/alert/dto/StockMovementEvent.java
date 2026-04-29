package com.stockpro.alert.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockMovementEvent {

    private int productId;
    private int warehouseId;
    private String movementType;
    private int quantityChanged;
    private int previousQuantity;
    private int newQuantity;
    private int performedBy;
    private String referenceId;
    private LocalDateTime movementDate;
}