package com.jackleow.presentation.flows

import com.jackleow.presentation.flows.counter.*
import com.jackleow.presentation.models.ChatMessage
import com.jackleow.presentation.flows.tokenizing.Tokenizer
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

private typealias TokenCounts = MultiSet<String>
private typealias TokensBySender = Map<String, FifoBoundedSet<String>>

object SendersByTokenCountFlow {
    private sealed interface Command
    private data class Next(val message: ChatMessage) : Command
    private data object Broadcast : Command
    private data object Clear : Command

    @Serializable
    data class Counts internal constructor(private val tokensAndCounts: List<List<CountOrTokens>>) {
        @Serializable(with = CountOrTokensSerializer::class)
        internal sealed interface CountOrTokens
        internal data class Count(val value: Int) : CountOrTokens
        internal data class Tokens(val values: List<String>) : CountOrTokens

        private class CountOrTokensSerializer : KSerializer<CountOrTokens> {
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

        constructor(tokenCounts: TokenCounts) : this(
            tokenCounts.elementsByCount.map {
                listOf(Count(it.key), Tokens(it.value))
            }
        )
    }

    private val log: Logger = LoggerFactory.getLogger(javaClass)

    operator fun invoke(
        name: String, extractToken: Tokenizer, tokensPerSender: Int,
        chatMessageSource: Flow<ChatMessage>, resetSource: Flow<Unit>,
        rejectedMessageSink: ChatBroadcastFlow
    ): Flow<Counts> {
        val singleBroadcast: Flow<Command> = flowOf(Broadcast) // To always emit existing element
        val senderAndTextSource: Flow<Command> = chatMessageSource.map(::Next)
        val clearSource: Flow<Command> = resetSource.map { Clear }
        val stateFlow: MutableStateFlow<Pair<TokensBySender, TokenCounts>> =
            MutableStateFlow(Pair(mapOf(), multiSetOf()))

        return listOf(singleBroadcast, senderAndTextSource, clearSource)
            .merge()
            .map { command: Command ->
                val (_, tokenCounts: TokenCounts) = stateFlow.updateAndGet { (tokensBySender, tokenCounts) ->
                    when (command) {
                        is Next -> {
                            val msg: ChatMessage = command.message
                            val extractedTokens: List<String> = extractToken(msg.text)

                            if (extractedTokens.isNotEmpty()) {
                                log.info(
                                    """Extracted tokens "${extractedTokens.joinToString("""", """")}""""
                                )
                                val sender: String? = msg.sender.let { if (it == "") null else it }
                                val prioritizedTokens: List<String> = extractedTokens.reversed()
                                val (
                                    newTokensBySender: TokensBySender,
                                    addedTokens: Set<String>,
                                    removedTokens: Set<String>
                                ) =
                                    if (sender != null) {
                                        val (
                                            tokens: FifoBoundedSet<String>,
                                            updates: List<FifoBoundedSet.Effect<String>>
                                        ) =
                                            (tokensBySender[sender] ?: FifoBoundedSet(tokensPerSender))
                                                .addAll(prioritizedTokens)
                                        val addedTokens: Set<String> = updates.reversed()
                                            .map {
                                                when (it) {
                                                    is FifoBoundedSet.Added -> it.added
                                                    is FifoBoundedSet.AddedEvicting -> it.added
                                                }
                                            }
                                            .toSet()
                                        val removedTokens: Set<String> = updates
                                            .mapNotNull {
                                                when (it) {
                                                    is FifoBoundedSet.Added -> null
                                                    is FifoBoundedSet.AddedEvicting -> it.evicting
                                                }
                                            }
                                            .toSet()

                                        Triple(tokensBySender + (sender to tokens), addedTokens, removedTokens)
                                    } else {
                                        Triple(tokensBySender, prioritizedTokens.toSet(), setOf())
                                    }
                                val newTokenCounts: TokenCounts = addedTokens
                                    .fold(
                                        removedTokens.fold(tokenCounts) { accum: TokenCounts, oldToken: String ->
                                            accum - oldToken
                                        }
                                    ) { accum: TokenCounts, newToken: String ->
                                        accum + newToken
                                    }

                                Pair(newTokensBySender, newTokenCounts)
                            } else {
                                log.info("No token extracted")
                                rejectedMessageSink.emit(msg)

                                Pair(tokensBySender, tokenCounts)
                            }
                        }

                        is Broadcast -> Pair(tokensBySender, tokenCounts)

                        is Clear -> Pair(mapOf(), multiSetOf())
                    }
                }

                Counts(tokenCounts)
            }
            .shareIn(CoroutineScope(Dispatchers.Default), SharingStarted.WhileSubscribed(), 1)
            .logSubscriptions(log) { increment, count ->
                "${if (increment) "+" else "-"}1 $name subscriber (=$count)"
            }
    }
}