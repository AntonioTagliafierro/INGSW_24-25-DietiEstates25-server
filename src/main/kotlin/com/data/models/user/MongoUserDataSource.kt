package com.data.models.user

import com.data.models.user.*
import com.data.requests.AuthRequest

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates

import com.mongodb.kotlin.client.coroutine.MongoDatabase
import kotlinx.coroutines.flow.firstOrNull


class MongoUserDataSource(
    db: MongoDatabase
): UserDataSource {
    
    private val users = db.getCollection<User>("user")

    override suspend fun updateUserPassword(email: String, newHash: String?, newSalt: String?): Boolean {
        val updateResult = users.updateOne(
            Filters.eq("email", email),
            Updates.combine(
                Updates.set("password", newHash),
                Updates.set("salt", newSalt)
            )
        )
        return updateResult.wasAcknowledged()
    }

    override suspend fun getUserByEmail(email: String): User? {
        println("Cerco utente con email: $email")

        val findUser = users.find(Filters.eq("email", email)).firstOrNull()

        println("Utente trovato risultato: ${findUser?.getEmail()}")

        return findUser
    }

    override suspend fun insertUser(user: User): Boolean {
        println("Inserendo utente: $user")

        val result = users.insertOne(user)

        println("Utente inserito: $result")
        return result.wasAcknowledged()
    }

    override suspend fun checkUserByEmail(user: User): Boolean {
        println("cercando utente: ${user.getEmail()}")

        if ( users.find(Filters.eq("email", user.getEmail())).firstOrNull() != null) return true
        else return false

    }

    override suspend fun getUserById(userId: String): User? {
        println("Cerco utente con id: $userId")

        val findUser = users.find(Filters.eq("id", userId)).firstOrNull()

        println("Utente trovato risultato: ${findUser?.getEmail()}")

        return findUser

    }

    override suspend fun updateUsername(email: String, username: String): Boolean {
        val updateResult = users.updateOne(
            Filters.eq("email", email),
            Updates.set("username", username)
        )
        return updateResult.wasAcknowledged()
    }

    override suspend fun updateFullName(email: String, value: String): Boolean {
        val parts = value.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }

        val surname = parts.last()
        val name = parts.dropLast(1).joinToString(" ")

        val updateResult = users.updateOne(
            Filters.eq("email", email),
            Updates.combine(
                Updates.set("name", name),
                Updates.set("surname", surname)
            )
        )
        return updateResult.wasAcknowledged()
    }


}



//    val userJson = """{
//    "type": "ThirdPartyUser",
//    "id": "63f14edc76dfd019bf6a18b5",
//    "username": "utente#63f14edc76dfd019bf6a18b5",
//    "email": "example@example.com",
//    "provider": "github",
//    "providerId": "12345"
//}"""
//
//    val user: User = Json.decodeFromString(userJson)
//    if (user is ThirdPartyUser) {
//        println("Provider: ${user.provider}")
//    }


