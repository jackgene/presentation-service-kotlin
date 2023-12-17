package com.jackleow.presentation.services

import com.jackleow.presentation.flows.ChatBroadcastFlow
import com.jackleow.presentation.flows.ModeratedTextCollectionFlow
import com.jackleow.presentation.flows.SendersByTokenCountFlow
import com.jackleow.presentation.flows.TranscriptionBroadcastFlow
import com.jackleow.presentation.flows.tokenizing.MappedKeywordsTokenizer
import com.jackleow.presentation.flows.tokenizing.NormalizedWordsTokenizer
import com.jackleow.presentation.models.ChatMessage
import com.jackleow.presentation.models.ModeratedText
import com.jackleow.presentation.models.Transcript
import io.ktor.server.config.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

class PresentationService(config: ApplicationConfig) {
    private val languagePollConfig: ApplicationConfig = config.config("presentation.language-poll")
    private val wordCloudConfig: ApplicationConfig = config.config("presentation.word-cloud")

    private val rejectedMessageSink: ChatBroadcastFlow = ChatBroadcastFlow("rejected")
    val chatMessageSink: ChatBroadcastFlow = ChatBroadcastFlow("chat")
    val resetSink: MutableSharedFlow<Unit> = MutableSharedFlow()

    val languagePollSource: Flow<SendersByTokenCountFlow.Counts> =
        SendersByTokenCountFlow(
            "language-poll",
            MappedKeywordsTokenizer(
                languagePollConfig.config("language-by-keyword")
                    .toMap()
                    .mapValues { it.value.toString() }
            ),
            languagePollConfig.property("max-votes-per-person").getString().toInt(),
            chatMessageSink, resetSink, rejectedMessageSink
        )
    val wordCloudSource: Flow<SendersByTokenCountFlow.Counts> =
        SendersByTokenCountFlow(
            "word-cloud",
            NormalizedWordsTokenizer(
                wordCloudConfig.property("stop-words")
                    .getList()
                    .toSet(),
                wordCloudConfig.property("min-word-length").getString().toInt(),
                wordCloudConfig.property("max-word-length").getString().toInt()
            ),
            wordCloudConfig.property("max-words-per-person").getString().toInt(),
            chatMessageSink, resetSink, rejectedMessageSink
        )
    val questionsSource: Flow<ModeratedText> =
        ModeratedTextCollectionFlow("question", chatMessageSink, resetSink, rejectedMessageSink)

    val rejectedMessageSource: Flow<ChatMessage> = rejectedMessageSink

    val transcriptionTextSink: TranscriptionBroadcastFlow = TranscriptionBroadcastFlow()
    val transcriptionSource: Flow<Transcript> = transcriptionTextSink
}
