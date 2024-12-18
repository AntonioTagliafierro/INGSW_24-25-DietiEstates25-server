package com.data.requests

import kotlinx.serialization.Serializable

@Serializable
data class AgencyRegistrationRequest(
    val email: String,
    val agencyName: String
)
