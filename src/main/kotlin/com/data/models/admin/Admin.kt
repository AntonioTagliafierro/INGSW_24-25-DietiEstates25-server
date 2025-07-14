package com.data.models.admin

import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId

data class Admin(
    @BsonId val id: ObjectId = ObjectId(),
    val agencyName: String,
    val email: String,
    val password: String,
    val salt: String,
    val type: String = "Adm  in" // Valore predefinito
)