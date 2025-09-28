package com.data.requests

import kotlinx.serialization.Serializable
import org.litote.kmongo.serialization.LocalDateSerializer
import java.time.LocalDate

@Serializable
data class AppointmentRequest(
    val propertyId: String,
    val userId: String,
    val agentId: String,
    @Serializable(with = LocalDateSerializer::class)
    val date: LocalDate,
)
