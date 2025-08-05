package com.security.token
import com.data.models.user.User

import com.security.token.TokenClaim
import com.security.token.TokenConfig
import com.security.token.TokenService

fun generateTokenForUser(
    user: User,
    tokenService: TokenService,
    config: TokenConfig
): String {
    return tokenService.generate(
        config = config,
        TokenClaim("userId", user.id.toString()),
        TokenClaim("username", user.getUsername()),
        TokenClaim("email", user.getEmail()),
        TokenClaim("type", user.type)
    )
}