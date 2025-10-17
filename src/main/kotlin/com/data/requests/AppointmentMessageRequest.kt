package com.data.requests

import kotlinx.serialization.Serializable
import org.litote.kmongo.serialization.LocalDateSerializer
import com.data.models.user.User
import java.time.LocalDate

@Serializable
data class AppointmentMessageRequest(
    val appointmentId: String,
    val sender: User,
    @Serializable(with = LocalDateSerializer::class)
    val date: LocalDate
)