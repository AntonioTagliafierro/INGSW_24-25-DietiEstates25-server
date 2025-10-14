package com.data.requests

import com.data.models.user.User
import kotlinx.serialization.Serializable

@Serializable
data class MessageRequest(
    val offerId: String,
    val sender: User,
    val amount: Double
)
