package com

import com.data.models.appointment.Appointment
import com.data.models.appointment.AppointmentDataSource
import com.data.models.appointment.AppointmentRequest
import com.data.models.appointment.AppointmentStatus
import com.data.models.appointment.MongoAppointmentDataSource
import com.data.models.appointment.UpdateStatusRequest
import com.data.models.notification.MongoNotificationDataSource
import com.data.models.notification.Notification
import com.data.models.notification.NotificationDataSource
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlinx.serialization.Serializable


@Serializable
data class UpdateAppointmentStatusRequest(
    val appointmentId: String,
    val status: String
)

fun Route.appointmentRoutes(
    appointmentDataSource: AppointmentDataSource,
    notificationDataSource: NotificationDataSource
){
    route("/appointment"){

        post("addappointment") {
            val request = call.receive<AppointmentRequest>()
            val appointment = Appointment(
                propertyId = request.propertyId,
                userEmail = request.userEmail,
                agentEmail = request.agentEmail,
                dateTime = request.dateTime
            )

            val success = appointmentDataSource.insertAppointment(appointment)
            if (success) {
                val notification = Notification(
                    recipientEmail = appointment.agentEmail,
                    message = "New request for appointment from ${appointment.userEmail} in ${appointment.dateTime} for ${appointment.propertyId}",
                )
                notificationDataSource.insertNotification(notification)
                call.respond(HttpStatusCode.OK, "You have sent your appointment's request succesfully")

            }else{
                call.respond(HttpStatusCode.InternalServerError, "Something went wrong")
            }

        }

        get("/agent/{email}") {
            val email = call.parameters["email"] ?: return@get call.respond(HttpStatusCode.BadRequest)
            val appointments = appointmentDataSource.getAppointmentsByAgent(email)
            call.respond(appointments)
        }

        post("/update-status") {
            val request = call.receive<UpdateAppointmentStatusRequest>()

            val newStatus = try {
                AppointmentStatus.valueOf(request.status.uppercase())
            } catch (e: IllegalArgumentException) {
                call.respond("Status not valid")
                return@post
            }

            val updated = appointmentDataSource.updateStatus(request.appointmentId, newStatus)
            if (!updated) {
                call.respond("Errore nell'aggiornamento dello status")
                return@post
            }

            // Recupera l'appuntamento aggiornato
            val appointment = appointmentDataSource.getAppointmentById(request.appointmentId)
            if (appointment != null) {
                val message = when(newStatus) {
                    AppointmentStatus.CONFIRMED -> "Il tuo appuntamento con ${appointment.agentEmail} è stato accettato."
                    AppointmentStatus.REFUSED -> "Il tuo appuntamento con ${appointment.agentEmail} è stato rifiutato."
                    else -> ""
                }

                if (message.isNotEmpty()) {
                    val notification = com.data.models.notification.Notification(
                        id = java.util.UUID.randomUUID().toString(),
                        recipientEmail = appointment.userEmail,
                        message = message
                    )
                    notificationDataSource.insertNotification(notification)
                }
            }

            call.respond("Status aggiornato correttamente")
        }
    }


}