package com

import com.data.models.admin.MongoAdminDataSource
import com.data.models.agency.MongoAgencyDataSource
import com.data.models.user.MongoUserDataSource
import io.ktor.server.application.*
import com.service.*
import com.security.hashing.SHA256HashingService
import com.security.token.JwtTokenService
import com.security.token.TokenConfig
import com.mongodb.kotlin.client.coroutine.MongoClient
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.plugins.configureMonitoring
import com.plugins.configureSecurity
import com.plugins.configureSerialization
import com.plugins.*
import com.security.token.GitHubOAuthService
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import kotlinx.serialization.json.Json
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.netty.*


fun main(args: Array<String>) {
    EngineMain.main(args)
}

fun Application.module() {

    val database = getDatabase()
    val userDataSource = MongoUserDataSource(database)
    val agencyDataSource = MongoAgencyDataSource(database)
    
    val gitHubOAuthService = GitHubOAuthService(
        clientId = System.getenv("GITHUB_CLIENT_ID"),
        clientSecret = System.getenv("GITHUB_CLIENT_SECRET"),
        redirectUri = "http://10.0.2.2:8080//callback/github",
        httpClient = HttpClient(CIO) {
            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = true // Per debug più leggibile
                    isLenient = true // Permette JSON meno rigorosi
                    ignoreUnknownKeys = true // Ignora campi non previsti nel modello
                })
            }
        }
    )


    val tokenService = JwtTokenService()
    val tokenConfig = TokenConfig(
        issuer = environment.config.property("jwt.issuer").getString(),
        audience = environment.config.property("jwt.audience").getString(),
        expiresIn = 365L * 1000L * 60L * 60L * 24L,
        secret = System.getenv("JWT_SECRET")
    )
    val hashingService = SHA256HashingService()

    configureSerialization()

    configureSecurity(tokenConfig)

    configureRouting(
        userDataSource,
        agencyDataSource,
        hashingService,
        tokenService,
        tokenConfig,
        gitHubOAuthService
    )

    configureMonitoring()


}

fun getDatabase(): MongoDatabase {
    val mongoPw = System.getenv("MONGO_PW")

    val client = MongoClient.create(connectionString = "mongodb+srv://dietiestates25:$mongoPw@ingsw.lehlq.mongodb.net/?retryWrites=true&w=majority&appName=INGSW")
    return client.getDatabase( databaseName = "myDatabase")
}