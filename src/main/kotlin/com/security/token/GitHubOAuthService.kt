package com.security.token

import com.data.responses.GitHubAccessTokenResponse
import com.data.responses.GitHubEmailResponse
import com.data.responses.GitHubUserResponse
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*

import io.ktor.client.call.*

import kotlinx.serialization.json.Json

import io.ktor.http.*
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.*

private const val JSON = "application/json"

class GitHubOAuthService(
    val clientId: String,
    val clientSecret: String,
    val redirectUri: String,
    val httpClient: HttpClient
) {

    suspend fun getAccessToken(code: String): String {
        val response: HttpResponse = httpClient.post("https://github.com/login/oauth/access_token") {
            url {
                parameters.append("client_id", clientId)
                parameters.append("client_secret", clientSecret)
                parameters.append("code", code)
                parameters.append("redirect_uri", redirectUri)
            }
            headers {
                append(HttpHeaders.Accept, JSON)
            }
        }

        val responseBody = response.body<GitHubAccessTokenResponse>()
        println("GitHub Token Response: $responseBody")

        return responseBody.access_token
    }

    // Usa l'access token per ottenere i dati dell'utente
    suspend fun getUserInfo(accessToken: String): GitHubUserResponse? {
        val response: HttpResponse = httpClient.get("https://api.github.com/user") {
            header("Authorization", "Bearer $accessToken")
            header("Accept", JSON) // Accetta JSON come risposta
        }

        val responseBody = response.bodyAsText()
        println("GitHub User API Response: $responseBody") // Debug della risposta

        // Decodifica solo i campi necessari
        return Json { ignoreUnknownKeys = true }.decodeFromString(responseBody)
    }


    suspend fun getPrimaryEmail(accessToken: String): String? {
        val response: HttpResponse = httpClient.get("https://api.github.com/user/emails") {
            header("Authorization", "Bearer $accessToken")
        }
        val responseBody = response.bodyAsText()

        // Debug della risposta
        println("Email API Response: $responseBody")

        // Controllo di eventuali errori
        if (response.status != HttpStatusCode.OK) {
            println("Errore nell'API: ${responseBody}")
            return null
        }

        // Decodifica come una lista di oggetti
        val emails = Json { ignoreUnknownKeys = true }
            .decodeFromString<List<GitHubEmailResponse>>(responseBody)

        // Trova l'email primaria e verificata
        return emails.firstOrNull { it.primary && it.verified }?.email
    }


    suspend fun getProfilePicBase64(accessToken: String): String? {
        // 1) Chiamo /user
        val userResp: HttpResponse = httpClient.get("https://api.github.com/user") {
            header("Authorization", "Bearer $accessToken")
            header("Accept", "application/json")
        }

        if (!userResp.status.isSuccess()) {
            println("GitHub /user error: ${userResp.status} - ${userResp.bodyAsText()}")
            return null
        }

        val userJson = userResp.bodyAsText()
        val jsonElement = Json.parseToJsonElement(userJson).jsonObject
        val avatarUrl = jsonElement["avatar_url"]?.jsonPrimitive?.content
            ?: return null

        // 2) Scarico l’immagine
        val avatarResp: HttpResponse = httpClient.get(avatarUrl) {
            header("Accept", "image/*")
        }

        if (!avatarResp.status.isSuccess()) {
            println("Avatar download error: ${avatarResp.status} - ${avatarResp.bodyAsText()}")
            return null
        }

        val bytes: ByteArray = avatarResp.body()
        val contentType = avatarResp.headers["Content-Type"] ?: "image/png"
        val b64 = Base64.getEncoder().encodeToString(bytes)

        return "data:$contentType;base64,$b64"
    }
}



