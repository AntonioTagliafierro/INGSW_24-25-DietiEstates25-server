package com.data.requests

import kotlinx.serialization.Serializable

@Serializable
data class GitHubAuthRequest(
    val code: String,
    val state: String
)