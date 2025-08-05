package com.data.requests

import kotlinx.serialization.Serializable

@Serializable
data class ImageRequest(
    val ownerEmail: String? = null,
    val ownerId: String? = null,                // può essere userId o houseId
    val base64Images: List<String>     // 1 immagine per profilo, max 2 per casa
)
