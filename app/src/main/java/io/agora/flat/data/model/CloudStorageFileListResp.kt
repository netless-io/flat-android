package io.agora.flat.data.model

data class CloudStorageFileListResp constructor(
    val totalUsage: Long,
    val files: List<CloudStorageFile>,
)

data class CloudStorageFile constructor(
    val fileUUID: String,
    val fileName: String,
    val fileSize: Int,
    val fileURL: String,
    val convertStep: FileConvertStep,
    val taskUUID: String,
    val taskToken: String,
    val createAt: String,
    // local
    val checked: Boolean = false,
)

enum class FileConvertStep {
    None,
    Converting,
    Done,
    Failed;
}