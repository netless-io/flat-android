package io.agora.flat.data.model

import com.google.gson.annotations.SerializedName

enum class FileConvertStep {
    None,
    Converting,
    Done,
    Failed;
}

enum class ResourceType {
    @SerializedName("WhiteboardConvert")
    WhiteboardConvert,

    @SerializedName("WhiteboardProjector")
    WhiteboardProjector,

    @SerializedName("NormalResources")
    NormalResources,

    @SerializedName("Directory")
    Directory,
}