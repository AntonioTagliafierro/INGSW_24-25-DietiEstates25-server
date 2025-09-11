package com.data.models.propertylisting

import kotlinx.serialization.Serializable
import org.bson.types.ObjectId

@Serializable
data class PropertyListingRequest(
    val title: String,
    val type: String, // "Rent" o "Sell"
    val price: Float,
    val property: PropertyRequest,
    val agentEmail: String
)

@Serializable
data class PropertyRequest(
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
    val propertyPicture: String? = null
)



fun PropertyListingRequest.toEntity(): PropertyListing {
    return PropertyListing(
        id = ObjectId(), // MongoDB assegna un nuovo ObjectId
        title = this.title,
        type = Type.fromLabel(this.type), // converte la stringa in enum
        price = this.price,
        property = this.property.toEntity(),
        agentEmail = this.agentEmail
    )
}

fun PropertyRequest.toEntity(): Property {
    return Property(
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
        energyClass = EnergyClass.fromLabel(this.energyClass),
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
        indicators = emptyList() // lo riempirai con GeoapifyService
    )
}