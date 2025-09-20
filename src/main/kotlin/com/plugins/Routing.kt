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
import com.service.mailservice.MailerSendService
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.server.application.*
import io.ktor.server.routing.*

fun Application.configureRouting(
    mailerSendService: MailerSendService,
    agencyDataSource: AgencyDataSource,
    userDataSource: UserDataSource,
    hashingService: HashingService,
    tokenService: TokenService,
    tokenConfig: TokenConfig,
    gitHubOAuthService: GitHubOAuthService,
    imageDataSource: ImageDataSource,
    propertyListingDataSource: PropertyListingDataSource,
    appointmentDataSource: AppointmentDataSource,
    notificationDataSource: NotificationDataSource
) {


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
            tokenConfig,
            imageDataSource
        )
        state()

        imageRoutes( imageDataSource , userDataSource )

        propertyListingRoutes(propertyListingDataSource)

        appointmentRoutes(appointmentDataSource, notificationDataSource)

        profileRoutes(userDataSource, hashingService)

        admin(
            hashingService,
            userDataSource,
            mailerSendService
        )
    }
}


