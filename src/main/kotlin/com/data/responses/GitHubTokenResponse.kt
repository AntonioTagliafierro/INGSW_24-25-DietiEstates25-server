package com.data.responses

import kotlinx.serialization.Serializable

@kotlinx.serialization.Serializable
data class GitHubEmailResponse(
    val email: String,
    val primary: Boolean,
    val verified: Boolean,
    val visibility: String? = null
)

@kotlinx.serialization.Serializable
data class GitHubAccessTokenResponse(
    val access_token: String,
    val token_type: String,
    val scope: String
)

// Classe per rappresentare la risposta dell'utente da GitHub
@Serializable
data class GitHubUserResponse(
    val login: String,    // Nome utente
    val email: String? = null // Email (potrebbe essere null se non è disponibile)
)


