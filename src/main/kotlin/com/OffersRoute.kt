package com

import com.data.models.activity.Activity
import com.data.models.activity.ActivityDataSource
import com.data.models.activity.ActivityType
import com.data.models.offer.Offer
import com.data.models.offer.OfferDataSource
import com.data.models.offer.OfferMessage
import com.data.models.offer.OfferStatus
import com.data.models.propertylisting.PropertyListingDataSource
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
    listingDataSource: PropertyListingDataSource,
    activityDataSource: ActivityDataSource
) {

    route("/offers") {

        get("/single") {
            val offerId = call.request.queryParameters["offerId"]

            if (offerId.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, "Parametro offerId mancante")
                return@get
            }

            try {
                val offer = offerDataSource.getOffer(offerId )
                call.respond(status = HttpStatusCode.OK, message = offer!!)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    "Errore durante il recupero offerte $offerId ${e.localizedMessage}"
                )
            }
        }

        get("/summary"){
            val propertyId = call.request.queryParameters["propertyId"]

            if (propertyId.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, "Parametro propertyId mancante")
                return@get
            }

            try {
                val summaries = offerDataSource.getSummaryOffers(propertyId)

                call.respond(HttpStatusCode.OK, summaries)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    "Errore durante il recupero dello storico globale: ${e.localizedMessage}"
                )
            }

        }

        post("/makeoffer") {
            val request = runCatching { call.receiveNullable<OfferRequest>() }.getOrNull() ?: run {
                call.respond(HttpStatusCode.BadRequest, "Payload mancante o malformato.")
                return@post
            }

            val newMessage = if(request.isAgent) {
                OfferMessage(
                    senderName = request.agentName,
                    timestamp = System.currentTimeMillis(),
                    amount = request.amount,
                    status = OfferStatus.PENDING,
                )
            }else{
                OfferMessage(
                    senderName = request.buyerName,
                    timestamp = System.currentTimeMillis(),
                    amount = request.amount,
                    status = OfferStatus.PENDING,
                )
            }

            val existingOffer = offerDataSource.getOffer(
                propertyId = request.propertyId,
                buyerName = request.buyerName
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
                
            } else {

                val success = offerDataSource.addOfferMessage(existingOffer.id.toString(), newMessage)
                if (!success) {
                    call.respond(HttpStatusCode.Conflict, "Errore durante l'inserimento del messaggio")
                    return@post
                }
                
            }

            val listingProperty = listingDataSource.getListingById(request.propertyId)

            if ( activityDataSource.insertActivity(
                    Activity(
                        userId = userDataSource.getUserByUsername(newMessage.senderName)!!.id.toString(),
                        type = if( existingOffer == null )ActivityType.INSERT else ActivityType.OFFERED,
                        text =  if( existingOffer == null ) "You inserted a listing on ${listingProperty!!.property.street}" else "You Offered ${newMessage.amount} of the listing on ${listingProperty!!.property.street}"
                    )
            )){
                val updatedOffer = offerDataSource.getOffer(
                    propertyId = request.propertyId,
                    buyerName = request.buyerName
                )

                call.respond(HttpStatusCode.Created, updatedOffer ?: newMessage)
            } else {
                call.respond(HttpStatusCode.Conflict, "Errore durante l'inserimento dell'activity")
                return@post
            }

            
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
                status = OfferStatus.PENDING,
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

            val offer = offerDataSource.getOffer(offerId)

            if ( !listingDataSource.acceptListing(offer!!.propertyId) ) {
                call.respond(HttpStatusCode.Conflict, "Errore durante l'update del listing ${offer.propertyId}")
                return@post
            }

            val acceptedUser = if ( offer.messages.last().senderName != offer.buyerName) offer.buyerName else offer.agentName

            if ( activityDataSource.insertActivity(
                    Activity(
                        userId = userDataSource.getUserByUsername(acceptedUser)!!.id.toString(),
                        type = ActivityType.ACCEPTED,
                        text = "You Accepted the offer with an amount of ${offer.messages.last().amount}"
                    )
                )){
                call.respond(HttpStatusCode.OK, "Offerta $offerId accettata con successo")
            }else{
                call.respond(HttpStatusCode.Conflict, "Errore durante l'inserimento dell'activity")
                return@post
            }
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

            val offer = offerDataSource.getOffer(offerId)
            val declinedUser = if ( offer!!.messages.last().senderName != offer.buyerName) offer.buyerName else offer.agentName

            if ( activityDataSource.insertActivity(
                    Activity(
                        userId = userDataSource.getUserByUsername(declinedUser)!!.id.toString(),
                        type = ActivityType.DECLINED,
                        text = "You declined the offer with an amount of ${offer.messages.last().amount}"
                    )
                )){
                call.respond(HttpStatusCode.OK, "Offerta $offerId rifiutata con successo")

            }else{
                call.respond(HttpStatusCode.Conflict, "Errore durante l'inserimento dell'activity")
                return@post
            }
        }

        get {
            val username = call.request.queryParameters["userName"]

            if (username.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, "Parametro username mancante")
                return@get
            }

            val user = userDataSource.getUserByUsername(username)

            try {
                val offers = offerDataSource.getOffers(username, user!!.role.label.contains("AGENT") )
                call.respond(HttpStatusCode.OK, offers)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    "Errore durante il recupero offerte $username ${e.localizedMessage}"
                )
            }
        }


    }

}