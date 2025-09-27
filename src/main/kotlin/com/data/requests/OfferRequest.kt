package com.data.requests
import kotlinx.serialization.Serializable

@Serializable
data class OfferRequest(
    val propertyId: String,
    val buyerId: String,
    val agentId: String,
    val amount: Double
)

