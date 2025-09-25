package com.data.models.propertylisting

import com.data.models.image.MongoImageDataSource
import com.mongodb.client.model.Filters
import com.mongodb.client.model.geojson.Point
import com.mongodb.client.model.geojson.Position
import com.mongodb.kotlin.client.coroutine.MongoCollection
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.service.GeoapifyService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withContext
import org.bson.Document
import sun.rmi.server.Dispatcher

class MongoPropertyListingDataSource(
    private val collection: MongoCollection<PropertyListing>,
    private val geoapifyService: GeoapifyService,
    private val imageDataSource: MongoImageDataSource
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
            val listings = collection.find().toList()
            listings.map { listing ->
                val images = imageDataSource.getHouseImages(listing.id.toString())
                listing.copy(
                    property = listing.property.copy(images = images)
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    //= withContext(Dispatchers.IO)
//    override suspend fun getListingsByEmail(email: String): List<PropertyListing>  {
//
//    return try {
//        val listings = collection.find(Filters.eq("agentEmail", email)).toList()
//        listings.map { listing ->
//            val images = imageDataSource.getHouseImages(listing.id.toString())
//            listing.copy(
//                property = listing.property.copy(images = images)
//            )
//        }
//    } catch (e: Exception) {
//        e.printStackTrace()
//        emptyList()
//    }
//
//
//    }
//    funzione che prende annunci e immagini in un unica query per ridurre i tempi
    override suspend fun getListingsByEmail(email: String): List<PropertyListing> = withContext(Dispatchers.IO) {
        try {
            val listings = collection.find(Filters.eq("agentEmail", email)).toList()

            // Recupera tutte le immagini in un’unica query
            val allIds = listings.map { it.id.toString() }
            val imagesMap = imageDataSource.getHouseImagesByIds(allIds)

            // Combina listings + immagini
            listings.map { listing ->
                val images = imagesMap[listing.id.toString()] ?: emptyList()
                listing.copy(
                    property = listing.property.copy(images = images)
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    override suspend fun getListingById(id: String): PropertyListing? = withContext(Dispatchers.IO) {
        try {
            val listing = collection.find(Filters.eq("id", id)).firstOrNull() ?: return@withContext null
            val images = imageDataSource.getHouseImages(listing.id.toString())
            listing.copy(property = listing.property.copy(images = images))
        } catch (e: Exception) {
            e.printStackTrace()
            null
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

    override suspend fun getListingsByTypeAndCity(type: String, city: String): List<PropertyListing> =
        withContext(Dispatchers.IO) {
            try {
                collection.find(
                    Filters.and(
                        Filters.eq("type", type),
                        Filters.eq("city", city)
                    )
                ).toList()
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }


}