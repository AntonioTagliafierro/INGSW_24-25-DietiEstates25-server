
package com.security.token

// Definizione di una data class chiamata TokenClaim.
// Questa classe rappresenta una "claim" (dichiarazione) associata a un token, ad esempio un JWT.
// Le "claims" sono informazioni aggiuntive incluse nei token, come attributi o metadati (es. ruolo, ID utente, ecc.).

data class TokenClaim(
    // Il "name" è il nome della claim, rappresentato come stringa.
    // Ad esempio, potrebbe essere qualcosa come "sub" (subject), "role" (ruolo), o "exp" (scadenza).
    val name: String,

    // Il "value" è il valore associato al nome della claim.
    // Ad esempio, se "name" è "role", "value" potrebbe essere "admin".
    val value: String
)
