package com.data.models.user

import com.data.models.image.ImageDataSource

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates

import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.security.hashing.HashingService
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList


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

        val findUser = users.find(Filters.eq("email", email.myToLowerCase())).firstOrNull()

        println("Utente trovato risultato: ${findUser?.email?.myToLowerCase()}")

        return findUser
    }

    override suspend fun insertUser(user: User): Boolean {
        println("Inserendo utente: $user")

        user.email = user.email.myToLowerCase()
        user.username = user.username.myToLowerCase()

        if(findUserByUsername (user.username)) {
            user.username += randomThreeNumbers()
        }

        val result = users.insertOne(user)

        println("Utente inserito: $result")
        return result.wasAcknowledged()
    }

    private suspend fun findUserByUsername(username: String): Boolean {
        println("Cerco utente con username: $username")

        val findUser = users.find(Filters.eq("username", username)).firstOrNull()
        return findUser != null
    }

    override suspend fun checkUserByEmail(user: User): Boolean {

        println("cercando utente: ${user.email}")
        val userEmail = user.email.myToLowerCase()
        val existingUser = users.find(Filters.eq("email", userEmail)).firstOrNull()

        return existingUser != null

    }

    override suspend fun getUserById(userId: String): User? {
        println("Cerco utente con id: $userId")

        val findUser = users.find(Filters.eq("id", userId)).firstOrNull()

        println("Utente trovato risultato: ${findUser?.email?.myToLowerCase()}")

        return findUser

    }

    override suspend fun getAllUsers(): List<User> {

        return try {
            val result = users.find(Filters.ne("role", "SUPER_ADMIN")).toList()

            if (result.isEmpty()) {
                println("Nessun utente trovato")
            } else {
                println("Recuperati ${result.size} utenti.")
            }

            result
        } catch (e: Exception) {
            println("Errore durante il recupero degli utenti ${e.message}")
            emptyList()
        }
    }

    override suspend fun getUsersByRole(role : String): List<User> {
        return try {

            val result = users.find(Filters.eq("role", role)).toList()

            if (result.isEmpty()) {
                println("Nessun utente trovato con ruolo: $role")
            } else {
                println("Recuperati ${result.size} utenti con ruolo '$role'.")
            }

            result
        } catch (e: Exception) {
            println("Errore durante il recupero degli utenti con ruolo $role: ${e.message}")
            emptyList()
        }
    }

    override suspend fun getAgencyUsers(userIds: List<String>): List<User> {
        return try {
            val result = users.find(
                Filters.and(
                    Filters.`in`("id", userIds),
                    Filters.ne("role", "AGENT_ADMIN")
                )
            ).toList()

            if (result.isEmpty()) {
                println("Nessun utente trovato con gli id: $userIds (esclusi AGENT_ADMIN)")
            } else {
                println("Recuperati ${result.size} utenti per l'agenzia (esclusi AGENT_ADMIN)")
            }

            result
        } catch (e: Exception) {
            println("Errore durante il recupero degli utenti: ${e.localizedMessage}")
            emptyList()
        }
    }

    override suspend fun updateUsername(email: String, username: String): Boolean {
        val userEmail = email.myToLowerCase()

        if(findUserByUsername(username)) return false

        val updateResult = users.updateOne(
            Filters.eq("email", userEmail),
            Updates.set("username", username)
        )
        return updateResult.wasAcknowledged()
    }

    override suspend fun updateUserRole(email: String , role : Role) :Boolean{

        val userEmail = email.myToLowerCase()

        val updateResult = users.updateOne(
            Filters.eq("email", userEmail),
            Updates.set("role", role.label)
        )
        return updateResult.wasAcknowledged()
    }

    override suspend fun deleteUser(email: String): Boolean {

        val userEmail = email.myToLowerCase()
        val deleteUser = users.deleteOne(Filters.eq("email", userEmail))

        return deleteUser.deletedCount > 0
    }

    override suspend fun updateFullName(email: String, value: String): Boolean {
        val parts = value.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }
        val userEmail  = email.myToLowerCase()
        val surname = parts.last()
        val name = parts.dropLast(1).joinToString(" ")

        val updateResult = users.updateOne(
            Filters.eq("email", userEmail),
            Updates.combine(
                Updates.set("name", name),
                Updates.set("surname", surname)
            )
        )
        return updateResult.wasAcknowledged()
    }

    override suspend fun ensureSysAdmin( hashingService: HashingService, imageDataSource: ImageDataSource) {

        val email = "admin@system.com"
        val existing = getUserByEmail(email)

        if (existing == null) {
            val hashed = hashingService.generateSaltedHash("admin123")

            val sysAdmin = User(hashed.hash!!, hashed.salt!!)

            imageDataSource.updatePpById(sysAdmin.id.toString(), "iVBORw0KGgoAAAANSUhEUgAAACgAAAAoCAIAAAADnC86AAAAL0lEQVR4nO3NAQ0AAAQAMPSvIScxbPYXeE7HibppxWKxWCwWi8VisVgsFovFP+MFC30B9H8Mi5gAAAAASUVORK5CYII=")

            insertUser(sysAdmin)

            println("Creato System Admin di default con email: $email")
        } else {
            println("System Admin già presente: $email")
        }

    }

}

fun String.myToLowerCase(): String {
    val result = StringBuilder(this.length)
    for (c in this) {
        if (c in 'A'..'Z') {
            result.append((c.code + 32).toChar())
        } else {
            result.append(c)
        }
    }
    return result.toString()
}

fun randomThreeNumbers(): List<Int> {
    return List(3) { (0..9).random() } // numeri da 0 a 9
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


