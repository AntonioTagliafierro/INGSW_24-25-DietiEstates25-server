package com.data.models.agency

import com.data.models.user.User
import com.mongodb.client.model.Filters
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import kotlinx.coroutines.flow.firstOrNull

class MongoAgencyDataSource (
    db: MongoDatabase
): AgencyDataSource {


    private val agencys = db.getCollection<Agency>("agency")
    private val agencyUsers = db.getCollection<AgencyUser>("agencyUser")

    override suspend fun insertAgency( agency: Agency): Boolean {
        println("Inserendo agenzia: $agency")

        val result = agencys.insertOne(agency)

        println("Agenzia inserita: $result")

        return result.wasAcknowledged()
    }

    override suspend fun insertAgencyUser(agencyUser: AgencyUser): Boolean {
        println("Inserendo agencyUser: $agencyUser")

        val result = agencyUsers.insertOne(agencyUser)

        println("Agenzia inserita: $result")

        return result.wasAcknowledged()
    }

    override suspend fun getAgency(nameAgency: String): Agency? {
        println("Cerco Agenzia con nome: $nameAgency")

        val findAgency = agencys.find(Filters.eq("name", nameAgency)).firstOrNull()

        println("Agenzia trovata risultato: ${findAgency}")

        return findAgency
    }


}