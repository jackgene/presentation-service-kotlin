package com.jackleow.presentation.flows

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onSubscription
import org.slf4j.Logger
import java.util.concurrent.atomic.AtomicInteger

fun <T> SharedFlow<T>.logSubscriptions(log: Logger, message: (Boolean, Int) -> String): Flow<T> {
    val subscriptionCount = AtomicInteger()

    return onSubscription { log.info(message(true, subscriptionCount.incrementAndGet())) }
        .onCompletion { log.info(message(false, subscriptionCount.decrementAndGet())) }
}
