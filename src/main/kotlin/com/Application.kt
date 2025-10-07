package com

import com.data.models.activity.MongoActivityDataSource
import com.data.models.agency.MongoAgencyDataSource
import com.data.models.appointment.MongoAppointmentDataSource
import com.data.models.image.MongoImageDataSource
import com.data.models.offer.MongoOfferDataSource
import com.data.models.propertylisting.MongoPropertyListingDataSource
import com.data.models.propertylisting.PropertyListing
import com.data.models.user.MongoUserDataSource
import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import io.ktor.server.application.*
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
import com.service.GeoapifyService
import com.service.mailservice.MailerSendService
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.jackson.*
import kotlinx.serialization.json.Json
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.netty.*
import kotlinx.coroutines.runBlocking


fun main(args: Array<String>) {

    EngineMain.main(args)
}

fun Application.module() {

    val database = getDatabase()
    val userDataSource = MongoUserDataSource(database)
    val offerDataSource = MongoOfferDataSource(database)
    val agencyDataSource = MongoAgencyDataSource(database)
    val imageDataSource = MongoImageDataSource(database)
    val activityDataSource = MongoActivityDataSource(database)
    val propertyListingCollection = database.getCollection<PropertyListing>("propertyListings")
    val appointmentDataSource = MongoAppointmentDataSource(database)


    val tokenService = JwtTokenService()
    val tokenConfig = TokenConfig(
        issuer = environment.config.property("jwt.issuer").getString(),
        audience = environment.config.property("jwt.audience").getString(),
        expiresIn = 365L * 1000L * 60L * 60L * 24L,
        secret = System.getenv("JWT_SECRET")
    )
    val hashingService = SHA256HashingService()

    runBlocking {
        userDataSource.ensureSysAdmin(hashingService, imageDataSource)
    }

    val sharedHttpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
    }

    val mailerSendHttpClient = HttpClient {
        install(ContentNegotiation) {
            jackson()
        }
    }


    val mailerSendService = MailerSendService(
        token = System.getenv("MAILERSEND_TOKEN"),
        baseUrl = System.getenv("MAILERSEND_BASE_URL"),
        httpClient = mailerSendHttpClient,
    )

    val geoapifyService = GeoapifyService(
        apiKey = System.getenv("GEOAPIFY_KEY"),
        httpClient = sharedHttpClient
    )

    // PropertyListing
    val propertyListingDataSource = MongoPropertyListingDataSource(
        collection = propertyListingCollection,
        geoapifyService = geoapifyService,
        imageDataSource = imageDataSource
    )




    val gitHubOAuthService = GitHubOAuthService(
        clientId = System.getenv("GITHUB_CLIENT_ID"),
        clientSecret = System.getenv("GITHUB_CLIENT_SECRET"),
        redirectUri = "http://10.0.2.2:8080//callback/github",
        httpClient = sharedHttpClient
    )

    configureSerialization()

    configureSecurity(tokenConfig)

    configureRouting(
        mailerSendService,
        agencyDataSource,
        userDataSource,
        hashingService,
        tokenService,
        tokenConfig,
        gitHubOAuthService,
        imageDataSource,
        propertyListingDataSource,
        activityDataSource,
        offerDataSource,
        appointmentDataSource
    )

    configureMonitoring()


}

fun getDatabase(): MongoDatabase {
    val mongoPw = System.getenv("MONGO_PW")

    val settings = MongoClientSettings.builder()
        .applyConnectionString(ConnectionString("mongodb://localhost:27017")).build()
    val client = MongoClient.create(settings)
    val database = client.getDatabase("immobiliDB")


    return database
}

