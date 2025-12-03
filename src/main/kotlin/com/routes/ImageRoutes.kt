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
            call.respond(HttpStatusCode.BadRequest, "Missing or malformed data.")
            return@post
        }

        val result = imageDataSource.updatePpById(
            ownerIdentifier = request.ownerId!!,
            base64Image = request.base64Images.first(),
        )

        if (result)
            call.respond(HttpStatusCode.OK, "Profile image updated.")
        else
            call.respond(HttpStatusCode.InternalServerError, "Error updating image.")
    }

    get("/user/profile/image/{userId}") {
        val userId = call.parameters["userId"]

        if (userId.isNullOrBlank()) {
            call.respond(HttpStatusCode.BadRequest, "Missing userId.")
            return@get
        }

        val imageBase64 = imageDataSource.getUserProfileImage(userId)

        if (imageBase64 == null) {
            call.respond(HttpStatusCode.Conflict, "No image associated with this user.")
        } else {
            call.respondText(imageBase64, ContentType.Text.Plain)
        }
    }

    post("/house/image") {
        val request = runCatching { call.receive<ImageRequest>() }.getOrNull() ?: run {
            call.respond(HttpStatusCode.BadRequest, "Missing or malformed data.")
            return@post
        }

        val success = imageDataSource.updateHouseImages(
            houseId = request.ownerId!!,
            base64Images = request.base64Images
        )

        if (success)
            call.respond(HttpStatusCode.OK, "Listing images updated.")
        else
            call.respond(HttpStatusCode.InternalServerError, "Error updating images.")
    }

    get("/propertylisting/image/{ownerId}") {
        val ownerId = call.parameters["ownerId"]

        if (ownerId.isNullOrBlank()) {
            call.respond(HttpStatusCode.BadRequest, "Missing ownerId.")
            return@get
        }

        val imageBase64 = imageDataSource.getUserProfileImage(ownerId)

        if (imageBase64 == null) {
            call.respond(HttpStatusCode.Conflict, "No image associated with this listing.")
        } else {
            call.respondText(imageBase64, ContentType.Text.Plain)
        }
    }
}
