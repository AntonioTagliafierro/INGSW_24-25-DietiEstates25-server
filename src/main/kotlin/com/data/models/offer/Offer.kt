package com.data.models.offer

import com.data.models.propertylisting.PropertyListing
import com.data.models.user.User
import com.security.serializer.ObjectIdSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.bson.types.ObjectId

@Serializable
data class Offer(
    @Serializable(with = ObjectIdSerializer::class)
    val id: ObjectId = ObjectId.get(),
    val listing : PropertyListing,
    val buyerUser: User,
    val agentUser: User,
    val messages: MutableList<OfferMessage> = mutableListOf(),
)

@Serializable
data class OfferMessage(
    @Serializable(with = ObjectIdSerializer::class)
    val id: ObjectId = ObjectId.get(),
    val sender: User,
    val timestamp: Long,
    val amount: Double?,
    val status: OfferStatus // null = idle, true = accettata, false = rifiutata
)

@Serializable
enum class OfferStatus {
    @SerialName("PENDING")
    PENDING,
    @SerialName("ACCEPTED")
    ACCEPTED,
    @SerialName("REJECTED")
    REJECTED
}

@Serializable
data class OfferSummary(
    val amount: Double?,
    val timestamp: Long,
    val status: OfferStatus
)
