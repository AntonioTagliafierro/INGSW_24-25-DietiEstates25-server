package com.data.requests

import kotlinx.serialization.Serializable

@Serializable
data class UserInfoRequest(
    val email: String,
    val value: String,        // es. nuovo username, nome, cognome
    val typeRequest: String   // es. "username", "name", "surname"
)