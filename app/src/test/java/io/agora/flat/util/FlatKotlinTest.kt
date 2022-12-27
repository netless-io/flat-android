package io.agora.flat.util

import kotlinx.coroutines.*
import org.junit.Test

class FlatKotlinTest {

    @Test
    fun testSupervisorRun() = runBlocking {
        try {
            supervisorRun()
        } catch (e: Error) {
            println("supervisorRun catch $e")
        }
        println("testSupervisorRun end")
    }

    private suspend fun supervisorRun() = supervisorScope {
        val deferredOne = async {
            try {
                delay(5000)
            } catch (e: Exception) {
                println("async one catch $e")
            }
        }
        val deferredTwo = async {
            println("async two throw exception")
            throw AssertionError("async two error")
        }

        try {
            awaitAll(deferredOne, deferredTwo)
        } catch (e: Exception) {
            println("awaitAll catch $e")
        }
    }
}