package com.jackleow.presentation.flows

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.shareIn
import org.slf4j.Logger

fun <T> MutableSharedFlow<T>.logSubscriptions(log: Logger, message: (Boolean, Int) -> String): MutableSharedFlow<T> {
    subscriptionCount
        .scan(0) { last: Int, next: Int ->
            if (next != last) {
                log.info(message(next > last, next))
            }
            next
        }
        .shareIn(CoroutineScope(Dispatchers.Default), SharingStarted.Eagerly)

    return this
}
