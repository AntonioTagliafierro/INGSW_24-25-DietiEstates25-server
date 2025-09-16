package com.data.requests

import kotlinx.serialization.Serializable

@Serializable
data class UserInfoRequest(
    val email: String,
    val value: String,
    val typeRequest: String
)