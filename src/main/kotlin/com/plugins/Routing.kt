package com.plugins

import com.*
import com.data.models.admin.AdminDataSource
import com.data.models.user.UserDataSource
import com.security.hashing.HashingService
import com.security.token.GitHubOAuthService
import com.security.token.TokenConfig
import com.security.token.TokenService
import io.ktor.server.application.*
import io.ktor.server.routing.*

fun Application.configureRouting(
    userDataSource: UserDataSource,
    adminDataSource: AdminDataSource,
    hashingService: HashingService,
    tokenService: TokenService,
    tokenConfig: TokenConfig,
    gitHubOAuthService: GitHubOAuthService
) {
    routing {
        signIn(userDataSource, hashingService, tokenService, tokenConfig)
        signUp(hashingService, userDataSource)
        //signUpAdmin(hashingService, adminDataSource)
        authenticate()
        getSecretInfo()
        githubAuthRoutes(gitHubOAuthService,userDataSource)
    }
}


