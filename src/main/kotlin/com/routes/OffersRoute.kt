package com.routes

import com.data.models.activity.Activity
import com.data.models.activity.ActivityDataSource
import com.data.models.activity.ActivityType
import com.data.models.offer.Offer
import com.data.models.offer.OfferDataSource
import com.data.models.offer.OfferMessage
import com.data.models.offer.OfferStatus
import com.data.models.propertylisting.PropertyListingDataSource
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
                call.respond(HttpStatusCode.BadRequest, "OfferId is mising")
                return@get
            }

            try {
                val offer = offerDataSource.getOffer(offerId )
                call.respond(status = HttpStatusCode.OK, message = offer!!)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    "Error retrieving offers $offerId ${e.localizedMessage}"
                )
            }
        }

        get("/singlebyuser") {
            val propertyId = call.request.queryParameters["propertyId"]
            val userId = call.request.queryParameters["userId"]

            if (propertyId.isNullOrBlank() || userId.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, "Parametri mancanti")
                return@get
            }

            try {

                val roleUser = userDataSource.getUserById(userId)?.role?.label
                val isAgent = if(roleUser == "LOCAL_USER"  || roleUser == "THIRDPARTY_USER") true else false
                val offer = offerDataSource.getOfferByUser(propertyId, userId, isAgent)
                if (offer == null) {
                    call.respond(HttpStatusCode.NotFound, "Offerta non trovata")
                } else {
                    call.respond(HttpStatusCode.OK, offer)
                }
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    "Errore durante il recupero: ${e.localizedMessage}"
                )
            }
        }


        get("/summary"){
            val propertyId = call.request.queryParameters["propertyId"]

            if (propertyId.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, "Missing propertyId parameter")
                return@get
            }

            try {
                val summaries = offerDataSource.getSummaryOffers(propertyId)

                call.respond(HttpStatusCode.OK, summaries)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    "Error retrieving global history: ${e.localizedMessage}"
                )
            }

        }

        post("/makeoffer") {
            val request = runCatching { call.receiveNullable<OfferRequest>() }.getOrNull() ?: run {
                call.respond(HttpStatusCode.BadRequest, "Missing or malformed payload.")
                return@post
            }

            val newMessage =
                OfferMessage(
                    sender = request.buyerUser,
                    timestamp = System.currentTimeMillis(),
                    amount = request.amount,
                    status = OfferStatus.PENDING,
                )


            val existingOffer = offerDataSource.getOffer(
                propertyId = request.property.id.toString(),
                buyerName = request.buyerUser.username
            )


            if (existingOffer == null) {

                val newOffer = Offer(
                    listing = request.property,
                    buyerUser = request.buyerUser,
                    agentUser = request.agent,
                    messages = mutableListOf(newMessage)
                )

                val wasCreated = offerDataSource.createOffer(newOffer, newMessage)
                if (!wasCreated) {
                    call.respond(HttpStatusCode.Conflict, "Error while creating the offer")
                    return@post
                }
                
            } else {

                val success = offerDataSource.addOfferMessage(existingOffer.id.toString(), newMessage)
                if (!success) {
                    call.respond(HttpStatusCode.Conflict, "Error inserting the message")
                    return@post
                }
                
            }


            if ( activityDataSource.insertActivity(
                    Activity(
                        userId = userDataSource.getUserByUsername(newMessage.sender.username)!!.id.toString(),
                        type = if( existingOffer == null )ActivityType.INSERT else ActivityType.OFFERED,
                        text =  if( existingOffer == null ) "You inserted a listing on ${request.property.property.street}" else "You Offered ${newMessage.amount} of the listing on ${request.property.property.street}"
                    )
            )){
                val updatedOffer = offerDataSource.getOffer(
                    propertyId = request.property.id.toString(),
                    buyerName = request.buyerUser.username
                )

                if (updatedOffer == null) {
                    call.respond(HttpStatusCode.Conflict, "Internal error: offer created but not retrievable")
                    return@post
                }

                call.respond(HttpStatusCode.Created, updatedOffer)
            } else {
                call.respond(HttpStatusCode.Conflict, "Error while inserting the activity")
                return@post
            }

            
        }
        
        post("/message") {
            val request = runCatching { call.receiveNullable<MessageRequest>() }.getOrNull() ?: run {
                call.respond(HttpStatusCode.BadRequest, "Missing or malformed payload.")
                return@post
            }

            val newMessage = OfferMessage(
                sender = request.sender,
                timestamp = System.currentTimeMillis(),
                amount = request.amount,
                status = OfferStatus.PENDING,
            )

            val success = offerDataSource.addOfferMessage(request.offerId, newMessage)
            if (!success) {
                call.respond(HttpStatusCode.Conflict, "Error inserting the message")
                return@post
            }

            call.respond(HttpStatusCode.OK, "Message posted successfully")
        }

        post("/accept") {
            val offerId = call.request.queryParameters["offerId"]

            if (offerId.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, "Missing offerId parameter")
                return@post
            }

            val success = offerDataSource.acceptOffer(offerId)
            if (!success) {
                call.respond(HttpStatusCode.Conflict, "Error while accepting the offer $offerId")
                return@post
            }

            val offer = offerDataSource.getOffer(offerId)

            if ( !listingDataSource.acceptListing(offer!!.id.toString()) ) {
                call.respond(HttpStatusCode.Conflict, "Error during listing update ${offer.id}")
                return@post
            }
            var proposedUser  = offer.agentUser

            val acceptedUser = if ( offer.messages.last().sender.username != offer.buyerUser.username){
                proposedUser = offer.agentUser
                offer.buyerUser
            } else {
                proposedUser = offer.buyerUser
                offer.agentUser
            }

            if ( activityDataSource.insertActivity(
                    Activity(
                        userId = acceptedUser.id.toString(),
                        type = ActivityType.ACCEPTED,
                        text = "You Accepted the offer that proposed ${proposedUser.username} with an amount of ${offer.messages.last().amount}"
                    )
                )){
                call.respond(HttpStatusCode.OK, "Offer $offerId successfully accepted")
            }else{
                call.respond(HttpStatusCode.Conflict, "Error while inserting the activity")
                return@post
            }
        }

        post("/decline") {
            val offerId = call.request.queryParameters["offerId"]

            if (offerId.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, "Missing offerId parameter")
                return@post
            }

            val success = offerDataSource.declineOffer(offerId)
            if (!success) {
                call.respond(HttpStatusCode.Conflict, "Error while declining the offer $offerId")
                return@post
            }

            val offer = offerDataSource.getOffer(offerId)

            var proposedUser  = offer!!.buyerUser

            val declinedUser = if ( offer.messages.last().sender.username != offer.buyerUser.username){
                proposedUser = offer.agentUser
                offer.buyerUser
            } else {
                proposedUser = offer.buyerUser
                offer.agentUser
            }

            if ( activityDataSource.insertActivity(
                    Activity(
                        userId = declinedUser.id.toString(),
                        type = ActivityType.DECLINED,
                        text = "You declined the offer  that proposed ${proposedUser.username} with an amount of ${offer.messages.last().amount}"
                    )
                )){
                call.respond(HttpStatusCode.OK, "Offer $offerId successfully declined")

            }else{
                call.respond(HttpStatusCode.Conflict, "Error while inserting the activity")
                return@post
            }
        }

        get {
            val username = call.request.queryParameters["userName"]

            if (username.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, "Missing username parameter")
                return@get
            }

            val user = userDataSource.getUserByUsername(username)

            try {
                val offers = offerDataSource.getOffers(username, user!!.role.label.contains("AGENT") )
                call.respond(HttpStatusCode.OK, offers)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    "Error retrieving offers $username ${e.localizedMessage}"
                )
            }
        }


    }

}