package com

import com.data.models.property.Property
import com.data.models.property.PropertyDataSource
import com.data.requests.PropertyUploadRequest
import com.service.GeoapifyService
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post

fun Route.propertyRoutes(
    propertyDataSource: PropertyDataSource,
    geoapifyService: GeoapifyService
) {
    authenticate {
        post("/properties/upload") {
            val request = call.receive<PropertyUploadRequest>()
            val principal = call.principal<JWTPrincipal>()!!
            val email = principal.getClaim("email", String::class)!!

            val indicators = geoapifyService.getNearbyIndicators(request.latitude, request.longitude)

            val property = Property(
                title = request.title,
                latitude = request.latitude,
                longitude = request.longitude,
                indicators = indicators,
                agentEmail = email
            )

            val wasInserted = propertyDataSource.insertProperty(property)
            if (!wasInserted) {
                call.respond(HttpStatusCode.InternalServerError, "Errore inserimento immobile")
                return@post
            }

            call.respond(HttpStatusCode.OK, "Immobile salvato con successo")
        }
    }
}