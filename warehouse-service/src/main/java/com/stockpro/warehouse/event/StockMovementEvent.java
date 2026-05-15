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
    private int quantityChanged;
    private int previousQuantity;
    private int newQuantity;
    private int performedBy;
    private String referenceId;
    private LocalDateTime movementDate;
}
