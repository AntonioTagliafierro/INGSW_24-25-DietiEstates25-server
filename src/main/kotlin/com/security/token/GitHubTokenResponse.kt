package com.security.token

@kotlinx.serialization.Serializable
data class GitHubEmailResponse(
    val email: String,
    val primary: Boolean,
    val verified: Boolean,
    val visibility: String?
)

@kotlinx.serialization.Serializable
data class GitHubAccessTokenResponse(
    val access_token: String,
    val token_type: String,
    val scope: String
)

