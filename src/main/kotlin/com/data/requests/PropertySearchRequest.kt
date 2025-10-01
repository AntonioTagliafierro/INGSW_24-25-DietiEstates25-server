package com.data.requests

import kotlinx.serialization.Serializable

@Serializable
data class PropertySearchRequest(
    val type: String,
    val city: String? = null,
    val minPrice: Float? = null,
    val maxPrice: Float? = null,
    val rooms: Int? = null,
    val energyClass: String? = null,
    val elevator: Boolean = false,
    val gatehouse: Boolean = false,
    val balcony: Boolean = false,
    val roof: Boolean = false
)