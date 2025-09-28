package com.data.requests
import kotlinx.serialization.Serializable

@Serializable
data class  PropertySearchRequest (

    val type: String? = null,
    val city: String? = null,
    val minPrice: Float? = null,
    val maxPrice: Float? = null,
    val elevator: Boolean? = null,
    val gatehouse: Boolean? = null,
    val balcony: Boolean? = null,
    val roof: Boolean? = null,
    val minRooms: Int? = null,
    val energyClass: String? = null

)