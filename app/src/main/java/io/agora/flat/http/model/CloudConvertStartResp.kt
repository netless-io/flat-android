package io.agora.flat.http.model

import io.agora.flat.data.model.ResourceType

data class CloudConvertStartResp constructor(
    val resourceType: ResourceType,
    val whiteboardConvert: WhiteboardConvert?,
    val whiteboardProjector: WhiteboardProjector?,
)

data class WhiteboardConvert constructor(
    val taskUUID: String,
    val taskToken: String,
)

data class WhiteboardProjector constructor(
    val taskUUID: String,
    val taskToken: String,
)