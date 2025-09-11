package com.data.models.appointment

import com.mongodb.kotlin.client.coroutine.MongoCollection
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import org.litote.kmongo.eq


class MongoAppointmentDataSource ( private val collection: MongoCollection<Appointment>
): AppointmentDataSource {
    override suspend fun insertAppointment(appointment: Appointment): Boolean = try {
        collection.insertOne(appointment)
        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }

    override suspend fun getAppointmentsByAgent(email: String): List<Appointment> =
        collection.find(com.mongodb.client.model.Filters.eq("agentEmail", email)).toList()

    override suspend fun updateStatus(id: String, status: AppointmentStatus): Boolean = try {
        collection.updateOne(
            com.mongodb.client.model.Filters.eq("id", id),
            com.mongodb.client.model.Updates.set("status", status)
        )
        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }

    override suspend fun getAppointmentById(id: String): Appointment? {
        return collection.find(Appointment::id eq id).firstOrNull()
    }

    override suspend fun getAppointmentsByUserEmail(email: String): List<Appointment> {
        return collection.find(Appointment::userEmail eq email).toList()
    }

}