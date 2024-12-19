package com.data.models.user

import com.auth0.jwt.interfaces.Payload
import kotlinx.serialization.Serializable
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId


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