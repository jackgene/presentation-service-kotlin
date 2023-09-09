package com.jackleow.presentation.plugins

import com.jackleow.presentation.services.PresentationService
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Duration
import kotlin.time.Duration.Companion.milliseconds

suspend fun Flow<Frame>.collectInto(outgoing: SendChannel<Frame>) {
    val closed: MutableSharedFlow<Frame?> = MutableSharedFlow()
    outgoing.invokeOnClose {
        runBlocking { closed.emit(null) }
    }

    merge(this, closed)
        .takeWhile { it != null }
        .filterNotNull()
        .collect(outgoing::send)
}

@OptIn(FlowPreview::class)
fun Application.configureSockets(service: PresentationService) {
    install(WebSockets) {
        timeout = Duration.ofSeconds(900)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }

    val batchPeriod = 100.milliseconds

    routing {
        route("/event") {
            webSocket("/language-poll") {
                service.languagePollSource
                    .sample(batchPeriod)
                    .map { Frame.Text(Json.encodeToString(it)) }
                    .collectInto(outgoing)
            }
            webSocket("/word-cloud") {
                service.wordCloudSource
                    .sample(batchPeriod)
                    .map { Frame.Text(Json.encodeToString(it)) }
                    .collectInto(outgoing)
            }
            webSocket("/question") { // websocketSession
                service.questionsSource
                    .map { Frame.Text(Json.encodeToString(it)) }
                    .collectInto(outgoing)
            }

            webSocket("/transcription") { // websocketSession
                service.transcriptionSource
                    .map { Frame.Text(Json.encodeToString(it)) }
                    .collectInto(outgoing)
            }
        }

        webSocket("/moderator/event") {
            service.rejectedMessageSource
                .map { Frame.Text(Json.encodeToString(it)) }
                .collectInto(outgoing)
        }
    }
}
