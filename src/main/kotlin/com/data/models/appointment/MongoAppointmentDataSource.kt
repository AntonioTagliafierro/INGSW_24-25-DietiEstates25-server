package com.data.models.appointment

import com.data.models.offer.Offer
import com.data.models.offer.OfferStatus
import com.data.models.offer.OfferSummary
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList


class MongoAppointmentDataSource (
    db: MongoDatabase
): AppointmentDataSource {
    private val appointments = db.getCollection<Appointment>("appointments")

    override suspend fun createAppointemnt(
        appointment: Appointment,
        firstMessage: AppointmentMessage
    ): Boolean {
        return try {
            val appointmentToInsert = appointment.copy(messages = mutableListOf(firstMessage))
            val result = appointments.insertOne(appointmentToInsert)
            println("Offerta creata: $appointmentToInsert")
            result.wasAcknowledged()
        } catch (e: Exception) {
            println("Errore durante la creazione dell'appuntamento: ${e.localizedMessage}")
            false
        }
    }

    override suspend fun addAppointmentMessage(
        appointmentId: String,
        newMessage: AppointmentMessage
    ): Boolean {
        return try {
            val appointment = appointments.find(Filters.eq("id", appointmentId)).firstOrNull()
                ?: return false.also { println("Nessun appuntamento trovato con id=$appointmentId") }

            val lastMessage = appointment.messages.last()

            if (lastMessage.status == AppointmentStatus.ACCEPTED) {
                println("L'ultimo appuntamento è già accettato  non è possibile aggiungere altri messaggi")
                return false
            }

            val updatedMessages = appointment.messages.toMutableList()
            updatedMessages[updatedMessages.lastIndex] = lastMessage.copy(status = AppointmentStatus.REJECTED)
            updatedMessages.add(newMessage)

            val result = appointments.updateOne(
                Filters.eq("id", appointmentId),
                Updates.set("messages", updatedMessages)
            )

            println("Messaggio aggiunto all'appuntamento $appointmentId: $newMessage")
            result.modifiedCount > 0
        } catch (e: Exception) {
            println("Errore durante l'aggiunta di un messaggio a $appointmentId: ${e.localizedMessage}")
            false
        }
    }

    override suspend fun acceptAppointment(appointmentId: String): Boolean {
        return updateLastMessageStatus(appointmentId,  true)
    }

    override suspend fun declineAppointment(appointmentId: String): Boolean {
        return updateLastMessageStatus(appointmentId,  false)
    }


    private suspend fun updateLastMessageStatus(appointmentId: String, accepted: Boolean): Boolean {
        val appointment = appointments.find(Filters.eq("id", appointmentId)).firstOrNull() ?: return false
        if (appointment.messages.isEmpty()) return false

        val updatedMessages = appointment.messages.toMutableList()
        val lastIndex = updatedMessages.lastIndex
        if(accepted) {
            updatedMessages[lastIndex] = updatedMessages[lastIndex].copy(status = AppointmentStatus.ACCEPTED)
        }else{
            updatedMessages[lastIndex] = updatedMessages[lastIndex].copy(status = AppointmentStatus.REJECTED)
        }


        val result = appointments.updateOne(
            Filters.eq("id", appointmentId),
            Updates.set("messages", updatedMessages)
        )
        return result.modifiedCount > 0
    }



    override suspend fun getAppointmentsByUserOrAgent(userId: String): List<Appointment> {
        return try {
            val result = appointments.find(
                Filters.or(
                    Filters.eq("userId", userId),
                    Filters.eq("agentId", userId)
                )
            ).toList()

            if (result.isEmpty()) {
                println("Nessun appuntamento trovato per user/agent $userId")
            } else {
                println("Recuperati ${result.size} appuntamenti per user/agent $userId")
            }

            result
        } catch (e: Exception) {
            println("Errore durante il recupero degli appuntamenti per user/agent $userId: ${e.localizedMessage}")
            emptyList()
        }
    }



    override suspend fun getAppointment(propertyId: String , buyerName: String): Appointment? {
        return try {
            val filter = Filters.and(
                Filters.eq("propertyId", propertyId),
                Filters.eq("buyerName", buyerName)
            )

            val appointment = appointments.find(filter).firstOrNull()
            if (appointment == null) {
                println("Nessun appuntamento trovato con propertyId=$propertyId e buyerdUsername=$buyerName")
            } else {
                println("Appuntamento trovato: $appointment")
            }
            appointment
        } catch (e: Exception) {
            println("Errore durante la ricerca dell'appuntamento: ${e.localizedMessage}")
            null
        }
    }

    override suspend fun getAppointment(propertyId: String): Appointment? {
        return try {
            val filter = Filters.and(
                Filters.eq("propertyId", propertyId),
            )

            val appointment = appointments.find(filter).firstOrNull()
            if (appointment == null) {
                println("Nessun appuntamento trovato con propertyId=$propertyId")
            } else {
                println("Appuntamento trovato: $appointment")
            }
            appointment
        } catch (e: Exception) {
            println("Errore durante la ricerca dell'appuntamento: ${e.localizedMessage}")
            null
        }
    }

    override suspend fun getSummaryAppointments(propertyId: String): List<AppointmentSummary> {
        return try {
            val appointmentSummaryById = appointments.find( Filters.eq("propertyId", propertyId)).toList()

            val summaries = appointmentSummaryById.flatMap { appointment ->
                appointment.messages.map { msg ->
                    AppointmentSummary(date = msg.date, status = msg.status)
                }
            }

            if (summaries.isEmpty()) {
                println("Nessun appuntamento trovato nel database.")
            } else {
                println("Recuperati ${summaries.size} messaggi di appuntamenti (solo data+stato).")
            }

            summaries
        } catch (e: Exception) {
            println("Errore durante il recupero degli appuntamenti : ${e.localizedMessage}")
            emptyList()
        }
    }


}