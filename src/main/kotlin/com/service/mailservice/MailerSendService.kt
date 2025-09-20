package com.service.mailservice

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.http.HttpStatusCode.Companion.Accepted
import io.ktor.http.content.*
import io.ktor.http.headers

class MailerSendService (
    val token : String,
    val baseUrl : String,
    val httpClient: HttpClient
){

//sendSuppAdminEmail(request.suppAdminEmail, username, password)
    suspend fun sendSuppAdminEmail(
        recipientEmail: String,
        recipientName: String,
        password: String
    ): HttpResponse {
        val subject = "Welcome to My App"

        val personalization = EmailRequest.CustomPersonalization(
            email = recipientEmail,
            data = mapOf(
                "name" to recipientName,
                "email" to "$recipientName@system.com",
                "password" to password,
                "support_email" to "dietiestates25@gmail.com"
            )
        )

        val emailRequest = EmailRequest(
            from = EmailRequest.Recipient(
                email = "dietiestates25@test-q3enl6kv06842vwr.mlsender.net",
                name = "DietiEstates25"
            ),
            to = listOf(
                EmailRequest.Recipient(
                    email = recipientEmail,
                    name = recipientName
                )
            ),
            subject = subject,
            templateId = "3zxk54vy7e14jy6v",
            personalization = listOf(personalization)
        )

        val response = sendSingleEmail(
            mailerSendClient = httpClient,
            url = "$baseUrl/email",
            token = token,
            emailRequest = emailRequest
        )

        if (response.status == Accepted) {
            println("Email sent to $recipientEmail")
        } else {
            handleError(response)
        }

        return response
    }

    private suspend fun handleError(response: HttpResponse) {

        val statusCode = response.status.value
        val errorBody = response.body<ErrorResponse>()

        println("Email sending failed with status code [$statusCode] and message $errorBody")

        errorBody.errors?.forEach { (fieldName, fieldErrors) ->
            println("    * field name: [$fieldName]. Messages: ")
            fieldErrors.forEach { error -> println("      - $error") }
        }
    }

    private suspend fun sendSingleEmail(
        mailerSendClient: HttpClient,
        url: String,
        token: String,
        emailRequest: EmailRequest
    ) = mailerSendClient.post(url) {
        headers {
            append(HttpHeaders.Authorization, "Bearer $token")
        }
        contentType(ContentType.Application.Json)
        setBody(emailRequest)
    }
}