package com.routes

import com.data.models.image.ImageDataSource
import com.data.requests.ImageRequest
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.imageRoutes(
    imageDataSource: ImageDataSource,
) {

    post("/user/profile/image") {
        val request = runCatching { call.receive<ImageRequest>() }.getOrNull() ?: run {
            call.respond(HttpStatusCode.BadRequest, "Dati mancanti o malformati.")
            return@post
        }

        val result = imageDataSource.updatePpById(
                ownerIdentifier = request.ownerId!!,
                base64Image = request.base64Images.first(),
            )


        if (result)
            call.respond(HttpStatusCode.OK, "Immagine profilo aggiornata.")
        else
            call.respond(HttpStatusCode.InternalServerError, "Errore aggiornamento immagine.")
    }

    get("/user/profile/image/{userId}") {
        val userId = call.parameters["userId"]

        if (userId.isNullOrBlank()) {
            call.respond(HttpStatusCode.BadRequest, "userId mancante")
            return@get
        }

        val imageBase64 = imageDataSource.getUserProfileImage(userId)

        if (imageBase64 == null) {
            call.respond(HttpStatusCode.Conflict, "Nessuna immagine associata all'utente")
        } else {
            call.respondText(imageBase64, ContentType.Text.Plain)
        }

    }

    post("/house/image") {
        val request = runCatching { call.receive<ImageRequest>() }.getOrNull() ?: run {
            call.respond(HttpStatusCode.BadRequest, "Dati mancanti o malformati.")
            return@post
        }

        val success = imageDataSource.updateHouseImages(
            houseId = request.ownerId!!,
            base64Images = request.base64Images
        )

        if (success)
            call.respond(HttpStatusCode.OK, "Immagini annuncio aggiornate.")
        else
            call.respond(HttpStatusCode.InternalServerError, "Errore aggiornamento immagini.")
    }

    get("/propertylisting/image/{ownerId}") {
        val ownerId = call.parameters["ownerId"]

        if (ownerId.isNullOrBlank()) {
            call.respond(HttpStatusCode.BadRequest, "ownerId mancante")
            return@get
        }

        val imageBase64 = imageDataSource.getUserProfileImage(ownerId)

        if (imageBase64 == null) {
            call.respond(HttpStatusCode.Conflict, "Nessuna immagine associata all'annuncio")
        } else {
            call.respondText(imageBase64, ContentType.Text.Plain)
        }

    }
}