package io.agora.flat.ui.activity.cloud.preview

import io.agora.flat.data.model.CloudStorageFile
import io.agora.flat.data.model.CoursewareType

data class PreviewState(
    val loading: Boolean,
    val type: CoursewareType = CoursewareType.Unknown,
    val file: CloudStorageFile? = null,
    val baseUrl: String? = null,
) {

}