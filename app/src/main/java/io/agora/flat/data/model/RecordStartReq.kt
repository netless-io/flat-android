package io.agora.flat.data.model

data class RecordStartReq constructor(
    val roomUUID: String,
    val agoraParams: AgoraRecordParams,
    val agoraData: AgoraRecordStartedData,
)

data class AgoraRecordStartedData constructor(
    val clientRequest: ClientRequest,
)

data class ClientRequest constructor(
    val recordingConfig: RecordingConfig,
)

data class RecordingConfig constructor(
    val channelType: Long? = null,
    val streamTypes: Int = 2,
    // val decryptionMode: Int?,
    // val secret: String?,
    // val audioProfile: Long?,
    // val videoStreamType: Int? = 1,
    // val streamMode: String? = "standard",
    // val maxIdleTime: Long? = 30,
    val transcodingConfig: TranscodingConfig?,
    // val subscribeVideoUids: List<String>?,
    // val unSubscribeVideoUids: List<String>?,
    // val subscribeAudioUids: List<String>?,
    // val unSubscribeAudioUids: List<String>?,
    val subscribeUidGroup: Int?,
)

data class TranscodingConfig constructor(
    // px
    val width: Int,
    // px
    val height: Int,
    val fps: Int,
    val bitrate: Int,
    val maxResolutionUid: String? = null,
    val mixedVideoLayout: Int? = null,
    val backgroundColor: String = "#FFFFFF",
    val defaultUserBackgroundImage: String? = null,
    val layoutConfig: List<LayoutConfig>? = null,
    val backgroundConfig: List<BackgroundConfig>? = null,
)

data class LayoutConfig constructor(
    val uid: String,
    // 屏幕里该画面左上角的横坐标的相对值，范围是 [0.0,1.0]
    val x_axis: Float,
    // 屏幕里该画面左上角的纵坐标的相对值，范围是 [0.0,1.0]
    val y_axis: Float,
    // 取值范围是 [0.0,1.0]
    val width: Float,
    // 取值范围是 [0.0,1.0]
    val height: Float,
    val alpha: Float = 1.0f,
    // 0：（默认）裁剪模式
    // 1：缩放模式
    val render_mode: Int = 0,
)

data class BackgroundConfig constructor(
    val uid: String,
    val image_url: String,
    val render_mode: Int = 0,
)