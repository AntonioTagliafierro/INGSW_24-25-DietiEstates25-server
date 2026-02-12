package com.routes

import com.data.models.activity.Activity
import com.data.models.activity.ActivityDataSource
import com.data.models.activity.ActivityType
import com.data.models.appointment.Appointment
import com.data.models.appointment.AppointmentDataSource
import com.data.models.appointment.AppointmentMessage
import com.data.models.appointment.AppointmentStatus
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
    activityDataSource: ActivityDataSource
) {
    route("/appointments") {

        post("/bookappointment") {
            val request = call.receiveNullable<AppointmentRequest>() ?: run {
                call.respond(HttpStatusCode.BadRequest, "Missing or malformed payload.")
                return@post
            }

            // Create appointment
            val appointment = Appointment(
                listing = request.listing,
                user = request.user,
                agent = request.agent,
                date = request.date
            )

            // First appointment message
            val message = AppointmentMessage(
                sender = request.user,
                timestamp = System.currentTimeMillis(),
                date = request.date,
                status = AppointmentStatus.PENDING
            )

            // Save to database
            val wasCreated = appointmentDataSource.createAppointment(appointment, message)
            if (wasCreated) {
                val result = mailerSendService.sendAppointmentEmail(
                    request.user,
                    request.agent,
                    request.listing,
                    appointment
                )

                if (result.status == Accepted) {
                    call.respond(HttpStatusCode.Created, appointment)
                } else {
                    call.respond(HttpStatusCode.InternalServerError, "Error sending email")
                }
            } else {
                call.respond(HttpStatusCode.InternalServerError, "Error creating appointment")
            }
        }

        post("/message") {
            val request = runCatching { call.receiveNullable<AppointmentMessageRequest>() }.getOrNull() ?: run {
                call.respond(HttpStatusCode.BadRequest, "Missing or malformed payload.")
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
                call.respond(HttpStatusCode.Conflict, "Error inserting message")
                return@post
            }

            call.respond(HttpStatusCode.OK, "Message successfully added")
        }

        post("/accept") {
            val appointmentId = call.request.queryParameters["appointmentId"]

            if (appointmentId.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, "Missing appointmentId parameter")
                return@post
            }

            val success = appointmentDataSource.acceptAppointment(appointmentId)
            if (!success) {
                call.respond(HttpStatusCode.Conflict, "Error accepting appointment $appointmentId")
                return@post
            }

            val appointment = appointmentDataSource.getAppointment(appointmentId)

            val acceptedUser =
                if (appointment?.messages?.last()?.sender?.name != appointment?.user?.name)
                    appointment?.user?.name
                else appointment?.agent?.name

            if (
                activityDataSource.insertActivity(
                    Activity(
                        userId = userDataSource.getUserByUsername(acceptedUser!!)!!.id.toString(),
                        type = ActivityType.ACCEPTED,
                        text = "You accepted the appointment on ${appointment?.messages?.last()?.date}"
                    )
                )
            ) {
                call.respond(HttpStatusCode.OK, "Appointment $appointmentId successfully accepted")
            } else {
                call.respond(HttpStatusCode.Conflict, "Error inserting activity")
                return@post
            }
        }

        post("/decline") {
            val appointmentId = call.request.queryParameters["appointmentId"]

            if (appointmentId.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, "Missing appointmentId parameter")
                return@post
            }

            val success = appointmentDataSource.declineAppointment(appointmentId)
            if (!success) {
                call.respond(HttpStatusCode.Conflict, "Error declining appointment $appointmentId")
                return@post
            }

            val appointment = appointmentDataSource.getAppointment(appointmentId)

            val declinedUser =
                if (appointment?.messages?.last()?.sender?.name != appointment?.user?.name)
                    appointment?.user?.name
                else appointment?.agent?.name

            if (
                activityDataSource.insertActivity(
                    Activity(
                        userId = userDataSource.getUserByUsername(declinedUser!!)!!.id.toString(),
                        type = ActivityType.ACCEPTED,
                        text = "You declined the appointment on ${appointment?.messages?.last()?.date}"
                    )
                )
            ) {
                call.respond(HttpStatusCode.OK, "Appointment $appointmentId successfully declined")
            } else {
                call.respond(HttpStatusCode.Conflict, "Error inserting activity")
                return@post
            }
        }

        get("/summary") {
            val propertyId = call.request.queryParameters["propertyId"]

            if (propertyId.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, "Missing propertyId parameter")
                return@get
            }

            try {
                val summaries = appointmentDataSource.getSummaryAppointments(propertyId)
                call.respond(HttpStatusCode.OK, summaries)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    "Error retrieving global history: ${e.localizedMessage}"
                )
            }
        }

        get("byuser") {
            val userId = call.request.queryParameters["userId"]

            if (userId.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, "Missing userId parameter")
                return@get
            }

            try {
                val appointments = appointmentDataSource.getAppointmentsByUserOrAgent(userId)
                call.respond(HttpStatusCode.OK, appointments)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    "Error retrieving appointments for userId=$userId: ${e.localizedMessage}"
                )
            }
        }

        get("/listingappointments") {
            val listingId = call.request.queryParameters["listingId"]
            val userId = call.request.queryParameters["userId"] // optional

            if (listingId.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, "Missing listingId parameter")
                return@get
            }

            try {
                val appointments = if (userId.isNullOrBlank()) {
                    appointmentDataSource.getAppointmentsByListing(listingId)
                } else {
                    appointmentDataSource.getAppointmentsByUserAndListing(userId, listingId)
                }

                call.respond(HttpStatusCode.OK, appointments)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    "Error retrieving appointments: ${e.localizedMessage}"
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
                    "Error retrieving appointments: ${e.localizedMessage}"
                )
            }
        }
    }
}