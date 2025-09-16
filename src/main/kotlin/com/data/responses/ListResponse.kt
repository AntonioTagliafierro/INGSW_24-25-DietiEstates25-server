package com.data.responses

import kotlinx.serialization.Serializable

@Serializable
data class ListResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val message: String? = null
)