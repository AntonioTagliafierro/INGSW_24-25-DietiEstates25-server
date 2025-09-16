package com

import com.data.models.agency.MongoAgencyDataSource
import com.data.models.appointment.Appointment
import com.data.models.appointment.MongoAppointmentDataSource
import com.data.models.image.MongoImageDataSource
import com.data.models.notification.MongoNotificationDataSource
import com.data.models.notification.Notification
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
import com.service.mailservice.configureClient
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import kotlinx.serialization.json.Json
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.netty.*
import kotlinx.coroutines.runBlocking


fun main(args: Array<String>) {
    EngineMain.main(args)
}

fun Application.module() {


    configureClient() // for MailerSender in Service

    val database = getDatabase()
    val userDataSource = MongoUserDataSource(database)
    val agencyDataSource = MongoAgencyDataSource(database)
    val imageDataSource = MongoImageDataSource(database)
    val propertyListingCollection = database.getCollection<PropertyListing>("propertyListings")
    val appointmentCollection = database.getCollection<Appointment>("appointments")
    val notificationCollection = database.getCollection<Notification>("notifications")
    runBlocking {
        userDataSource.ensureSysAdmin()
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
    val geoapifyService = GeoapifyService(
        apiKey = System.getenv("GEOAPIFY_KEY"),
        httpClient = sharedHttpClient
    )

    // PropertyListing
    val propertyListingDataSource = MongoPropertyListingDataSource(
        collection = propertyListingCollection,
        geoapifyService = geoapifyService
    )

    // Appointments
    val appointmentDataSource = MongoAppointmentDataSource(appointmentCollection)
    // Notifications
    val notificationDataSource = MongoNotificationDataSource(notificationCollection)


    val gitHubOAuthService = GitHubOAuthService(
        clientId = System.getenv("GITHUB_CLIENT_ID"),
        clientSecret = System.getenv("GITHUB_CLIENT_SECRET"),
        redirectUri = "http://10.0.2.2:8080//callback/github",
        httpClient = sharedHttpClient
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
        agencyDataSource,
        userDataSource,
        hashingService,
        tokenService,
        tokenConfig,
        gitHubOAuthService,
        sharedHttpClient,
        imageDataSource,
        propertyListingDataSource,
        appointmentDataSource,
        notificationDataSource
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

