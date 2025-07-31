package com.data.requests

import kotlinx.serialization.Serializable

@Serializable
data class AuthRequest(
    val username: String? = null,
    val email: String,
    val password: String? = null,
    val newPassword: String? = null,
    val agencyName: String? = null,
    val provider: String? = null
)