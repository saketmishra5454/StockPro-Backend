package com.stockpro.alert.service;

import com.stockpro.alert.entity.Alert;

import java.util.List;

public interface AlertService {

    Alert sendAlert(Alert alert);

    void sendLowStockAlert(int productId, int warehouseId, int currentQty);

    void sendOverstockAlert(int productId, int warehouseId, int currentQty, int maxStockLevel);

    void sendBulkAlert(List<Integer> recipientIds, Alert template);

    Alert markAsRead(int alertId);

    void markAllRead(int recipientId);

    Alert acknowledge(int alertId);

    List<Alert> getAll();

    List<Alert> getByRecipient(int recipientId);

    long getUnreadCount(int recipientId);

    List<Alert> getUnacknowledged(int recipientId);

    void deleteAlert(int alertId);

    void sendEmail(String to, String subject, String body);
}
