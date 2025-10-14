package com.data.models.offer

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList

class MongoOfferDataSource (
    db: MongoDatabase
) : OfferDataSource {
    private val offers = db.getCollection<Offer>("offers")


    override suspend fun getOffer(propertyId: String , buyerName: String): Offer? {
        return try {
            val filter = Filters.and(
                Filters.eq("propertyId", propertyId),
                Filters.eq("buyerName", buyerName)
            )

            val offer = offers.find(filter).firstOrNull()
            if (offer == null) {
                println("Nessuna offerta trovata con propertyId=$propertyId e buyerdUsername=$buyerName")
            } else {
                println("Offerta trovata: $offer")
            }
            offer
        } catch (e: Exception) {
            println("Errore durante la ricerca dell'offerta: ${e.localizedMessage}")
            null
        }
    }

    override suspend fun getOffer(offerId : String): Offer? {
        return try {
            val filter = Filters.eq("id", offerId)

            val offer = offers.find(filter).firstOrNull()
            if (offer == null) {
                println("Nessuna offerta trovata con offerId=$offerId ")
            } else {
                println("Offerta trovata: $offer")
            }
            offer
        } catch (e: Exception) {
            println("Errore durante la ricerca dell'offerta: ${e.localizedMessage}")
            null
        }
    }

    override suspend fun createOffer(offer: Offer, firstMessage: OfferMessage): Boolean {
        return try {
            val offerToInsert = offer.copy(messages = mutableListOf(firstMessage))
            val result = offers.insertOne(offerToInsert)
            println("Offerta creata: $offerToInsert")
            result.wasAcknowledged()
        } catch (e: Exception) {
            println("Errore durante la creazione dell'offerta: ${e.localizedMessage}")
            false
        }
    }

    override suspend fun addOfferMessage(offerId: String, newMessage: OfferMessage): Boolean {
        return try {
            val offer = offers.find(Filters.eq("id", offerId)).firstOrNull()
                ?: return false.also { println("Nessuna offerta trovata con id=$offerId") }

            val lastMessage = offer.messages.last()

            if (lastMessage.status == OfferStatus.ACCEPTED) {
                println("L'ultima offerta è già accettata  non è possibile aggiungere altri messaggi")
                return false
            }

            val updatedMessages = offer.messages.toMutableList()
            updatedMessages[updatedMessages.lastIndex] = lastMessage.copy(status = OfferStatus.REJECTED)
            updatedMessages.add(newMessage)

            val result = offers.updateOne(
                Filters.eq("id", offerId),
                Updates.set("messages", updatedMessages)
            )

            println("Messaggio aggiunto all'offerta $offerId: $newMessage")
            result.modifiedCount > 0
        } catch (e: Exception) {
            println("Errore durante l'aggiunta di un messaggio a $offerId: ${e.localizedMessage}")
            false
        }
    }

    override suspend fun acceptOffer(offerId: String): Boolean =
        updateLastMessageStatus(offerId, true)

    override suspend fun declineOffer(offerId: String): Boolean =
        updateLastMessageStatus(offerId, false)

    private suspend fun updateLastMessageStatus(offerId: String, accepted: Boolean): Boolean {
        val offer = offers.find(Filters.eq("id", offerId)).firstOrNull() ?: return false
        if (offer.messages.isEmpty()) return false

        val updatedMessages = offer.messages.toMutableList()
        val lastIndex = updatedMessages.lastIndex
        if(accepted) {
            updatedMessages[lastIndex] = updatedMessages[lastIndex].copy(status = OfferStatus.ACCEPTED)
        }else{
            updatedMessages[lastIndex] = updatedMessages[lastIndex].copy(status = OfferStatus.REJECTED)
        }


        val result = offers.updateOne(
            Filters.eq("id", offerId),
            Updates.set("messages", updatedMessages)
        )
        return result.modifiedCount > 0
    }

    override suspend fun getSummaryOffers(propertyId :String): List<OfferSummary> {
        return try {
            val offerSummaryById = offers.find( Filters.eq("propertyId", propertyId)).toList()

            val summaries = offerSummaryById.flatMap { offer ->
                offer.messages.map { msg ->
                    OfferSummary(
                        amount = msg.amount,
                        status = msg.status,
                        timestamp = msg.timestamp,
                    )
                }
            }

            if (summaries.isEmpty()) {
                println("Nessuna offerta trovata nel database.")
            } else {
                println("Recuperati ${summaries.size} messaggi di offerta (solo prezzo+stato).")
            }

            summaries
        } catch (e: Exception) {
            println("Errore durante il recupero delle offerte: ${e.localizedMessage}")
            emptyList()
        }
    }

    override suspend fun getOffers(username: String, isAgent : Boolean): List<Offer> {
        return try {

            val result = if ( isAgent ) {
                offers.find(
                    Filters.eq("agentName", username)
                ).toList()
            }else{
                offers.find(
                    Filters.eq("buyerName", username)
                ).toList()
            }

            if (result.isEmpty()) {
                println("Nessuna offerta trovata per user/agent $username")
            } else {
                println("Recuperate ${result.size} offerte per user/agent $username")
            }

            result
        } catch (e: Exception) {
            println("Errore durante il recupero delle offerte per user/agent $username: ${e.localizedMessage}")
            emptyList()
        }
    }


}