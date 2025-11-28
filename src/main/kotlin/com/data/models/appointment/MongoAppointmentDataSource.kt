package com.data.models.appointment


import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList


class MongoAppointmentDataSource (
    db: MongoDatabase
): AppointmentDataSource {
    private val appointments = db.getCollection<Appointment>("appointments")

    override suspend fun createAppointment(
        appointment: Appointment,
        firstMessage: AppointmentMessage
    ): Boolean {
        return try {
            val appointmentToInsert = appointment.copy(messages = mutableListOf(firstMessage))
            val result = appointments.insertOne(appointmentToInsert)
            println("Appuntamento creata: $appointmentToInsert")
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

        val newStatus = if (accepted) AppointmentStatus.ACCEPTED else AppointmentStatus.REJECTED

        // Aggiorna l'ultimo messaggio
        updatedMessages[lastIndex] = updatedMessages[lastIndex].copy(status = newStatus)

        // Aggiorna sia i messaggi che lo status dell'appuntamento
        val result = appointments.updateOne(
            Filters.eq("id", appointmentId),
            Updates.combine(
                Updates.set("messages", updatedMessages),
                Updates.set("status", newStatus)
            )
        )

        return result.modifiedCount > 0
    }



    override suspend fun getAppointmentsByUserOrAgent(userId: String): List<Appointment> {
        return try {
            val result = appointments.find(
                Filters.or(
                    Filters.eq("user.id", userId),
                    Filters.eq("agent.id", userId)
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





    override suspend fun getAppointment(appointmentId: String): Appointment? {
        return try {
            val filter = Filters.and(
                Filters.eq("id", appointmentId),
            )

            val appointment = appointments.find(filter).firstOrNull()
            if (appointment == null) {
                println("Nessun appuntamento trovato con Id=$appointmentId")
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

    override suspend fun getAppointmentsByListing(listingId: String): List<Appointment> {
        return try {
            val result = appointments.find(
                Filters.eq("listing.id", listingId)
            ).toList()

            if (result.isEmpty()) {
                println("Nessun appuntamento trovato per listing $listingId")
            } else {
                println("Recuperati ${result.size} appuntamenti per listing $listingId")
            }

            result
        } catch (e: Exception) {
            println("Errore durante il recupero degli appuntamenti per listing $listingId: ${e.localizedMessage}")
            emptyList()
        }
    }

    override suspend fun getAppointmentsByUserAndListing(userId: String, listingId: String): List<Appointment> {
        return try {
            val result = appointments.find(
                Filters.and(
                    Filters.eq("listing.id", listingId),
                    Filters.eq("user.id", userId)
                )
            ).toList()

            if (result.isEmpty()) {
                println("Nessun appuntamento trovato per user $userId e listing $listingId")
            } else {
                println("Recuperati ${result.size} appuntamenti per user $userId e listing $listingId")
            }

            result
        } catch (e: Exception) {
            println("Errore durante il recupero appuntamenti per user $userId e listing $listingId: ${e.localizedMessage}")
            emptyList()
        }
    }

    override suspend fun getAppointments(): List<Appointment> {
        return try {
            val appointments = appointments.find().toList()
            // return attachImagesToListings(listings)
            appointments
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}