package com.stockpro.warehouse.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockMovementEvent implements Serializable {

    private int productId;
    private int warehouseId;
    private String movementType;      // STOCK_IN, STOCK_OUT, TRANSFER_IN, TRANSFER_OUT, ADJUSTMENT
    private int quantityChanged;      // positive = increase, negative = decrease
    private int previousQuantity;     // stock before this change
    private int newQuantity;          // stock after this change
    private int performedBy;          // userId who triggered this (0 if system/automated)
    private String referenceId;       // PO id or transfer id (null if not applicable)
    private LocalDateTime movementDate;
}