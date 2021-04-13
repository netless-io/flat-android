package link.netless.flat.data.model

data class WeChatCallbackReq constructor(
    val state: String,
    val code: String,
)
