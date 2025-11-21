package com.routes

import com.data.models.activity.Activity
import com.data.models.activity.ActivityDataSource
import com.data.models.activity.ActivityType
import com.data.models.appointment.Appointment
import com.data.models.appointment.AppointmentDataSource
import com.data.models.appointment.AppointmentMessage
import com.data.models.appointment.AppointmentStatus
import com.data.models.propertylisting.PropertyListingDataSource
import com.data.models.user.UserDataSource
import com.data.requests.AppointmentMessageRequest
import com.data.requests.AppointmentRequest
import com.service.mailservice.MailerSendService
import io.ktor.http.HttpStatusCode
import io.ktor.http.HttpStatusCode.Companion.Accepted
import io.ktor.server.request.receiveNullable
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.appointmentRouting(
    appointmentDataSource: AppointmentDataSource,
    userDataSource: UserDataSource,
    mailerSendService: MailerSendService,
    listingDataSource: PropertyListingDataSource,
    activityDataSource: ActivityDataSource
) {
    route("/appointments") {



        post("/bookappointment") {
            val request = call.receiveNullable<AppointmentRequest>() ?: run {
                call.respond(HttpStatusCode.BadRequest, "Payload mancante o malformato.")
                return@post
            }

            // Creazione dell'appuntamento
            val appointment = Appointment(
                listing = request.listing,
                user = request.user,
                agent = request.agent,
                date = request.date
            )


            // Primo messaggio dell'appuntamento
            val message = AppointmentMessage(
                sender = request.user, // usa direttamente request.user
                timestamp = System.currentTimeMillis(),
                date = request.date,
                status = AppointmentStatus.PENDING
            )

            // Salvataggio nel database
            val wasCreated = appointmentDataSource.createAppointment(appointment, message)
            if (wasCreated) {
                // Invia email
                val result = mailerSendService.sendAppointmentEmail(
                    request.user,
                    request.agent,
                    request.listing,
                    appointment
                )

                if (result.status == Accepted) {
                    call.respond(HttpStatusCode.Created, appointment)
                } else {
                    call.respond(HttpStatusCode.InternalServerError, "Errore invio email")
                }
            } else {
                call.respond(HttpStatusCode.InternalServerError, "Errore creazione appuntamento")
            }
        }

        post("/message") {
            val request = runCatching { call.receiveNullable<AppointmentMessageRequest>() }.getOrNull() ?: run {
                call.respond(HttpStatusCode.BadRequest, "Payload mancante o malformato.")
                return@post
            }

            val newMessage = AppointmentMessage(
                sender = request.sender,
                timestamp = System.currentTimeMillis(),
                date = request.date.toString(),
                status = AppointmentStatus.PENDING
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

            val appointment = appointmentDataSource.getAppointment(appointmentId)



            val acceptedUser =
                if (appointment?.messages?.last()?.sender?.name != appointment?.user?.name) appointment?.user?.name else appointment?.agent?.name

            if (activityDataSource.insertActivity(
                    Activity(
                        userId = userDataSource.getUserByUsername(acceptedUser!!)!!.id.toString(),
                        type = ActivityType.ACCEPTED,
                        text = "You Accepted the appointment on ${appointment?.messages?.last()?.date}"
                    )
                )
            ) {
                call.respond(HttpStatusCode.OK, "Appuntamento $appointmentId accettato con successo")
            } else {
                call.respond(HttpStatusCode.Conflict, "Errore durante l'inserimento dell'activity")
                return@post
            }
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

            val appointment = appointmentDataSource.getAppointment(appointmentId)


            val declinedUser =
                if (appointment?.messages?.last()?.sender?.name != appointment?.user?.name) appointment?.user?.name else appointment?.agent?.name

            if (activityDataSource.insertActivity(
                    Activity(
                        userId = userDataSource.getUserByUsername(declinedUser!!)!!.id.toString(),
                        type = ActivityType.ACCEPTED,
                        text = "You declined the appointment on ${appointment?.messages?.last()?.date}"
                    )
                )
            ) {
                call.respond(HttpStatusCode.OK, "Appuntamento $appointmentId accettato con successo")
            } else {
                call.respond(HttpStatusCode.Conflict, "Errore durante l'inserimento dell'activity")
                return@post
            }
        }

        get("/summary") {
            val propertyId = call.request.queryParameters["propertyId"]

            if (propertyId.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, "Parametro propertyId mancante")
                return@get
            }

            try {
                val summaries = appointmentDataSource.getSummaryAppointments(propertyId)

                call.respond(HttpStatusCode.OK, summaries)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    "Errore durante il recupero dello storico globale: ${e.localizedMessage}"
                )
            }

        }

        get("byuser") {
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

        get("/listingappointments") {
            val listingId = call.request.queryParameters["listingId"]
            val userId = call.request.queryParameters["userId"] // opzionale

            if (listingId.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, "Parametro listingId mancante")
                return@get
            }

            try {
                val appointments = if (userId.isNullOrBlank()) {
                    //Tutti gli appuntamenti del listing
                    appointmentDataSource.getAppointmentsByListing(listingId)
                } else {
                    //Solo appuntamenti di quell'utente per il dato listing
                    appointmentDataSource.getAppointmentsByUserAndListing(userId, listingId)
                }

                call.respond(HttpStatusCode.OK, appointments)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    "Errore durante il recupero degli appuntamenti: ${e.localizedMessage}"
                )
            }
        }

        get("/all") {

            try {
                val appointments = appointmentDataSource.getAppointments()

                call.respond(HttpStatusCode.OK, appointments)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    "Errore durante il recupero degli appuntamenti: ${e.localizedMessage}"
                )
            }
        }

    }


}