package io.agora.flat.ui.activity.cloud.preview

import io.agora.flat.data.model.CloudStorageFile
import io.agora.flat.data.model.CoursewareType
import io.agora.flat.data.model.ResourceType
import java.net.URLEncoder

data class PreviewState(
    val loading: Boolean,
    val type: CoursewareType = CoursewareType.Unknown,
    val file: CloudStorageFile? = null,
    val baseUrl: String? = null,
) {
    // provider by flat web
    val previewUrl: String
        get() {
            if (file == null) {
                return ""
            }
            val encodeURL = URLEncoder.encode(file.fileURL, "utf-8")
            return "$baseUrl/preview/${encodeURL}/${file.taskToken}/${file.taskUUID}/${file.region}/${
                if (file.resourceType == ResourceType.WhiteboardProjector) "projector/" else ""
            }"
        }
}