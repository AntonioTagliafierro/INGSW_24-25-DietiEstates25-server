package com.security.token

interface TokenService {

    fun generate(
        config: TokenConfig,

        // Una vararg di claims, ovvero una lista variabile di oggetti TokenClaim.
        // Ogni claim rappresenta un'informazione (nome-valore) da includere nel token.
        vararg claims: TokenClaim

    ): String
}