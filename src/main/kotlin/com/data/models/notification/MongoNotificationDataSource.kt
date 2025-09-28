package com.data.models.notification

import com.mongodb.kotlin.client.coroutine.MongoCollection
import kotlinx.coroutines.flow.toList

class MongoNotificationDataSource ( private val collection: MongoCollection<Notification>
) : NotificationDataSource {
    override suspend fun insertNotification(notification: Notification): Boolean = try {
        collection.insertOne(notification)
        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }

    override suspend fun getNotificationsByRecipient(email: String): List<Notification> =
        collection.find(com.mongodb.client.model.Filters.eq("recipientEmail", email)).toList()

    override suspend fun markAsRead(id: String): Boolean = try {
        collection.updateOne(
            com.mongodb.client.model.Filters.eq("id", id),
            com.mongodb.client.model.Updates.set("read", true)
        )
        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}