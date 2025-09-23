package com.data.requests
@kotlinx.serialization.Serializable
data class AdminRequest(
    val adminEmail: String,
    val adminId: String,
    val suppAdminEmail: String,
    val email : String
)
