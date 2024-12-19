package com.plugins

import com.authenticate
import com.data.models.user.UserDataSource
import com.getSecretInfo
import com.security.hashing.HashingService
import com.security.token.TokenConfig
import com.security.token.TokenService
import com.signIn
import com.signUp
import io.ktor.server.application.*
import io.ktor.server.routing.*

fun Application.configureRouting(
    userDataSource: UserDataSource,
    hashingService: HashingService,
    tokenService: TokenService,
    tokenConfig: TokenConfig
) {
    routing {
        signIn(userDataSource, hashingService, tokenService, tokenConfig)
        signUp(hashingService, userDataSource)
        authenticate()
        getSecretInfo()
    }
}
