package com.stockpro.alert.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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
