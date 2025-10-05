package com.plugins

import com.*
import com.data.models.activity.ActivityDataSource
import com.data.models.agency.AgencyDataSource
import com.data.models.image.ImageDataSource
import com.data.models.offer.OfferDataSource
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
    activityDataSource: ActivityDataSource,
    offerDataSource : OfferDataSource,
) {


    routing {
        offerRouting(
            offerDataSource,
            userDataSource,
            propertyListingDataSource,
            activityDataSource,
        )
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


