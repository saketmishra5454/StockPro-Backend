package com.stockpro.report.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


//  StockLevelDto — mirrors StockLevel entity from warehouse-service.
//  FIXED: was Map<String,Object> — type-safe Jackson deserialization.

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockLevelDto {
    private int stockId;
    private int warehouseId;
    private int productId;
    private int quantity;
    private int reservedQuantity;
    private String location;
}