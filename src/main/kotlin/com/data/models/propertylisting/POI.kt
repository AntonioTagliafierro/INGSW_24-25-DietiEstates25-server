package com.data.models.propertylisting

import kotlinx.serialization.Serializable


@Serializable
data class POI(
    val name: String,
    val type: String,
    val lat: Double,
    val lon: Double,
    val distance: Double
)