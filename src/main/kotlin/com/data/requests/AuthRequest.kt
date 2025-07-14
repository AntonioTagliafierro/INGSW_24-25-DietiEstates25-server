package com.data.requests

import kotlinx.serialization.Serializable

@Serializable
data class AuthRequest(
    val username: String? = null,
    val email: String,
    val password: String,
    val newPassword: String? = null,
    val provider: String? = null
)