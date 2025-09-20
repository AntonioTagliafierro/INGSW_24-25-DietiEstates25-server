package com.data.models.user


import com.security.serializer.ObjectIdSerializer
import kotlinx.serialization.Serializable
import org.bson.types.ObjectId
import kotlinx.serialization.SerialName
import kotlin.random.Random

@Serializable
enum class Role(val label: String) {
    @SerialName("SUPER_ADMIN")
    SUPER_ADMIN("SUPER_ADMIN"),

    @SerialName("SUPPORT_ADMIN")
    SUPPORT_ADMIN("SUPPORT_ADMIN"),

    @SerialName("AGENCY_ADMIN")
    AGENCY_ADMIN("AGENCY_ADMIN"),

    @SerialName("PENDING_AGENCY_ADMIN")
    PENDING_AGENCY_ADMIN("PENDING_AGENCY_ADMIN"),

    @SerialName("AGENT_USER")
    AGENT_USER("AGENT_USER"),

    @SerialName("LOCAL_USER")
    LOCAL_USER("LOCAL_USER"),

    @SerialName("THIRDPARTY_USER")
    THIRDPARTY_USER("THIRDPARTY_USER")
}

@Serializable
open class User(
    @Serializable(with = ObjectIdSerializer::class)
    val id: ObjectId = ObjectId.get(),
    private var username: String,
    private val email: String,
    val name: String? = null,
    val surname: String? = null,
    var role: Role,
    val password: String? = null,
    val salt:   String? = null
) {
    // costruttore (admin )
    constructor(password: String , salt : String) : this(
        id = ObjectId.get(),
        "Admin",
        "admin@system.com",
        "System",
        "Admin",
        role = Role.SUPER_ADMIN,
        password = password,
        salt = salt
    )

    // 1° costruttore (third‐party user)
    constructor(email: String, password: String?, salt: String?, username: String?) : this(
        id       = ObjectId.get(),
        username = username ?: email.substringBefore("@"),
        email    = email,
        role     = Role.THIRDPARTY_USER,
        password = password,
        salt     = salt
    )

    // 2° costruttore (local user)
    constructor(email: String, password: String?, salt: String?) : this(
        id       = ObjectId.get(),
        username = email.substringBefore("@"),
        email    = email,
        role     = Role.LOCAL_USER,
        password = password,
        salt     = salt
    )

    // 3° costruttore (agency user e suppdmin)
    constructor(email: String, password: String?, salt: String?, role: Role) : this(
        id       = ObjectId.get(),
        username = email.substringBefore("@"),
        email    = email,
        role     = role,
        password = password,
        salt     = salt
    )

    fun getEmail ( ) :String{
        return email
    }

    fun getUsername():String{
        return username
    }

    fun generateRandomPassword(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#%^&*"
        val length = Random.nextInt(4, 6 + 1)
        return (1..length)
            .map { chars.random() }
            .joinToString("")
    }

}
