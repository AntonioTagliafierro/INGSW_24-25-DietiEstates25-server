package com.plugins

import com.*
import com.data.models.activity.ActivityDataSource
import com.data.models.agency.AgencyDataSource
import com.data.models.appointment.AppointmentDataSource
import com.data.models.image.ImageDataSource
import com.data.models.notification.NotificationDataSource
import com.data.models.propertylisting.PropertyListingDataSource
import com.data.models.user.UserDataSource
import com.security.hashing.HashingService
import com.security.token.GitHubOAuthService
import com.security.token.TokenConfig
import com.security.token.TokenService
import com.service.mailservice.MailerSendService
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
    notificationDataSource: NotificationDataSource,
    activityDataSource: ActivityDataSource,
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
            agencyDataSource,
            imageDataSource,
            activityDataSource
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

        imageRoutes( imageDataSource )

        propertyListingRoutes(propertyListingDataSource)

        appointmentRoutes(appointmentDataSource, notificationDataSource)

        profileRoutes(userDataSource, hashingService, activityDataSource)

        admin(
            hashingService,
            userDataSource,
            mailerSendService,
            agencyDataSource,
            activityDataSource
        )
    }
}


