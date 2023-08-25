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
        val htmlPath: String = application.environment.config.property("htmlPath").getString()
        val routePattern: Regex = """(.*) to (Everyone|Me)(?: \(Direct Message\))?""".toRegex()

        get("/") {
            call.respondFile(File(htmlPath))
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
                routePattern.matches(route) ->
                    routePattern.matchEntire(route)
                        ?.destructured
                        ?.let { (sender, recipient) -> sender to recipient }

                route.startsWith("Me to ") -> null

                else -> throw BadRequestException("""malformed "route": $route""")
            }

            senderAndRecipient?.let {
                val (sender: String, recipient: String) = it
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
