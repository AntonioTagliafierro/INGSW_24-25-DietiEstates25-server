package com.security.token
import com.data.models.user.User
import com.data.models.user.myToLowerCase


fun generateTokenForUser(
    user: User,
    tokenService: TokenService,
    config: TokenConfig
): String {
    return tokenService.generate(
        config = config,
        TokenClaim("userId", user.id.toString()),
        TokenClaim("username", user.username),
        TokenClaim("email", user.email.myToLowerCase()),
        TokenClaim("role", user.role.label)
    )
}