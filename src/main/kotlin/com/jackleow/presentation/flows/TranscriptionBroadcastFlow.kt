package com.jackleow.presentation.flows

import com.jackleow.presentation.models.Transcript
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class TranscriptionBroadcastFlow(
    private val transcriptionFlow: MutableSharedFlow<Transcript> = MutableSharedFlow()
) : Flow<Transcript> by transcriptionFlow {
    private val log: Logger = LoggerFactory.getLogger(javaClass)

    init {
        transcriptionFlow.logSubscriptions(log) { increment, count ->
            "${if (increment) "+" else "-"}1 subscriber (=$count)"
        }
    }

    suspend fun emit(text: String) {
        log.info("Received transcription text: $text")
        transcriptionFlow.emit(Transcript(text))
    }
}
