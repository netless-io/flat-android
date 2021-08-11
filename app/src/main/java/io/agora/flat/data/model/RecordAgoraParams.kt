package io.agora.flat.data.model

import com.google.gson.annotations.SerializedName

/**
 * agora record params describe
 * https://docs.agora.io/cn/cloud-recording/cloud_recording_api_rest
 */
data class AgoraRecordParams constructor(
    val resourceid: String,
    val mode: AgoraRecordMode,
    val sid: String? = null,
)

enum class AgoraRecordMode {
    @SerializedName("individual")
    Individual,

    @SerializedName("mix")
    Mix,

    @SerializedName("web")
    Web,
}