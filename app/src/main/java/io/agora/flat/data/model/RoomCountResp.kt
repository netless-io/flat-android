package io.agora.flat.data.model

import com.google.gson.annotations.SerializedName

data class RoomCount(
    @SerializedName("alreadyJoinedRoomCount")
    val count: Int,
)
