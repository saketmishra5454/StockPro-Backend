package com.stockpro.product.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockLevelDto {

    private Long id;
    private Long productId;
    private Long warehouseId;
    private Integer quantity;
    private Integer reorderThreshold;

    public boolean isBelowReorderLevel() {
        if (quantity == null || reorderThreshold == null) return false;
        return quantity < reorderThreshold;
    }
}
