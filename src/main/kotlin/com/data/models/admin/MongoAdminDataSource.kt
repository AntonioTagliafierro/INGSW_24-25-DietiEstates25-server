package com.data.models.admin

import com.data.models.admin.*

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Filters.eq
import com.mongodb.kotlin.client.coroutine.FindFlow
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import kotlinx.coroutines.flow.firstOrNull
import org.bson.Document

class MongoAdminDataSource (
    db: MongoDatabase
): AdminDataSource {

    private val admins = db.getCollection<Admin>("admin")

    override suspend fun getAdminByEmail(email: String): Admin? {
        println("Cerco admin con email: $email")

        val findAdmin = admins.find(Filters.eq("email", email)).firstOrNull()

        println("Admin trovato risultato: ${findAdmin?.email}")

        return findAdmin
    }
    override suspend fun insertAdmin(admin: Admin): Boolean {
        println("Inserendo admin: $admin")

        val result = admins.insertOne(admin)

        println("Admin inserito: $result")
        return result.wasAcknowledged()
    }
}