package com.jackleow.presentation.flows

import com.jackleow.presentation.models.ChatMessage
import com.jackleow.presentation.models.ModeratedText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object ModeratedTextCollectionFlow {
    private sealed interface Command
    private data class Next(val text: String) : Command
    private data object Broadcast : Command
    private data object Clear : Command

    private val log: Logger = LoggerFactory.getLogger(javaClass)

    operator fun invoke(
        name: String,
        chatMessageSource: Flow<ChatMessage>,
        resetSource: Flow<Unit>,
        rejectedMessageSink: ChatBroadcastFlow
    ): Flow<ModeratedText> {
        val singleBroadcast: Flow<Command> = flowOf(Broadcast) // To always emit existing element
        val acceptedTextSource: Flow<Command> = chatMessageSource
            .filter {
                when {
                    it.sender != "" -> {
                        rejectedMessageSink.emit(it)
                        false
                    }

                    else -> true
                }
            }
            .map { Next(it.text) }
        val clearSource: Flow<Command> = resetSource.map { Clear }
        val stateFlow: MutableStateFlow<List<String>> = MutableStateFlow(listOf())

        return listOf(singleBroadcast, acceptedTextSource, clearSource)
            .merge()
            .map { command: Command ->
                stateFlow.updateAndGet { accum: List<String> ->
                    when (command) {
                        is Next -> accum + command.text
                        is Broadcast -> accum
                        is Clear -> listOf()
                    }
                }
            }
            .map { ModeratedText(it) }
            .shareIn(CoroutineScope(Dispatchers.Default), SharingStarted.WhileSubscribed(), 1)
            .logSubscriptions(log) { increment, count ->
                "${if (increment) "+" else "-"}1 $name subscriber (=$count)"
            }
    }
}
