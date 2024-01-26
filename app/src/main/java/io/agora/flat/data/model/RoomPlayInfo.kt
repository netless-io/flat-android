package io.agora.flat.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * 加入房间后获取的信息
 */
@Parcelize
data class RoomPlayInfo(
    // 房间类型
    val roomType: RoomType,
    // 当前房间的 UUID
    val roomUUID: String,
    // 房间创建者的 UUID
    val ownerUUID: String,
    // 白板的 room token
    val whiteboardRoomToken: String,
    // 白板的 room uuid
    val whiteboardRoomUUID: String,
    // rtc 的 uid
    val rtcUID: Int,
    // rtc 分享屏幕
    val rtcShareScreen: RtcShareScreen,
    // rtc token
    val rtcToken: String,
    // rtm token
    val rtmToken: String,

    val region: String,

    val billing: Billing? = null,
) : Parcelable

@Parcelize
data class RtcShareScreen(val uid: Int, val token: String) : Parcelable

@Parcelize
data class Billing(
    val expireAt: String,
    val maxUser: Int,
    // 0 = normal, 1 = pro
    val vipLevel: Int
) : Parcelable