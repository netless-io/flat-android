package io.agora.flat.data

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class RoomServiceFetcherTest {

    @Before
    fun setUp() {
    }

    @After
    fun tearDown() {
    }

    data class FetchEnvCase(
        val uuid: String,
        val env: String,
        val expected: String,
    )

    @Test
    fun test_fetch_env() {
        val cases = listOf(
            // sg invite code
            FetchEnvCase("22884465997", "cn_dev", "sg_dev"),

            // cn invite code
            FetchEnvCase("12122312333", "sg_prod", "cn_prod"),

            // old invite code
            FetchEnvCase("2231213331", "cn_prod", "cn_prod"),

            // sg uuid
            FetchEnvCase("SG-73e84969-1850-44db-ae01-7e46192cb01c", "cn_prod", "sg_prod"),

            // cn uuid
            FetchEnvCase("CN-73e84969-1850-44db-ae01-7e46192cb01c", "sg_dev", "cn_dev")
        )

        cases.forEach { it ->
            assertEquals(it.expected, ServiceFetcher.fetchEnv(it.uuid, it.env))
        }
    }
}