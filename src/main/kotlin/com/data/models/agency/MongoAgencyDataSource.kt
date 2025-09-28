package com.data.models.agency


import com.data.models.user.myToLowerCase
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


    override suspend fun getAgencyByAgentId(agentId: String): Agency? {
        return try {

            val relation = agencyUsers.find(Filters.eq("userId", agentId)).firstOrNull()

            if (relation == null) {
                println("Nessuna relazione trovata per userId=$agentId")
                return null
            }

            val agency = agencies.find(Filters.eq("id", relation.agencyId)).firstOrNull()

            if (agency == null) {
                println("Nessuna agenzia trovata con id=${relation.agencyId}")
            } else {
                println("Agenzia trovata: ${agency.name} (${agency.id})")
            }

            agency
        } catch (e: Exception) {
            println("Errore durante il recupero agenzia per userId=$agentId: ${e.localizedMessage}")
            null
        }
    }

    override suspend fun insertAgency( agency: Agency): Boolean {
        println("Inserendo agenzia: $agency")

        agency.agencyEmail = agency.agencyEmail.myToLowerCase()

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

    override suspend fun getAgencyByEmail(email: String): Agency? {
        return try {
            val agency = agencies.find(Filters.eq("agencyEmail", email.myToLowerCase())).firstOrNull()

            if (agency == null) {
                println("Nessuna agenzia trovata con email: $email")
            } else {
                println("Agenzia trovata: ${agency.name} (${agency.id})")
            }

            agency
        } catch (e: Exception) {
            println("Errore durante il recupero dell'agenzia con email $email: ${e.localizedMessage}")
            null
        }
    }

    override suspend fun getAgencyUserIds(agencyId: String): List<String> {
        return try {
            val results = agencyUsers.find(Filters.eq("agencyId", agencyId)).toList()

            if (results.isEmpty()) {
                println("Nessun utente trovato per l'agenzia $agencyId")
                emptyList()
            } else {
                println("Recuperati ${results.size} utenti per l'agenzia $agencyId")
                results.map { it.userId }
            }
        } catch (e: Exception) {
            println("Errore durante il recupero degli utenti per agenzia $agencyId: ${e.localizedMessage}")
            emptyList()
        }
    }

}