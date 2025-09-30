package com.data.models.propertylisting

import com.data.models.image.MongoImageDataSource
import com.data.models.user.myToLowerCase
import com.mongodb.client.model.Filters
import com.mongodb.kotlin.client.coroutine.MongoCollection
import com.service.GeoapifyService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withContext
import org.bson.Document
import org.bson.conversions.Bson

class MongoPropertyListingDataSource(
    private val collection: MongoCollection<PropertyListing>,
    private val geoapifyService: GeoapifyService,
    private val imageDataSource: MongoImageDataSource
) : PropertyListingDataSource {


    override suspend fun insertListing(listing: PropertyListing): Boolean = withContext(Dispatchers.IO) {

        try {
            val pois = geoapifyService.getPOIs(listing.property.latitude, listing.property.longitude)
            val listingWithPois = listing.copy(property = listing.property.copy(pois = pois))
            collection.insertOne(listingWithPois)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }



    override suspend fun attachImagesToListings(listings: List<PropertyListing>): List<PropertyListing> {
        val allIds = listings.map { it.id.toString() }
        val imagesMap = imageDataSource.getHouseImagesByIds(allIds)

        println(" Trovati ${listings.size} documenti in MongoDB")

        return listings.map { listing ->
            val images = imagesMap[listing.id.toString()] ?: emptyList()
            listing.copy(property = listing.property.copy(images = images))

        }
    }

    override suspend fun getAllListings(): List<PropertyListing> = withContext(Dispatchers.IO) {
        try {
            val listings = collection.find().toList()
            attachImagesToListings(listings)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    override suspend fun getListingsByEmail(email: String): List<PropertyListing> = withContext(Dispatchers.IO) {
        try {
            val listings = collection.find(Filters.eq("agent.email", email.myToLowerCase())).toList()
            attachImagesToListings(listings)
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
                println("Query con type=$type, city=$city")
                val listings = collection.find(
                    Filters.and(
                        Filters.eq("type", type),
                        Filters.eq("property.city", city) //  campo annidato
                    )
                ).toList()

                println(" Trovati ${listings.size} documenti in MongoDB")
                attachImagesToListings(listings)
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }

    override suspend fun searchWithFilters(query: Bson): List<PropertyListing> = withContext(Dispatchers.IO) {
        try {
            collection.find(query).toList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }




}
