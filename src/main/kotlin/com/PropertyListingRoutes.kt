package com

import com.data.models.propertylisting.PropertyListingDataSource
import com.data.models.propertylisting.PropertyListingRequest
import com.data.models.propertylisting.toEntity
import com.data.models.propertylisting.toResponse
import com.data.responses.ListResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.propertyListingRoutes(propertyListingDataSource: PropertyListingDataSource) {
    route("/propertylisting") {

        post("/addpropertylisting") {
            val request = call.receive<PropertyListingRequest>()
            val entity = request.toEntity()




            println(entity.id)

            println(entity.id.toString())
            println(entity)

            val success = propertyListingDataSource.insertListing(entity)


            if (success) {
                call.respond(HttpStatusCode.OK, entity.id.toString())
            } else {
                call.respond(HttpStatusCode.InternalServerError, "Error adding property listing")
            }

//            if (success) {
//                call.respond(HttpStatusCode.OK, data = entity.id.toString())
//            } else {
//                call.respond(HttpStatusCode.InternalServerError, "Error adding property listing")
//            }
        }

        get("getallpropertieslisting") {
            val listings = propertyListingDataSource.getAllListings()
            val response = listings.map { it.toResponse() }
            call.respond(HttpStatusCode.OK, response)
        }

        get("getpropertieslistingbyemail") {
            val email = call.receive<String>()
            if (email.isNullOrBlank()) {
                return@get call.respond(HttpStatusCode.BadRequest, "Email is required")
            }

            val listings = propertyListingDataSource.getListingsByEmail(email)


            //val response = listings.map { it.toResponse() }

            call.respond(HttpStatusCode.OK, ListResponse(success = true, data = listings))


            //call.respond(HttpStatusCode.OK, response)
        }

        get("getpropertieslistingbyid") {
            val id = call.receive<String>()
            if (id.isNullOrBlank()) {
                return@get call.respond(HttpStatusCode.BadRequest, "ListingID is required")
            }

            val listings = propertyListingDataSource.getListingById(id)


            //val response = listings.map { it.toResponse() }

            call.respond(HttpStatusCode.OK, ListResponse(success = true, data = listings))


            //call.respond(HttpStatusCode.OK, response)
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

        get("/search") {
            val type = call.parameters["type"]
            val city = call.parameters["city"]

            if (type.isNullOrBlank() || city.isNullOrBlank()) {
                return@get call.respond(HttpStatusCode.BadRequest, "Missing type or city parameter")
            }

            val listings = propertyListingDataSource.getListingsByTypeAndCity(type, city)
            val response = listings.map { it.toResponse() }
            call.respond(HttpStatusCode.OK, response)
        }



    }
}