package com.data.responses

import kotlinx.serialization.Serializable

@Serializable
data class UserResponse(
    val id: String,
    val username: String,
    val name: String? = null,
    val surname: String? = null,
    val email: String,
    val type: String
)