package com

import com.data.models.activity.Activity
import com.data.models.activity.ActivityDataSource
import com.data.models.activity.ActivityType
import com.data.models.appointment.Appointment
import com.data.models.appointment.AppointmentDataSource
import com.data.models.appointment.AppointmentMessage
import com.data.models.appointment.AppointmentStatus
import com.data.models.propertylisting.PropertyListingDataSource
import com.data.models.user.UserDataSource
import com.data.models.user.myToLowerCase
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

//        post("/bookappointment") {
//            val request = kotlin.runCatching { call.receiveNullable<AppointmentRequest>() }.getOrNull() ?: run {
//                call.respond(HttpStatusCode.BadRequest, "Payload mancante o malformato.")
//                return@post
//            }
//
//            val appointment = Appointment(
//                listing = request.listing,
//                user = request.user,
//                agent = request.agent,
//                date = request.date
//            )
//
//            val message = AppointmentMessage(
//                senderName = request.user.name!!,
//                timestamp = System.currentTimeMillis(),
//                date = request.date,
//                status = AppointmentStatus.PENDING
//            )
//            val existingAppointment = appointmentDataSource.getAppointment(
//                propertyId = request.listing.id.toString(),
//                buyerName = request.user.name
//            )
//
//            if (existingAppointment == null) {
//
//                val newAppointment = Appointment(
//                    listing = request.listing,
//                    user = request.user,
//                    agent = request.agent,
//                    date = request.date,
//                    messages = mutableListOf(message)
//                )
//
//                val wasCreated = appointmentDataSource.createAppointemnt(newAppointment, message)
//                if (!wasCreated) {
//                    call.respond(HttpStatusCode.Conflict, "Errore durante la creazione dell'appuntamento")
//                    return@post
//                }
//
//            } else {
//
//                val success = appointmentDataSource.addAppointmentMessage(existingAppointment.id.toString(), message)
//                if (!success) {
//                    call.respond(HttpStatusCode.Conflict, "Errore durante l'inserimento del messaggio")
//                    return@post
//                }
//
//            }
//            val wasAcknowledged = appointmentDataSource.createAppointemnt(appointment, message)
//            if (!wasAcknowledged) {
//                call.respond(HttpStatusCode.Conflict, "Errore durante la creazione dell'appuntamento")
//                return@post
//            }
//            val listingProperty = listingDataSource.getListingById(request.listing.id.toString())
//
//            if ( activityDataSource.insertActivity(
//                    Activity(
//                        userId = userDataSource.getUserByUsername(message.senderName)!!.id.toString(),
//                        type = if( appointment == null )ActivityType.INSERT else ActivityType.OFFERED,
//                        text =  if( existingAppointment == null ) "You inserted a listing on ${listingProperty!!.property.street}" else "You ask for an appointment on ${message.date} for the listing on ${listingProperty!!.property.street}"
//                    )
//                )){
//                val updatedAppointment = appointmentDataSource.getAppointment(
//                    propertyId = request.listing.id.toString(),
//                    buyerName = request.user.name
//                )
//
//                call.respond(HttpStatusCode.Created, updatedAppointment ?: message)
//            } else {
//                call.respond(HttpStatusCode.Conflict, "Errore durante l'inserimento dell'activity")
//                return@post
//            }
//        }

        post("/bookappointment") {
            val request = call.receiveNullable<AppointmentRequest>() ?: run {
                call.respond(HttpStatusCode.BadRequest, "Payload mancante o malformato.")
                return@post
            }

            val listing = listingDataSource.getListingById(request.listingId)
            val user = userDataSource.getUserById(request.userId)
            val agent = userDataSource.getUserById(request.agentId)

            if (listing == null || user == null || agent == null) {
                call.respond(HttpStatusCode.BadRequest, "Dati non validi.")
                return@post
            }

            val appointment = Appointment(
                listing = listing,
                user = user,
                agent = agent,
                date = request.date
            )

            val message = AppointmentMessage(
                senderName = user.username,
                timestamp = System.currentTimeMillis(),
                date = request.date,
                status = AppointmentStatus.PENDING
            )

            val wasCreated = appointmentDataSource.createAppointemnt(appointment, message)
            if (wasCreated) {
                val result = mailerSendService.sendAppointmentEmail(user, agent, listing, appointment)
                if (result.status == Accepted) {
                    call.respond(HttpStatusCode.Created, appointment)
                }
            } else
                call.respond(HttpStatusCode.InternalServerError, "Errore creazione appuntamento")
        }

        post("/message") {
            val request = runCatching { call.receiveNullable<AppointmentMessageRequest>() }.getOrNull() ?: run {
                call.respond(HttpStatusCode.BadRequest, "Payload mancante o malformato.")
                return@post
            }

            val newMessage = AppointmentMessage(
                senderName = request.senderId,
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

            if (!listingDataSource.acceptListing(appointment!!.listing.id.toString())) {
                call.respond(HttpStatusCode.Conflict, "Errore durante l'update del listing ${appointment.listing.id}")
                return@post
            }

            val acceptedUser =
                if (appointment.messages.last().senderName != appointment.user.name) appointment.user.name else appointment.agent.name

            if (activityDataSource.insertActivity(
                    Activity(
                        userId = userDataSource.getUserByUsername(acceptedUser!!)!!.id.toString(),
                        type = ActivityType.ACCEPTED,
                        text = "You Accepted the appointment on ${appointment.messages.last().date}"
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
                if (appointment?.messages?.last()?.senderName != appointment?.user?.name) appointment?.user?.name else appointment?.agent?.name

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

        get("byUser") {
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


    }


}