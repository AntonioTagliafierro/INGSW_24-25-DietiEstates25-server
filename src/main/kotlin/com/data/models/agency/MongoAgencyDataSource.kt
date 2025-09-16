package com.data.models.agency


import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList

class MongoAgencyDataSource (
    db: MongoDatabase
): AgencyDataSource {


    private val agencies = db.getCollection<Agency>("agency")
    private val agencyUsers = db.getCollection<AgencyUser>("agencyUser")

    override suspend fun insertAgency( agency: Agency): Boolean {
        println("Inserendo agenzia: $agency")

        val result = agencies.insertOne(agency)

        println("Agenzia inserita: $result")

        return result.wasAcknowledged()
    }

    override suspend fun insertAgencyUser(agencyUser: AgencyUser): Boolean {
        println("Inserendo agencyUser: $agencyUser")

        val result = agencyUsers.insertOne(agencyUser)

        println("Agenzia inserita: $result")

        return result.wasAcknowledged()
    }

    override suspend fun updateAgencyState(userId: String): Boolean {

        val agencyUser = agencyUsers.find(Filters.eq("userId", userId)).firstOrNull()
            ?: return false

        val agencyId = agencyUser.agencyId.toString()

        val updateResult = agencies.updateOne(
            Filters.eq("id", agencyId),
            Updates.set("pending", false)
        )

        return updateResult.modifiedCount > 0
    }

    override suspend fun deleteAgency(userId: String): Boolean {

        val agencyUser = agencyUsers.find(Filters.eq("userId", userId)).firstOrNull()
            ?: return false

        val agencyId = agencyUser.agencyId

        val deleteAgencyResult = agencies.deleteOne(Filters.eq("id", agencyId))

        val deleteAgencyUserResult = agencyUsers.deleteOne(Filters.eq("userId", userId))

        return deleteAgencyResult.deletedCount > 0 && deleteAgencyUserResult.deletedCount > 0
    }

    override suspend fun getAllAgencies(): List<Agency> {
        return try {
            val result = agencies.find().toList()

            if (result.isEmpty()) {
                println("Nessuna agenzia trovata nel database.")
            } else {
                println("Recuperate ${result.size} agenzie dal database.")
            }

            result
        } catch (e: Exception) {
            println("Errore durante il recupero delle agenzie: ${e.message}")
            emptyList()
        }
    }

    override suspend fun getAgency(nameAgency: String): Agency? {
        println("Cerco Agenzia con nome: $nameAgency")

        val findAgency = agencies.find(Filters.eq("name", nameAgency)).firstOrNull()

        println("Agenzia trovata risultato: ${findAgency}")

        return findAgency
    }


}