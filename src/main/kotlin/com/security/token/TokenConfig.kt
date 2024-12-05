
package com.security.token

data class TokenConfig(
    // Il "issuer" rappresenta l'entità che emette il token.
    val issuer: String,

    // L'"audience" è l'entità per la quale il token è destinato.
    val audience: String,

    // "expiresIn" rappresenta il tempo di scadenza del token.
    val expiresIn: Long,

    // Il "secret" è una stringa utilizzata per firmare il token.
    val secret: String
)
