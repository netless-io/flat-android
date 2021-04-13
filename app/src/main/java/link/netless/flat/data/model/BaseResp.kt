package link.netless.flat.data.model

import com.google.gson.annotations.SerializedName

data class BaseResp<T>(
    @SerializedName("status") val status: Int,
    @SerializedName("code") val code: Int?,
    @SerializedName("data") val data: T
)

