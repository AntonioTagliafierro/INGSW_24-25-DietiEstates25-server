package com

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


fun Route.agencyRequests(
    hashingService: HashingService,
    userDataSource: UserDataSource,
    agencyDataSource: AgencyDataSource,
    imageDataSource: ImageDataSource,
    activityDataSource: ActivityDataSource
){
    route("/agency") {

        post("/request") {
            val request = kotlin.runCatching { call.receiveNullable<AuthRequest>() }.getOrNull() ?: run {
                call.respond(HttpStatusCode.BadRequest, "Payload mancante o malformato.")
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
                    call.respond(HttpStatusCode.Conflict, "Errore durante l'inserimento user")
                    return@post
                }

                user = userDataSource.getUserByEmail(request.email)
            } else {
                call.respond(HttpStatusCode.Unauthorized, "Questa email esiste già")
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
                call.respond(HttpStatusCode.Conflict, "Errore durante l'inserimento agency")
                return@post
            }

            val agency = agencyDataSource.getAgency(request.agencyName)

            val agencyUser = AgencyUser(
                agencyId = agency!!.id.toString(),
                userId = user.id.toString()
            )

            wasAcknowledged = agencyDataSource.insertAgencyUser(agencyUser)

            if (!wasAcknowledged) {
                call.respond(HttpStatusCode.Conflict, "Errore durante l'inserimento agencyUser")
                return@post
            }

            call.respond(HttpStatusCode.OK, agencyUser)
        }


        post("/request-decision") {
            val request = runCatching { call.receiveNullable<UserInfoRequest>() }.getOrNull() ?: run {
                call.respond(HttpStatusCode.BadRequest, "Payload mancante o malformato.")
                return@post
            }

            val user = userDataSource.getUserByEmail(request.value)
                ?: run {
                    call.respond(HttpStatusCode.Conflict, "Errore durante il retrieve dell'agente")
                    return@post
                }

            val admin = userDataSource.getUserByEmail(request.email)
                ?: run {
                    call.respond(HttpStatusCode.Conflict, "Errore durante il retrieve dell'agente")
                    return@post
                }

            if (request.typeRequest == "accepted") {

                val roleUpdated = userDataSource.updateUserRole(user.email.myToLowerCase(), Role.AGENT_ADMIN)
                if (!roleUpdated) {
                    call.respond(HttpStatusCode.InternalServerError, "Errore durante l'update del ruolo")
                    return@post
                }

                val agencyUpdated = agencyDataSource.updateAgencyState(user.id.toString())
                if (!agencyUpdated) {
                    call.respond(HttpStatusCode.InternalServerError, "Errore durante l'update dell'agenzia")
                    return@post
                }

                activityDataSource.insertActivity(
                    Activity(
                        userId = admin.id.toString(),
                        type = ActivityType.ACCEPTED,
                        text = activityDataSource.textACCEPTED(user.email.myToLowerCase()),
                    )
                )

                call.respond(HttpStatusCode.OK, "Richiesta accettata con successo")
            } else {

                val roleUpdated = userDataSource.deleteUser(user.email.myToLowerCase())
                if (!roleUpdated) {
                    call.respond(HttpStatusCode.InternalServerError, "Errore durante l'update del ruolo")
                    return@post
                }

                val agencyDeleted = agencyDataSource.deleteAgency(user.id.toString())
                if (!agencyDeleted) {
                    call.respond(HttpStatusCode.InternalServerError, "Errore durante l'eliminazione dell'agenzia")
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

                call.respond(HttpStatusCode.OK, "Richiesta rifiutata con successo")
            }
        }

        get("/agencies") {
            val agencies = try {
                agencyDataSource.getAllAgencies()
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ListResponse<List<Agency>>(success = false, message = "Errore DB: ${e.message}")
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
                    "Parametro userId mancante"
                )
                return@get
            }

            try {
                val agency = agencyDataSource.getAgencyByAgentId(userId)

                if (agency == null) {
                    call.respond(
                        HttpStatusCode.NotFound,
                        "Nessuna agenzia trovata per userId=$userId"
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
                    "Errore durante il recupero dell'agenzia: ${e.localizedMessage}"
                )
            }
        }

        put("/pic") {
            try {
                val params = call.receive<Map<String, String>>()  // legge il JSON
                val agencyId = params["agencyId"]
                val profilePic = params["profilePic"]

                if (agencyId.isNullOrBlank() || profilePic.isNullOrBlank()) {
                    call.respond(HttpStatusCode.BadRequest, "Parametri mancanti: agencyId e profilePic obbligatori")
                    return@put
                }

                val result = imageDataSource.updatePpById(agencyId, profilePic)

                if (result) {
                    call.respond(HttpStatusCode.OK,( "Operazione completata"))
                } else {
                    call.respond(HttpStatusCode.BadRequest, ( "Errore sconosciuto"))
                }

            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, "Errore server: ${e.localizedMessage}")
            }
        }

        post("/agent"){
            val request = kotlin.runCatching { call.receiveNullable<UserInfoRequest>() }.getOrNull() ?: run {
                call.respond(HttpStatusCode.BadRequest, "Payload mancante o malformato.")
                return@post
            }

            val agent = userDataSource.getUserByEmail(request.email)
            if (agent == null) {
                call.respond(HttpStatusCode.NotFound, "Nessun utente trovato con email ${request.email}")
                return@post
            }

            val agency = agencyDataSource.getAgencyByEmail(request.value)
            if (agency == null) {
                call.respond(HttpStatusCode.NotFound, "Nessuna agenzia trovata con email ${request.value}")
                return@post
            }


            val wasAcknowledged = agencyDataSource.insertAgencyUser(
                AgencyUser(
                    agencyId = agency.id.toString(),
                    userId = agent.id.toString()
                )
            )

            if (!wasAcknowledged) {
                call.respond(HttpStatusCode.Conflict, "Errore durante l'iserimento")
                return@post
            }

            call.respond(HttpStatusCode.OK, "Operazione completata con successo")
        }


    }

    get("/agents/name") {
        val email = call.request.queryParameters["email"]

        if (email.isNullOrBlank()) {
            call.respond(HttpStatusCode.BadRequest, "Parametro 'email' mancante")
            return@get
        }

        try {
            val agentName = userDataSource.getUserByEmail(email)

            if (agentName == null) {
                call.respond(HttpStatusCode.NotFound, "Nessun agente trovato con email $email")
            } else {
                call.respond(HttpStatusCode.OK, agentName.username)
            }
        } catch (e: Exception) {
            call.respond(HttpStatusCode.InternalServerError, "Errore: ${e.localizedMessage}")
        }
    }
}
