package com

import io.ktor.server.application.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {

    val database = createDatabase()
/*
    val mongoPw = System.getenv("MANGO_PW")
    val dbName = environment.config.tryGetString("db.mongo.database.name") ?: "INGSW"
    val db = MongoClients.create(
        "mongodb+srv://dietiestates25:<$mongoPw>@ingsw.lehlq.mongodb.net/?retryWrites=true&w=majority&appName=INGSW"
    ).getDatabase(dbName)
*/
    val userDataSource = MongoUserDataSource(database)

    GlobalScope.launch {
        try {
            val user = User(
                email = "test@gmail.com",
                password = "test123",
                salt = "salt"
            )
            userDataSource.insertUser(user)

            println("Utente inserito con successo!")

        } catch (e: Exception) {
            println("Errore durante l'inserimento dell'utente: ${e.message}")
        }
    }


    val tokenService = JwtTokenService()
    val tokenConfig = TokenConfig(
        issuer = environment.config.property("jwt.issuer").getString(),
        audience = environment.config.property("jwt.audience").getString(),
        expiresIn = 365L * 1000L * 60L * 60L * 24L,
        secret = System.getenv("JWT_SECRET")
    )
    val hashingService = SHA256HashingService()

    configureRouting(userDataSource, hashingService, tokenService, tokenConfig)
    configureMonitoring()
    configureSerialization()
    configureSecurity(tokenConfig)

}
