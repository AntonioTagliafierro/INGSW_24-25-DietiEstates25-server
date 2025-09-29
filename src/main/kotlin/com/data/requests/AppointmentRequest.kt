package com.data.requests

import com.data.models.propertylisting.PropertyListing
import com.data.models.user.User
import kotlinx.serialization.Serializable
import org.litote.kmongo.serialization.LocalDateSerializer
import java.time.LocalDate

@Serializable
data class AppointmentRequest(
    val listing: PropertyListing,
    val user: User,
    val agent: User,
    @Serializable(with = LocalDateSerializer::class)
    val date: LocalDate,
)