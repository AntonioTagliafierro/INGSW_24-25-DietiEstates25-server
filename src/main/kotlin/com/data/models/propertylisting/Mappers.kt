package com.data.models.propertylisting

class Mappers {

    // Request → Domain
    fun PropertyListingRequest.toDomain(): PropertyListing {
        return PropertyListing(
            title = this.title,
            type = Type.valueOf(this.type), // enum dal valore stringa
            price = this.price,
            property = this.property.toDomain(),
            agentEmail = this.agentEmail
        )
    }

    fun PropertyRequest.toDomain(): Property {
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
            energyClass = EnergyClass.valueOf(this.energyClass),
            parking = this.parking,
            garden = this.garden,
            elevator = this.elevator,
            gatehouse = this.gatehouse,
            balcony = this.balcony,
            roof = this.roof,
            airConditioning = this.airConditioning,
            heatingSystem = this.heatingSystem,
            description = this.description,
            propertyPicture = this.propertyPicture
        )
    }

    // Domain → Response
    fun PropertyListing.toResponse(): PropertyListingResponse {
        return PropertyListingResponse(
            id = this.id?.toHexString(),
            title = this.title,
            type = this.type!!.name,
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
            energyClass = this.energyClass!!.name,
            parking = this.parking,
            garden = this.garden,
            elevator = this.elevator,
            gatehouse = this.gatehouse,
            balcony = this.balcony,
            roof = this.roof,
            airConditioning = this.airConditioning,
            heatingSystem = this.heatingSystem,
            description = this.description,
            propertyPicture = this.propertyPicture
        )
    }
}