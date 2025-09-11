package com.data.models.appointment

data class AppointmentRequest(
    val propertyId: String,
    val userEmail: String,
    val agentEmail: String,
    val dateTime: String
)