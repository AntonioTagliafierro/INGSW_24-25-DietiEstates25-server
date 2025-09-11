package com.data.models.propertylisting


import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId
import kotlinx.serialization.Serializable

data class PropertyListing(
    @BsonId val id: ObjectId = ObjectId(),
    val title: String,
    val type: Type?,
    val price: Float,
    val property: Property,
    val agentEmail: String
)

enum class Type(val label: String) {
    A("Rent"),
    B("Sell");

    companion object {
        fun fromLabel(label: String): Type? = values().find { it.label == label }
    }
}

@Serializable
data class GeoLocation(
    val type: String = "Point",          // sempre "Point"
    val coordinates: List<Double>        // [longitude, latitude]
)

data class Property(
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
    val energyClass: EnergyClass?,
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
    val indicators: List<String> = emptyList()
) {
    val location: GeoLocation = GeoLocation(coordinates = listOf(longitude, latitude))
}

enum class EnergyClass(val label: String) {
    A("A"),
    B("B"),
    C("C"),
    D("D"),
    E("E"),
    F("F"),
    G("G");

    companion object {
        fun fromLabel(label: String): EnergyClass? = values().find { it.label == label }
    }
}