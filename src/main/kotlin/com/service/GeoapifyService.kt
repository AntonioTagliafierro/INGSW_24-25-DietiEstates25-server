package com.service

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

class GeoapifyService(
    private val apiKey: String,
    private val httpClient: HttpClient
) {

    /**
     * Restituisce una lista di indicatori presenti nei dintorni
     * (es. scuole, parchi, fermate bus) entro un raggio in metri.
     */
    suspend fun getIndicators(lat: Double, lon: Double, radius: Int = 3000): List<String> {
        val categories = mapOf(
            "Vicino a scuola" to "education.school",
            "Vicino a parco" to "leisure.park",
            "Vicino a fermata bus" to "transport.bus"
        )

        val results = mutableListOf<String>()

        for ((label, category) in categories) {
            val url = "https://api.geoapify.com/v2/places" +
                    "?categories=$category" +
                    "&filter=circle:$lon,$lat,$radius" +
                    "&limit=1" +
                    "&apiKey=$apiKey"

            try {
                // La chiamata HTTP è sospendibile, non blocca il thread
                val response = withContext(Dispatchers.IO) {
                    httpClient.get(url) {
                        accept(ContentType.Application.Json)
                    }.body<GeoapifyResponse>()
                }

                if (response.features.isNotEmpty()) {
                    results.add(label)
                }

            } catch (e: Exception) {
                println("Errore Geoapify per $label: ${e.message}")

            }
        }

        return results
    }
}

@Serializable
data class GeoapifyResponse(val features: List<Feature>)

@Serializable
data class Feature(
    val properties: FeatureProperties? = null
)

@Serializable
data class FeatureProperties(
    val name: String? = null,
    val address_line1: String? = null
)