package io.agora.flat.ui.activity.cloud.preview

import io.agora.flat.data.model.CloudFile
import io.agora.flat.data.model.CoursewareType
import io.agora.flat.util.JsonUtils
import java.net.URLEncoder

data class PreviewState(
    val loading: Boolean,
    val type: CoursewareType = CoursewareType.Unknown,
    val file: CloudFile? = null,
    val baseUrl: String? = null,
) {
    // provider by flat web
    val previewUrl: String
        get() {
            if (file == null) return ""
            return "$baseUrl/preview/${URLEncoder.encode(JsonUtils.toJson(file), "utf-8")}/"
        }
}