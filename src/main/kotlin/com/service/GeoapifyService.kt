package com.service

import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.request.*
import kotlinx.serialization.Serializable

class GeoapifyService(private val apiKey: String, private val httpClient: HttpClient) {

    suspend fun getNearbyIndicators(lat: Double, lon: Double): List<String> {
        val categories = mapOf(
            "Vicino a scuola" to "education.school",
            "Vicino a parco" to "leisure.park",
            "Vicino a fermata bus" to "transport.bus"
        )

        val radius = 500 // metri
        val results = mutableListOf<String>()

        for ((label, category) in categories) {
            val url = "https://api.geoapify.com/v2/places?categories=$category&filter=circle:$lon,$lat,$radius&limit=1&apiKey=$apiKey"

            val response = httpClient.get(url).body<GeoapifyResponse>()
            if (response.features.isNotEmpty()) {
                results.add(label)
            }
        }

        return results
    }
}

@Serializable
data class GeoapifyResponse(val features: List<Feature>)
@Serializable
data class Feature(val properties: Map<String, String>)