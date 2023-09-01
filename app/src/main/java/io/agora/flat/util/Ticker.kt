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

    fun countDownFlow(totalSeconds: Long) = flow {
        for (i in totalSeconds - 1 downTo 0) {
            emit(i)
            delay(1000)
        }
    }
}