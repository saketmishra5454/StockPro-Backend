package com.stockpro.alert.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductDto {
    private int productId;
    private String sku;
    private String name;
    private int reorderLevel;
    private int maxStockLevel;
    private boolean active;
}
