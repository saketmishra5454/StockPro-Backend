package com.stockpro.report.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockMovementDto {
    private int movementId;
    private int productId;
    private int warehouseId;
    private String movementType;
    private int quantity;
    private double unitCost;
    private int performedBy;
    private String notes;
    private LocalDateTime movementDate;
    private int balanceAfter;
}
