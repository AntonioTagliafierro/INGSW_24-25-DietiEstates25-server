package com.data.models.agency

import com.security.serializer.ObjectIdSerializer
import kotlinx.serialization.Serializable
import org.bson.types.ObjectId

@Serializable
open class Agency(
    @Serializable(with = ObjectIdSerializer::class)
    val id: ObjectId = ObjectId.get(),
    var name: String,
    var pending : Boolean,
    val agencyEmail: String
)