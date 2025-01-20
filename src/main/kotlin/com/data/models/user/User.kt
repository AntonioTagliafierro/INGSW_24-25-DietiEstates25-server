package com.data.models.user
import com.security.serializer.*


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.bson.types.ObjectId

@Serializable
open class User(
    @Serializable(with = ObjectIdSerializer::class)
    val id: ObjectId = ObjectId.get(), // `id` è final per default
    private var username: String, // `username` è modificabile
    private val email: String,
    val type: String
){

    constructor(email: String, username: String) : this(
        email = email,
        username = username,
        type = "thirdPartyUser"
    )

    constructor(email: String) : this(
        email = email,
        username = "utente#${ObjectId.get()}",
        type = "localUser"
    )

    fun getEmail ( ) :String{
        return email
    }

    fun getUsername():String{
        return username
    }

}
