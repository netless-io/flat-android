package io.agora.flat.common.version

data class VersionCheckResult(
    val appUrl: String? = null,
    val appVersion: String? = null,
    val title: String = "",
    val description: String = "",
    val showUpdate: Boolean = false,
    val forceUpdate: Boolean = false,
) {
    companion object {
        val Empty = VersionCheckResult()
    }
}
