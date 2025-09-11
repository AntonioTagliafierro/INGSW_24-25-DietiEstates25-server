package com

import com.data.models.notification.NotificationDataSource
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.notificationRoutes(notificationDataSource: NotificationDataSource) {
    route("/notifications") {

        // Recupera tutte le notifiche di un utente
        get("/{userEmail}") {
            val userEmail = call.parameters["userEmail"]
            if (userEmail == null) {
                call.respond("Email utente mancante")
                return@get
            }

            val notifications = notificationDataSource.getNotificationsByRecipient(userEmail)
            call.respond(notifications)
        }

        // Segna una notifica come letta
        post("/mark-read/{notificationId}") {
            val notificationId = call.parameters["notificationId"]
            if (notificationId == null) {
                call.respond("ID notifica mancante")
                return@post
            }

            val updated = notificationDataSource.markAsRead(notificationId)
            if (!updated) {
                call.respond("Errore nell'aggiornamento della notifica")
            } else {
                call.respond("Notifica segnata come letta")
            }
        }

    }
}