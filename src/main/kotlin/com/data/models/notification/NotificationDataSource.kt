package com.data.models.notification

interface NotificationDataSource {
    suspend fun insertNotification(notification: Notification): Boolean
    suspend fun getNotificationsByRecipient(email: String): List<Notification>
    suspend fun markAsRead(id: String): Boolean
}