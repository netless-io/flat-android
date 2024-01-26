package io.agora.flat.common.rtm

import com.google.gson.Gson
import com.google.gson.JsonObject
import io.agora.flat.data.model.RoomStatus
import java.util.*

data class ClassRemoteData(val t: String, val v: JsonObject)

/**
 * a class to parse and convert event in rtm
 */
sealed class ClassRtmEvent {
    companion object {
        val gson = Gson()

        private val eventClasses = mapOf(
            "raise-hand" to RaiseHandEvent::class.java,
            "update-room-status" to RoomStateEvent::class.java,
            "ban" to RoomBanEvent::class.java,
            "request-device" to RequestDeviceEvent::class.java,
            "request-device-response" to RequestDeviceResponseEvent::class.java,
            "notify-device-off" to NotifyDeviceOffEvent::class.java,
            "reward" to RewardEvent::class.java,
            "enter" to EnterRoomEvent::class.java,
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
            } catch (_: Exception) {
            }
            return UnknownEvent()
        }

        /**
         * parse service event
         */
        fun parseSys(message: String, sender: String? = null): ClassRtmEvent {
            try {
                val event = ExpirationWarningEvent.parse(message)
                if (event != null) {
                    return event
                }
            } catch (_: Exception) {
            }
            return UnknownEvent()
        }

        fun toText(event: ClassRtmEvent): String {
            val element = gson.toJsonTree(event)
            if (event is EventWithSender) {
                element.asJsonObject.remove("sender")
            }
            val type = eventTypes[event::class.java]
            if (type != null) {
                return gson.toJson(ClassRemoteData(type, element.asJsonObject))
            }
            return ""
        }
    }

}

interface EventWithSender {
    var sender: String?
}

data class RaiseHandEvent(
    override var sender: String? = null,
    val roomUUID: String,
    val raiseHand: Boolean,
) : ClassRtmEvent(), EventWithSender

data class RoomStateEvent(
    override var sender: String? = null,
    val roomUUID: String,
    val status: RoomStatus,
) : ClassRtmEvent(), EventWithSender

data class RoomBanEvent(
    override var sender: String? = null,
    val roomUUID: String,
    val status: Boolean,
) : ClassRtmEvent(), EventWithSender

data class RequestDeviceEvent(
    override var sender: String? = null,
    val roomUUID: String,
    val mic: Boolean? = null,
    val camera: Boolean? = null,
) : ClassRtmEvent(), EventWithSender

data class RequestDeviceResponseEvent(
    override var sender: String? = null,
    val roomUUID: String,
    val mic: Boolean? = null,
    val camera: Boolean? = null,
) : ClassRtmEvent(), EventWithSender

data class NotifyDeviceOffEvent(
    override var sender: String? = null,
    val roomUUID: String,
    val mic: Boolean? = null,
    val camera: Boolean? = null,
) : ClassRtmEvent(), EventWithSender

data class RewardEvent(
    override var sender: String? = null,
    val roomUUID: String,
    val userUUID: String,
) : ClassRtmEvent(), EventWithSender

data class EnterRoomEvent(
    override var sender: String? = null,
    val roomUUID: String,
    val userUUID: String,
    val userInfo: EventUserInfo,
) : ClassRtmEvent(), EventWithSender

data class EventUserInfo(
    var rtcUID: Int,
    var name: String,
    var avatarURL: String,
)

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

// from service
data class ExpirationWarningEvent(
    val roomLevel: Int,
    val expireAt: Long,
    val leftMinutes: Int,
    val message: String,
) : ClassRtmEvent() {
    companion object {
        fun parse(message: String): ExpirationWarningEvent? {
            try {
                val obj = gson.fromJson(message, JsonObject::class.java)
                val roomLevel = obj.get("roomLevel").asInt
                val expireAt = obj.get("expireAt").asLong
                val leftMinutes = obj.get("leftMinutes").asInt
                val message = obj.get("message").asString
                return ExpirationWarningEvent(roomLevel, expireAt, leftMinutes, message)
            } catch (_: Exception) {
            }
            return null
        }
    }
}