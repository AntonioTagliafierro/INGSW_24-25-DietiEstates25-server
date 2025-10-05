package com.data.requests
import kotlinx.serialization.Serializable

@Serializable
data class OfferRequest(
    val propertyId: String,
    val buyerName: String,
    val agentName: String,
    val amount: Double,
    val isAgent: Boolean
)

