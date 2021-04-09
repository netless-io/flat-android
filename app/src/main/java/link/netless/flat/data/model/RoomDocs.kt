package link.netless.flat.data.model

data class RoomDocs constructor(
    val docType: DocsType,
    val docUUID: String,
    val isPreload: Boolean
) {
}