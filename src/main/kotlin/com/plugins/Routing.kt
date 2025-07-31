package com.plugins

import com.*
import com.data.models.admin.AdminDataSource
import com.data.models.agency.AgencyDataSource
import com.data.models.user.UserDataSource
import com.security.hashing.HashingService
import com.security.token.GitHubOAuthService
import com.security.token.TokenConfig
import com.security.token.TokenService
import io.ktor.server.application.*
import io.ktor.server.routing.*

fun Application.configureRouting(
    userDataSource: UserDataSource,
    agencyDataSource: AgencyDataSource,
    hashingService: HashingService,
    tokenService: TokenService,
    tokenConfig: TokenConfig,
    gitHubOAuthService: GitHubOAuthService,

    ) {
    routing {
        userAuth( hashingService,userDataSource,tokenService, tokenConfig)
        agencyRequests(
            hashingService,
            userDataSource,
            agencyDataSource,
        )
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
    }
}


