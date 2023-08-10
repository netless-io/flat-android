package io.agora.flat.data.model

import com.google.gson.annotations.SerializedName

data class UserInfoWithToken(
    val name: String,
    val avatar: String,
    @SerializedName("userUUID") val uuid: String,
    val token: String,
    val hasPhone: Boolean,
    val hasPassword: Boolean,
)