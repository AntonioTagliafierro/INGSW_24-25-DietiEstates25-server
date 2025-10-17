package com.plugins



import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.routing.*
import java.io.File

fun Application.configureStaticFiles() {
    routing {
        // Serve direttamente le immagini caricate
        static("/uploads/images") {
            files("uploads/images")
        }
    }
}