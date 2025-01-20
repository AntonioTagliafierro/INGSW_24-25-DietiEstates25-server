package com.data.models.user

import com.data.responses.GitHubEmailResponse
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class ThirdPartyUser(
    username: String,
    email: String,
    val provider: String, // Es. "github", "google"
) : User(email, username ) {

}

