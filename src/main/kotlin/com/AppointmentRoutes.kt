package com

import com.data.models.appointment.Appointment
import com.data.models.appointment.AppointmentDataSource
import com.data.models.appointment.AppointmentMessage
import com.data.requests.AppointmentMessageRequest
import com.data.requests.AppointmentRequest
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receiveNullable
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route




fun Route.appointmentRoutes(
    appointmentDataSource: AppointmentDataSource
){
    route("/appointment"){

        post("/create") {
            val request = kotlin.runCatching { call.receiveNullable<AppointmentRequest>() }.getOrNull() ?: run {
                call.respond(HttpStatusCode.BadRequest, "Payload mancante o malformato.")
                return@post
            }

            val appointment = Appointment(
                propertyId = request.propertyId,
                userId = request.userId,
                agentId = request.agentId,
                date = request.date
            )

            val firstMessage = AppointmentMessage(
                senderId = request.userId,
                timestamp = System.currentTimeMillis(),
                date = request.date,
                accepted = null
            )

            val wasAcknowledged = appointmentDataSource.createAppointemnt(appointment, firstMessage)
            if (!wasAcknowledged) {
                call.respond(HttpStatusCode.Conflict, "Errore durante la creazione dell'appuntamento")
                return@post
            }

            call.respond(HttpStatusCode.OK, appointment)
        }


        post("/message") {
            val request = runCatching { call.receiveNullable<AppointmentMessageRequest>() }.getOrNull() ?: run {
                call.respond(HttpStatusCode.BadRequest, "Payload mancante o malformato.")
                return@post
            }

            val newMessage = AppointmentMessage(
                senderId = request.senderId,
                timestamp = System.currentTimeMillis(),
                date = request.date,
                accepted = null
            )

            val success = appointmentDataSource.addAppointmentMessage(request.appointmentId, newMessage)
            if (!success) {
                call.respond(HttpStatusCode.Conflict, "Errore durante l'inserimento del messaggio")
                return@post
            }

            call.respond(HttpStatusCode.OK, "Messaggio inserito con successo")
        }

        post("/accept") {
            val appointmentId = call.request.queryParameters["appointmentId"]

            if (appointmentId.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, "Parametro appointmentId mancante")
                return@post
            }

            val success = appointmentDataSource.acceptAppointment(appointmentId)
            if (!success) {
                call.respond(HttpStatusCode.Conflict, "Errore durante l'accettazione dell'appuntamento $appointmentId")
                return@post
            }

            call.respond(HttpStatusCode.OK, "Appuntamento $appointmentId accettato con successo")
        }

        post("/decline") {
            val appointmentId = call.request.queryParameters["appointmentId"]

            if (appointmentId.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, "Parametro appointmentId mancante")
                return@post
            }

            val success = appointmentDataSource.declineAppointment(appointmentId)
            if (!success) {
                call.respond(HttpStatusCode.Conflict, "Errore durante il rifiuto dell'appuntamento $appointmentId")
                return@post
            }

            call.respond(HttpStatusCode.OK, "Appuntamento $appointmentId rifiutato con successo")
        }

        get("/all") {
            try {
                val summaries = appointmentDataSource.getAllAppointments()
                call.respond(HttpStatusCode.OK, summaries)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    "Errore durante il recupero dello storico globale: ${e.localizedMessage}"
                )
            }
        }

        get ("byUser"){
            val userId = call.request.queryParameters["userId"]

            if (userId.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, "Parametro userId mancante")
                return@get
            }

            try {
                val appointments = appointmentDataSource.getAppointmentsByUserOrAgent(userId)
                call.respond(HttpStatusCode.OK, appointments)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    "Errore durante il recupero appuntamenti per userId=$userId: ${e.localizedMessage}"
                )
            }
        }

        get("/byListing") {
            val listingId = call.request.queryParameters["listingId"]

            if (listingId.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, "Parametro listingId mancante")
                return@get
            }

            try {
                val appointments = appointmentDataSource.getAppointmentByListingId(listingId)
                call.respond(HttpStatusCode.OK, appointments)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    "Errore durante il recupero appuntamenti per listingId=$listingId: ${e.localizedMessage}"
                )
            }
        }



    }


}