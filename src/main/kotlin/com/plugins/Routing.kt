package com.plugins

import com.*
import com.data.models.agency.AgencyDataSource
import com.data.models.agency.MongoAgencyDataSource
import com.data.models.appointment.AppointmentDataSource
import com.data.models.image.ImageDataSource
import com.data.models.notification.NotificationDataSource
import com.data.models.propertylisting.PropertyListingDataSource
import com.data.models.user.UserDataSource
import com.security.hashing.HashingService
import com.security.token.GitHubOAuthService
import com.security.token.TokenConfig
import com.security.token.TokenService
import com.service.GeoapifyService
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.server.application.*
import io.ktor.server.routing.*

fun Application.configureRouting(
    agencyDataSource: AgencyDataSource,
    userDataSource: UserDataSource,
    hashingService: HashingService,
    tokenService: TokenService,
    tokenConfig: TokenConfig,
    gitHubOAuthService: GitHubOAuthService,
    httpClient: HttpClient,
    imageDataSource: ImageDataSource,
    propertyListingDataSource: PropertyListingDataSource,
    appointmentDataSource: AppointmentDataSource,
    notificationDataSource: NotificationDataSource
) {



    val geoapifyService = GeoapifyService(
        apiKey = System.getenv("GEOAPIFY_KEY")?: "dummy_key",
        httpClient = HttpClient(CIO)
    )
    routing {
        userAuth(
            hashingService,
            userDataSource,
            tokenService,
            tokenConfig
        )
        agencyRequests(
            hashingService,
            userDataSource,
            agencyDataSource
        )
        authenticate(
            userDataSource
        )
        getSecretInfo()
        githubAuthVerification(
            gitHubOAuthService,
            userDataSource,
            hashingService,
            tokenService,
            tokenConfig
        )
        state()

        imageRoutes( imageDataSource )

        propertyListingRoutes(propertyListingDataSource)

        appointmentRoutes(appointmentDataSource, notificationDataSource)

        profileRoutes(userDataSource, hashingService)
    }
}


