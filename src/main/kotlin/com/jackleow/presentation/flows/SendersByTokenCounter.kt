package com.jackleow.presentation.flows

import com.jackleow.presentation.models.ChatMessage
import com.jackleow.presentation.tokenizing.Tokenizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.NothingSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object SendersByTokenCounter {
    private sealed interface Command
    private data class Next(val message: ChatMessage) : Command
    private data object Broadcast : Command
    private data object Clear : Command

    @Serializable
    data class ChatMessageAndTokens(
        val chatMessage: ChatMessage,
        val tokens: List<String>
    )

    @Serializable
    data class Counts(
        val chatMessagesAndTokens: List<ChatMessageAndTokens>,
        val tokensBySender: Map<String, List<String>>,
        val tokensAndCounts: List<List<CountOrTokens>>
    ) {
        companion object {
            @Serializable(with = CountOrTokensSerializer::class)
            sealed interface CountOrTokens
            data class Count(val value: Int) : CountOrTokens
            data class Tokens(val values: List<String>) : CountOrTokens

            class CountOrTokensSerializer : KSerializer<CountOrTokens> {
                private val delegateSerializer = ListSerializer(String.serializer())

                @OptIn(ExperimentalSerializationApi::class)
                override val descriptor: SerialDescriptor =
                    SerialDescriptor("CountOrTokens", NothingSerializer().descriptor)

                override fun deserialize(decoder: Decoder): CountOrTokens {
                    throw UnsupportedOperationException()
                }

                override fun serialize(encoder: Encoder, value: CountOrTokens) {
                    when (value) {
                        is Count -> {
                            encoder.encodeInt(value.value)
                        }

                        is Tokens -> {
                            encoder.encodeSerializableValue(delegateSerializer, value.values)
                        }
                    }
                }
            }
        }

        constructor(
            chatMessagesAndTokens: List<ChatMessageAndTokens>,
            tokensBySender: Map<String, List<String>>,
            tokensByCounts: Map<Int, List<String>>
        ) : this(
            chatMessagesAndTokens,
            tokensBySender,
            tokensByCounts.map {
                listOf(Count(it.key), Tokens(it.value))
            }
        )
    }

    private val log: Logger = LoggerFactory.getLogger(javaClass)

    fun flow(
        name: String, extractToken: Tokenizer, tokensPerSender: Int,
        chatMessageSource: Flow<ChatMessage>, resetSource: Flow<Unit>,
        rejectedMessageSink: ChatBroadcastFlow
    ): Flow<Counts> {
        val singleBroadcast: Flow<Command> = flowOf(Broadcast) // To always emit existing element
        val senderAndTextSource: Flow<Command> = chatMessageSource.map { Next(it) }
        val clearSource: Flow<Command> = resetSource.map { Clear }
        val stateFlow: MutableStateFlow<Pair<List<ChatMessageAndTokens>, Map<String, List<String>>>> =
            MutableStateFlow(Pair(listOf(), mapOf()))

        return listOf(singleBroadcast, senderAndTextSource, clearSource)
            .merge()
            .map { command: Command ->
                val (chatMessagesAndTokens: List<ChatMessageAndTokens>, tokensBySender: Map<String, List<String>>) =
                    stateFlow.updateAndGet { (chatMessagesAndTokens, tokensBySender) ->
                        when (command) {
                            is Next -> {
                                val msg: ChatMessage = command.message
                                val oldTokens: List<String> = tokensBySender.getOrDefault(msg.sender, listOf())
                                val extractedTokens: List<String> = extractToken(msg.text)

                                if (extractedTokens.isNotEmpty()) {
                                    log.info("""Extracted tokens "${extractedTokens.joinToString("""", """")}"""")
                                    val newTokens: List<String> = (extractedTokens + oldTokens)
                                        .distinct()
                                        .take(tokensPerSender)
                                    Pair(
                                        chatMessagesAndTokens + ChatMessageAndTokens(msg, extractedTokens),
                                        tokensBySender + (msg.sender to newTokens)
                                    )
                                } else {
                                    log.info("No token extracted")
                                    rejectedMessageSink.emit(msg)
                                    Pair(chatMessagesAndTokens, tokensBySender)
                                }
                            }

                            is Broadcast -> Pair(chatMessagesAndTokens, tokensBySender)

                            is Clear -> Pair(listOf(), mapOf())
                        }
                    }
                val countsByToken: Map<String, Int> = tokensBySender
                    .flatMap { it.value.map { token -> token to it.key } }
                    .groupBy({ it.first }, { it.second })
                    .mapValues { it.value.size }
                val tokensAndCounts: Map<Int, List<String>> = countsByToken
                    .map { it.value to it.key }
                    .groupBy({ it.first }, { it.second })

                Counts(chatMessagesAndTokens, tokensBySender, tokensAndCounts)
            }
            .shareIn(CoroutineScope(Dispatchers.Default), SharingStarted.WhileSubscribed(), 1)
            .logSubscriptions(log) { increment, count ->
                "${if (increment) "+" else "-"}1 $name subscriber (=$count)"
            }
    }
}