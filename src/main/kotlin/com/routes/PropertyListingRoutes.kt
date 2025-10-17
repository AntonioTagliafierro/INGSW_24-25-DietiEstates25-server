package com.routes

import com.data.models.propertylisting.PropertyListingDataSource
import com.data.requests.PropertyListingRequest

import com.data.requests.PropertySearchRequest
import com.data.requests.toEntity

import com.data.responses.ListResponse
import com.data.responses.PropertyListingResponse
import com.data.responses.toResponse
import com.mongodb.client.model.Filters
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import org.bson.conversions.Bson

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

        get("/{id}") {
            val id = call.parameters["id"]

            if (id.isNullOrBlank()) {
                return@get call.respond(HttpStatusCode.BadRequest, "ListingID is required")
            }

            val listing = propertyListingDataSource.getListingById(id)

            println("PORCODIO CHE SBALLO ${listing?.property?.city ?: "AOOOO..."}")

            if (listing != null) {
                call.respond(HttpStatusCode.OK, listing)
            } else {
                call.respond(HttpStatusCode.NotFound, "Listing not found")
            }
        }

        get("getpropertieslistingbyid/{id}") {
            val id = call.parameters["id"]

            if (id.isNullOrBlank()) {
                return@get call.respond(HttpStatusCode.BadRequest, "ListingID is required")
            }

            val listing = propertyListingDataSource.getListingById(id)

            if (listing != null) {
                call.respond(HttpStatusCode.OK, listing)
            } else {
                call.respond(HttpStatusCode.NotFound, "Listing not found")
            }
        }


        get("/search") {
            val type = call.request.queryParameters["type"]
            val city = call.request.queryParameters["city"]

            println(" Ricevuta chiamata GET /search con type=$type, city=$city")

            if (type.isNullOrBlank() || city.isNullOrBlank()) {
                println(" Parametri mancanti")
                return@get call.respond(HttpStatusCode.BadRequest, "Missing type or city parameter")
            }

            try {
                val listings = propertyListingDataSource.getListingsByTypeAndCity(type, city)
                println(" Recuperati ${listings.size} risultati dal DB")

                val response = ListResponse(
                    success = true,
                    data = listings,
                    message = null
                )

                call.respond(HttpStatusCode.OK, response)
            } catch (e: Exception) {
                e.printStackTrace()
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ListResponse<List<PropertyListingResponse>>(
                        success = false,
                        data = null,
                        message = "Errore lato server: ${e.message}"
                    )
                )
            }
        }


        post("/searchWithFilters") {
            val request = call.receive<PropertySearchRequest>()

            val filters = mutableListOf<Bson>()

            // Filtra per type (sempre obbligatorio)
            request.type?.let { filters += Filters.eq("type", it) }

            // Solo se city NON è "ALL" aggiungiamo il filtro
            if (!request.city.isNullOrBlank() && request.city != "ALL") {
                filters += Filters.eq("property.city", request.city)
            }

            // Prezzo minimo
            request.minPrice?.let { filters += Filters.gte("price", it.toInt()) }

            // Prezzo massimo
            request.maxPrice?.let { filters += Filters.lte("price", it.toInt()) }

            // Stanze (>= rooms)
            request.rooms?.let {
                if (it > 0) filters += Filters.gte("property.numberOfRooms", it)
            }

            // Classe energetica
            request.energyClass?.let { filters += Filters.eq("property.energyClass", it) }

            // Boolean flags
            if (request.elevator == true) filters += Filters.eq("property.elevator", true)
            if (request.gatehouse == true) filters += Filters.eq("property.gatehouse", true)
            if (request.balcony == true) filters += Filters.eq("property.balcony", true)
            if (request.roof == true) filters += Filters.eq("property.roof", true)

            // Query finale
            val query = if (filters.isEmpty()) Filters.empty() else Filters.and(filters)

            val listings = propertyListingDataSource.searchWithFilters(query)

            if (listings.isEmpty()) {
                call.respond(HttpStatusCode.NotFound, "Nessuna proprietà trovata")
            } else {
                val response = listings.map { it.toResponse() }
                call.respond(HttpStatusCode.OK, response)
            }
        }
    }

    }
