package com.data.models.offer

import com.security.serializer.ObjectIdSerializer
import kotlinx.serialization.Serializable
import org.bson.types.ObjectId

@Serializable
data class Offer(
    @Serializable(with = ObjectIdSerializer::class)
    val id: ObjectId = ObjectId.get(),
    val propertyId: String,
    val buyerName: String,
    val agentName: String,
    val messages: MutableList<OfferMessage> = mutableListOf()
)

@Serializable
data class OfferMessage(
    @Serializable(with = ObjectIdSerializer::class)
    val id: ObjectId = ObjectId.get(),
    val senderId: String,
    val timestamp: Long,
    val amount: Double?,
    val accepted: Boolean? = null // null = idle, true = accettata, false = rifiutata
)

@Serializable
data class OfferSummary(
    val amount: Double?,
    val accepted: Boolean?
)