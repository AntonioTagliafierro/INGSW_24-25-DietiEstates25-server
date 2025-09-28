package com.security.token
import com.data.models.user.User



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
        TokenClaim("role", user.role.label)
    )
}