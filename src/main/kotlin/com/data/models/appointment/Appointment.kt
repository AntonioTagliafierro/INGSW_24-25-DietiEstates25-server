package com.data.models.appointment

import com.data.models.propertylisting.ListingSummary
import com.data.models.user.User
import com.security.serializer.ObjectIdSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.bson.types.ObjectId


@Serializable
data class Appointment(
    @Serializable(with = ObjectIdSerializer::class)
    val id: ObjectId = ObjectId.get(),
    val listing: ListingSummary,
    val user: User,
    val agent: User,
    val date: String,
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
    val sender: User,
    val timestamp: Long,
    val date: String,
    val status: AppointmentStatus
)

@Serializable
data class AppointmentSummary(
    val date: String,
    val status: AppointmentStatus
)

