package com.service.mailservice

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.HttpStatusCode.Companion.Accepted
import io.ktor.http.contentType
import io.ktor.http.headers
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.Application
import io.ktor.server.util.url
import kotlinx.coroutines.runBlocking
import java.util.UUID

fun Application.configureClient() {
    val token = environment.config.property("mailersend.token").getString()
    val baseUrl = environment.config.property("mailersend.baseUrl").getString()
    val templateID = environment.config.property("mailersend.template").getString()

    val mailerSendClient = HttpClient {
        install(ContentNegotiation) {
            jackson()
        }

    }

    runBlocking {
        sendMessageUsingTemplate(
            mailerSendClient = mailerSendClient,
            token = token,
            baseUrl = baseUrl,
            templateId = templateID
        )

    }
}


suspend fun sendSingleEmail(
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

//example

suspend fun sendSimpleEmail(
    mailerSendClient: HttpClient,
    baseUrl: String,
    token: String
) {
    val subject =
        "Hello from {\$company}" // \ for bypass the interpolation of $ cause mailersend also uses $ to read variables in tamplates\
    val html = """
        Hello <b>{&{'$'}name} </b>, nice to meet you.
    """.trimIndent()
    val text = """
        Hello {&{'$'}name}, nice to meet you.
    """.trimIndent()

    val emailRequest = EmailRequest(
        from = EmailRequest.Recipient(
            email = "dietiestates25@gmail.com",
            name = "DietiEstates25"
        ),
        to = listOf(
            EmailRequest.Recipient(
                email = "example@gmail.com",
                name = "Test"
            )

        ),
        subject = subject,
        html = html,
        text = text,
        variables = listOf(
            EmailRequest.Variable(
                email = "example@gmail.com",
                substitutions = listOf(
                    EmailRequest.Variable.Substitution(
                        variable = "company",
                        value = "DietiEstates25"
                    ),
                    EmailRequest.Variable.Substitution(
                        variable = "name",
                        value = "Test"
                    )
                )
            )
        )

    )


    val response = sendSingleEmail(
        mailerSendClient = mailerSendClient,
        url = "$baseUrl/email",
        token = token,
        emailRequest = emailRequest
    )

    if (response.status == Accepted) {
        println("Email was sent!")
    } else {
        handleError(response)
    }


}

suspend fun sendMessageUsingTemplate(
    mailerSendClient: HttpClient,
    baseUrl: String,
    token: String,
    templateId: String
) {
    val subject = "My wathever subject"

    val emailRequest = EmailRequest(
        from = EmailRequest.Recipient(
            email = "dietiestates25@gmail.com",
            name = "DietiEstates25"
        ),
        to = listOf(
            EmailRequest.Recipient(
                email = "example@gmail.com",
                name = "Test"
            )
        ),
        subject = subject,
        templateId = templateId,
        personalization = listOf(
            EmailRequest.CustomPersonalization(
                email = "example@gmail.com",
                data = EmailRequest.CustomPersonalization.PersonalizationData(
                    name = "Test",
                    resetPasswordCode = UUID.randomUUID().toString()
                )
            )
        )
    )
    val response = sendSingleEmail(
        mailerSendClient = mailerSendClient,
        url = "$baseUrl/email",
        token = token,
        emailRequest = emailRequest
    )

    if (response.status == Accepted) {
        println("Email was sent!")
    } else {
        handleError(response)
    }

}

suspend fun handleError(response: HttpResponse) {
    val statusCode = response.status.value
    val errorBody = response.body<ErrorResponse>()

    println("Email sending failed with status code [$statusCode] and message $errorBody")

    errorBody.errors?.forEach { (fieldName, fieldErrors) ->
        println("    * field name: [$fieldName]. Messages: ")
        fieldErrors.forEach { error -> println("      - $error") }
    }
}

