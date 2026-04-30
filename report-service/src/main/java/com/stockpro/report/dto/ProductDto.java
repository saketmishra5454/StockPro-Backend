package com.stockpro.report.dto;

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
    private String category;
    private String brand;
    private double costPrice;
    private double sellingPrice;
    private int reorderLevel;
    private boolean isActive;
}