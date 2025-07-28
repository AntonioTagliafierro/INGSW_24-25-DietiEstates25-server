package com.data.models.property

import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId


    data class Property(
    @BsonId val id: ObjectId = ObjectId(),
    val title: String,
    val latitude: Double,
    val longitude: Double,
    val indicators: List<String>,
    val agentEmail: String
    )