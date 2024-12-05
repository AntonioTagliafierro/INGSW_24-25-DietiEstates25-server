package com.data.models.user

import com.data.models.user.*

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Filters.eq
import com.mongodb.kotlin.client.coroutine.FindFlow
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import kotlinx.coroutines.flow.firstOrNull
import org.bson.Document

class MongoUserDataSource(
    db: MongoDatabase
): UserDataSource {

    private val users = db.getCollection<User>("user")
    //find(eq(User::email.toString(), email))
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