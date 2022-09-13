package io.agora.flat.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class CloudFile constructor(
    val fileUUID: String,
    val fileName: String,
    val fileSize: Long,
    val fileURL: String,
    val resourceType: ResourceType?,
    val createAt: Long,
    val meta: CloudFileMeta? = null,
) : Parcelable {
    val convertStep: FileConvertStep
        get() {
            return when (resourceType) {
                ResourceType.WhiteboardConvert -> {
                    whiteboardConvert.convertStep
                }
                ResourceType.WhiteboardProjector -> {
                    whiteboardProjector.convertStep
                }
                else -> {
                    FileConvertStep.Done
                }
            }
        }
    val whiteboardConvert: WhiteboardConvertPayload
        get() {
            return meta!!.whiteboardConvert!!
        }

    val whiteboardProjector: WhiteboardProjectorPayload
        get() {
            return meta!!.whiteboardProjector!!
        }
}

@Parcelize
data class CloudFileMeta constructor(
    val whiteboardConvert: WhiteboardConvertPayload?,
    val whiteboardProjector: WhiteboardProjectorPayload?,
) : Parcelable

@Parcelize
data class WhiteboardConvertPayload constructor(
    val region: String,
    val convertStep: FileConvertStep,
    val taskUUID: String,
    val taskToken: String,
) : Parcelable

@Parcelize
data class WhiteboardProjectorPayload constructor(
    val region: String,
    val convertStep: FileConvertStep,
    val taskUUID: String,
    val taskToken: String,
) : Parcelable