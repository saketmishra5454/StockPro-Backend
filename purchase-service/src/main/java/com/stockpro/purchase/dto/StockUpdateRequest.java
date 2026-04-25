package com.stockpro.purchase.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockUpdateRequest {
    private int warehouseId;
    private int productId;
    private int delta;  // positive = adding stock (goods received)
}