package io.agora.flat.common.rtm

import com.google.gson.Gson
import com.google.gson.JsonObject
import java.util.*

data class ClassRemoteData(val t: String, val v: JsonObject)

sealed class ClassRtmEvent {
    companion object {
        val gson = Gson()

        private val eventClasses = mapOf(
            "on-stage" to OnStageEvent::class.java,
            "raise-hand" to RaiseHandEvent::class.java,
        )

        private val eventTypes = mapOf(
            OnStageEvent::class.java to "on-stage",
            RaiseHandEvent::class.java to "raise-hand",
        )

        fun parse(message: String, sender: String? = null): ClassRtmEvent {
            try {
                val data = gson.fromJson(message, ClassRemoteData::class.java)
                val eventClazz = eventClasses[data.t]
                if (eventClazz != null) {
                    val result = gson.fromJson(data.v, eventClazz)
                    if (result is P2pEvent) {
                        result.sender = sender
                    }
                    return result as ClassRtmEvent
                }
            } catch (e: Exception) {
            }
            return UnknownEvent();
        }

        fun toText(event: ClassRtmEvent): String {
            val element = gson.toJsonTree(event)
            if (element.isJsonObject) {
                element.asJsonObject.remove("sender")
            }
            val type = eventTypes[event::class.java]
            if (type != null) {
                val jsonObject = JsonObject()
                jsonObject.addProperty("t", type)
                jsonObject.add("v", element)
                return gson.toJson(jsonObject)
            }
            return ""
        }
    }

}

interface P2pEvent {
    var sender: String?
}

data class OnStageEvent(
    override var sender: String? = null,
    val roomUUID: String,
    val onStage: Boolean,
) : ClassRtmEvent(), P2pEvent

data class RaiseHandEvent(
    override var sender: String? = null,
    val roomUUID: String,
    val raiseHand: Boolean,
) : ClassRtmEvent(), P2pEvent

data class OnMemberJoined(
    val channelId: String,
    val userId: String,
)

data class OnMemberLeft(
    val channelId: String,
    val userId: String,
)

data class UnknownEvent(
    val id: Long = UUID.randomUUID().mostSignificantBits,
) : ClassRtmEvent()