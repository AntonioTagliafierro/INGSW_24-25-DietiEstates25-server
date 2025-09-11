package com.data.models.propertylisting

import com.mongodb.client.model.Filters
import com.mongodb.client.model.geojson.Point
import com.mongodb.client.model.geojson.Position
import com.mongodb.kotlin.client.coroutine.MongoCollection
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.service.GeoapifyService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withContext
import org.bson.Document
import sun.rmi.server.Dispatcher

class MongoPropertyListingDataSource(
    private val collection: MongoCollection<PropertyListing>,
    private val geoapifyService: GeoapifyService
) : PropertyListingDataSource {
    override suspend fun insertListing(listing: PropertyListing): Boolean = withContext(Dispatchers.IO) {

        try {
            val indicators = geoapifyService.getIndicators(listing.property.latitude, listing.property.longitude)
            val listingWithIndicators = listing.copy(property = listing.property.copy(indicators = indicators))
            collection.insertOne(listingWithIndicators)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    override suspend fun getAllListings(): List<PropertyListing> = withContext(Dispatchers.IO) {
        try {
            collection.find().toList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    override suspend fun getListingsByEmail(email: String): List<PropertyListing> = withContext(Dispatchers.IO) {

        try {

            collection.find(Filters.eq("email", email)).toList()

        } catch (e: Exception) {

            e.printStackTrace()
            emptyList()
        }

    }

    override suspend fun getListingWithinRadius(
        lat: Double,
        lon: Double,
        radius: Int
    ): List<PropertyListing> = withContext(Dispatchers.IO) {

        try {
            // Filtro geospaziale con Document
            val filter = Document(
                "property.location",
                Document(
                    "\$nearSphere",
                    Document(
                        "\$geometry", Document("type", "Point")
                            .append("coordinates", listOf(lon, lat))
                    ).append("\$maxDistance", radius)
                )
            )

            collection.find(filter).toList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

}