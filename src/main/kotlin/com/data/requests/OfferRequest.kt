package com.data.requests
import com.data.models.propertylisting.PropertyListing
import com.data.models.user.User
import kotlinx.serialization.Serializable

@Serializable
data class OfferRequest(
    val property: PropertyListing,
    val buyerUser: User,
    val agent : User,
    val amount: Double
)

