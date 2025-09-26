package com.data.models.propertylisting

import kotlinx.serialization.Serializable

@Serializable
data class PropertyListingResponse(
    val id: String?,
    val title: String,
    val type: String,
    val price: Float,
    val property: PropertyResponse,
    val agentEmail: String
)



@Serializable
data class PropertyResponse(
    val city: String,
    val cap: String,
    val country: String,
    val province: String,
    val street: String,
    val civicNumber: String,
    val latitude: Double,
    val longitude: Double,
    val size: Float,
    val numberOfRooms: Int,
    val numberOfBathrooms: Int,
    val energyClass: String,
    val parking: Boolean,
    val garden: Boolean,
    val elevator: Boolean,
    val gatehouse: Boolean,
    val balcony: Boolean,
    val roof: Boolean,
    val airConditioning: Boolean,
    val heatingSystem: Boolean,
    val description: String,
    val propertyPicture: String? = null,
    val pois: List<POI> = emptyList(),
)


fun PropertyListing.toResponse(): PropertyListingResponse {
    return PropertyListingResponse(
        id = this.id.toHexString(),
        title = this.title,
        type = this.type!!.label,
        price = this.price,
        property = this.property.toResponse(),
        agentEmail = this.agentEmail
    )
}

fun Property.toResponse(): PropertyResponse {
    return PropertyResponse(
        city = this.city,
        cap = this.cap,
        country = this.country,
        province = this.province,
        street = this.street,
        civicNumber = this.civicNumber,
        latitude = this.latitude,
        longitude = this.longitude,
        size = this.size,
        numberOfRooms = this.numberOfRooms,
        numberOfBathrooms = this.numberOfBathrooms,
        energyClass = this.energyClass!!.label,
        parking = this.parking,
        garden = this.garden,
        elevator = this.elevator,
        gatehouse = this.gatehouse,
        balcony = this.balcony,
        roof = this.roof,
        airConditioning = this.airConditioning,
        heatingSystem = this.heatingSystem,
        description = this.description,
        propertyPicture = this.propertyPicture,
        pois = this.pois
    )
}