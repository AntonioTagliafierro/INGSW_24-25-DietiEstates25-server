package com.data.models.user

import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId

import com.auth0.jwt.interfaces.Payload



data class User (
    val username: String? = "Default Name",
    val email: String,
    val password : String?,
    val salt: String?,
    val isThirdParty: Boolean = false,
    @BsonId val id: ObjectId = ObjectId.get()
)

fun Payload.toUser(): User? {
    return try {
        User(
            email = getClaim("email").asString(),
            password = getClaim("password").asString(),
            salt = getClaim("salt").asString()
        )
    } catch (e: Exception) {
        null
    }
}