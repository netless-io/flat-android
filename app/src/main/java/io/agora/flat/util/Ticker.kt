package io.agora.flat.util

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow

object Ticker {
    fun tickerFlow(period: Long, initialDelay: Long = 0) = flow {
        delay(initialDelay)
        while (true) {
            emit(Unit)
            delay(period)
        }
    }
}