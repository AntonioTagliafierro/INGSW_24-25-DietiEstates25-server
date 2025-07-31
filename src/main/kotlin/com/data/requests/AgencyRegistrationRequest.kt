package com.data.requests

import com.service.GeneratePassword
import kotlinx.serialization.Serializable

@Serializable
data class AgencyRequest(
    val email: String,
    val password: String? = null,
    val agencyName: String
)
