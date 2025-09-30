package com.data.models.propertylisting

import org.bson.conversions.Bson

interface PropertyListingDataSource {
    suspend fun insertListing(listing: PropertyListing): Boolean
    suspend fun getAllListings(): List<PropertyListing>
    suspend fun getListingsByEmail(email: String): List<PropertyListing>
    suspend fun getListingWithinRadius(lat: Double, lon: Double, radius: Int): List<PropertyListing>
    suspend fun getListingsByTypeAndCity(type: String, city: String): List<PropertyListing>

    suspend fun attachImagesToListings(listings: List<PropertyListing>): List<PropertyListing>
    suspend fun searchWithFilters(query: Bson): List<PropertyListing>

    suspend fun getListingById(id : String): PropertyListing?
}