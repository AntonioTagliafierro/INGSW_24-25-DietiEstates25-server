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

            if (offer.messages.isEmpty()) return false

            val lastMessage = offer.messages.last()


            if (lastMessage.accepted == true) {
                println("L'ultima offerta è già accettata  non è possibile aggiungere altri messaggi")
                return false
            }

            val updatedMessages = offer.messages.toMutableList()
            updatedMessages[updatedMessages.lastIndex] = lastMessage.copy(accepted = null)
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

    private suspend fun updateLastMessageStatus(offerId: String, status: Boolean): Boolean {
        val offer = offers.find(Filters.eq("_id", offerId)).firstOrNull() ?: return false
        if (offer.messages.isEmpty()) return false

        val updatedMessages = offer.messages.toMutableList()
        val lastIndex = updatedMessages.lastIndex
        updatedMessages[lastIndex] = updatedMessages[lastIndex].copy(accepted = status)

        val result = offers.updateOne(
            Filters.eq("_id", offerId),
            Updates.set("messages", updatedMessages)
        )
        return result.modifiedCount > 0
    }

    override suspend fun getAllOffers(): List<OfferSummary> {
        return try {
            val allOffers = offers.find().toList()
            val summaries = allOffers.flatMap { offer ->
                offer.messages.map { msg ->
                    OfferSummary(amount = msg.amount, accepted = msg.accepted)
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

    override suspend fun getOffersByUserOrAgent(userId: String): List<Offer> {
        return try {
            val result = offers.find(
                Filters.or(
                    Filters.eq("buyerId", userId),
                    Filters.eq("agentId", userId)
                )
            ).toList()

            if (result.isEmpty()) {
                println("Nessuna offerta trovata per user/agent $userId")
            } else {
                println("Recuperate ${result.size} offerte per user/agent $userId")
            }

            result
        } catch (e: Exception) {
            println("Errore durante il recupero delle offerte per user/agent $userId: ${e.localizedMessage}")
            emptyList()
        }
    }


}