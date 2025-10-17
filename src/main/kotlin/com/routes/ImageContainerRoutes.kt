package com.routes

import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.io.File
import java.util.*

fun Route.imageContainerRoutes(baseUrl: String) {

    // POST per upload immagini
    post("/images/upload") {
        val multipart = call.receiveMultipart()
        val uploadedUrls = mutableListOf<String>()

        val uploadDir = File("uploads/images/listings")
        if (!uploadDir.exists()) uploadDir.mkdirs()

        multipart.forEachPart { part ->
            if (part is PartData.FileItem) {
                // Determina estensione dal nome originale o dal MIME type
                val ext = part.originalFileName?.let { File(it).extension }
                    .takeIf { !it.isNullOrEmpty() }
                    ?: when (part.contentType?.contentSubtype?.lowercase()) {
                        "jpeg", "jpg" -> "jpg"
                        "png" -> "png"
                        "webp" -> "webp"
                        else -> "jpg"
                    }

                val fileName = "${UUID.randomUUID()}.$ext"
                val file = File(uploadDir, fileName)

                part.streamProvider().use { input ->
                    file.outputStream().use { it.write(input.readBytes()) }
                }

                // URL completo pronto per il client
                uploadedUrls.add("$baseUrl/uploads/images/listings/$fileName")
            }
            part.dispose()
        }

        call.respond(HttpStatusCode.OK, uploadedUrls)
    }

    // GET per servire un'immagine singola (opzionale, static già gestisce)
    get("/images/listings/{fileName}") {
        val fileName = call.parameters["fileName"] ?: return@get call.respond(HttpStatusCode.BadRequest)
        val file = File("uploads/images/listings/$fileName")
        if (!file.exists()) return@get call.respond(HttpStatusCode.NotFound)
        call.respondFile(file)
    }
}