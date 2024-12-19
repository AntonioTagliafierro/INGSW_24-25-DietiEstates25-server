package com.data.responses

@kotlinx.serialization.Serializable
data class GitHubUserResponse(
    val login: String?,
    val email: String?,
    val avatar_url: String?,
    val html_url: String?,
    val public_repos: Int?,
    val followers: Int?,
    val following: Int?
)

