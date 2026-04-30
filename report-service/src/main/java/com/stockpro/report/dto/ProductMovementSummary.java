package com.stockpro.report.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


//  ProductMovementSummary — used in top/slow moving product reports.
//  Aggregates movement data per product for ranking.

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductMovementSummary {
    private int productId;
    private String productName;
    private String sku;
    private int totalUnitsIn;
    private int totalUnitsOut;
    private int totalUnitsMoved;   // in + out combined
    private double totalValue;
}