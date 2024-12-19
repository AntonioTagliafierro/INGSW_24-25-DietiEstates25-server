package com.security.token

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*

import io.ktor.client.call.*

import kotlinx.serialization.json.Json

import io.ktor.http.*

class GitHubOAuthService(
    val clientId: String,
    val clientSecret: String,
    val redirectUri: String,
    val httpClient: HttpClient
) {
    val jsonParser = Json { ignoreUnknownKeys = true } // Parser JSON riutilizzabile

    suspend fun getAccessToken(code: String): String? {
        val response: HttpResponse = httpClient.post("https://github.com/login/oauth/access_token") {
            contentType(ContentType.Application.Json)
            setBody(
                mapOf(
                    "client_id" to clientId,
                    "client_secret" to clientSecret,
                    "code" to code,
                    "redirect_uri" to redirectUri
                )
            )
            accept(ContentType.Application.Json)
        }

        return try {
            val tokenResponse = response.body<GitHubAccessTokenResponse>()
            tokenResponse.access_token
        } catch (e: Exception) {
            println("Errore durante il parsing del token: ${e.localizedMessage}")
            null
        }
    }

    suspend fun getUserData(accessToken: String): String {
        try {
            val response: HttpResponse = httpClient.get("https://api.github.com/user") {
                header("Authorization", "Bearer $accessToken")
            }
            return response.bodyAsText()
        } catch (e: Exception) {
            println("Errore durante il recupero dei dati utente: ${e.localizedMessage}")
            throw e
        }
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

        // Parsing della risposta
        val emails = Json { ignoreUnknownKeys = true }
            .decodeFromString<List<GitHubEmailResponse>>(responseBody)

        // Ritorna la prima email primaria e verificata
        return emails.firstOrNull { it.primary && it.verified }?.email
    }




}
