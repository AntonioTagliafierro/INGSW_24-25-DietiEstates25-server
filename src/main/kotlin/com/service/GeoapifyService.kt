package com.service

import com.data.models.propertylisting.POI
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

class GeoapifyService(
    private val apiKey: String,
    private val httpClient: HttpClient
) {


    suspend fun getPOIs(
        lat: Double,
        lon: Double,
        radius: Int = 3000
    ): List<POI> {

        val categories = mapOf(
            "School" to "education.school",
            "University" to "education.university",
            "Park" to "leisure.park",
            "Bus Stop" to "transport.bus",
            "Restaurant" to "catering.restaurant",
            "Hospital" to "healthcare.hospital",
            "Stadium" to "sports.stadium",
            "Train Station" to "transport.train_station",
            "Metro" to "transport.subway_station"
        )

        val results = mutableListOf<POI>()

        for ((typeLabel, category) in categories) {
            val url = "https://api.geoapify.com/v2/places" +
                    "?categories=$category" +
                    "&filter=circle:$lon,$lat,$radius" +
                    "&limit=5" +
                    "&apiKey=$apiKey"

            try {
                val response = withContext(Dispatchers.IO) {
                    httpClient.get(url) {
                        accept(ContentType.Application.Json)
                    }.body<GeoapifyResponse>()
                }

                response.features.forEach { feature ->
                    feature.properties?.let { prop ->
                        if (prop.lat != null && prop.lon != null) {
                            val distance = haversine(lat, lon, prop.lat, prop.lon)
                            results.add(
                                POI(
                                    name = prop.name ?: typeLabel,
                                    type = typeLabel,
                                    lat = prop.lat,
                                    lon = prop.lon,
                                    distance = distance
                                )
                            )
                        }
                    }
                }

            } catch (e: Exception) {
                println("Errore Geoapify per $typeLabel: ${e.message}")
            }
        }

        return results
    }

    // Calcolo distanza in metri tra due coordinate (Haversine formula)
    private fun haversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371000.0 // raggio terrestre in metri
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat/2).pow(2.0) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon/2).pow(2.0)
        val c = 2 * atan2(sqrt(a), sqrt(1-a))
        return R * c
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
    val address_line1: String? = null,
    val lat: Double? = null,
    val lon: Double? = null
)