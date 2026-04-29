package com.stockpro.alert.service;

import com.stockpro.alert.entity.Alert;

import java.util.List;


 // AlertService interface — all alert operations.

public interface AlertService {

    // Send a single alert — used by purchase-service for PO alerts
    Alert sendAlert(Alert alert);

    // Create a LOW_STOCK alert for a specific product+warehouse combination
    void sendLowStockAlert(int productId, int warehouseId, int currentQty);

    // Send the same alert to multiple recipients at once
    void sendBulkAlert(List<Integer> recipientIds, String title, String message);

    // Mark a single alert as read
    Alert markAsRead(int alertId);

    // Mark ALL alerts for a recipient as read in one operation
    void markAllRead(int recipientId);

    // Acknowledge an alert — user has acted on it
    Alert acknowledge(int alertId);

    // Get all alerts for a recipient (their full notification inbox)
    List<Alert> getByRecipient(int recipientId);

    // Count unread alerts — for notification badge number
    long getUnreadCount(int recipientId);

    // Get all unacknowledged alerts for a recipient
    List<Alert> getUnacknowledged(int recipientId);

    // Delete an alert permanently
    void deleteAlert(int alertId);


     //Send an email using JavaMailSender.

    void sendEmail(String to, String subject, String body);
}