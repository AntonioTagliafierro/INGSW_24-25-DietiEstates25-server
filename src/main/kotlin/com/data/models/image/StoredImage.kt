package com.data.models.image

import com.security.serializer.ObjectIdSerializer
import kotlinx.serialization.Serializable
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId


@Serializable
data class StoredImage(
    @Serializable(with = ObjectIdSerializer::class)
    val id: ObjectId = ObjectId.get(),
    val ownerId: String,
    val base64: String
)