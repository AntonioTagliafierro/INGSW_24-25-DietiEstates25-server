package com.data.models.appointment

import org.bson.types.ObjectId

data class Appointment(
    val id: String = ObjectId().toString(),
    val propertyId: String,
    val userEmail: String,
    val agentEmail: String,
    val dateTime: String, // ISO 8601
    val status: AppointmentStatus = AppointmentStatus.PENDING
)

enum class AppointmentStatus {
    PENDING,
    CONFIRMED,
    REFUSED
}