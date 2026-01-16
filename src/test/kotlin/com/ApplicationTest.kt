package com

import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.config.*
import io.ktor.server.testing.*
import kotlin.test.Test
import kotlin.test.assertEquals

class ApplicationTest {

    @Test
    fun testRoot() = testApplication {

        environment {
            config = MapApplicationConfig(
                "jwt.issuer" to "test-issuer",
                "jwt.audience" to "test-audience",
                "jwt.realm" to "test-realm",
                "jwt.secret" to "test-secret"
            )
        }

        application {
            module()
        }

        client.get("/").apply {
            assertEquals(HttpStatusCode.OK, status)
        }
    }
}