package com.stockpro.report.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductMovementSummary {
    private int productId;
    private String productName;
    private String sku;
    private int totalUnitsIn;
    private int totalUnitsOut;
    private int totalUnitsMoved;
    private double totalValue;
}
