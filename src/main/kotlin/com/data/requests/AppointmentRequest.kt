package com.data.requests


import kotlinx.serialization.Serializable


@Serializable
data class AppointmentRequest(
    val listingId: String,
    val userId: String,
    val agentId: String,
    val date: String,
)