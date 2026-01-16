package com.routes

import com.data.models.activity.Activity
import com.data.models.activity.ActivityDataSource
import com.data.models.activity.ActivityType
import com.data.models.agency.Agency
import com.data.models.agency.AgencyDataSource
import com.data.models.user.Role
import com.data.models.user.User
import com.data.models.user.UserDataSource
import com.data.models.user.myToLowerCase
import com.data.requests.AdminRequest
import com.data.requests.UserInfoRequest
import com.data.responses.ListResponse
import com.security.hashing.HashingService
import com.service.mailservice.MailerSendService
import io.ktor.http.*
import io.ktor.http.HttpStatusCode.Companion.Accepted
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.admin(
    hashingService: HashingService,
    userDataSource: UserDataSource,
    mailerSendService : MailerSendService,
    agencyDataSource: AgencyDataSource,
    activityDataSource: ActivityDataSource
){
    route("/admin") {

        route("/users") {

            get {
                val request = runCatching { call.receiveNullable<UserInfoRequest>() }.getOrNull() ?: run {
                    call.respond(HttpStatusCode.BadRequest, "Missing or malformed payload.")
                    return@get
                }

                if (request.typeRequest == "super_admin") {

                    val filteredUsers = try {
                        userDataSource.getUsersByRole(request.value)
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ListResponse<List<Agency>>(success = false, message = "DB error: ${e.message}")
                        )
                        return@get
                    }

                    call.respond(
                        HttpStatusCode.OK,
                        ListResponse(success = true, data = filteredUsers)
                    )

                } else if (request.typeRequest == "agent_user") {

                    val filteredUsers = try {
                        val agency = agencyDataSource.getAgencyByEmail(request.email)
                        if (agency == null) {
                            call.respond(HttpStatusCode.BadRequest, "Agency not found.")
                            return@get
                        }

                        val userIds = agencyDataSource.getAgencyUserIds(agency.id.toString())
                        if (userIds.isEmpty()) {
                            call.respond(HttpStatusCode.OK, ListResponse(success = true, data = emptyList<User>()))
                            return@get
                        }

                        userDataSource.getAgencyUsers(userIds)

                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ListResponse<List<User>>(success = false, message = "DB error: ${e.localizedMessage}")
                        )
                        return@get
                    }

                    call.respond(
                        HttpStatusCode.OK,
                        ListResponse(success = true, data = filteredUsers)
                    )

                } else {

                    val users = try {
                        userDataSource.getAllUsers()
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ListResponse<List<Agency>>(success = false, message = "DB error: ${e.message}")
                        )
                        return@get
                    }

                    call.respond(
                        HttpStatusCode.OK,
                        ListResponse(success = true, data = users)
                    )
                }
            }

            get("/by-email") {
                val email = call.request.queryParameters["email"]

                if (email.isNullOrBlank()) {
                    call.respond(HttpStatusCode.BadRequest, "Missing or empty parameter 'email'")
                    return@get
                }

                val user = userDataSource.getUserByEmail(email)

                if (user != null) {
                    call.respond(HttpStatusCode.OK, mapOf("id" to user.id.toString()))
                } else {
                    call.respond(HttpStatusCode.NotFound, "No user found for email=$email")
                }
            }

            post {

                val request = runCatching { call.receiveNullable<AdminRequest>() }.getOrNull() ?: run {
                    call.respond(HttpStatusCode.BadRequest, "Missing or malformed payload.")
                    return@post
                }

                val admin = userDataSource.getUserByEmail(request.adminEmail)

                if (admin == null) {
                    call.respond(HttpStatusCode.Unauthorized, "Admin not found.")
                    return@post
                }

                if (admin.id.toString() != request.adminId) {
                    call.respond(HttpStatusCode.Unauthorized, "Unable to validate credentials.")
                    return@post
                }

                if (userDataSource.getUserByEmail(request.email) != null ) {
                    call.respond(HttpStatusCode.Unauthorized, "Support admin already exists.")
                    return@post
                }

                val password = admin.generateRandomPassword()
                val saltedHash = hashingService.generateSaltedHash(password)
                println(password)
                val user = User(
                    email = request.email,
                    role = if (request.email.contains("system")) Role.SUPPORT_ADMIN else Role.AGENT_USER,
                    password = saltedHash.hash!!,
                    salt = saltedHash.salt!!
                )

                val wasAcknowledged = userDataSource.insertUser(user)
                if (!wasAcknowledged) {
                    call.respond(HttpStatusCode.Conflict, "Error inserting user.")
                    return@post
                }

                val result = mailerSendService.sendSuppAdminEmail(
                    request.suppAdminEmail,
                    user.email.myToLowerCase(),
                    password
                )

                if (result.status == Accepted) {

                    activityDataSource.insertActivity(
                        Activity(
                            userId = admin.id.toString(),
                            type = ActivityType.INSERT ,
                            text = activityDataSource.textINSERT(user.email)
                        )
                    )

                    call.respond(HttpStatusCode.OK, "Credentials successfully sent to ${request.suppAdminEmail}")
                } else {

                    call.respond(HttpStatusCode.Conflict, "Error sending email.")
                    return@post
                }
            }

        }

    }
}