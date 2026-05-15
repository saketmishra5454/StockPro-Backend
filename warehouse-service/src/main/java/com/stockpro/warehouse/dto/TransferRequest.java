package com.stockpro.warehouse.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransferRequest {
    private int fromWarehouseId;
    private int toWarehouseId;
    private int productId;
    private int quantity;
}
