package com

import com.data.models.propertylisting.PropertyListingDataSource
import com.data.models.propertylisting.PropertyListingRequest
import com.data.models.propertylisting.toEntity
import com.data.models.propertylisting.toResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.propertyListingRoutes(propertyListingDataSource: PropertyListingDataSource) {
    route("/propertiesListing") {

        post("/addpropertylisting") {
            val request = call.receive<PropertyListingRequest>()
            val entity = request.toEntity()

            val success = propertyListingDataSource.insertListing(entity)

            if (success) {
                call.respond(HttpStatusCode.OK, "Listing added successfully")
            } else {
                call.respond(HttpStatusCode.InternalServerError, "Error adding property listing")
            }
        }

        get("getallpropertieslisting") {
            val listings = propertyListingDataSource.getAllListings()
            val response = listings.map { it.toResponse() }
            call.respond(HttpStatusCode.OK, response)
        }

        get("getpropertieslistingbyemail/{email}") {
            val email = call.parameters["email"]
            if (email.isNullOrBlank()) {
                return@get call.respond(HttpStatusCode.BadRequest, "Email is required")
            }

            val listings = propertyListingDataSource.getListingsByEmail(email)
            val response = listings.map { it.toResponse() }
            call.respond(HttpStatusCode.OK, response)
        }

        get("getpropertieslistingwithinradius") {
            val lat = call.parameters["lat"]?.toDoubleOrNull()
            val lon = call.parameters["lon"]?.toDoubleOrNull()
            val radius = call.parameters["radius"]?.toIntOrNull() ?: 1000

            if (lat == null || lon == null) {
                return@get call.respond(HttpStatusCode.BadRequest, "Missing or invalid coordinates")
            }

            val listings = propertyListingDataSource.getListingWithinRadius(lat, lon, radius)
            val response = listings.map { it.toResponse() }
            call.respond(HttpStatusCode.OK, response)
        }



    }
}