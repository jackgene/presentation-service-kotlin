package com.jackleow.presentation.flows

import com.jackleow.presentation.models.ChatMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class ChatBroadcastFlow(
    val name: String,
    private val chatFlow: MutableSharedFlow<ChatMessage> = MutableSharedFlow()
) : Flow<ChatMessage> by chatFlow {
    private val log: Logger = LoggerFactory.getLogger(javaClass)

    init {
        chatFlow.logSubscriptions(log) { increment, count ->
            "${if (increment) "+" else "-"}1 $name subscriber (=$count)"
        }
    }

    suspend fun emit(message: ChatMessage) {
        log.info("Received $name message - $message")
        chatFlow.emit(message)
    }
}