package io.agora.flat.common.board

import com.google.gson.Gson
import com.google.gson.JsonObject
import org.junit.Test

class WhiteSyncedStateTest {
    private val gson = Gson()

    @Test
    fun testGetMapStates() {
        val testJson = "{\"1\":{\"camera\":true,\"mic\":false},\"2\":{\"camera\":false,\"mic\":false},\"3\":null}"
        val devicesStates = getMapState(testJson, DeviceState::class.java)

        val testJsonBoolean = "{\"1\":true,\"2\":false,\"3\":null}"
        val jsonStates = getMapState(testJsonBoolean, Boolean::class.java)
    }

    private fun <T> getMapState(state: String, type: Class<T>): Map<String, T> {
        val deviceStates = mutableMapOf<String, T>()
        try {
            val jsonObject = gson.fromJson(state, JsonObject::class.java)
            jsonObject.entrySet().forEach {
                if (!it.value.isJsonNull) {
                    deviceStates[it.key] = gson.fromJson(it.value, type)
                }
            }
        } catch (e: Exception) {

        }
        return deviceStates
    }
}