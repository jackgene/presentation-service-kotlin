package com.jackleow.presentation.services

import com.jackleow.presentation.flows.ChatBroadcastFlow
import com.jackleow.presentation.flows.ModeratedTextCollector
import com.jackleow.presentation.flows.SendersByTokenCounter
import com.jackleow.presentation.flows.TranscriptionBroadcastFlow
import com.jackleow.presentation.models.ChatMessage
import com.jackleow.presentation.models.ModeratedText
import com.jackleow.presentation.models.Transcript
import com.jackleow.presentation.tokenizing.Tokenizing
import io.ktor.server.config.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

class PresentationService(config: ApplicationConfig) {
    private val languagePollConfig: ApplicationConfig = config.config("presentation.languagePoll")
    private val wordCloudConfig: ApplicationConfig = config.config("presentation.wordCloud")

    private val rejectedMessageSink: ChatBroadcastFlow = ChatBroadcastFlow("rejected")
    val chatMessageSink: ChatBroadcastFlow = ChatBroadcastFlow("chat")
    val resetSink: MutableSharedFlow<Unit> = MutableSharedFlow()

    val languagePollSource: Flow<SendersByTokenCounter.Counts> =
        SendersByTokenCounter.flow(
            "language-poll",
            Tokenizing.mappedKeywordsTokenizer(
                languagePollConfig.config("languageByKeyword")
                    .toMap()
                    .mapValues { it.value.toString() }
            ),
            languagePollConfig.property("maxVotesPerPerson").getString().toInt(),
            chatMessageSink, resetSink, rejectedMessageSink
        )
    val wordCloudSource: Flow<SendersByTokenCounter.Counts> =
        SendersByTokenCounter.flow(
            "word-cloud",
            Tokenizing.normalizedWordsTokenizer(
                wordCloudConfig.property("stopWords")
                    .getList()
                    .toSet(),
                wordCloudConfig.property("minWordLength").getString().toInt()
            ),
            wordCloudConfig.property("maxWordsPerPerson").getString().toInt(),
            chatMessageSink, resetSink, rejectedMessageSink
        )
    val questionsSource: Flow<ModeratedText> =
        ModeratedTextCollector.flow("question", chatMessageSink, resetSink, rejectedMessageSink)

    val rejectedMessageSource: Flow<ChatMessage> = rejectedMessageSink

    val transcriptionTextSink: TranscriptionBroadcastFlow = TranscriptionBroadcastFlow()
    val transcriptionSource: Flow<Transcript> = transcriptionTextSink
}
