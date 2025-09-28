package com.data.models.appointment

import com.data.models.offer.OfferMessage
import com.security.serializer.ObjectIdSerializer
import kotlinx.serialization.Serializable
import org.bson.types.ObjectId
import org.litote.kmongo.serialization.LocalDateSerializer
import java.time.LocalDate


@Serializable
data class Appointment(
    @Serializable(with = ObjectIdSerializer::class)
    val id: ObjectId = ObjectId.get(),
    val propertyId: String,
    val userId: String,
    val agentId: String,
    @Serializable(with = LocalDateSerializer::class)
    val date: LocalDate,
    val status: AppointmentStatus = AppointmentStatus.PENDING,
    val messages: MutableList<AppointmentMessage> = mutableListOf()
)

enum class AppointmentStatus { PENDING, ACCEPTED, REJECTED }

@Serializable
data class AppointmentMessage(
    @Serializable(with = ObjectIdSerializer::class)
    val id: ObjectId = ObjectId.get(),
    val senderId: String,
    val timestamp: Long,
    @Serializable(with = LocalDateSerializer::class)
    val date: LocalDate,
    val accepted: Boolean? = null,
    val text: String? = null
)

@Serializable
data class AppointmentSummary(
    @Serializable(with = LocalDateSerializer::class)
    val date: LocalDate,
    val status: Boolean?
)

