package com.jackleow.presentation.plugins

import com.jackleow.presentation.models.ChatMessage
import com.jackleow.presentation.models.ErrorResponse
import com.jackleow.presentation.services.PresentationService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.io.File

fun Application.configureRouting(service: PresentationService) {
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            if (cause is BadRequestException) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse(cause.message))
            } else {
                call.respond(HttpStatusCode.InternalServerError, ErrorResponse(cause.message))
            }
        }
    }

    routing {
        get("/") {
            val path: String = application.environment.config.property("presentation.htmlPath").getString()
            call.respondFile(File(path))
        }

        // Moderation
        route("/moderator") {
            get("") {
                call.respondText(
                    text = object {}.javaClass.getResource("/static/moderator.html")
                        ?.readText()
                        ?: throw IllegalStateException("/static/moderator.html not found"),
                    contentType = ContentType.Text.Html
                )
            }
        }
        post("/chat") {
            val route: String = call.parameters["route"] ?: throw MissingRequestParameterException(""""route"""")
            val text: String = call.parameters["text"] ?: throw MissingRequestParameterException(""""text"""")

            val senderAndRecipient: Pair<String, String>? = when {
                route.endsWith(" to Me (Direct Message)") ->
                    route.dropLast(23) to "Me"

                route.endsWith(" to Me") ->
                    route.dropLast(6) to "Me"

                route.endsWith(" to Everyone") ->
                    route.dropLast(12) to "Everyone"

                route.startsWith("Me to ") -> null

                else -> throw BadRequestException("""malformed "route": $route""")
            }

            if (senderAndRecipient != null) {
                val (sender: String, recipient: String) = senderAndRecipient
                service.chatMessageSink.emit(ChatMessage(sender, recipient, text))
            }
            call.response.status(HttpStatusCode.NoContent)
        }
        get("/reset") {
            service.resetSink.emit(Unit)
            call.response.status(HttpStatusCode.NoContent)
        }

        // Transcription
        get("/transcriber") {
            call.respondText(
                object {}.javaClass.getResource("/static/transcriber.html")
                    ?.readText()
                    ?: throw IllegalStateException("/static/transcriber.html not found"),
                contentType = ContentType.Text.Html
            )
        }
        post("/transcription") {
            val text: String = call.parameters["text"] ?: throw MissingRequestParameterException(""""text"""")
            service.transcriptionTextSink.emit(text)
            call.response.status(HttpStatusCode.NoContent)
        }
    }
}
