package com.data.models.user


import com.security.serializer.ObjectIdSerializer
import kotlinx.serialization.Serializable
import org.bson.types.ObjectId

@Serializable
open class User(
    @Serializable(with = ObjectIdSerializer::class)
    val id: ObjectId = ObjectId.get(),
    private var username: String,
    private val email: String,
    val type: String,
    val password: String? = null,   // ← default null
    val salt:   String? = null      // ← default null
) {
    // 1° costruttore (third‐party)
    constructor(email: String, username: String?) : this(
        id       = ObjectId.get(),
        username = if (username != null ) "$username#${ObjectId.get()}" else "$email#${ObjectId.get()}",
        email    = email,
        type     = "thirdPartyUser"
    )

    // 2° costruttore (locale)
    constructor(email: String, password: String?, salt: String?) : this(
        id       = ObjectId.get(),
        username = "$email#${ObjectId.get()}",
        email    = email,
        type     = "localUser",
        password = password,
        salt     = salt
    )

    fun getEmail ( ) :String{
        return email
    }

    fun getUsername():String{
        return username
    }

}
