package com.data.requests

import kotlinx.serialization.Serializable

@Serializable
data class MessageRequest(
    val offerId: String,
    val senderId: String,
    val amount: Double
)
