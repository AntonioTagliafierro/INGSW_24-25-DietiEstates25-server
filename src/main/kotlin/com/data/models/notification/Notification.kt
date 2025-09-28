package com.data.models.notification

import org.bson.types.ObjectId

data class Notification(
    val id: String = ObjectId().toString(),
    val recipientEmail: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val read: Boolean = false
)
