package com.plugins

import com.*
import com.data.models.admin.AdminDataSource
import com.data.models.property.MongoPropertyDataSource
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
    userDataSource: UserDataSource,
    adminDataSource: AdminDataSource,
    hashingService: HashingService,
    tokenService: TokenService,
    tokenConfig: TokenConfig,
    gitHubOAuthService: GitHubOAuthService,
    httpClient: HttpClient
) {

    val propertyDataSource = MongoPropertyDataSource(getDatabase())

    val geoapifyService = GeoapifyService(
        apiKey = System.getenv("GEOAPIFY_KEY"),
        httpClient = HttpClient(CIO)
    )
    routing {
        userAuth( hashingService,userDataSource,tokenService, tokenConfig)
        signUpAdmin(hashingService, adminDataSource)
        authenticate()
        getSecretInfo()
        githubAuthVerification(
            gitHubOAuthService,
            userDataSource,
            hashingService,
            tokenService,
            tokenConfig
        )
        state()
        propertyRoutes(propertyDataSource, geoapifyService)
    }
}


