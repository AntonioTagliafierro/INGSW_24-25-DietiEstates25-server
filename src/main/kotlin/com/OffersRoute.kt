package com

import com.data.models.offer.Offer
import com.data.models.offer.OfferDataSource
import com.data.models.offer.OfferMessage
import com.data.models.user.Role
import com.data.models.user.UserDataSource
import com.data.requests.MessageRequest
import com.data.requests.OfferRequest
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.offerRouting(
    offerDataSource: OfferDataSource,
    userDataSource: UserDataSource,
) {
    route("/offers") {

        post("/makeoffer") {
            val request = runCatching { call.receiveNullable<OfferRequest>() }.getOrNull() ?: run {
                call.respond(HttpStatusCode.BadRequest, "Payload mancante o malformato.")
                return@post
            }

            val newMessage = OfferMessage(
                senderName = request.buyerName,
                timestamp = System.currentTimeMillis(),
                amount = request.amount,
                accepted = null
            )

            val existingOffer = offerDataSource.getOfferByPropertyAndBuyer(
                propertyId = request.propertyId,
                buyerId = request.buyerName
            )

            if (existingOffer == null) {

                val newOffer = Offer(
                    propertyId = request.propertyId,
                    buyerName = request.buyerName,
                    agentName = request.agentName,
                    messages = mutableListOf(newMessage)
                )

                val wasCreated = offerDataSource.createOffer(newOffer, newMessage)
                if (!wasCreated) {
                    call.respond(HttpStatusCode.Conflict, "Errore durante la creazione dell'offerta")
                    return@post
                }

                call.respond(HttpStatusCode.Created, newOffer)
            } else {

                val success = offerDataSource.addOfferMessage(existingOffer.id.toString(), newMessage)
                if (!success) {
                    call.respond(HttpStatusCode.Conflict, "Errore durante l'inserimento del messaggio")
                    return@post
                }

                call.respond(HttpStatusCode.OK, "Messaggio aggiunto all'offerta esistente")
            }
        }

        post("/create") {
            val request = kotlin.runCatching { call.receiveNullable<OfferRequest>() }.getOrNull() ?: run {
                call.respond(HttpStatusCode.BadRequest, "Payload mancante o malformato.")
                return@post
            }

            val offer = Offer(
                propertyId = request.propertyId,
                buyerName = request.buyerName,
                agentName = request.agentName,
                messages = mutableListOf()
            )

            val firstMessage = OfferMessage(
                senderName = request.buyerName,
                timestamp = System.currentTimeMillis(),
                amount = request.amount,
                accepted = null
            )

            val wasAcknowledged = offerDataSource.createOffer(offer, firstMessage)
            if (!wasAcknowledged) {
                call.respond(HttpStatusCode.Conflict, "Errore durante la creazione dell'offerta")
                return@post
            }


            call.respond(HttpStatusCode.OK, offer)
        }


        post("/message") {
            val request = runCatching { call.receiveNullable<MessageRequest>() }.getOrNull() ?: run {
                call.respond(HttpStatusCode.BadRequest, "Payload mancante o malformato.")
                return@post
            }

            val newMessage = OfferMessage(
                senderName = request.senderId,
                timestamp = System.currentTimeMillis(),
                amount = request.amount,
                accepted = null
            )

            val success = offerDataSource.addOfferMessage(request.offerId, newMessage)
            if (!success) {
                call.respond(HttpStatusCode.Conflict, "Errore durante l'inserimento del messaggio")
                return@post
            }

            call.respond(HttpStatusCode.OK, "Messaggio inserito con successo")
        }

        post("/accept") {
            val offerId = call.request.queryParameters["offerId"]

            if (offerId.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, "Parametro offerId mancante")
                return@post
            }

            val success = offerDataSource.acceptOffer(offerId)
            if (!success) {
                call.respond(HttpStatusCode.Conflict, "Errore durante l'accettazione dell'offerta $offerId")
                return@post
            }

            call.respond(HttpStatusCode.OK, "Offerta $offerId accettata con successo")
        }

        post("/decline") {
            val offerId = call.request.queryParameters["offerId"]

            if (offerId.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, "Parametro offerId mancante")
                return@post
            }

            val success = offerDataSource.declineOffer(offerId)
            if (!success) {
                call.respond(HttpStatusCode.Conflict, "Errore durante il rifiuto dell'offerta $offerId")
                return@post
            }

            call.respond(HttpStatusCode.OK, "Offerta $offerId rifiutata con successo")
        }

        get("/all") {
            try {
                val summaries = offerDataSource.getAllOffers()
                call.respond(HttpStatusCode.OK, summaries)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    "Errore durante il recupero dello storico globale: ${e.localizedMessage}"
                )
            }
        }

        get {
            val userId = call.request.queryParameters["userId"]

            if (userId.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, "Parametro userId mancante")
                return@get
            }

            val user = userDataSource.getUserById(userId)

            try {
                val offers = offerDataSource.getOffersByUserOrAgent(user!!.username, user.role == Role.AGENT_USER)
                call.respond(HttpStatusCode.OK, offers)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    "Errore durante il recupero offerte per userId=$user!!.username: ${e.localizedMessage}"
                )
            }
        }
    }

}