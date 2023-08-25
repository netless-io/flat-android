package io.agora.flat.data.model

import com.google.gson.annotations.SerializedName

data class UserInfoRebind(
    val name: String,
    val avatar: String,
    @SerializedName("userUUID") val uuid: String,
    val token: String,
    val hasPhone: Boolean,
    val hasPassword: Boolean,
    val rebind: RebindExt,
) {
    fun toUserInfo(): UserInfo {
        return UserInfo(
            name = name,
            avatar = avatar,
            uuid = uuid,
            hasPhone = hasPhone,
            hasPassword = hasPassword
        )
    }
}

data class RebindExt(
    val agora: Int,
    val apple: Int,
    val github: Int,
    val google: Int,
    val wechat: Int,
    val email: Int
)