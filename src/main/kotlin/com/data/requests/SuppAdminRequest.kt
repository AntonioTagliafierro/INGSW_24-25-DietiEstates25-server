package com.data.requests
@kotlinx.serialization.Serializable
data class SuppAdminRequest(
    val adminEmail: String,
    val adminId: String,
    val suppAdminEmail: String,
    val usernameSuppAdmin: String
)
