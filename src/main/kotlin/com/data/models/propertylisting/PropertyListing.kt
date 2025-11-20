package com.data.models.propertylisting


import com.security.serializer.ObjectIdSerializer
import kotlinx.serialization.SerialName
import org.bson.types.ObjectId
import kotlinx.serialization.Serializable
import com.data.models.user.User




@Serializable
data class PropertyListing(
    @Serializable(with = ObjectIdSerializer::class)
    val id: ObjectId = ObjectId.get(),
    val title: String,
    val type: Type?,
    val price: Float,
    val property: Property,
    val agent: User
)

@Serializable
data class ListingSummary(
    val id: String,
    val title: String,
    val property: PropertySummary,
)

@Serializable
enum class Type(val label: String) {
    @SerialName("Rent")
    RENT("Rent"),
    @SerialName("Sell")
    SELL("Sell")

}

//    companion object {
//        fun fromLabel(label: String): Type? = values().find { it.label == label }
//    }


@Serializable
data class GeoLocation(
    val type: String = "Point",          // sempre "Point"
    val coordinates: List<Double>        // [longitude, latitude]
)



@Serializable
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
    val pois: List<POI> = emptyList(),
    val images: List<String> = emptyList()
) {
    val location: GeoLocation = GeoLocation(coordinates = listOf(longitude, latitude))
}

@Serializable
data class PropertySummary(
    val city: String,
    val street: String,
    val civicNumber: String
)

@Serializable
enum class EnergyClass(val label: String) {
    @SerialName("A")
    A("A"),
    @SerialName("B")
    B("B"),
    @SerialName("C")
    C("C"),
    @SerialName("D")
    D("D"),
    @SerialName("E")
    E("E"),
    @SerialName("F")
    F("F"),
    @SerialName("G")
    G("G");

//    companion object {
//        fun fromLabel(label: String): EnergyClass? = values().find { it.label == label }
//    }
}