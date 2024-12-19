package com.data.models.user

import com.data.models.user.*

import com.mongodb.client.model.Filters
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import kotlinx.coroutines.flow.firstOrNull


class MongoUserDataSource(
    db: MongoDatabase
): UserDataSource {
    
    private val users = db.getCollection<User>("user")

    override suspend fun getUserByEmail(email: String): User? {
        println("Cerco utente con email: $email")

        val findUser = users.find(Filters.eq("email", email)).firstOrNull()

        println("Utente trovato risultato: ${findUser?.email}")

        return findUser
    }

    override suspend fun insertUser(user: User): Boolean {
        println("Inserendo utente: $user")

        val result = users.insertOne(user)

        println("Utente inserito: $result")
        return result.wasAcknowledged()
    }

    override suspend fun insertThirdPartyUser(user: User): Boolean {

        user.copy(isThirdParty = true)
        
        println("Inserendo utente: $user")

        val result = users.insertOne(user)

        println("Utente inserito: $result")
        return result.wasAcknowledged()
    }


}