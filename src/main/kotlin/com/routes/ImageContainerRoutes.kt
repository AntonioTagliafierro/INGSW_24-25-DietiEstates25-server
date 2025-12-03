package com.routes

import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.io.File
import java.util.*

fun Route.imageContainerRoutes(baseUrl: String) {

    post("/images/upload") {
        val multipart = call.receiveMultipart()
        val uploadedUrls = mutableListOf<String>()

        val uploadDir = File("uploads/images/listings")
        if (!uploadDir.exists()) uploadDir.mkdirs()

        multipart.forEachPart { part ->
            println("Received part: ${part.name} - ${part::class.simpleName}")

            if (part is PartData.FileItem) {
                // Estensione file dal nome originale o dal contentType
                val ext = part.originalFileName?.let { File(it).extension }
                    .takeIf { !it.isNullOrEmpty() }
                    ?: when (part.contentType?.contentSubtype?.lowercase()) {
                        "jpeg", "jpg" -> "jpg"
                        "png" -> "png"
                        "webp" -> "webp"
                        else -> "jpg"
                    }

                // genera un nome casuale
                val fileName = "${UUID.randomUUID()}.$ext"
                val file = File(uploadDir, fileName)

                try {
                    part.streamProvider().use { input ->
                        file.outputStream().use { it.write(input.readBytes()) }
                    }
                    uploadedUrls.add("$baseUrl/uploads/images/listings/$fileName")
                    println("File saved: ${file.absolutePath}")
                } catch (e: Exception) {
                    println("Error: File not saved: ${e.message}")
                }
            }

            part.dispose()
        }

        if (uploadedUrls.isEmpty()) {
            call.respond(HttpStatusCode.BadRequest, "No valid file sent")
        } else {
            call.respond(HttpStatusCode.OK, uploadedUrls)
        }
    }

    get("/images/listings/{fileName}") {
        val fileName = call.parameters["fileName"] ?: return@get call.respond(HttpStatusCode.BadRequest)
        val file = File("uploads/images/listings/$fileName")
        if (!file.exists()) return@get call.respond(HttpStatusCode.NotFound)
        call.respondFile(file)
    }
}