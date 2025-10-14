package com.service.mailservice

import com.data.models.appointment.Appointment
import com.data.models.propertylisting.ListingSummary
import com.data.models.propertylisting.PropertyListing
import com.data.models.user.User
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.http.HttpStatusCode.Companion.Accepted

class MailerSendService(
    val token: String,
    val baseUrl: String,
    val httpClient: HttpClient
) {

    suspend fun sendAppointmentEmail(
        user: User,
        agent: User,
        listing: ListingSummary,
        appointment: Appointment
    ): HttpResponse {
        val subject = "Welcome to My App"
        val userUsername: String = user.username
        val agentEmail: String = "antonio.tagliafierro1998@gmail.com" // perché API gratuita manda email solo all'email dell'account
        val agentUsername: String = agent.username
        val title: String = listing.title
        val address: String = listing.property.city + " " + listing.property.street + " " + listing.property.civicNumber
        val date: String = appointment.date

        val personalization = EmailRequest.CustomPersonalization(
            email = agentEmail,
            data = mapOf(
                "name" to userUsername,
                "agentName" to agentUsername,
                "title" to title,
                "address" to address,
                "date" to date,
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
                    email = agentEmail,
                    name = agentUsername
                )
            ),
            subject = subject,
            templateId = "k68zxl21dke4j905",
            personalization = listOf(personalization)
        )

        val response = sendSingleEmail(
            mailerSendClient = httpClient,
            url = "$baseUrl/email",
            token = token,
            emailRequest = emailRequest
        )

        if (response.status == Accepted) {
            println("Email sent to $agentEmail")
        } else {
            handleError(response)
        }

        return response

    }

    suspend fun sendSuppAdminEmail(
        recipientEmail: String,
        email: String,
        password: String
    ): HttpResponse {
        val subject = "Welcome to My App"
        val username = email.substringBefore("@")

        val personalization = EmailRequest.CustomPersonalization(
            email = recipientEmail,
            data = mapOf(
                "name" to username,
                "email" to email,
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
                    name = username
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