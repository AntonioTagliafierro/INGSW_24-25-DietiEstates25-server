package com.data.models.appointment

import com.data.models.offer.OfferSummary
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import com.mongodb.kotlin.client.coroutine.MongoCollection
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import org.litote.kmongo.eq


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

            if (appointment.messages.isEmpty()) return false

            val lastMessage = appointment.messages.last()


            if (lastMessage.accepted == true) {
                println("L'ultimo appuntamento è già accettato non è possibile aggiungere altri messaggi")
                return false
            }

            val updatedMessages = appointment.messages.toMutableList()
            updatedMessages[updatedMessages.lastIndex] = lastMessage.copy(accepted = null)
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
        return updateAppointmentStatus(appointmentId, AppointmentStatus.ACCEPTED, true)
    }

    override suspend fun declineAppointment(appointmentId: String): Boolean {
        return updateAppointmentStatus(appointmentId, AppointmentStatus.REJECTED, false)
    }


    private suspend fun updateAppointmentStatus(
        appointmentId: String,
        newStatus: AppointmentStatus,
        messageAccepted: Boolean
    ): Boolean {
        val appointment = appointments.find(Filters.eq("_id", appointmentId)).firstOrNull()
            ?: return false

        if (appointment.messages.isEmpty()) return false

        val updatedMessages = appointment.messages.toMutableList()
        val lastIndex = updatedMessages.lastIndex
        updatedMessages[lastIndex] = updatedMessages[lastIndex].copy(accepted = messageAccepted)

        val result = appointments.updateOne(
            Filters.eq("_id", appointmentId),
            Updates.combine(
                Updates.set("messages", updatedMessages),
                Updates.set("status", newStatus)
            )
        )
        return result.modifiedCount > 0
    }

    override suspend fun getAllAppointments(): List<AppointmentSummary> {
        return try {
            val allAppointments = appointments.find().toList()
            val summaries = allAppointments.flatMap { appointment ->
                appointment.messages.map { msg ->
                    AppointmentSummary(date = msg.date, status = msg.accepted)
                }
            }

            if (summaries.isEmpty()) {
                println("Nessun appuntamento trovato nel database.")
            } else {
                println("Recuperati ${summaries.size} messaggi di appuntamento (solo data+stato).")
            }

            summaries
        } catch (e: Exception) {
            println("Errore durante il recupero degli appuntamentii: ${e.localizedMessage}")
            emptyList()
        }
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

    override suspend fun getAppointmentByListingId(listingId: String): List<Appointment> {
        return try {
            val result = appointments.find(Filters.eq("listingId", listingId)).toList()

            if (result.isEmpty()) {
                println("Nessun appuntamento trovato per annuncio $listingId")
            } else {
                println("Recuperati ${result.size} appuntamenti per annuncio $listingId")
            }

            result
        } catch (e: Exception) {
            println("Errore durante il recupero degli appuntamenti per annuncio $listingId: ${e.localizedMessage}")
            emptyList()
        }
    }


}