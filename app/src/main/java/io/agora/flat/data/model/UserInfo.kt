package io.agora.flat.data.model

import com.google.gson.annotations.SerializedName

data class UserInfo(
    val name: String,
    val avatar: String,
    @SerializedName("userUUID") val uuid: String,
    val hasPhone: Boolean = false,
    val hasPassword: Boolean = false,
)