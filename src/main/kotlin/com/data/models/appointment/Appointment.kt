package com.data.models.appointment

import com.data.models.propertylisting.PropertyListing
import com.data.models.user.User
import com.security.serializer.ObjectIdSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.bson.types.ObjectId
import org.litote.kmongo.serialization.LocalDateSerializer
import java.time.LocalDate


@Serializable
data class Appointment(
    @Serializable(with = ObjectIdSerializer::class)
    val id: ObjectId = ObjectId.get(),
    val listing: PropertyListing,
    val user: User,
    val agent: User,
    @Serializable(with = LocalDateSerializer::class)
    val date: LocalDate,
    val status: AppointmentStatus = AppointmentStatus.PENDING,
    val messages: MutableList<AppointmentMessage> = mutableListOf()
)

@Serializable
enum class AppointmentStatus {
    @SerialName("PENDING")
    PENDING,
    @SerialName("ACCEPTED")
    ACCEPTED,
    @SerialName("REJECTED")
    REJECTED
}

@Serializable
data class AppointmentMessage(
    @Serializable(with = ObjectIdSerializer::class)
    val id: ObjectId = ObjectId.get(),
    val senderName: String,
    val timestamp: Long,
    @Serializable(with = LocalDateSerializer::class)
    val date: LocalDate,
    val status: AppointmentStatus
)

@Serializable
data class AppointmentSummary(
    @Serializable(with = LocalDateSerializer::class)
    val date: LocalDate,
    val status: AppointmentStatus
)

