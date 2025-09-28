package com.data.requests

import kotlinx.serialization.Serializable
import org.litote.kmongo.serialization.LocalDateSerializer
import java.time.LocalDate

@Serializable
data class AppointmentMessageRequest(
    val appointmentId: String,
    val senderId: String,
    @Serializable(with = LocalDateSerializer::class)
    val date: LocalDate
)
