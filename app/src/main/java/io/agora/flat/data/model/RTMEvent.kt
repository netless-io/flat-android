package io.agora.flat.data.model

import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.annotations.SerializedName

sealed class RTMEvent(type: RTMessageType = RTMessageType.ChannelMessage) {
    @SerializedName("t")
    var t = type

    // ChannelId
    @SerializedName("r")
    var r = ""

    data class ChannelMessage(
        @SerializedName("v")
        val text: String,
    ) : RTMEvent()

    data class Notice(
        @SerializedName("v")
        val text: String,
    ) : RTMEvent(RTMessageType.Notice)

    data class RaiseHand(
        @SerializedName("v")
        val v: Boolean,
    ) : RTMEvent(RTMessageType.RaiseHand)

    data class AcceptRaiseHand(
        @SerializedName("v")
        val value: AcceptRaiseHandValue,
    ) : RTMEvent(RTMessageType.AcceptRaiseHand)

    data class CancelHandRaising(
        @SerializedName("v")
        val v: Boolean,
    ) : RTMEvent(RTMessageType.CancelHandRaising)

    data class BanText(
        @SerializedName("v")
        val v: Boolean,
    ) : RTMEvent(RTMessageType.BanText)

    data class Speak(
        @SerializedName("v")
        val v: List<SpeakItem>,
    ) : RTMEvent(RTMessageType.Speak)

    data class DeviceState(
        @SerializedName("v")
        val value: DeviceStateValue,
    ) : RTMEvent(RTMessageType.DeviceState)

    data class ClassMode(
        @SerializedName("v")
        val classModeType: ClassModeType,
    ) : RTMEvent(RTMessageType.ClassMode)

    data class RoomStatus(
        @SerializedName("v")
        val roomStatus: io.agora.flat.data.model.RoomStatus,
    ) : RTMEvent(RTMessageType.RoomStatus)

    data class RequestChannelStatus(
        @SerializedName("v")
        val value: RequestChannelStatusValue,
    ) : RTMEvent(RTMessageType.RequestChannelStatus)

    data class ChannelStatus(
        @SerializedName("v") val value: ChannelStatusValue,
    ) : RTMEvent(RTMessageType.ChannelStatus)

    data class AllOffStage(
        @SerializedName("v") val value: Boolean
    ) : RTMEvent(RTMessageType.AllOffStage)

    companion object {
        val gson = Gson()

        fun parseRTMEvent(text: String): RTMEvent {
            try {
                val jsonObject = JsonParser.parseString(text).asJsonObject
                val type = jsonObject.get("t").asString
                // val value = jsonObject.get("v")
                return when (RTMessageType.valueOf(type)) {
                    RTMessageType.Notice -> gson.fromJson(jsonObject, Notice::class.java)
                    RTMessageType.RaiseHand -> gson.fromJson(jsonObject, RaiseHand::class.java)
                    RTMessageType.AcceptRaiseHand -> gson.fromJson(jsonObject, AcceptRaiseHand::class.java)
                    RTMessageType.CancelHandRaising -> gson.fromJson(jsonObject, CancelHandRaising::class.java)
                    RTMessageType.BanText -> gson.fromJson(jsonObject, BanText::class.java)
                    RTMessageType.Speak -> gson.fromJson(jsonObject, Speak::class.java)
                    RTMessageType.DeviceState -> gson.fromJson(jsonObject, DeviceState::class.java)
                    RTMessageType.ClassMode -> gson.fromJson(jsonObject, ClassMode::class.java)
                    RTMessageType.RoomStatus -> gson.fromJson(jsonObject, RoomStatus::class.java)
                    RTMessageType.RequestChannelStatus -> gson.fromJson(jsonObject, RequestChannelStatus::class.java)
                    RTMessageType.ChannelStatus -> gson.fromJson(jsonObject, ChannelStatus::class.java)
                    RTMessageType.AllOffStage -> gson.fromJson(jsonObject, AllOffStage::class.java)
                    // TODO
                    else -> ChannelMessage(text)
                }
            } catch (e: Exception) {
                return ChannelMessage(text)
            }
        }

        fun toText(event: RTMEvent): String {
            return gson.toJson(event)
        }
    }
}

data class RTMUserState(
    val name: String,
    val camera: Boolean,
    val mic: Boolean,
    /** this filed is only accepted from room creator */
    val isSpeak: Boolean,
)

data class AcceptRaiseHandValue(
    val userUUID: String,
    val accept: Boolean,
)

data class SpeakItem(
    val userUUID: String,
    val speak: Boolean,
)

data class DeviceStateValue(
    val userUUID: String,
    val camera: Boolean,
    val mic: Boolean,
)

data class RequestChannelStatusValue(
    val roomUUID: String,
    /** these users should response */
    val userUUIDs: List<String>,
    /** also inform others about current user states */
    val user: RTMUserState,
)

data class ChannelStatusValue(
    /** ban messaging */
    val ban: Boolean,
    /** room status */
    val rStatus: RoomStatus,
    /** class mode type */
    val rMode: ClassModeType,
    /** users with non-default states */
    val uStates: HashMap<String, String>,
)

enum class RTMessageType {
    /** group message */
    ChannelMessage,

    /** a notice message */
    Notice,

    /** A joiner raises hand */
    RaiseHand,

    /** creator accept hand raising from a joiner */
    AcceptRaiseHand,

    /** creator cancel all hand raising */
    CancelHandRaising,

    /** creator ban all rtm */
    BanText,

    /** creator allows a joiner or joiners allows themselves to speak */
    Speak,

    /**
     * joiner updates own camera and mic state
     * creator may turn off joiner's camera and mic but not turn on
     */
    DeviceState,

    /** creator updates class mode */
    ClassMode,

    /** creator updates class status */
    RoomStatus,

    /** joiner request room's status */
    RequestChannelStatus,

    /** send room's status */
    ChannelStatus,

    AllOffStage;
}

enum class RTMUserProp(val flag: String) {
    IsSpeak("S"),
    IsRaiseHand("R"),
    Camera("C"),
    Mic("M");
}