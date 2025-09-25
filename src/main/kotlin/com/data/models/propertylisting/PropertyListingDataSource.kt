package com.data.models.propertylisting

interface PropertyListingDataSource {
    suspend fun insertListing(listing: PropertyListing): Boolean
    suspend fun getAllListings(): List<PropertyListing>
    suspend fun getListingsByEmail(email: String): List<PropertyListing>
    suspend fun getListingWithinRadius(lat: Double, lon: Double, radius: Int): List<PropertyListing>
    suspend fun getListingsByTypeAndCity(type: String, city: String): List<PropertyListing>
    suspend fun getListingsById(id: String): PropertyListing?

}