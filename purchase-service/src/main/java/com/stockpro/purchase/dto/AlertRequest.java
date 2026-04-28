package com.stockpro.purchase.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlertRequest {

    private int recipientId;
    private String type;
    private String severity;
    private String title;
    private String message;
    private int warehouseId;
}