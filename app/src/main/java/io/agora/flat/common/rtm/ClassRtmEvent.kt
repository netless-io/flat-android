package io.agora.flat.common.rtm

import com.google.gson.Gson
import com.google.gson.JsonObject
import io.agora.flat.data.model.RoomStatus
import java.util.*

data class ClassRemoteData(val t: String, val v: JsonObject)

sealed class ClassRtmEvent {
    companion object {
        val gson = Gson()

        private val eventClasses = mapOf(
            "on-stage" to OnStageEventWithSender::class.java,
            "raise-hand" to RaiseHandEventWithSender::class.java,
            "update-room-status" to RaiseHandEventWithSender::class.java,
        )

        private val eventTypes = eventClasses.map { it.value to it.key }.toMap()

        fun parse(message: String, sender: String? = null): ClassRtmEvent {
            try {
                val data = gson.fromJson(message, ClassRemoteData::class.java)
                val eventClazz = eventClasses[data.t]
                if (eventClazz != null) {
                    val result = gson.fromJson(data.v, eventClazz)
                    if (result is EventWithSender) {
                        result.sender = sender
                    }
                    return result as ClassRtmEvent
                }
            } catch (e: Exception) {
            }
            return UnknownEvent()
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

interface EventWithSender {
    var sender: String?
}

data class OnStageEventWithSender(
    override var sender: String? = null,
    val roomUUID: String,
    val onStage: Boolean,
) : ClassRtmEvent(), EventWithSender

data class RaiseHandEventWithSender(
    override var sender: String? = null,
    val roomUUID: String,
    val raiseHand: Boolean,
) : ClassRtmEvent(), EventWithSender

data class RoomStateEvent(
    override var sender: String? = null,
    val roomUUID: String,
    val state: RoomStatus,
) : ClassRtmEvent(), EventWithSender

data class OnMemberJoined(
    val channelId: String,
    val userId: String,
) : ClassRtmEvent()

object OnRemoteLogin : ClassRtmEvent()

data class OnMemberLeft(
    val channelId: String,
    val userId: String,
) : ClassRtmEvent()

data class ChatMessage(
    val message: String,
    val sender: String,
) : ClassRtmEvent()

data class UnknownEvent(
    val id: Long = UUID.randomUUID().mostSignificantBits,
) : ClassRtmEvent()