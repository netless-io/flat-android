package io.agora.flat.data.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

data class CloudStorageFileListResp constructor(
    val totalUsage: Long,
    val files: List<CloudStorageFile>,
)

@Parcelize
data class CloudStorageFile constructor(
    val fileUUID: String,
    @SerializedName("fileName")
    val fileName: String,
    val fileSize: Long,
    val fileURL: String,
    val convertStep: FileConvertStep,
    val taskUUID: String,
    val taskToken: String,
    val region: String = "cn-hz",
    val createAt: Long,
    val resourceType: ResourceType? = null,
) : Parcelable

enum class FileConvertStep {
    None,
    Converting,
    Done,
    Failed;
}

enum class ResourceType {
    WhiteboardConvert,
    WhiteboardProjector,
    LocalCourseware,
    OnlineCourseware,
    NormalResources,
}