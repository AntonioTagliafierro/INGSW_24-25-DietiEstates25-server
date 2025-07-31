package com.data.models.agency
import kotlinx.serialization.Serializable


@Serializable
class AgencyUser (
    val userId: String,
    val agencyId: String,
    val role: String
)
