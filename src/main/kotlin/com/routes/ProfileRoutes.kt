package com.routes

import com.data.models.activity.Activity
import com.data.models.activity.ActivityDataSource
import com.data.models.user.UserDataSource
import com.data.requests.AuthRequest
import com.data.requests.UserInfoRequest
import com.data.responses.ListResponse
import com.security.hashing.HashingService
import com.security.hashing.SaltedHash
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*


fun Route.profileRoutes(
    userDataSource: UserDataSource,
    hashingService: HashingService,
    activityDataSource: ActivityDataSource
) {

    route("/user/profile") {


        post("/reset-password") {
            val request = runCatching { call.receiveNullable<AuthRequest>() }.getOrNull() ?: run {
                call.respond(HttpStatusCode.BadRequest, "Invalid request")
                return@post
            }

            // Trova l'utente
            val user = userDataSource.getUserByEmail(request.email)

            if (user == null) {
                call.respond(HttpStatusCode.NotFound, "User not found")
                return@post
            }


            // Verifica vecchia password
            val isOldPasswordValid = hashingService.verify(
                value = request.password!!,
                saltedHash = SaltedHash(
                    hash = user.password,
                    salt = user.salt
                )
            )

            if (!isOldPasswordValid) {
                call.respond(HttpStatusCode.Unauthorized, "Incorrect old password")
                return@post
            }

            // Hash della nuova password
            val newHashed = hashingService.generateSaltedHash(request.newPassword!!)

            val updateResult = userDataSource.updateUserPassword(
                email = request.email,
                newHash = newHashed.hash,
                newSalt = newHashed.salt
            )

            if (!updateResult) {
                call.respond(HttpStatusCode.InternalServerError, "Failed to update password")
                return@post
            }


            call.respond(HttpStatusCode.OK, "Password updated successfully")
        }

        post("/user-info") {
            val request = runCatching { call.receiveNullable<UserInfoRequest>() }.getOrNull() ?: run {
                call.respond(HttpStatusCode.BadRequest, "Invalid request format")
                return@post
            }

            // Controllo base
            if (request.email.isBlank() || request.value.isBlank() || request.typeRequest.isBlank()) {
                call.respond(HttpStatusCode.BadRequest, "Missing required fields")
                return@post
            }

            // Recupera l'utente
            val user = userDataSource.getUserByEmail(request.email)
            if (user == null) {
                call.respond(HttpStatusCode.NotFound, "User not found")
                return@post
            }

            // Esegui update in base al campo richiesto
            val success = when (request.typeRequest.lowercase()) {
                "name and surname" -> userDataSource.updateFullName(request.email, request.value)
                "username" -> userDataSource.updateUsername(request.email, request.value)
                else -> {
                    call.respond(HttpStatusCode.BadRequest, "Invalid typeRequest")
                    return@post
                }
            }

            if (!success) {
                call.respond(HttpStatusCode.InternalServerError, "Update failed")
            } else {
                call.respond(HttpStatusCode.OK, "User info updated successfully")
            }
        }

        get("/activities") {
            val userId = call.request.queryParameters["userId"]

            if (userId.isNullOrBlank()) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ListResponse<List<Activity>>(success = false, data = null, message = "Missing or invalid userId")
                )
                return@get
            }

            val activities = activityDataSource.getAllActivityByUser(userId)

            if (activities.isEmpty()) {
                call.respond(
                    HttpStatusCode.OK,
                    ListResponse<List<Activity>>(success = false, data = null, message = "No activities found for userId=$userId")
                )
            } else {
                call.respond(
                    HttpStatusCode.OK,
                    ListResponse(success = true, data = activities, message = null)
                )
            }
        }



    }

}
