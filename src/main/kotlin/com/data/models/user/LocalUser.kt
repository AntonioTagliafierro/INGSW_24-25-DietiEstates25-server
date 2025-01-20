package com.data.models.user

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


class LocalUser(
    email : String,
    val password : String?,
    val salt: String?,
    val roles: List<String> = emptyList(),
) : User( email )

