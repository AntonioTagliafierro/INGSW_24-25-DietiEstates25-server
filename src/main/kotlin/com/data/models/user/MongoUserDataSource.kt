package com.data.models.user

import com.data.models.user.*
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters
import org.bson.Document

class MongoUserDataSource(
    db: MongoDatabase
): UserDataSource {

    private val users = db.getCollection( "email", User::class.java)

    override suspend fun getUserByEmail(email: String): User? {
        return users.find(Filters.eq("email", email)).firstOrNull()
    }

    override suspend fun insertUser(user: User): Boolean {
        println("Inserendo utente: $user")
        val result = users.insertOne(user)
        println("DAJE")
        return result.wasAcknowledged()
    }
}