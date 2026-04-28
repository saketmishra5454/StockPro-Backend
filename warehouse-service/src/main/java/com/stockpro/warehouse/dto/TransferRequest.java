package com.stockpro.warehouse.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * TransferRequest DTO — request body for POST /api/stock/transfer
 *
 * Using a DTO instead of @RequestParams keeps the API clean
 * and allows easy validation in future.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransferRequest {
    private int fromWarehouseId;
    private int toWarehouseId;
    private int productId;
    private int quantity;
}