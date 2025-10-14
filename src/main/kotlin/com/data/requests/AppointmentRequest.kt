package com.data.requests


import kotlinx.serialization.Serializable
import com.data.models.user.User
import com.data.models.propertylisting.ListingSummary

@Serializable
data class AppointmentRequest(
    val listing: ListingSummary,
    val user: User,
    val agent: User,
    val date: String
)