package com.routes

import com.data.models.activity.Activity
import com.data.models.activity.ActivityDataSource
import com.data.models.activity.ActivityType
import com.data.models.agency.Agency
import com.data.models.agency.AgencyDataSource
import com.data.models.agency.AgencyUser
import com.data.models.image.ImageDataSource
import com.data.models.user.Role
import com.data.models.user.User
import com.data.models.user.UserDataSource
import com.data.models.user.myToLowerCase
import com.data.requests.AuthRequest
import com.data.requests.UserInfoRequest
import com.data.responses.ListResponse
import com.security.hashing.HashingService
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

private const val INVALID_PAYLOAD = "Missing or malformed payload."

fun Route.agencyRequests(
    hashingService: HashingService,
    userDataSource: UserDataSource,
    agencyDataSource: AgencyDataSource,
    imageDataSource: ImageDataSource,
    activityDataSource: ActivityDataSource
){
    route("/agency") {

        post("/request") {
            val request = runCatching { call.receiveNullable<AuthRequest>() }.getOrNull() ?: run {
                call.respond(HttpStatusCode.BadRequest, INVALID_PAYLOAD)
                return@post
            }

            var user = userDataSource.getUserByEmail(request.email)

            if (user == null) {
                val saltedHash = hashingService.generateSaltedHash(request.password!!)

                val wasAcknowledged = userDataSource.insertUser(
                    User(
                        email = request.email,
                        password = saltedHash.hash,
                        salt = saltedHash.salt,
                        role = Role.PENDING_AGENT_ADMIN
                    )
                )

                if (!wasAcknowledged) {
                    call.respond(HttpStatusCode.Conflict, "Error while inserting user.")
                    return@post
                }

                user = userDataSource.getUserByEmail(request.email)
            } else {
                call.respond(HttpStatusCode.Unauthorized, "This email already exists.")
                return@post
            }

            var wasAcknowledged = agencyDataSource.insertAgency(
                Agency(
                    name = request.agencyName!!,
                    agencyEmail = user!!.email.myToLowerCase(),
                    pending = true
                )
            )

            if (!wasAcknowledged) {
                call.respond(HttpStatusCode.Conflict, "Error while inserting agency.")
                return@post
            }

            val agency = agencyDataSource.getAgency(request.agencyName)

            val agencyUser = AgencyUser(
                agencyId = agency!!.id.toString(),
                userId = user.id.toString()
            )

            wasAcknowledged = agencyDataSource.insertAgencyUser(agencyUser)

            if (!wasAcknowledged) {
                call.respond(HttpStatusCode.Conflict, "Error while inserting agencyUser.")
                return@post
            }

            call.respond(HttpStatusCode.OK, agencyUser)
        }


        post("/request-decision") {
            val request = runCatching { call.receiveNullable<UserInfoRequest>() }.getOrNull() ?: run {
                call.respond(HttpStatusCode.BadRequest, INVALID_PAYLOAD)
                return@post
            }

            val user = userDataSource.getUserByEmail(request.value)
                ?: run {
                    call.respond(HttpStatusCode.Conflict, "Error retrieving agent.")
                    return@post
                }

            val admin = userDataSource.getUserByEmail(request.email)
                ?: run {
                    call.respond(HttpStatusCode.Conflict, "Error retrieving admin.")
                    return@post
                }

            if (request.typeRequest == "accepted") {

                val roleUpdated = userDataSource.updateUserRole(user.email.myToLowerCase(), Role.AGENT_ADMIN)
                if (!roleUpdated) {
                    call.respond(HttpStatusCode.InternalServerError, "Error while updating user role.")
                    return@post
                }

                val agencyUpdated = agencyDataSource.updateAgencyState(user.id.toString())
                if (!agencyUpdated) {
                    call.respond(HttpStatusCode.InternalServerError, "Error while updating agency state.")
                    return@post
                }

                activityDataSource.insertActivity(
                    Activity(
                        userId = admin.id.toString(),
                        type = ActivityType.ACCEPTED,
                        text = activityDataSource.textACCEPTED(user.email.myToLowerCase()),
                    )
                )

                call.respond(HttpStatusCode.OK, "Request successfully approved.")
            } else {

                val roleUpdated = userDataSource.deleteUser(user.email.myToLowerCase())
                if (!roleUpdated) {
                    call.respond(HttpStatusCode.InternalServerError, "Error while removing user.")
                    return@post
                }

                val agencyDeleted = agencyDataSource.deleteAgency(user.id.toString())
                if (!agencyDeleted) {
                    call.respond(HttpStatusCode.InternalServerError, "Error while deleting agency.")
                    return@post
                }

                imageDataSource.deleteImages(user.id.toString())

                val agency = agencyDataSource.getAgencyByEmail(user.email.myToLowerCase())
                if (agency != null) {
                    imageDataSource.deleteImages(agency.id.toString())
                }

                activityDataSource.insertActivity(
                    Activity(
                        userId = admin.id.toString(),
                        type = ActivityType.DECLINED,
                        text = activityDataSource.textACCEPTED(user.email.myToLowerCase())
                    )
                )

                call.respond(HttpStatusCode.OK, "Request successfully declined.")
            }
        }

        get("/agencies") {
            val agencies = try {
                agencyDataSource.getAllAgencies()
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ListResponse<List<Agency>>(success = false, message = "DB error: ${e.message}")
                )
                return@get
            }

            call.respond(
                HttpStatusCode.OK,
                ListResponse(success = true, data = agencies)
            )
        }

        get {
            val userId = call.request.queryParameters["userId"]

            if (userId.isNullOrBlank()) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    "Missing parameter userId"
                )
                return@get
            }

            try {
                val agency = agencyDataSource.getAgencyByAgentId(userId)

                if (agency == null) {
                    call.respond(
                        HttpStatusCode.NotFound,
                        "No agency found for userId=$userId"
                    )
                } else {
                    call.respond(
                        HttpStatusCode.OK,
                        agency
                    )
                }
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    "Error retrieving agency: ${e.localizedMessage}"
                )
            }
        }

        put("/pic") {
            try {
                val params = call.receive<Map<String, String>>()  // reads JSON
                val agencyId = params["agencyId"]
                val profilePic = params["profilePic"]

                if (agencyId.isNullOrBlank() || profilePic.isNullOrBlank()) {
                    call.respond(HttpStatusCode.BadRequest, "Missing parameters: agencyId and profilePic are required.")
                    return@put
                }

                val result = imageDataSource.updatePpById(agencyId, profilePic)

                if (result) {
                    call.respond(HttpStatusCode.OK,( "Operation completed."))
                } else {
                    call.respond(HttpStatusCode.BadRequest, ( "Unknown error."))
                }

            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, "Server error: ${e.localizedMessage}")
            }
        }

        post("/agent"){
            val request = runCatching { call.receiveNullable<UserInfoRequest>() }.getOrNull() ?: run {
                call.respond(HttpStatusCode.BadRequest, INVALID_PAYLOAD)
                return@post
            }

            val agent = userDataSource.getUserByEmail(request.email)
            if (agent == null) {
                call.respond(HttpStatusCode.NotFound, "No user found with email ${request.email}")
                return@post
            }

            val agency = agencyDataSource.getAgencyByEmail(request.value)
            if (agency == null) {
                call.respond(HttpStatusCode.NotFound, "No agency found with email ${request.value}")
                return@post
            }


            val wasAcknowledged = agencyDataSource.insertAgencyUser(
                AgencyUser(
                    agencyId = agency.id.toString(),
                    userId = agent.id.toString()
                )
            )

            if (!wasAcknowledged) {
                call.respond(HttpStatusCode.Conflict, "Error while inserting agent.")
                return@post
            }

            call.respond(HttpStatusCode.OK, "Operation successfully completed.")
        }


    }

    get("/agents/name") {
        val email = call.request.queryParameters["email"]

        if (email.isNullOrBlank()) {
            call.respond(HttpStatusCode.BadRequest, "Missing parameter 'email'.")
            return@get
        }

        try {
            val agentName = userDataSource.getUserByEmail(email)

            if (agentName == null) {
                call.respond(HttpStatusCode.NotFound, "No agent found with email $email")
            } else {
                call.respond(HttpStatusCode.OK, agentName.username)
            }
        } catch (e: Exception) {
            call.respond(HttpStatusCode.InternalServerError, "Error: ${e.localizedMessage}")
        }
    }
}